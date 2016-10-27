package com.bluegosling.buildgen;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.bluegosling.concurrent.SameThreadExecutor;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.FluentFutureTask;
import com.bluegosling.tuples.Pair;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.Tables;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * Performs analysis of all Java source files in a given directory. This recursively scans the
 * directory for ".java" files, parses them, extracts the relevant references from the AST to
 * compute dependencies, and then performs post-processing on the dependency graphs. The result
 * is a {@link SourceDependencyAnalyzer}.
 * 
 * <p>The analysis results only contain information about files found in the given directory.
 * They will not include, for example, dependencies on other classpath entries.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see #analyze()
 */
public class SourceDependencyAnalyzer {
   private static final Predicate<File> JAVA_SOURCE_FILE_FILTER =
         f -> f.isFile() && f.getName().toLowerCase().endsWith(".java");
         
   private static final NoResolveClassLoader LOADER = new NoResolveClassLoader();
   
   /**
    * A simple class loader that only delegates to the parent loader and does not actually define
    * any classes. It's sole purpose is to expose the protected {@link #loadClass(String, boolean)}
    * method so that analysis can reflectively check the existence of a class without incurring the
    * linking of said class.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class NoResolveClassLoader extends ClassLoader {
      @Override public Class<?> loadClass(String className, boolean resolve)
            throws ClassNotFoundException {
         return super.loadClass(className, resolve);
      }
   }
   
   final Set<File> paths;
   final Consumer<File> progressCallback;
   final Map<CompilationUnit, File> rootByCompilationUnit = new LinkedHashMap<>();
   final Map<CompilationUnit, String> packageByCompilationUnit = new LinkedHashMap<>();
   final PackageTrieSet packageIndex = new PackageTrieSet();
   final Map<String, CompilationUnit> compilationUnitByClass = new LinkedHashMap<>();
   final SetMultimap<CompilationUnit, String> classesByCompilationUnit =
         LinkedHashMultimap.create();
   final Set<String> allClasses = compilationUnitByClass.keySet();
   final SetMultimap<CompilationUnit, String> importsByCompilationUnit =
         LinkedHashMultimap.create();
   final SetMultimap<CompilationUnit, String> importWildcardsByCompilationUnit = 
         LinkedHashMultimap.create();
   final SetMultimap<String, String> depsByElement = LinkedHashMultimap.create();
   final Map<String, String> packageByElement = new LinkedHashMap<>();
   final FluentFutureTask<Results> result;
   
   /**
    * Constructs an analyzer that will examine the given directory or java source file.
    * 
    * @param path a directory or java source file
    */
   public SourceDependencyAnalyzer(File path) {
      this(path, p -> {});
   }

   /**
    * Constructs an analyzer that will examine all of the given directories and/or java source
    * files.
    * 
    * @param paths directories and/or java source files
    */
   public SourceDependencyAnalyzer(Iterable<? extends File> paths) {
      this(paths, p -> {});
   }

   /**
    * Constructs an analyzer that will examine all of the given directories and/or java source
    * files.
    * 
    * @param paths directories and/or java source files
    */
   public SourceDependencyAnalyzer(File... paths) {
      this(p -> {}, paths);
   }

   /**
    * Constructs an analyzer that will examine the given directory or java source file and invoke
    * the given callback as each file is analyzed.
    * 
    * @param path a directory or java source file
    * @param progressCallback a callback that is invoked with each file as it is analyzed
    */
   public SourceDependencyAnalyzer(File path, Consumer<File> progressCallback) {
      this(ImmutableSet.of(path), progressCallback);
   }

   /**
    * Constructs an analyzer that will examine all of the given directories and/or java source
    * files and invoke the given callback as each file is analyzed.
    * 
    * @param paths directories and/or java source files
    * @param progressCallback a callback that is invoked with each file as it is analyzed
    */
   public SourceDependencyAnalyzer(Iterable<? extends File> paths,
         Consumer<File> progressCallback) {
      this.paths = ImmutableSet.copyOf(paths);
      this.progressCallback = progressCallback;
      this.result = new FluentFutureTask<>(this::doAnalysis);
   }

   /**
    * Constructs an analyzer that will examine all of the given directories and/or java source
    * files and invoke the given callback as each file is analyzed.
    * 
    * @param progressCallback a callback that is invoked with each file as it is analyzed
    * @param paths directories and/or java source files
    */
   public SourceDependencyAnalyzer(Consumer<File> progressCallback, File...paths) {
      this(ImmutableSet.copyOf(paths), progressCallback);
   }

   private Results doAnalysis() throws Exception {
      for (File path : paths) {
         for (File file : Files.fileTreeTraverser().preOrderTraversal(path)
               .filter(JAVA_SOURCE_FILE_FILTER)) {
            progressCallback.accept(file);
            CompilationUnit cu = asCompilationUnit(file);
            rootByCompilationUnit.put(cu, path);
            new AstVisitor(cu).visit();
         }
      }
      for (String packageName : packageByCompilationUnit.values()) {
         packageIndex.add(packageName);
      }
      Map<CompilationUnit, Set<JavaClass>> classDeps = resolveDependencies();
      for (CompilationUnit f : classDeps.keySet()) {
         Set<String> containedClassNames = classesByCompilationUnit.get(f);
         assert containedClassNames != null && !containedClassNames.isEmpty();
         String packageName = packageByCompilationUnit.get(f);
         assert packageName != null;
         File root = rootByCompilationUnit.get(f);
         assert root != null;
         for (String className : containedClassNames) {
            f.addClass(new JavaClass(className, packageName, f, root));
         }
      }
      return new Results(classDeps);
   }

   /**
    * Performs the file analysis asynchronously. The given executor is used to run the analysis.
    *  
    * @param executor the executor used to run the analysis
    * @return the future results of the analysis
    */
   public FluentFuture<Results> analyze(Executor executor) {
      executor.execute(result);
      return result;
   }

   /**
    * Performs the file analysis.
    * 
    * @return the results of the analysis
    */
   public Results analyze() {
      FluentFuture<Results> future = analyze(SameThreadExecutor.get());
      try {
         assert future.isDone();
         return Uninterruptibles.getUninterruptibly(future);
      } catch (ExecutionException e) {
         Throwable th = e.getCause();
         if (th instanceof RuntimeException) {
            throw (RuntimeException) th;
         } else if (th instanceof Error) {
            throw (Error) th;
         } else {
            throw new RuntimeException(th);
         }
      }
   }
   
   private Map<CompilationUnit, Set<JavaClass>> resolveDependencies() {
      Map<CompilationUnit, Set<JavaClass>> resolved = new LinkedHashMap<>();
      for (Entry<CompilationUnit, String> entry : importsByCompilationUnit.entries()) {
         JavaClass resolvedClass = resolveClass(entry.getValue());
         if (resolvedClass != null) {
            resolved.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>())
                  .add(resolvedClass);
         }
      }
      for (Entry<String, String> entry : depsByElement.entries()) {
         CompilationUnit source = findCompilationUnit(entry.getKey());
         JavaClass resolvedClass = resolveClass(entry.getValue(), entry.getKey(), source);
         if (resolvedClass != null) {
            resolved.computeIfAbsent(source, k -> new LinkedHashSet<>())
                  .add(resolvedClass);
         }
      }
      // include an identity mapping for every source file
      for (Entry<String, CompilationUnit> entry : compilationUnitByClass.entrySet()) {
         JavaClass resolvedClass = resolveClass(entry.getKey());
         if (resolvedClass != null) {
            resolved.computeIfAbsent(entry.getValue(), k -> new LinkedHashSet<>())
                  .add(resolvedClass);
         }
      }
      return resolved;
   }
   
   private JavaClass resolveClass(String className) {
      return resolveClass(className, packageIndex.findPackage(className));
   }
   
   private JavaClass resolveClass(String className, String knownPackagePrefix) {
      CompilationUnit target = compilationUnitByClass.get(className);
      if (target != null) {
         String packageName = packageByCompilationUnit.get(target);
         if (packageName == null) {
            packageName = ""; // unnamed package
         }
         assert packageName.startsWith(knownPackagePrefix);
         return new JavaClass(className, packageName, target, rootByCompilationUnit.get(target));
      }
      // Not in a compilation unit? Try resolving from class path. At this point, we have a
      // canonical name. But if the class in question is a nested class, its binary name may be
      // different. So we have to try replacing dots with dollar signs until we've either found
      // the class or have exhausted all possible binary names.
      return findClass(className, knownPackagePrefix);
   }
   
   private JavaClass findClass(String className, String knownPackagePrefix) {
      StringBuilder sb = new StringBuilder(className);
      int pos = className.length();
      while (pos > knownPackagePrefix.length()) {
         try {
            String binaryName = sb.toString();
            Class<?> clazz = LOADER.loadClass(binaryName, false);
            return new JavaClass(className, clazz.getPackage().getName());
         } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // We also catch NoClassDefError because of case-insensitive file system on Mac OS.
            // This happens if we are trying to resolve the scope of a method reference on a
            // variable, such as "foo::getBar", and there is also a class with that name but with
            // different case, e.g. "Foo". When we try to resolve the type on the left side of the
            // "::", the class loader will find "Foo" (because Mac OS will load "Foo.java" when we
            // ask for "foo.java"). When it detects the case mismatch between the name of the class
            // indicated in the class file and the actual requested name, the class loader will then
            // fail with a NoClassDefFoundError.
         }
         // didn't find it, so try replacing last dot with a dollar in case
         // this is a nested class
         pos = sb.lastIndexOf(".", pos - 1);
         if (pos != -1) {
            assert sb.charAt(pos) == '.';
            sb.setCharAt(pos, '$');
         }
      }
      // couldn't find any match
      return null;
   }

   private Iterable<String> contexts(String declaringElement) {
      String packageName = packageByElement.get(declaringElement);
      assert packageName != null;
      return () -> new Iterator<String>() {
         String current = declaringElement;

         @Override
         public boolean hasNext() {
            return current != null;
         }

         @Override
         public String next() {
            if (current == null) {
               throw new NoSuchElementException();
            }
            String ret = current;
            int pos = Math.max(current.lastIndexOf('.'), current.lastIndexOf('#'));
            if (pos == -1) {
               current = null;
            } else {
               current = current.substring(0, pos);
               if (current.equals(packageName)) {
                  current = null;
               }
            }
            return ret;
         }
      };
   }
   
   private CompilationUnit findCompilationUnit(String element) {
      for (String e : contexts(element)) {
         CompilationUnit f = compilationUnitByClass.get(e);
         if (f != null) {
            return f;
         }
      }
      return null;
   }
   
   private JavaClass resolveClass(String name, String declaringElement,
         CompilationUnit compilationUnit) {
      // The logic here isn't perfect, but it suffices to find all actual dependencies. There
      // will still be some identifiers that cannot be resolved because we don't apply full
      // semantics of the Java language like [1] method handles belonging to variables, not types
      // (e.g. mytask::run vs. Runnable::run); [2] nested types that belong to supertypes
      // (e.g. Entry, not imported but referenced in a class that implements the enclosing Map);
      // and [3] symbols that refer to type variables, not types. But these unresolved
      // identifiers do not prevent the code from understanding the actual dependencies between
      // compilation units.
      String packageName = packageByElement.get(declaringElement);
      assert packageName != null;
      // try to resolve name per scope/context of enclosing elements
      for (String context : contexts(declaringElement)) {
         if (context.equals(packageName)) {
            // we need to look at imports before we try to resolve to siblings in same package
            break;
         }
         if (name.equals(getSimpleName(context))) {
            return resolveClass(context);
         }
         JavaClass enclosedClass = resolveClass(context + "." + name, packageName);
         if (enclosedClass != null) {
            return enclosedClass;
         }
      }
      // look at imports
      int pos = name.indexOf('.');
      String namePrefix = pos == -1 ? null : name.substring(0, pos);
      String nameSuffix = pos == -1 ? null : name.substring(pos + 1);
      for (String importedType : importsByCompilationUnit.get(compilationUnit)) {
         if (namePrefix == null) {
            if (name.equals(getSimpleName(importedType))) {
               return resolveClass(importedType);
            }
         } else {
            if (namePrefix.equals(getSimpleName(importedType))) {
               return resolveClass(importedType + "." + nameSuffix);
            }
         }
      }
      // look at wildcard imports
      for (String importedScope : importWildcardsByCompilationUnit.get(compilationUnit)) {
         JavaClass qualifiedClass = resolveClass(importedScope + "." + name);
         if (qualifiedClass != null) {
            return qualifiedClass;
         }
      }
      // look at other types in the same package
      if (!packageName.isEmpty()) {
         String qualifiedName = packageName.isEmpty() ? name : packageName + "." + name;
         JavaClass qualifiedClass = resolveClass(qualifiedName, packageName);
         if (qualifiedClass != null) {
            return qualifiedClass;
         }
      }
      // almost done: look at java.lang
      JavaClass qualifiedClass = resolveClass("java.lang." + name, "java.lang");
      if (qualifiedClass != null) {
         return qualifiedClass;
      }
      // final step: type could already be qualified (or be in the unnamed package)
      return resolveClass(name);
   }
   
   private String getSimpleName(String element) {
      int pos = element.lastIndexOf('.');
      return pos == -1 ? element : element.substring(pos + 1);
   }
   
   /**
    * A visitor that can extract type declarations and references from a Java AST, used to build
    * a dependency graph.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class AstVisitor extends VoidVisitorAdapter<Void> {
      private final CompilationUnit file;
      private final ArrayDeque<String> currentScope = new ArrayDeque<>();
      private String packageName = "";
      
      AstVisitor(CompilationUnit file) {
         this.file = file;
      }

      public void visit() throws IOException, ParseException {
         visit(JavaParser.parse(file), null);
      }

      @Override
      public void visit(ImportDeclaration n, Void arg) {
         if (n.isStatic() || !n.isAsterisk()) {
            String importedName = nameToString(n.getName());
            if (n.isStatic() && !n.isAsterisk()) {
               // last component is the static element, which we don't care about
               int pos = importedName.lastIndexOf('.');
               assert pos > 0;
               importedName = importedName.substring(0, pos);
            }
            importsByCompilationUnit.put(file, importedName);
         } else {
            importWildcardsByCompilationUnit.put(file, nameToString(n.getName()));
         }
         super.visit(n, arg);
      }

      @Override
      public void visit(PackageDeclaration n, Void arg) {
         Preconditions.checkState(currentScope.isEmpty());
         currentScope.push(n.getPackageName());
         packageName = n.getPackageName();
         packageByCompilationUnit.put(file, packageName);
         super.visit(n, arg);
      }
      
      private String nameToString(NameExpr name) {
         if (!(name instanceof QualifiedNameExpr)) {
            return name.getName();
         }
         StringBuilder sb = new StringBuilder();
         nameToStringBuilder(name, sb);
         return sb.toString();
      }
      
      private void nameToStringBuilder(NameExpr name, StringBuilder sb) {
         if (name instanceof QualifiedNameExpr) {
            NameExpr q = ((QualifiedNameExpr) name).getQualifier();
            nameToStringBuilder(q, sb);
            sb.append(".");
         }
         sb.append(name.getName());
      }
      
      private String typeToString(ClassOrInterfaceType type) {
         if (type.getScope() == null) {
            return type.getName();
         }
         StringBuilder sb = new StringBuilder();
         typeToStringBuilder(type, sb);
         return sb.toString();
      }
      
      private void typeToStringBuilder(ClassOrInterfaceType type, StringBuilder sb) {
         ClassOrInterfaceType outer = type.getScope();
         if (outer != null) {
            typeToStringBuilder(outer, sb);
            sb.append(".");
         }
         sb.append(type.getName());
      }
      
      private String makeTypeName(String name) {
         String scope = currentScope.peek();
         String typeName = scope == null ? name : scope + "." + name;
         classesByCompilationUnit.put(file, typeName);
         File previous = compilationUnitByClass.put(typeName, file);
         Preconditions.checkState(previous == null,
               "Type %s is declared in more than one compilation unit: %s and %s",
               typeName, file, previous);
         return typeName;
      }

      private String makeMethodName(String name) {
         return currentScope.peek() + "#" + name;
      }
      
      private void pushElement(String name) {
         packageByElement.put(name, packageName);
         currentScope.push(name);
      }
      
      private void popElement() {
         currentScope.pop();
      }

      @Override
      public void visit(ClassOrInterfaceDeclaration n, Void arg) {
         pushElement(makeTypeName(n.getName()));
         try {
            super.visit(n, arg);
         } finally {
            popElement();
         }
      }

      @Override
      public void visit(EnumDeclaration n, Void arg) {
         pushElement(makeTypeName(n.getName()));
         try {
            super.visit(n, arg);
         } finally {
            popElement();
         }
      }

      @Override
      public void visit(AnnotationDeclaration n, Void arg) {
         pushElement(makeTypeName(n.getName()));
         try {
            super.visit(n, arg);
         } finally {
            popElement();
         }
      }

      @Override
      public void visit(ConstructorDeclaration n, Void arg) {
         // MethodDeclaration defines "throws" as TypeExpr, but unfortunately
         // ConstructorDeclaration does not...
         for (NameExpr expr : n.getThrows()) {
            depsByElement.put(currentScope.peek(), nameToString(expr));
         }
         pushElement(makeMethodName("<init>"));
         try {
            super.visit(n, arg);
         } finally {
            popElement();
         }
      }

      @Override
      public void visit(MethodDeclaration n, Void arg) {
         pushElement(makeMethodName(n.getName()));
         try {
            super.visit(n, arg);
         } finally {
            popElement();
         }
      }

      @Override
      public void visit(ClassOrInterfaceType n, Void arg) {
         depsByElement.put(currentScope.peek(), typeToString(n));
      }
   }

   /**
    * Enumerates all distinct paths, between all endpoints, in a package dependency graph.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class PathEnumerator {
      private final SetMultimap<PackageDirectory, PackageDirectory> packageDependencies;
      final Table<PackageDirectory, PackageDirectory, List<PackageDirectory>> shortestPaths =
            HashBasedTable.create();
      final Set<List<PackageDirectory>> packageCycles = new LinkedHashSet<>();
      final Set<PackageDirectory> packagesExplored = new HashSet<>();
      
      PathEnumerator(SetMultimap<PackageDirectory, PackageDirectory> packageDependencies) {
         this.packageDependencies = packageDependencies;
      }

      public void enumerateAllPaths() {
         for (Entry<PackageDirectory, Collection<PackageDirectory>> entry
               : packageDependencies.asMap().entrySet()) {
            enumerateRecursively(entry.getKey(), entry.getValue(), new Path());
         }
      }

      private void enumerateRecursively(PackageDirectory file,
            Collection<PackageDirectory> dependencies, Path current) {
         if (packagesExplored.contains(file)) {
            return;
         }
         for (List<PackageDirectory> tail : current.tailPaths()) {
            PackageDirectory start = tail.get(0);
            List<PackageDirectory> existing = shortestPaths.get(start, file);
            if (existing == null || existing.size() > tail.size() + 1) {
               List<PackageDirectory> path = new ArrayList<>(tail.size() + 1);
               path.addAll(tail);
               path.add(file);
               shortestPaths.put(start, file, path);
            }
         }
         if (!current.push(file)) {
            packageCycles.add(current.asCycle(file));
            return;
         }
         try {
            for (PackageDirectory dependency : dependencies) {
               if (file.equals(dependency)) {
                  continue;
               }
               enumerateRecursively(dependency, packageDependencies.get(dependency), current);
            }
         } finally {
            PackageDirectory f = current.pop();
            assert f == file;
            packagesExplored.add(file);
         }
      }
   }
   
   /**
    * Represents a non-cyclic path through a dependency graph.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Path {
      private final List<PackageDirectory> path = new ArrayList<>();
      private final Set<PackageDirectory> pathElements = new HashSet<>();
      
      Path() {
      }
      
      Iterable<List<PackageDirectory>> tailPaths() {
         return () -> new Iterator<List<PackageDirectory>>() {
            int index = 0;

            @Override
            public boolean hasNext() {
               return index < path.size();
            }

            @Override
            public List<PackageDirectory> next() {
               if (index >= path.size()) {
                  throw new NoSuchElementException();
               }
               return path.subList(index++, path.size());
            }
         };
      }
      
      boolean push(PackageDirectory f) {
         if (pathElements.add(f)) {
            path.add(f);
            return true;
         }
         return false;
      }
      
      PackageDirectory pop() {
         PackageDirectory f = path.remove(path.size() - 1);
         boolean removed = pathElements.remove(f);
         assert removed;
         return f;
      }
      
      List<PackageDirectory> asCycle(PackageDirectory end) {
         int pos = path.indexOf(end);
         assert pos >= 0;
         List<PackageDirectory> cycle = new ArrayList<>(path.size() - pos + 1);
         cycle.addAll(path.subList(pos, path.size()));
         cycle.add(end);
         return cycle;
      }
   }
  
   /**
    * Analysis over a corpus of Java source code. The focus of the analysis is the dependency graph,
    * connecting compilation units and packages. This can be used to identify, for example, cycles
    * in the dependencies between packages. It can also be used to extract dependencies from source
    * code for the purpose of {@linkplain BuildGen generating build configuration}.
    *  
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class Results {
      /**
       * The set of all source files. 
       */
      private final Set<CompilationUnit> allFiles;
      
      private final Set<JavaClass> allExternalClasses;
      
      /**
       * The set of all package directories.
       */
      private final Set<PackageDirectory> allPackages;

      private final Set<JavaPackage> allExternalPackages;

      /**
       * The set of source file dependencies. Each file in the map is a Java source file. The key is
       * a source file and the associated values are its dependencies, which are also source files.
       */
      private final SetMultimap<CompilationUnit, CompilationUnit> fileDependencies;

      private final SetMultimap<CompilationUnit, JavaClass> externalClassDependencies;

      /**
       * The set of source file dependents. Each file in the map is a Java source file. The key is
       * a source file and the associated values are the files that depend on the keys, which are
       * also source files.
       */
      private final SetMultimap<CompilationUnit, CompilationUnit> fileDependents;

      /**
       * The set of package dependencies. Each file in the map is a folder that represents a package
       * of Java source. The key is a package folder and the associated values are its dependencies,
       * which are also package folders.
       */
      private final SetMultimap<PackageDirectory, PackageDirectory> packageDependencies;

      private final SetMultimap<PackageDirectory, JavaPackage> externalPackageDependencies;

      /**
       * The set of package dependencies. Each file in the map is a folder that represents a package
       * of Java source. The key is a package folder and the associated values are its dependencies,
       * which are also package folders.
       */
      private final SetMultimap<PackageDirectory, PackageDirectory> packageDependents;

      /**
       * The set of files in each package. The key is a package folder and the associated values are
       * the Java source files found in that package.
       */
      private final SetMultimap<PackageDirectory, CompilationUnit> filesByPackage;
      
      private final SetMultimap<JavaPackage, JavaClass> externalClassesByPackage;
      
      /**
       * The set of packages that each file depends on. The key is a Java source file and the values
       * are the packages that file depends on.
       */
      private final SetMultimap<CompilationUnit, PackageDirectory> packageDependenciesByFile;

      private final SetMultimap<CompilationUnit, JavaPackage> externalPackageDependenciesByFile;

      /**
       * The set of packages that each file depends on. The key is a Java source file and the values
       * are the packages that file depends on.
       */
      private final SetMultimap<PackageDirectory, CompilationUnit> fileDependenciesByPackage;

      private final SetMultimap<PackageDirectory, JavaClass> externalDependenciesByPackage;

      /**
       * The set of files that depend on each package. The key is a package folder and the values
       * are the Java source files that have a dependency on that package.
       */
      private final SetMultimap<PackageDirectory, CompilationUnit> fileDependentsByPackage;

      /**
       * The set of files that depend on each package. The key is a package folder and the values
       * are the Java source files that have a dependency on that package.
       */
      private final SetMultimap<CompilationUnit, PackageDirectory> packageDependentsByFile;

      /**
       * A table of shortest paths in the package dependency graph. The path in each cell is the
       * shortest path from the first key to the second. The second key must be a dependency
       * (possibly transitively/indirectly) of the first in order for a path to be present.
       */
      private final Table<PackageDirectory, PackageDirectory, List<PackageDirectory>> shortestPaths;

      /**
       * A table of file-level details for a particular package-level dependency. The table keys
       * represent a dependent and depdendency package, respectively. The value is a map of all
       * files in the dependent package to all dependencies in the other package.
       */
      private final Table<PackageDirectory, PackageDirectory, SetMultimap<CompilationUnit, CompilationUnit>>
      fileDependencyDetails;
      
      /**
       * The cycles in the package dependency graph. Each cycle identified is reduced to its
       * shortest form (sub-paths replaced by shorter paths that connect the two endpoints). The
       * resulting collection is the distinct set of such shortened cycles. Each cycle has the same
       * package as its first and last element.
       */
      private final Set<List<PackageDirectory>> packageCycles;
      
      Results(Map<CompilationUnit, Set<JavaClass>> compilationDependencies) {
         SetMultimap<CompilationUnit, CompilationUnit> fileDependencies =
               LinkedHashMultimap.create();
         SetMultimap<CompilationUnit, JavaClass> externalClassDependencies =
               LinkedHashMultimap.create();
         Set<CompilationUnit> allFiles = new TreeSet<>();
         Set<JavaClass> allExternalClasses = new TreeSet<>();
         Set<PackageDirectory> allPackages = new TreeSet<>();
         Set<JavaPackage> allExternalPackages = new TreeSet<>();
         SetMultimap<CompilationUnit, CompilationUnit> fileDependents = LinkedHashMultimap.create();
         SetMultimap<PackageDirectory, PackageDirectory> packageDependencies =
               LinkedHashMultimap.create();
         SetMultimap<PackageDirectory, JavaPackage> externalPackageDependencies =
               LinkedHashMultimap.create();
         SetMultimap<PackageDirectory, PackageDirectory> packageDependents =
               LinkedHashMultimap.create();
         SetMultimap<PackageDirectory, CompilationUnit> filesByPackage =
               LinkedHashMultimap.create();
         SetMultimap<JavaPackage, JavaClass> externalClassesByPackage = LinkedHashMultimap.create();
         SetMultimap<CompilationUnit, PackageDirectory> packageDependenciesByFile =
               LinkedHashMultimap.create();
         SetMultimap<CompilationUnit, JavaPackage> externalPackageDependenciesByFile =
               LinkedHashMultimap.create();
         SetMultimap<CompilationUnit, PackageDirectory> packageDependentsByFile =
               LinkedHashMultimap.create();
         SetMultimap<PackageDirectory, CompilationUnit> fileDependenciesByPackage =
               LinkedHashMultimap.create();
         SetMultimap<PackageDirectory, JavaClass> externalDependenciesByPackage =
               LinkedHashMultimap.create();
         SetMultimap<PackageDirectory, CompilationUnit> fileDependentsByPackage =
               LinkedHashMultimap.create();
         Table<PackageDirectory, PackageDirectory, SetMultimap<CompilationUnit, CompilationUnit>>
         fileDependencyDetails = HashBasedTable.create();
         
         for (Entry<CompilationUnit, Set<JavaClass>> entry : compilationDependencies.entrySet()) {
            CompilationUnit source = entry.getKey();
            
            assert !source.getContainedClasses().isEmpty();
            assert source.getContainedClasses().stream().allMatch(c -> c.getSourceFile() != null);
            assert source.getContainedClasses().stream().allMatch(c -> c.getSourceRoot() != null);
            assert source.getParentFile().asJavaPackage() != null;
            assert source.getParentFile().asJavaPackage().getPackageDirectory() != null;
            assert source.getParentFile().asJavaPackage().getSourceRoot() != null;
            
            for (JavaClass dep : entry.getValue()) {
               CompilationUnit target = dep.getSourceFile();
               if (target != null) {
                  allFiles.add(source);
                  allFiles.add(target);
                  allPackages.add(source.getParentFile());
                  allPackages.add(target.getParentFile());
                  fileDependencies.put(source, target);
                  fileDependents.put(target, entry.getKey());
                  packageDependencies.put(source.getParentFile(), target.getParentFile());
                  packageDependents.put(target.getParentFile(), source.getParentFile());
                  filesByPackage.put(source.getParentFile(), source);
                  filesByPackage.put(target.getParentFile(), target);
                  packageDependenciesByFile.put(source, target.getParentFile());
                  packageDependentsByFile.put(target, source.getParentFile());
                  fileDependenciesByPackage.put(source.getParentFile(), target);
                  fileDependentsByPackage.put(target.getParentFile(), source);
                  SetMultimap<CompilationUnit, CompilationUnit> details =
                        fileDependencyDetails.get(source.getParentFile(), target.getParentFile());
                  if (details == null) {
                     details = LinkedHashMultimap.create();
                     fileDependencyDetails.put(source.getParentFile(),
                           target.getParentFile(), details);
                  }
                  details.put(source, target);
               } else {
                  allExternalClasses.add(dep);
                  allExternalPackages.add(dep.getPackage());
                  externalClassDependencies.put(source, dep);
                  externalPackageDependencies.put(source.getParentFile(), dep.getPackage());
                  externalClassesByPackage.put(dep.getPackage(), dep);
                  externalPackageDependenciesByFile.put(source, dep.getPackage());
                  externalDependenciesByPackage.put(source.getParentFile(), dep);
               }
            }
         }
         
         this.allFiles = Collections.unmodifiableSet(allFiles);
         this.allExternalClasses = Collections.unmodifiableSet(allExternalClasses);
         this.allPackages = Collections.unmodifiableSet(allPackages);
         this.allExternalPackages = Collections.unmodifiableSet(allExternalPackages);
         this.fileDependencies = Multimaps.unmodifiableSetMultimap(fileDependencies);
         this.externalClassDependencies =
               Multimaps.unmodifiableSetMultimap(externalClassDependencies);
         this.fileDependents = Multimaps.unmodifiableSetMultimap(fileDependents);
         this.packageDependencies = Multimaps.unmodifiableSetMultimap(packageDependencies);
         this.externalPackageDependencies =
               Multimaps.unmodifiableSetMultimap(externalPackageDependencies);
         this.packageDependents = Multimaps.unmodifiableSetMultimap(packageDependents);
         this.filesByPackage = Multimaps.unmodifiableSetMultimap(filesByPackage);
         this.externalClassesByPackage =
               Multimaps.unmodifiableSetMultimap(externalClassesByPackage);
         this.packageDependenciesByFile =
               Multimaps.unmodifiableSetMultimap(packageDependenciesByFile);
         this.externalPackageDependenciesByFile =
               Multimaps.unmodifiableSetMultimap(externalPackageDependenciesByFile);
         this.packageDependentsByFile = Multimaps.unmodifiableSetMultimap(packageDependentsByFile);
         this.fileDependenciesByPackage =
               Multimaps.unmodifiableSetMultimap(fileDependenciesByPackage);
         this.externalDependenciesByPackage =
               Multimaps.unmodifiableSetMultimap(externalDependenciesByPackage);
         this.fileDependentsByPackage = Multimaps.unmodifiableSetMultimap(fileDependentsByPackage);
         
         for (Cell<PackageDirectory, PackageDirectory, SetMultimap<CompilationUnit, CompilationUnit>> cell
               : fileDependencyDetails.cellSet()) {
            fileDependencyDetails.put(cell.getRowKey(), cell.getColumnKey(),
                  Multimaps.unmodifiableSetMultimap(cell.getValue()));
         }
         this.fileDependencyDetails = Tables.unmodifiableTable(fileDependencyDetails);
         
         PathEnumerator en = new PathEnumerator(packageDependencies);
         en.enumerateAllPaths();
         this.shortestPaths = Tables.unmodifiableTable(en.shortestPaths);
         this.packageCycles = Collections.unmodifiableSet(computeShortestCycles(en.packageCycles));
      }
      
      private Set<List<PackageDirectory>> computeShortestCycles(
            Set<List<PackageDirectory>> cycles) {
         Set<List<PackageDirectory>> shortenedCycles = new LinkedHashSet<>();
         for (List<PackageDirectory> cycle : cycles) {
            if (cycle.size() == 3) {
               // can't get any shorter
               shortenedCycles.add(cycle);
               continue;
            }
            assert cycle.size() > 3;
            
            List<PackageDirectory> shortest = cycle;
            for (int i = 1, j = cycle.size() - 2; i <= j; i++, j--) {
               List<PackageDirectory> path1 = getDependencyPath(cycle.get(0), cycle.get(i));
               List<PackageDirectory> path2 =
                     getDependencyPath(cycle.get(i), cycle.get(cycle.size() - 1));
               if (path1 != null && path2 != null
                     && path1.size() + path2.size() - 1 < shortest.size()) {
                  shortest = new ArrayList<>(path1.size() + path2.size() - 1);
                  shortest.addAll(path1);
                  shortest.addAll(path2.subList(1, path2.size()));
                  if (shortest.size() == 3) {
                     break;
                  }
               }
               if (i != j) {
                  path1 = getDependencyPath(cycle.get(0), cycle.get(j));
                  path2 = getDependencyPath(cycle.get(j), cycle.get(cycle.size() - 1));
                  if (path1 != null && path2 != null
                        && path1.size() + path2.size() - 1 < shortest.size()) {
                     shortest = new ArrayList<>(path1.size() + path2.size() - 1);
                     shortest.addAll(path1);
                     shortest.addAll(path2.subList(1, path2.size()));
                     if (shortest.size() == 3) {
                        break;
                     }
                  }
               }
            }
            shortenedCycles.add(shortest);
         }
         return shortenedCycles;
      }

      /**
       * Returns all compilation units analyzed.
       * 
       * @return all compilation units analyzed
       */
      public Set<CompilationUnit> getCompilationUnits() {
         return allFiles;
      }
      
      public Set<JavaClass> getExternalClasses() {
         return allExternalClasses;
      }

      /**
       * Returns all package directories analyzed.
       *  
       * @return all package directories analyzed
       */
      public Set<PackageDirectory> getPackages() {
         return allPackages;
      }
      
      public Set<JavaPackage> getExternalPackages() {
         return allExternalPackages;
      }
      
      /**
       * Returns the compilation units found in the given package directory
       * 
       * @param pkg a package directory
       * @return all compilation units in the given directory
       */
      public Set<CompilationUnit> getCompilationUnitsForPackage(File pkg) {
         return pkg.isDirectory() ? filesByPackage.get(asPackage(pkg)) : ImmutableSet.of();
      }
      
      public Set<JavaClass> getExternalPackageContents(JavaPackage pkg) {
         return externalClassesByPackage.get(pkg);
      }

      /**
       * Gets all compilation units on which the given file or directory depends.
       * 
       * @param file a source file or package directory
       * @return the other source files on which the given file depends
       */
      public Set<CompilationUnit> getDependencies(File file) {
         return file.isDirectory()
               ? fileDependenciesByPackage.get(asPackage(file))
               : fileDependencies.get(asCompilationUnitUnknown(file));
      }
      
      public Set<JavaClass> getExternalDependencies(File file) {
         return file.isDirectory()
               ? externalDependenciesByPackage.get(asPackage(file))
               : externalClassDependencies.get(asCompilationUnitUnknown(file));
      }

      /**
       * Gets all compilation units that depend on the given source file or package directory.
       * 
       * @param file a source file or package directory
       * @return the other source files that depend on the given file
       */
      public Set<CompilationUnit> getDependents(File file) {
         return file.isDirectory()
               ? fileDependentsByPackage.get(asPackage(file))
               : fileDependents.get(asCompilationUnitUnknown(file));
      }

      /**
       * Gets all package directories on which the given source file or package directory depends.
       * 
       * @param file a source file or package directory
       * @return the other package directories on which the given file depends
       */
      public Set<PackageDirectory> getPackageDependencies(File file) {
         return file.isDirectory()
               ? packageDependencies.get(asPackage(file))
               : packageDependenciesByFile.get(asCompilationUnitUnknown(file));
      }

      public Set<JavaPackage> getExternalPackageDependencies(File file) {
         return file.isDirectory()
               ? externalPackageDependencies.get(asPackage(file))
               : externalPackageDependenciesByFile.get(asCompilationUnitUnknown(file));
      }

      /**
       * Gets all package directories that depend on the given source file or package directory.
       * 
       * @param file a source file or package directory
       * @return the other package directories that depend on the given file
       */
      public Set<PackageDirectory> getPackageDependents(File file) {
         return file.isDirectory()
               ? packageDependents.get(asPackage(file))
               : packageDependentsByFile.get(asCompilationUnitUnknown(file));
      }
      
      /**
       * Gets all file-level dependencies between the given two packages. The returned map's keys
       * are files in the first given package that depend on files in the second given package. The
       * values in the map are the set of those dependencies in the second given package.
       * 
       * @param package1 a package directory
       * @param package2 a package directory
       * @return a map of source file dependencies for files in the first given package that depend
       *    on files in the second given package
       */
      public SetMultimap<CompilationUnit, CompilationUnit> getFileDependencyDetails(File package1,
            File package2) {
         if (!package1.isDirectory() || !package2.isDirectory()) {
            return ImmutableSetMultimap.of();
         }
         SetMultimap<CompilationUnit, CompilationUnit> ret =
               fileDependencyDetails.get(asPackage(package1), asPackage(package2));
         return ret == null ? ImmutableSetMultimap.of() : ret;
      }

      /**
       * Gets all package cycles. Each list in the set is a path with greater than one element that
       * starts at a package and ends with the same package. Each element in the path is a
       * dependency of the preceding element.
       * 
       * @return all package cycles
       */
      public Set<List<PackageDirectory>> getPackageCycles() {
         return packageCycles;
      }
      
      /**
       * Finds the dependency path from one given file or directory to another. The dependency path
       * is expressed in terms of packages. So it will start with the first given file (if it is a
       * package directory, otherwise its containing package directory) and will end with the
       * second.
       * 
       * <p>If the first given path does not depend on the second given path (e.g. the second path
       * is not a dependency of the first) then the return list is empty.
       * 
       * <p>If the first path directly depends on the second, the returned path has just two
       * elements: the first and then second given package directories. If the dependency is
       * indirect, then the path is the list of transitive dependencies that demonstrate the
       * indirect dependency.
       * 
       * @param a a source file or package directory
       * @param b another source file or package directory
       * @return the path from the first package to the second package, where each element is a
       *       dependency of its preceding element
       */
      public List<PackageDirectory> getDependencyPath(File a, File b) {
         List<PackageDirectory> path = shortestPaths.get(packageOf(a), packageOf(b));
         return path == null ? ImmutableList.of() : path;
      }
   }
   
   private static CompilationUnit asCompilationUnit(File f) {
      assert JAVA_SOURCE_FILE_FILTER.apply(f);
      return new CompilationUnit(f);
   }
   
   private static CompilationUnit asCompilationUnitUnknown(File f) {
      return new CompilationUnit(f);
   }
   
   static PackageDirectory asPackage(File f) {
      return new PackageDirectory(f);
   }
   
   private static PackageDirectory packageOf(File f) {
      return f.isDirectory() ? asPackage(f) : asPackage(f.getParentFile());
   }
   
   /**
    * Runs the dependency analyzer on a path provided as the sole command-line argument.
    * 
    * @param args command-line arguments (expected to be a single path)
    */
   public static void main(String args[]) {
      if (args.length == 0) {
         System.err.printf(
               "Usage:\n"
               + "  %s file-or-directory...\n"
               + "All given files (which must be Java source files) as well as the Java\n"
               + "source files contained in all given directories, are analyzed. If any\n"
               + "package dependency cycles are found, they are printed to standard out.\n",
               SourceDependencyAnalyzer.class.getName());
         System.exit(1);
      }
      List<File> paths = Lists.transform(Arrays.asList(args), File::new);
      for (File path : paths) {
         if (!path.exists()) {
            System.err.printf(
                  "The given path, %s, does not exist so could not be analyzed.\n", path);
            System.exit(1);
         }
         if (!path.isDirectory() && !JAVA_SOURCE_FILE_FILTER.apply(path)) {
            System.err.printf(
                  "The given path, %s, is neither a directory nor a Java source file.\n", path);
            System.exit(1);
         }
      }
      
      Results results =
            new SourceDependencyAnalyzer(paths, p -> System.out.println("Analyzing " + p))
            .analyze();
      
      // print any cyclic dependencies
      Set<Pair<PackageDirectory, PackageDirectory>> relationships = new LinkedHashSet<>();
      for (List<PackageDirectory> cycle : results.getPackageCycles()) {
         System.out.println("Found cycle: " + cycle);
         for (int i = 1; i < cycle.size(); i++) {
            relationships.add(Pair.create(cycle.get(i - 1), cycle.get(i)));
         }
      }
      // and show the dependencies that caused the cycles
      for (Pair<PackageDirectory, PackageDirectory> pair : relationships) {
         System.out.println("From " + pair.getFirst() + " to " + pair.getSecond() + ":");
         for (Entry<CompilationUnit, CompilationUnit> entry
               : results.getFileDependencyDetails(pair.getFirst(), pair.getSecond()).entries()) {
            System.out.println("   " + entry.getKey() + " -> " + entry.getValue());
         }
      }
      
      // also print external dependencies
      for (JavaPackage pkg : results.getExternalPackages()) {
         System.out.println("External package: " + pkg.getPackageName());
      }
   }
}