package com.bluegosling.reflect;

import static java.util.Objects.requireNonNull;

import com.bluegosling.concurrent.Awaitable;
import com.bluegosling.concurrent.ThreadFactories;
import com.bluegosling.concurrent.executors.SameThreadExecutor;
import com.bluegosling.concurrent.fluent.FluentExecutorService;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.SettableFluentFuture;
import com.bluegosling.util.Stopwatch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Scans directories and JARs for classes, indexing them by package. This can be used to get
 * information about all packages available in given resources. It also includes convenience
 * methods for including JVM class paths and boot class paths in the search and for including
 * {@link URLClassLoader}s in the search (though only local file URLs can be scanned).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class PackageScanner {
   /**
    * The default number of threads used to scan: 4. This value is fixed: it need not scale with the
    * number of available CPU resources since the process is I/O bound. While the scan is faster
    * with multiple threads working in parallel, this goes only up to a point, after which disk
    * seeks from competing threads could deteriorate performance.
    * 
    * <p>Even with a solid-state disk, where disk seeks aren't a factor, performance improvements
    * going from 4 to 16 threads were non-existent.
    */
   private static final int DEFAULT_SCAN_PARALLELISM = 4;
   
   /**
    * The separator used to split the class path.
    */
   private static final String PATH_SEP;
   static {
      String sep = System.getProperty("path.separator");
      PATH_SEP = sep == null ? File.pathSeparator : sep;
   }
   
   /**
    * Combines two path strings into a single path. This is different than simple concatenation in
    * that it adds or de-duplicates path separators if necessary (like if neither or both given
    * strings already have one).
    *
    * @param head the prefix of the new path
    * @param tail the suffix of the new path
    * @return the new path
    */
   static String joinPaths(String head, String tail) {
      if (head.isEmpty()) {
         return tail;
      } else if (tail.isEmpty()) {
         return head;
      }
      StringBuilder sb = new StringBuilder(head.length() + tail.length() + 1);
      sb.append(head);
      if (!head.endsWith("/")) {
         sb.append('/');
      }
      if (tail.startsWith("/")) {
         tail = tail.substring(1);
      }
      sb.append(tail);
      return sb.toString();
   }

   private int parallelism = DEFAULT_SCAN_PARALLELISM;
   private final Map<String, ClassLoader> pathsToScan = new LinkedHashMap<>();
   private final Set<URLClassLoader> classLoaders = new HashSet<>();
   private final CustomClassLoader customLoader = new CustomClassLoader();
   private ScanResult result;
   
   /**
    * Creates a new package scanner. A new scanner has no paths configured. Callers must
    * subsequently include directories, JARs, or other paths in the scan before starting.
    */
   public PackageScanner() {
   }
   
   private void checkNotStarted() {
      if (result != null) {
         throw new IllegalStateException("scan already started");
      }
   }
   
   /**
    * Sets the parallelism of the scan. This is the maximum number of threads that will be used to
    * perform the scan in parallel.
    *
    * @param parallel the maximum number of threads used to perform the scan
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    */
   public synchronized PackageScanner withParallelism(int parallel) {
      checkNotStarted();
      this.parallelism = parallel;
      return this;
   }
   
   /**
    * Includes JVM boot paths in the scan. Unfortunately, the bootstrap class path and programmatic
    * access to it are not part of the JRE library specs. So this method may not be portable across
    * JVM vendors.
    * 
    * <p>This method tried to find boot classpaths and extension paths for the Oracle JVM by looking
    * at a system property, {@code sun.boot.class.path}. If that cannot be found, it looks for the
    * Java home directory by examining another system property, {@code java.home}, and then looking
    * for {@code lib/rt.jar} therein. It also includes extension paths by including any JAR files
    * located in a {@code lib/ext} sub-directory of the Java home.
    * 
    * <p>This method also includes the default locations for Android's boot classpaths:
    * {@code core.jar}, {@code ext.jar}, {@code framework.jar}, {@code android.policy.jar}, and
    * {@code services.jar}; all located in {@code /system/framework}.
    * 
    * <p>When attempting to load classes for files found in these paths,
    * {@link Class#forName(String)} is used.
    *
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    */
   public synchronized PackageScanner includeBootClasspaths() {
      checkNotStarted();
      includeSunBootPaths();
      includeAndroidBootPaths();
      return this;
   }
   
   /**
    * Includes this JVM's class path in the scan. The class path is accessed by system property,
    * {@code java.class.path}.
    *
    * <p>When attempting to load classes for files found in these paths,
    * {@link Class#forName(String)} is used.
    *
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    */
   public synchronized PackageScanner includeClasspath() {
      checkNotStarted();
      includePathsFromProperty("java.class.path");
      return this;
   }

   /**
    * Includes the given path in the scan.
    *
    * <p>When attempting to load classes for files found in these paths,
    * {@link Class#forName(String)} is tried first. If that fails, a custom class loader is used.
    *
    * @param path a path
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    */
   public synchronized PackageScanner includePath(String path) {
      checkNotStarted();
      pathsToScan.put(path, customLoader);
      return this;
   }
   
   /**
    * Includes the given class loader in the scan. This method only uses local file URLs. If the
    * given class loader references remote URLs, those URLs are not included in the scan and are
    * effectively ignored.
    * 
    * <p>When attempting to load classes for files found in these paths, the given class loader
    * is used.
    *
    * @param cl the class loader
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    */
   public synchronized PackageScanner includeURLClassLoader(URLClassLoader cl) {
      checkNotStarted();
      if (classLoaders.add(cl)) {
         URL[] urls = cl.getURLs();
         for (URL url : urls) {
            pathsToScan.put(url.getPath(), cl);
         }
      }
      return this;
   }

   /**
    * Includes the given class loaders in the scan.  
    *
    * @param cls the class loaders
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    * @see #includeURLClassLoader(URLClassLoader)
    */
   public synchronized PackageScanner includeURLClassLoaders(URLClassLoader... cls) {
      checkNotStarted();
      for (URLClassLoader cl : cls) {
         includeURLClassLoader(cl);
      }
      return this;
   }

   /**
    * Includes the given class loaders in the scan.  
    *
    * @param cls the class loaders
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    * @see #includeURLClassLoader(URLClassLoader)
    */
   public synchronized PackageScanner includeURLClassLoaders(Iterable<URLClassLoader> cls) {
      checkNotStarted();
      for (URLClassLoader cl : cls) {
         includeURLClassLoader(cl);
      }
      return this;
   }

   /**
    * Includes a few default class loaders in the scan.
    * <ul>
    * <li>{@code Thread.currentThread().getContextClassLoader()}</li>
    * <li>{@code PackageScanner.class.getClassLoader()}</li>
    * <li>{@code ClassLoader.getSystemClassLoader()}</li>
    * </ul>
    * If one or more of these refer to the same class loader, it is only included once in the scan.
    * If any of these are not instances of {@link URLClassLoader} then they are ignored. 
    *
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    * @see #includeURLClassLoader(URLClassLoader)
    */
   public synchronized PackageScanner includeDefaultClassLoaders() {
      checkNotStarted();
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl instanceof URLClassLoader) {
         includeURLClassLoader((URLClassLoader) cl);
      }
      cl = PackageScanner.class.getClassLoader();
      if (cl instanceof URLClassLoader) {
         includeURLClassLoader((URLClassLoader) cl);
      }
      cl = ClassLoader.getSystemClassLoader();
      if (cl instanceof URLClassLoader) {
         includeURLClassLoader((URLClassLoader) cl);
      }
      return this;
   }
   
   /**
    * Includes any JAR files found in the given directory.
    *
    * <p>When attempting to load classes for files found in these paths,
    * {@link Class#forName(String)} is tried first. If that fails, a custom class loader is used.
    * 
    * @param directory the path to a directory
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    */
   public synchronized PackageScanner includeJarsFromDirectory(String directory) {
      checkNotStarted();
      includeJarsFromDirectory(directory, customLoader);
      return this;
   }
   
   private void includeJarsFromDirectory(String directory, ClassLoader cl) {
      File dir = new File(directory);
      if (!dir.exists() || !dir.isDirectory()) {
         return;
      }
      for (String childName : dir.list((f, n) -> f.isFile() && n.toLowerCase().endsWith(".jar"))) {
         pathsToScan.put(joinPaths(dir.getPath(), childName), cl);
      }
   }
   
   /**
    * Includes all several default locations that correspond to paths from which the JVM can load
    * classes. This is equivalent to the following:
    * <pre>
    * scanner.includeBootClasspaths()
    *     .includeClasspath()
    *     .includeDefaultClassLoaders();
    * </pre>
    *
    * @return {@code this}, for method chaining
    * @throws IllegalStateException if the scan has already been started
    */
   public synchronized PackageScanner includeAllDefaults() {
      return includeBootClasspaths().includeClasspath().includeDefaultClassLoaders();
   }
   
   private void includeSunBootPaths() {
      String home = System.getProperty("java.home");
      if (!includePathsFromProperty("sun.boot.class.path") && home != null) {
         // guess...
         pathsToScan.put(joinPaths(home, "lib/rt.jar"), null);
         pathsToScan.put(joinPaths(home, "jre/lib/rt.jar"), null);
      }
      if (home == null) {
         return;
      }
      // extensions
      includeJarsFromDirectory(joinPaths(home, "lib/ext"), null);
      includeJarsFromDirectory(joinPaths(home, "jre/lib/ext"), null);
   }
   
   private void includeAndroidBootPaths() {
      pathsToScan.put("/system/framework/core.jar", null);
      pathsToScan.put("/system/framework/ext.jar", null);
      pathsToScan.put("/system/framework/framework.jar", null);
      pathsToScan.put("/system/framework/android.policy.jar", null);
      pathsToScan.put("/system/framework/services.jar", null);
   }

   private boolean includePathsFromProperty(String property) {
      String classpath = System.getProperty(property);
      if (classpath == null || classpath.isEmpty()) {
         return false;
      }
      for (String component : classpath.split(Pattern.quote(PATH_SEP))) {
         pathsToScan.put(component, null);
      }
      return true;
   }
   
   /**
    * Starts the scan. The scan is initiated asynchronously.
    *
    * @return the result of the scan
    */
   public synchronized ScanResult start() {
      if (result == null) {
         result = new ScanResult(parallelism, pathsToScan, customLoader);
      }
      return result;
   }
   
   // TODO: doc
   public static class ClassInfo {
      private final ClassLoader classLoader;
      private final String packageName;
      private final String name;
      private volatile Object clazzOrException;
      
      ClassInfo(ClassLoader classLoader, String packageName, String className) {
         assert packageName.isEmpty() || className.startsWith(packageName + ".");
         this.classLoader = classLoader;
         this.packageName = packageName;
         this.name = className;
      }
      
      public String getPackageName() {
         return packageName;
      }
      
      public String getName() {
         return name;
      }
      
      public String getUnqualifiedName() {
         return packageName.isEmpty() ? name : name.substring(packageName.length() + 1);
      }
      
      /**
       * 
       * TODO: document me!
       *
       * @return
       * @throws LinkageError if the linkage fails
       * @throws ClassNotFoundException if the class cannot be located by
       *       the specified class loader
       */
      public Class<?> asClass() throws ClassNotFoundException {
         Object c = clazzOrException;
         if (c == null) {
            try {
               c = clazzOrException = classLoader == null
                     ? Class.forName(name, false, PackageScanner.class.getClassLoader())
                     : Class.forName(name, false, classLoader);
            } catch (Throwable th) {
               if (th instanceof Error || th instanceof RuntimeException
                     || th instanceof ClassNotFoundException) {
                  c = clazzOrException = th;
               } else {
                  c = clazzOrException = new RuntimeException(th);
               }
            }
         }
         if (c instanceof Class) {
            return (Class<?>) c;
         } else if (c instanceof Error) {
            throw (Error) c;
         } else if (c instanceof ClassNotFoundException) {
            throw (ClassNotFoundException) c;
         } else {
            throw (RuntimeException) c;
         }
      }
      
      @Override public boolean equals(Object o) {
         if (o instanceof ClassInfo) {
            ClassInfo other = (ClassInfo) o;
            return Objects.equals(classLoader, other.classLoader)
                  && name.equals(other.name);
         }
         return false;
      }
      
      @Override public int hashCode() {
         return Objects.hash(classLoader, name);
      }
      
      @Override public String toString() {
         return name;
      }
   }
   
   /**
    * The results of scanning a single path.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class PathResult {
      private final String pathScanned;
      private final ClassLoader classLoader;
      private final Stopwatch elapsed;
      private final Map<String, Set<ClassInfo>> packageIndex;
      private final Map<String, Throwable> exceptions;
      private volatile boolean done;
      
      PathResult(String pathScanned, ClassLoader classLoader, Stopwatch elapsed) {
         this.pathScanned = pathScanned;
         this.classLoader = classLoader;
         this.elapsed = elapsed;
         this.packageIndex = new HashMap<>();
         this.exceptions = new HashMap<>();
      }
      
      private void checkNotDone() {
         if (done) {
            throw new IllegalStateException("scan is already complete");
         }
      }

      private void checkDone() {
         if (!done) {
            throw new IllegalStateException("scan is still in progress");
         }
      }
      
      void done() {
         done = true;
      }
      
      /**
       * Adds a search result for this path. The given string is a relative path that encountered
       * when searching the directory (recursively) or ZIP/JAR archive for this path. If it is not a
       * class file, it will be ignored.
       * 
       * @param pathName the name of the path encountered
       */
      void addResult(String pathName) {
         checkNotDone();
         if (!pathName.toLowerCase().endsWith(".class")) {
            return;
         }
         int start = pathName.charAt(0) == '/' || pathName.charAt(0) == '\\' ? 1 : 0;
         String className = pathName.substring(start, pathName.length() - ".class".length())
               .replace('\\', '/');
         boolean first = true;
         for (int i = 0; i < className.length(); i++) {
            char ch = className.charAt(i);
            if (ch == '/') {
               first = true;
               continue;
            }
            // if the package or class name is invalid, discard
            if (first) {
               first = false;
               if (!Character.isJavaIdentifierStart(ch)) {
                  return;
               }
            } else if (!Character.isJavaIdentifierPart(ch)) {
               return;
            }
         }
         className = className.replace('/', '.');
         
         // we don't synchronize because this is only called from single thread, before this
         // result is published to the larger scan result
         String packageName = getPackageName(className);
         packageIndex.computeIfAbsent(packageName,
               k -> new LinkedHashSet<>()).add(new ClassInfo(classLoader, packageName, className));
      }

      /**
       * Adds an exception for this path. The given string is the encountered path (relative) that
       * induced the exception. It may be an I/O exception, but it could also be a link error if
       * the exception occurred while trying to load and resolve a class.
       *
       * @param pathName the name of the path that caused the exception
       * @param e the exception
       */
      void addException(String pathName, Throwable e) {
         checkNotDone();
         exceptions.put(pathName.replace('/', '.').replace('\\', '.'), e);
      }
      
      /**
       * Returns the path that was scanned. This is either a directory or a JAR (or ZIP) file.
       *
       * @return the path that was scanned
       */
      public String getPathScanned() {
         return pathScanned;
      }
      
      /**
       * 
       * TODO: document me!
       *
       * @param packageName
       * @return
       */
      public boolean includesPackage(String packageName) {
         checkDone();
         return packageIndex.containsKey(packageName);
      }
      
      /**
       * Returns the set of package names found in this path.
       *
       * @return the set of package names found
       */
      public Set<String> getPackagesFound() {
         checkDone();
         return packageIndex.keySet();
      }
      
      /**
       * Returns the set of classes found in this path that are in the given package.
       *
       * @param packageName the package name
       * @return the set of classes found in this path that are in the given package
       */
      public Set<ClassInfo> getClassesFound(String packageName) {
         checkDone();
         Set<ClassInfo> classes = packageIndex.get(packageName);
         return classes == null ? Collections.emptySet() : Collections.unmodifiableSet(classes);
      }
      
      /**
       * Returns the amount of time elapsed during the scan of this path, in the given unit.
       *
       * @param unit a time unit
       * @return the amount of time elapsed during the scan of this path
       */
      public long getScanDuration(TimeUnit unit) {
         return elapsed.read(unit);
      }

      /**
       * Returns the exception associated with given searched path. If the given path was not
       * scanned as part of this path or if it was scanned and resolved successfully, this will
       * return {@code null}.
       *
       * @param source a path that is the source of an exception
       * @return the exception thrown while trying to scan or resolve the given path or
       *       {@code null} if no exception was thrown or the given name was never scanned
       * @see #getScanExceptionSources()
       */
      public synchronized Throwable getScanException(String source) {
         checkDone();
         return exceptions.get(requireNonNull(source));
      }
      
      /**
       * Returns all exceptions encountered while trying to scan and resolve classes. This may
       * include any {@linkplain #getScanException I/O exception} encountered while reading from
       * this path. It will also include any exceptions thrown while trying to scan and resolve
       * individual classes in this path. 
       *
       * @return all exceptions encountered while trying to scan and resolve classes
       * @see #getScanExceptionSources()
       * @see #getScanException(String)
       */
      public Collection<Throwable> getAllScanExceptions() {
         checkDone();
         return Collections.unmodifiableCollection(exceptions.values());
      }

      /**
       * Returns the set of paths that were found in this path but could not be scanned or resolved.
       * The cause of failure can be queried using {@link #getScanException(String)}.
       *
       * @return the set of paths that were found in this path but could not be scanned or resolved
       */
      public Set<String> getScanExceptionSources() {
         checkDone();
         return Collections.unmodifiableSet(exceptions.keySet());
      }
   }
   
   static String getPackageName(String className) {
      int i = className.lastIndexOf('.');
      return i <= 0 ? "" : className.substring(0, i);
   }
   
   /**
    * The results of a scan. This object implements {@link Awaitable} because the results can be
    * inspected while the scan is still concurrently running. Awaiting this result to become done
    * will block until all path scanning operations have completed.
    * 
    * <p>Several operations are not possible until the scan operations complete. So several methods
    * implicitly block for it to complete when invoked.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class ScanResult implements Awaitable {
      private final Stopwatch elapsed = new Stopwatch();
      private final Map<String, FluentFuture<PathResult>> results = new HashMap<>();
      private final FluentFuture<Map<String, Set<ClassInfo>>> operation;
      
      ScanResult(int parallelism, Map<String, ClassLoader> pathsToScan,
            CustomClassLoader customLoader) {
         elapsed.start();
         FluentExecutorService ex =
               FluentExecutorService.makeFluent(Executors.newFixedThreadPool(parallelism,
                     ThreadFactories.newGroupingThreadFactory("package scanner")));
         Set<URL> urls = new HashSet<>();
         for (Entry<String, ClassLoader> entry : pathsToScan.entrySet()) {
            String path = entry.getKey();
            ClassLoader cl = entry.getValue();
            File file = new File(path);
            if (!file.exists()) {
               continue;
            }
            try {
               path = file.getCanonicalPath();
            } catch (IOException ignored) {
            }
            if (results.containsKey(path)) {
               continue;
            }
            FluentFuture<PathResult> future;
            Stopwatch fileElapsed = new Stopwatch();
            if (file.isDirectory()) {
               future = ex.submit(() -> scanDirectory(file, cl, fileElapsed));
            } else {
               future = ex.submit(() -> scanZip(file, cl, fileElapsed));
            }
            try {
               urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
               throw new AssertionError(e); // should we ignore instead?
            }
            future = future.recover(th -> {
               PathResult r = new PathResult(file.getAbsolutePath(), cl, fileElapsed.stop());
               r.addException("", th);
               r.done();
               return r;
            });
            results.put(path, future);
         }
         // no more tasks will be submitted; let executor terminate when scan completes
         ex.shutdown();
         
         customLoader.setURLs(urls);
         
         operation = FluentFuture.join(results.values()).chainTo(r -> {
            Map<String, Set<ClassInfo>> map = new HashMap<>();
            for (PathResult result : r) {
               for (String packageName : result.getPackagesFound()) {
                  map.computeIfAbsent(packageName, k -> new HashSet<>())
                        .addAll(result.getClassesFound(packageName));
               }
            }
            elapsed.stop();
            return map;
         }, SameThreadExecutor.get());
      }

      /**
       * Returns the amount of time elapsed during this scan, in the given unit. This is the total
       * wall-clock time for all paths to be scanned.
       *
       * @param unit a time unit
       * @return the amount of time elapsed during the scan of this path
       */
      public long getScanDuration(TimeUnit unit) {
         return elapsed.read(unit);
      }

      /**
       * Returns the set of paths scanned.
       *
       * @return the set of paths scanned
       */
      public Set<String> getPathsScanned() {
         return Collections.unmodifiableSet(results.keySet());
      }

      /**
       * Returns the result of scanning the given path. If the given path is still being scanned,
       * this blocks for it to complete.
       *
       * @param path a path
       * @return the result of scanning the given path, or {@code null} if the given path was not
       *       part of this scan
       */
      public PathResult getPathResult(String path) {
         FluentFuture<PathResult> futureResult = results.get(path);
         if (futureResult == null) {
            return null;
         }
         futureResult.awaitUninterruptibly();
         return futureResult.getResult();
      }

      /**
       * Determines whether the given package was encountered in this scan. If paths are still being
       * scanned, this may block. As soon as the package is seen during one of the path scans, the
       * method will return true. But the entire scan must complete before it will return false.
       *
       * @param packageName the name of a package 
       * @return true if the given package name was encountered in this scan
       */
      public boolean includesPackage(String packageName) {
         for (FluentFuture<PathResult> future : results.values()) {
            future.awaitUninterruptibly();
            if (future.getResult().includesPackage(packageName)) {
               return true;
            }
         }
         return false;
      }

      /**
       * 
       * TODO: document me!
       *
       * @param packageName
       * @return
       */
      public Package getPackage(String packageName) {
         for (FluentFuture<PathResult> future : results.values()) {
            future.awaitUninterruptibly();
            for (ClassInfo classInfo : future.getResult().getClassesFound(packageName)) {
               try {
                  return classInfo.asClass().getPackage();
               } catch (Throwable th) {
                  // intentional fall-through to next loop iteration
               }
            }
         }
         return null;
      }

      public Set<String> getPackagesFound() {
         awaitUninterruptibly();
         return Collections.unmodifiableSet(operation.getResult().keySet());
      }
      
      public Set<ClassInfo> getClassesFound(String packageName) {
         awaitUninterruptibly();
         Set<ClassInfo> classes = operation.getResult().get(packageName);
         return classes == null ? Collections.emptySet() : Collections.unmodifiableSet(classes);
      }

      @Override
      public void await() throws InterruptedException {
         operation.await();
      }
      
      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         return operation.await(limit, unit);
      }
      
      @Override
      public boolean isDone() {
         return operation.isDone();
      }
      
      private PathResult scanDirectory(File dir, ClassLoader cl, Stopwatch dirElapsed) {
         PathResult pathResult = new PathResult(dir.getAbsolutePath(), cl, dirElapsed.start());
         try {
            scanDirectory("", pathResult, dir, cl);
         } finally {
            pathResult.done();
            dirElapsed.stop();
         }
         return pathResult;
      }
      
      private void scanDirectory(String prefix, PathResult pathResult, File dir,
            ClassLoader cl) {
         try (DirectoryStream<Path> listing = Files.newDirectoryStream(dir.toPath())) {
            for (Path path : listing) {
               File child = path.toFile();
               String childName = joinPaths(prefix, child.getName());
               if (child.isDirectory()) {
                  scanDirectory(childName, pathResult, child, cl);
               } else {
                  pathResult.addResult(childName);
               }
            }
         } catch (Throwable t) {
            pathResult.addException(prefix, t);
         }
      }

      private PathResult scanZip(File archive, ClassLoader cl, Stopwatch zipElapsed) {
         PathResult pathResult = new PathResult(archive.getAbsolutePath(), cl, zipElapsed.start());
         try (ZipInputStream in = new ZipInputStream(new FileInputStream(archive))) {
            while (true) {
               ZipEntry entry = in.getNextEntry();
               if (entry == null) {
                  break;
               }
               pathResult.addResult(entry.getName());
            }
         } catch (Throwable t) {
            pathResult.addException("", t);
         } finally {
            pathResult.done();
            zipElapsed.stop();
         }
         return pathResult;
      }
   }
   
   private static class CustomClassLoader extends ClassLoader {
      private final SettableFluentFuture<URLClassLoader> futureLoader = new SettableFluentFuture<>();
      
      CustomClassLoader() {
         super(PackageScanner.class.getClassLoader());
      }
      
      public void setURLs(Set<URL> urls) {
         futureLoader.setValue(new URLClassLoader(urls.toArray(new URL[urls.size()])));
      }
      
      @Override public Class<?> loadClass(String name)
            throws ClassNotFoundException {
         // If the loader is set, use it
         if (futureLoader.isDone()) {
            return futureLoader.getResult().loadClass(name);
         }
         
         // Otherwise, first try going up the class loader hierarchy. If that fails, wait for the
         // loader to be set and then use it.
         try {
            return super.loadClass(name);
         } catch (ClassNotFoundException e) {
         }
         futureLoader.awaitUninterruptibly();
         return futureLoader.getResult().loadClass(name);
      }
   }
   
   
   
   public static void main(String args[]) throws Exception {
      ScanResult result = new PackageScanner()
            .includeAllDefaults()
            .includePath("/Users/jh/Development/personal/apt-reflect/bin")
            .start();
      result.await();
      System.out.println("Scan took " + result.getScanDuration(TimeUnit.MILLISECONDS) + " millis");
      for (String path : result.getPathsScanned()) {
         System.out.println("  Path " + path + " took "
               + result.getPathResult(path).getScanDuration(TimeUnit.MILLISECONDS) + " millis");
      }
      System.out.println("\n\n");
      List<String> packages = new ArrayList<>(result.getPackagesFound());
      packages.sort(Comparator.naturalOrder());
      int numClasses = 0;
      int failedToLoad = 0;
      for (String pkg : packages) {
         System.out.println(pkg + ":");
         List<ClassInfo> classes = new ArrayList<>(result.getClassesFound(pkg));
         classes.sort(Comparator.comparing(ClassInfo::getName));
         for (ClassInfo cls : classes) {
            try {
               cls.asClass();
               System.out.println("    " + cls.getName());
            } catch (ClassNotFoundException e) {
               failedToLoad++;
               System.out.println("  ! " + cls.getName());
            }
            numClasses++;
         }
      }
      System.out.println("\n\n");
      for (String path : result.getPathsScanned()) {
         PathResult r = result.getPathResult(path);
         for (String source : r.getScanExceptionSources()) {
            System.out.print(path + ": " + source + ": ");
            r.getScanException(source).printStackTrace(System.out);
         }
      }
      
      System.out.println(numClasses + " classes scanned, "
            + failedToLoad + " of which could not be loaded");
   }
}
