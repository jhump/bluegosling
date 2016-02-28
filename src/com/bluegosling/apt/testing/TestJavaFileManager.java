package com.bluegosling.apt.testing;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

/**
 * An in-memory file manager. This can be used, for example, to run compilation and annotation
 * processor tasks from a unit test.
 * 
 * <p>Client code will instantiate a {@link TestJavaFileManager}, optionally providing a parent
 * {@link ClassLoader} (for use when {@linkplain #getClassLoader creating class loaders}). Then the
 * code defines all necessary input files using {@link #createFileObject(JavaFileManager.Location,
 * String, String, byte[]) createFileObject} or {@link #createJavaFileObject(
 * JavaFileManager.Location, String, JavaFileObject.Kind, byte[]) createJavaFileObject} (each of
 * these methods is overloaded to easily define either text file contents or binary file contents).
 * Finally, the new file manager can be used for new {@linkplain JavaCompiler#getTask compilation
 * tasks}.
 * 
 * <p>{@link FileObject}s returned by this class are all in-memory. Changes made to these file
 * contents by {@link OutputStream}s and {@link Writer}s are only seen after they've been flushed.
 * Closing streams automatically flushes them. All open streams can also be flushed using the file
 * manager's {@link #flush()} method.
 * 
 * <p>This file manager cannot be used after it has been {@linkplain #close() closed}. Methods that
 * impact the in-memory file system will throw {@link IOException}s.
 * 
 * <p>This file manager does not have any supported options.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class TestJavaFileManager implements JavaFileManager {

   /**
    * The default character set when defining file contents as text: UTF-8.
    */
   static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

   /**
    * A regular expression pattern for finding a sequence of one or more forward slashes.
    */
   private static final Pattern MULTIPLE_SLASHES = Pattern.compile("/+");
   
   /**
    * A regular expression pattern for matching the end of a path. This matches the last
    * path component, after all forward slashes and up to the end of the string.
    */
   private static final Pattern END_OF_PATH = Pattern.compile("[^/]+$");
   
   /**
    * All of the files in this in-memory file system. The keys are file paths.
    */
   private final Map<String, TestJavaFileObject> files = new HashMap<String, TestJavaFileObject>();
   
   /**
    * The set of files that are open for writing. These objects are the various
    * {@link OutputStream}s that are open on files in the in-memory file system. This allows
    * the file manager to flush all open files when {@link #flush()} is called.
    */
   private final Set<TestJavaFileObject> openedForWriting = new HashSet<TestJavaFileObject>();
   
   private final JavaFileManager platformFileManager;
   private final ClassLoader parentClassLoader;
   private boolean closed;
   
   /**
    * Constructs a new file manager. The invoking thread's context class loader will be used as the
    * parent class loader for any {@linkplain #getClassLoader class loaders created by the file
    * manager}.
    * 
    * @param platformFileManager the platform file manager
    * @see JavaCompiler#getStandardFileManager
    */
   public TestJavaFileManager(JavaFileManager platformFileManager) {
      this(platformFileManager, getDefaultClassLoader());
   }
   
   private static ClassLoader getDefaultClassLoader() {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      return loader == null ? ClassLoader.getSystemClassLoader() : loader;
   }
   
   /**
    * Constructs a new file manager. The specified class loader will be used as the parent class loader
    * for any {@linkplain #getClassLoader class loaders created by the file manager}.
    * 
    * @param platformFileManager the platform file manager
    *    (see {@link JavaCompiler#getStandardFileManager})
    * @param parentClassLoader the parent class loader
    */
   public TestJavaFileManager(JavaFileManager platformFileManager, ClassLoader parentClassLoader) {
      this.platformFileManager = platformFileManager;
      this.parentClassLoader = parentClassLoader;
   }
   
   private void checkState() throws IOException {
      if (closed) {
         throw new IOException("File manager has been closed");
      }
   }

   /**
    * Unlinks a path from the set of in-memory files. This does not mark the associated
    * {@link FileObject} as deleted, however.
    * 
    * @param filePath the path to unlink
    * @return true if deletion was successful; false otherwise (like if the named file
    *    does not exist)
    * 
    * @see TestJavaFileObject#delete()
    */
   synchronized boolean delete(String filePath) {
      return files.remove(filePath) != null;
   }
   
   synchronized void openedForWriting(TestJavaFileObject file) throws IOException {
      checkState();
      openedForWriting.add(file);
   }

   synchronized void closedForWriting(TestJavaFileObject file) {
      openedForWriting.remove(file);
   }

   @Override
   public int isSupportedOption(String option) {
      return -1;
   }
   
   /**
    * Builds the canonical folder path for the specified package in the specified location.
    * 
    * @param location the location of the package
    * @param packageName the name of the package
    * @return an absolute and canonical path for the specified package
    */
   private static String canonicalName(Location location, String packageName) {
      return canonicalName(location, packageName, "");
   }
   
   /**
    * Builds the canonical file path for the specified Java file.
    * 
    * @param location the location of the file
    * @param className the fully-qualified class name
    * @param kind the kind of file
    * @return an absolute and canonical path for the specified file
    */
   private static String canonicalName(Location location, String className, Kind kind) {
      return canonicalName(location, "", className.replace('.', '/') + kind.extension);
   }
   
   /**
    * Builds the canonical file path for the specified file.
    * 
    * @param location the location of the file
    * @param packageName the package that contains the file
    * @param relativeFileName the path to the file, relative to the package folder
    * @return an absolute and canonical path for the specified file
    */
   private static String canonicalName(Location location, String packageName,
         String relativeFileName) {
      String path = location.getName() + "/" + packageName.replace('.', '/') + "/"
         + relativeFileName;
      return removeMultipleSlashes(path);
   }
   
   /**
    * Removes sequences of multiple slashes. Such a sequence is stripped down to a single slash. So
    * {@code "//"} or {@code "////"} both become {@code "/"}. This is useful for constructing
    * canonical file paths.
    * 
    * @param input an input string
    * @return a copy of the input string but with multiple slashes stripped
    */
   static String removeMultipleSlashes(String input) {
      return MULTIPLE_SLASHES.matcher(input).replaceAll("/");
   }

   @Override
   public synchronized ClassLoader getClassLoader(final Location location) {
      if (location == StandardLocation.PLATFORM_CLASS_PATH) {
         return platformFileManager.getClassLoader(location);
      }
      return new ClassLoader(parentClassLoader) {
         @Override
         public Class<?> findClass(String name) throws ClassNotFoundException {
            try {
               byte classContents[] =
                     ((TestJavaFileObject) getJavaFileForInput(location, name, Kind.CLASS))
                           .getByteContents();
               return defineClass(name, classContents, 0, classContents.length);
            } catch (IOException e) {
               throw new ClassNotFoundException(name, e);
            }
         }
      };
   }

   @Override
   public synchronized Iterable<JavaFileObject> list(Location location, String packageName,
         Set<Kind> kinds, boolean recurse) throws IOException {
      if (location == StandardLocation.PLATFORM_CLASS_PATH) {
         return platformFileManager.list(location, packageName, kinds, recurse);
      }
      checkState();
      String pathPrefix = canonicalName(location, packageName);
      List<JavaFileObject> list = new ArrayList<JavaFileObject>();
      for (Map.Entry<String, TestJavaFileObject> entry : files.entrySet()) {
         // if not recursive, we make sure there is only one path element past
         // the prefix so we're only returning the one level in the hierarchy
         if (entry.getKey().startsWith(pathPrefix)
               && kinds.contains(TestJavaFileObject.determineKind(entry.getKey()))
               && (recurse
                     || END_OF_PATH.matcher(entry.getKey().substring(pathPrefix.length() + 1))
                           .matches())) {
            list.add(entry.getValue());
         }
      }
      if (location == StandardLocation.CLASS_PATH) {
         for (JavaFileObject fileObj
               : platformFileManager.list(location,  packageName, kinds, recurse)) {
            list.add(fileObj);
         }
      }
      return Collections.unmodifiableCollection(list);
   }

   @Override
   public String inferBinaryName(Location location, JavaFileObject file) {
      if (location != StandardLocation.PLATFORM_CLASS_PATH && !closed
            && file instanceof TestJavaFileObject) {
         TestJavaFileObject fileObj = (TestJavaFileObject) file;
         String pathPrefix = canonicalName(location, "");
         String fileName = fileObj.getName();
         if (fileObj.getFileManager() != this || fileObj.isDeleted()
               || !fileName.startsWith(pathPrefix)) {
            return null;
         }
         // strip the location prefix
         fileName = fileName.substring(pathPrefix.length());
         // and strip the extension
         int pos = fileName.lastIndexOf('.');
         if (pos > 0) {
            String extension = fileName.substring(pos);
            for (Kind kind : Kind.values()) {
               if (kind != Kind.OTHER && extension.equals(kind.extension)) {
                  fileName = fileName.substring(0, pos);
                  break;
               }
            }
         }
         // finally, convert slashes to dots
         return fileName.replace('/', '.');
      } else if ((location == StandardLocation.PLATFORM_CLASS_PATH
            || location == StandardLocation.CLASS_PATH) && !(file instanceof TestJavaFileObject)) {
         return platformFileManager.inferBinaryName(location, file);
      }
      return null;
   }

   @Override
   public boolean isSameFile(FileObject a, FileObject b) {
      if (a instanceof TestJavaFileObject || b instanceof TestJavaFileObject) {
         return a == b;
      } else {
         return platformFileManager.isSameFile(a, b);
      }
   }

   @Override
   public boolean handleOption(String current, Iterator<String> remaining) {
      return false;
   }

   @Override
   public boolean hasLocation(Location location) {
      return true;
   }
   
   private synchronized TestJavaFileObject getFile(String filePath, boolean createIfNotFound,
         boolean readOnly) {
      TestJavaFileObject file = files.get(filePath);
      if (file == null) {
         if (!createIfNotFound) {
            return null;
         }
         file = new TestJavaFileObject(this, filePath, readOnly);
         files.put(filePath,  file);
      }
      return file;
   }
   
   private synchronized TestJavaFileObject createFile(String filePath, String contents,
         Charset charset) {
      if (files.containsKey(filePath)) {
         throw new IllegalArgumentException("File "+ filePath + " already created!");
      }
      TestJavaFileObject file = new TestJavaFileObject(this, filePath, contents, charset);
      files.put(filePath, file);
      return file;
   }
   
   private synchronized TestJavaFileObject createFile(String filePath, byte contents[]) {
      if (files.containsKey(filePath)) {
         throw new IllegalArgumentException("File "+ filePath + " already created!");
      }
      TestJavaFileObject file = new TestJavaFileObject(this, filePath, contents);
      files.put(filePath, file);
      return file;
   }
   
   /**
    * Resets the file system. Resetting the file system deletes all files and invalidates
    * any open streams for writing files. After a reset, input files will need to be
    * re-seeded using {@link #createFileObject(JavaFileManager.Location, String, String, byte[])
    * createFileObject} or {@link #createJavaFileObject(JavaFileManager.Location, String, 
    * JavaFileObject.Kind, byte[]) createJavaFileObject}
    */
   public synchronized void reset() {
      openedForWriting.clear();
      Iterable<FileObject> filesToDelete = new ArrayList<FileObject>(files.values());
      files.clear(); // need to clear before we iterate to prevent
                     // ConcurrentModificationException since deleting a file tries
                     // to also clear the reference here
      for (FileObject file : filesToDelete) {
         file.delete();
      }
   }
   
   /**
    * Creates a new java file that has text contents in the in-memory file system.
    * 
    * @param location the location of the file
    * @param className the name of the class
    * @param kind the kind of file
    * @param contents the contents of the new file
    * @param charset the character set used to encode the file
    * @return the newly created file
    * @throws IllegalArgumentException if the specified file already exists or the specified
    *    destination is the {@link StandardLocation#PLATFORM_CLASS_PATH}
    * 
    * @see #createJavaFileObject(JavaFileManager.Location, String, JavaFileObject.Kind, byte[])
    */
   public synchronized TestJavaFileObject createJavaFileObject(Location location, String className,
         Kind kind, String contents, Charset charset) {
         if (location == StandardLocation.PLATFORM_CLASS_PATH) {
         throw new IllegalArgumentException("Cannot create files in the platform class path");
      }
      return createFile(canonicalName(location, className, kind), contents, charset);
   }

   /**
    * Creates a new java file that has text contents (UTF-8 encoded) in the in-memory file system.
    * 
    * @param location the location of the file
    * @param className the name of the class
    * @param kind the kind of file
    * @param contents the contents of the new file
    * @return the newly created file
    * @throws IllegalArgumentException if the specified file already exists or the specified
    *    destination is the {@link StandardLocation#PLATFORM_CLASS_PATH}
    * 
    * @see #createJavaFileObject(JavaFileManager.Location, String, JavaFileObject.Kind, byte[])
    */
   public synchronized TestJavaFileObject createJavaFileObject(Location location, String className,
         Kind kind, String contents) {
      return createJavaFileObject(location, className, kind, contents, DEFAULT_CHARSET);
   }

   /**
    * Creates a new java file that has binary contents in the in-memory file system.
    * 
    * @param location the location of the file
    * @param className the name of the class
    * @param kind the kind of file
    * @param contents the contents of the new file
    * @return the newly created file
    * @throws IllegalArgumentException if the specified file already exists or the specified
    *    destination is the {@link StandardLocation#PLATFORM_CLASS_PATH}
    *    
    * @see #createJavaFileObject(JavaFileManager.Location, String, JavaFileObject.Kind, String)
    */
   public synchronized TestJavaFileObject createJavaFileObject(Location location, String className,
         Kind kind, byte contents[]) {
      if (location == StandardLocation.PLATFORM_CLASS_PATH) {
         throw new IllegalArgumentException("Cannot create files in the platform class path");
      }
      return createFile(canonicalName(location, className, kind), contents);
   }

   /**
    * Creates a new text file in the in-memory file system.
    * 
    * @param location the location of the file
    * @param packageName the package that contains the file
    * @param relativeName the path to the file, relative to the package folder
    * @param contents the contents of the new file
    * @param charset the character set used to encode the file
    * @return the newly created file
    * @throws IllegalArgumentException if the specified file already exists or the specified
    *    destination is the {@link StandardLocation#PLATFORM_CLASS_PATH}
    *    
    * @see #createFileObject(javax.tools.JavaFileManager.Location, String, String, byte[])
    */
   public synchronized TestJavaFileObject createFileObject(Location location, String packageName,
         String relativeName, String contents, Charset charset) {
      if (location == StandardLocation.PLATFORM_CLASS_PATH) {
         throw new IllegalArgumentException("Cannot create files in the platform class path");
      }
      return createFile(canonicalName(location, packageName, relativeName), contents, charset);
   }
   
   /**
    * Creates a new text file (UTF-8 encoded) in the in-memory file system.
    * 
    * @param location the location of the file
    * @param packageName the package that contains the file
    * @param relativeName the path to the file, relative to the package folder
    * @param contents the contents of the new file
    * @return the newly created file
    * @throws IllegalArgumentException if the specified file already exists or the specified
    *    destination is the {@link StandardLocation#PLATFORM_CLASS_PATH}
    *    
    * @see #createFileObject(javax.tools.JavaFileManager.Location, String, String, byte[])
    */
   public synchronized TestJavaFileObject createFileObject(Location location, String packageName,
         String relativeName, String contents) {
      return createFileObject(location, packageName, relativeName, contents, DEFAULT_CHARSET);
   }

   /**
    * Creates a new binary file in the in-memory file system.
    * 
    * @param location the location of the file
    * @param packageName the package that contains the file
    * @param relativeName the path to the file, relative to the package folder
    * @param contents the contents of the new file
    * @return the newly created file
    * @throws IllegalArgumentException if the specified file already exists or the specified
    *    destination is the {@link StandardLocation#PLATFORM_CLASS_PATH}
    *    
    * @see #createFileObject(javax.tools.JavaFileManager.Location, String, String, String)
    */
   public synchronized TestJavaFileObject createFileObject(Location location, String packageName,
         String relativeName, byte contents[]) {
      if (location == StandardLocation.PLATFORM_CLASS_PATH) {
         throw new IllegalArgumentException("Cannot create files in the platform class path");
      }
      return createFile(canonicalName(location, packageName, relativeName), contents);
   }

   @Override
   public synchronized JavaFileObject getJavaFileForInput(Location location, String className,
         Kind kind) throws IOException {
      if (location == StandardLocation.PLATFORM_CLASS_PATH) {
         return platformFileManager.getJavaFileForInput(location, className, kind);
      }
      checkState();
      String filePath = canonicalName(location, className, kind);
      JavaFileObject fileObj = getFile(filePath, false, !location.isOutputLocation());
      if (fileObj != null) {
         return fileObj;
      } else if (location == StandardLocation.CLASS_PATH) {
         return platformFileManager.getJavaFileForInput(location, className, kind);
      } else {
         throw new IOException("File not found: " + filePath);
      }
   }

   @Override
   public synchronized TestJavaFileObject getJavaFileForOutput(Location location, String className,
         Kind kind, FileObject sibling) throws IOException {
      checkState();
      if (!location.isOutputLocation()) {
         throw new IOException("Specified location is for input files");
      }
      return getFile(canonicalName(location, className, kind), true, false);
   }

   @Override
   public synchronized FileObject getFileForInput(Location location, String packageName,
         String relativeName) throws IOException {
      if (location == StandardLocation.PLATFORM_CLASS_PATH) {
         return platformFileManager.getFileForInput(location, packageName, relativeName);
      }
      checkState();
      String filePath = canonicalName(location, packageName, relativeName);
      FileObject fileObj = getFile(filePath, false, !location.isOutputLocation());
      if (fileObj != null) {
         return fileObj;
      } else if (location == StandardLocation.CLASS_PATH) {
         return platformFileManager.getFileForInput(location, packageName, relativeName);
      } else {
         throw new IOException("File not found: " + filePath);
      }
   }

   @Override
   public synchronized TestJavaFileObject getFileForOutput(Location location, String packageName,
         String relativeName, FileObject sibling) throws IOException {
      checkState();
      if (!location.isOutputLocation()) {
         throw new IOException("Specified location is for input files");
      }
      return getFile(canonicalName(location, packageName, relativeName), true, false);
   }

   @Override
   public synchronized void flush() throws IOException {
      for (TestJavaFileObject file : openedForWriting) {
         try {
            file.flush();
         } catch (IOException ignore) {
         }
      }
   }

   @Override
   public synchronized void close() throws IOException {
      reset();
      closed = true;
   }
}
