package com.bluegosling.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.bluegosling.concurrent.executors.SameThreadExecutor;
import com.bluegosling.concurrent.fluent.FluentFutureTask;
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
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.io.Files;

/**
 * Analyzes a corpus of Java source code. The focus of the analysis is the dependency graph,
 * connecting compilation units and package. This can be used to identify, for example, cycles in
 * the dependencies between packages.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SourceDependencyAnalysis {
   private static final Predicate<File> JAVA_SOURCE_FILE_FILTER =
         f -> !f.isDirectory() && f.getName().toLowerCase().endsWith(".java");
         
   /**
    * The set of source file dependencies. Each file in the map is a Java source file. The key is
    * a source file and the associated values are its dependencies, which are also source files.
    */
   private final SetMultimap<CompilationUnit, CompilationUnit> fileDependencies;

   /**
    * The set of source file dependees. Each file in the map is a Java source file. The key is
    * a source file and the associated values are the files that depend on the keys, which are also
    * source files.
    */
   private final SetMultimap<CompilationUnit, CompilationUnit> dependees;

   /**
    * The set of package dependencies. Each file in the map is a folder that represents a package of
    * Java source. The key is a package folder and the associated values are its dependencies, which
    * are also package folders.
    */
   private final SetMultimap<Package, Package> packageDependencies;
   
   /**
    * The set of files in each package. The key is a package folder and the associated values are
    * the Java source files found in that package.
    */
   private final SetMultimap<Package, CompilationUnit> filesByPackage;
   
   /**
    * The set of packages that each file depends on. The key is a Java source file and the values
    * are the packages that file depends on.
    */
   private final SetMultimap<CompilationUnit, Package> packageDependenciesByFile;

   
   /**
    * The set of files that depend on each package. The key is a package folder and the values are
    * the Java source files that have a dependency on that package.
    */
   private final SetMultimap<Package, CompilationUnit> dependeesByPackage;

   /**
    * A table of shortest paths in the package dependency graph. The path in each cell is the
    * shortest path from the first key to the second. The second key must be a dependency (possibly
    * transitively/indirectly) of the first in order for a path to be present.
    */
   private final Table<Package, Package, List<Package>> shortestPaths;
   
   /**
    * The cycles in the package dependency graph. Each cycle identified is reduced to its shortest
    * form (sub-paths replaced by shorter paths that connect the two endpoints). The resulting
    * collection is the distinct set of such shortened cycles. Each cycle has the same package as
    * its first and last element.
    */
   private final Set<List<Package>> packageCycles;
   
   SourceDependencyAnalysis(SetMultimap<CompilationUnit, CompilationUnit> fileDependencies) {
      this.fileDependencies = Multimaps.unmodifiableSetMultimap(fileDependencies);
      
      SetMultimap<CompilationUnit, CompilationUnit> dependees = LinkedHashMultimap.create();
      SetMultimap<Package, Package> packageDependencies = LinkedHashMultimap.create();
      SetMultimap<Package, CompilationUnit> filesByPackage = LinkedHashMultimap.create();
      SetMultimap<CompilationUnit, Package> packageDependenciesByFile = LinkedHashMultimap.create();
      SetMultimap<Package, CompilationUnit> dependeesByPackage = LinkedHashMultimap.create();
      for (Entry<CompilationUnit, CompilationUnit> entry : fileDependencies.entries()) {
         dependees.put(entry.getValue(), entry.getKey());
         packageDependencies.put(entry.getKey().getParentFile(), entry.getValue().getParentFile());
         filesByPackage.put(entry.getKey().getParentFile(), entry.getKey());
         packageDependenciesByFile.put(entry.getKey(), entry.getValue().getParentFile());
         dependeesByPackage.put(entry.getValue().getParentFile(), entry.getKey());
      }
      
      this.dependees = Multimaps.unmodifiableSetMultimap(dependees);
      this.packageDependencies = Multimaps.unmodifiableSetMultimap(packageDependencies);
      this.filesByPackage = Multimaps.unmodifiableSetMultimap(filesByPackage);
      this.packageDependenciesByFile = Multimaps.unmodifiableSetMultimap(packageDependenciesByFile);
      this.dependeesByPackage = Multimaps.unmodifiableSetMultimap(dependeesByPackage);
      
      PathEnumerator en = new PathEnumerator(packageDependencies);
      en.enumerateAllPaths();
      this.shortestPaths = Tables.unmodifiableTable(en.shortestPaths);
      this.packageCycles = Collections.unmodifiableSet(computeShortestCycles(en.packageCycles));
   }
   
   private Set<List<Package>> computeShortestCycles(Set<List<Package>> cycles) {
      Set<List<Package>> shortenedCycles = new LinkedHashSet<>();
      for (List<Package> cycle : cycles) {
         if (cycle.size() == 3) {
            // can't get any shorter
            shortenedCycles.add(cycle);
            continue;
         }
         assert cycle.size() > 3;
         
         List<Package> shortest = cycle;
         for (int i = 1, j = cycle.size() - 2; i <= j; i++, j--) {
            List<Package> path1 = getPath(cycle.get(0), cycle.get(i));
            List<Package> path2 = getPath(cycle.get(i), cycle.get(cycle.size() - 1));
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
               path1 = getPath(cycle.get(0), cycle.get(j));
               path2 = getPath(cycle.get(j), cycle.get(cycle.size() - 1));
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

   public Set<CompilationUnit> getCompilationUnits() {
      return fileDependencies.keySet();
   }

   public Set<Package> getPackages() {
      return packageDependencies.keySet();
   }
   
   public Set<CompilationUnit> getCompilationUnitsForPackage(File pkg) {
      return filesByPackage.get(asPackage(pkg));
   }

   public Set<CompilationUnit> getDependencies(File compilationUnit) {
      return fileDependencies.get(asCompilationUnit(compilationUnit));
   }

   public Set<CompilationUnit> getDependees(File compilationUnit) {
      return dependees.get(asCompilationUnit(compilationUnit));
   }

   public Set<Package> getPackageDependenciesForPackage(File packageName) {
      return packageDependencies.get(asPackage(packageName));
   }

   public Set<Package> getPackageDependenciesForCompilationUnit(File compilationUnit) {
      return packageDependenciesByFile.get(asCompilationUnit(compilationUnit));
   }

   public Set<CompilationUnit> getPackageDependees(File packageName) {
      return dependeesByPackage.get(asPackage(packageName));
   }

   public Set<List<Package>> getPackageCycles() {
      return packageCycles;
   }
   
   public List<Package> getPath(File a, File b) {
      return shortestPaths.get(a, b);
   }
   
   /**
    * Enumerates all distinct paths, between all endpoints, in a package dependency graph.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class PathEnumerator {
      private final SetMultimap<Package, Package> packageDependencies;
      final Table<Package, Package, List<Package>> shortestPaths = HashBasedTable.create();
      final Set<List<Package>> packageCycles = new LinkedHashSet<>();
      final Set<Package> packagesExplored = new HashSet<>();
      
      PathEnumerator(SetMultimap<Package, Package> packageDependencies) {
         this.packageDependencies = packageDependencies;
      }

      public void enumerateAllPaths() {
         for (Entry<Package, Collection<Package>> entry : packageDependencies.asMap().entrySet()) {
            enumerateRecursively(entry.getKey(), entry.getValue(), new Path());
         }
      }

      private void enumerateRecursively(Package file, Collection<Package> dependencies,
            Path current) {
         if (packagesExplored.contains(file)) {
            return;
         }
         for (List<Package> tail : current.tailPaths()) {
            Package start = tail.get(0);
            List<Package> existing = shortestPaths.get(start, file);
            if (existing == null || existing.size() > tail.size() + 1) {
               List<Package> path = new ArrayList<>(tail.size() + 1);
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
            for (Package dependency : dependencies) {
               if (file.equals(dependency)) {
                  continue;
               }
               enumerateRecursively(dependency, packageDependencies.get(dependency), current);
            }
         } finally {
            Package f = current.pop();
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
      private final List<Package> path = new ArrayList<>();
      private final Set<Package> pathElements = new HashSet<>();
      
      Path() {
      }
      
      Iterable<List<Package>> tailPaths() {
         return () -> new Iterator<List<Package>>() {
            int index = 0;

            @Override
            public boolean hasNext() {
               return index < path.size();
            }

            @Override
            public List<Package> next() {
               if (index >= path.size()) {
                  throw new NoSuchElementException();
               }
               return path.subList(index++, path.size());
            }
         };
      }
      
      boolean push(Package f) {
         if (pathElements.add(f)) {
            path.add(f);
            return true;
         }
         return false;
      }
      
      Package pop() {
         Package f = path.remove(path.size() - 1);
         boolean removed = pathElements.remove(f);
         assert removed;
         return f;
      }
      
      List<Package> asCycle(Package end) {
         int pos = path.indexOf(end);
         assert pos >= 0;
         List<Package> cycle = new ArrayList<>(path.size() - pos + 1);
         cycle.addAll(path.subList(pos, path.size()));
         cycle.add(end);
         return cycle;
      }
   }
  
   /**
    * Performs analysis of all Java source files in a given directory. This recursively scans the
    * directory for ".java" files, parses them, extracts the relevant references from the AST to
    * compute dependencies, and then performs post-processing on the dependency graphs. The result
    * is a {@link SourceDependencyAnalysis}.
    *  
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class Analyzer {
      final File path;
      final Consumer<File> progressCallback;
      final Map<String, File> compilationUnitByClass = new LinkedHashMap<>();
      final Set<String> allClasses = compilationUnitByClass.keySet();
      final SetMultimap<File, String> importsByCompilationUnit = LinkedHashMultimap.create();
      final SetMultimap<File, String> importWildcardsByCompilationUnit = LinkedHashMultimap.create();
      final SetMultimap<String, String> depsByElement = LinkedHashMultimap.create();
      final Map<String, String> packageByElement = new LinkedHashMap<>();
      final FluentFutureTask<SourceDependencyAnalysis> result;
      
      public Analyzer(File path) {
         this(path, p -> {});
      }
      
      public Analyzer(File path, Consumer<File> progressCallback) {
         this.path = path;
         this.progressCallback = progressCallback;
         this.result = new FluentFutureTask<>(this::doAnalysis);
      }
      
      private SourceDependencyAnalysis doAnalysis() throws Exception {
         for (File file : Files.fileTreeTraverser().preOrderTraversal(path)
               .filter(JAVA_SOURCE_FILE_FILTER)) {
            progressCallback.accept(file);
            new AstVisitor(file).visit();
         }
         
         SetMultimap<CompilationUnit, CompilationUnit> fileDeps = resolveDependencies();
         return new SourceDependencyAnalysis(fileDeps);
      }

      public SourceDependencyAnalysis analyze(Executor executor)
            throws InterruptedException, ExecutionException {
         executor.execute(result);
         return result.get();
      }

      public SourceDependencyAnalysis analyze() throws InterruptedException, ExecutionException {
         return analyze(SameThreadExecutor.get());
      }
      
      private SetMultimap<CompilationUnit, CompilationUnit> resolveDependencies() {
         // The logic here isn't perfect, but it suffices to find all actual dependencies. There
         // will still be some identifiers that cannot be resolved because we don't apply full
         // semantics of the Java language like [1] method handles belonging to variables, not types
         // (e.g. mytask::run vs. Runnable::run); [2] nested types that belong to supertypes
         // (e.g. Entry, not imported but referenced in a class that imports and implements Map);
         // [3] types that are resolvable but due to the class path and not in the analyzed corpus
         // of source code (e.g. String from java.lang package); and [4] symbols that refer to type
         // variables, not types. But these unresolved identifiers do not prevent the code from
         // understanding the actual dependencies between compilation units.
         SetMultimap<CompilationUnit, CompilationUnit> resolved = LinkedHashMultimap.create();
         for (Entry<File, String> entry : importsByCompilationUnit.entries()) {
            File target = compilationUnitByClass.get(entry.getValue());
            if (target != null) {
               resolved.put(asCompilationUnit(entry.getKey()), asCompilationUnit(target));
            }
         }
         for (Entry<String, String> entry : depsByElement.entries()) {
            File source = findCompilationUnit(entry.getKey());
            String resolvedName = resolveClassName(entry.getValue(), entry.getKey(), source);
            if (resolvedName == null) {
               continue;
            }
            File target = compilationUnitByClass.get(resolvedName);
            if (target != null) {
               resolved.put(asCompilationUnit(source), asCompilationUnit(target));
            }
         }
         // include an identity mapping for every source file
         for (File f : compilationUnitByClass.values()) {
            resolved.put(asCompilationUnit(f), asCompilationUnit(f));
         }
         return resolved;
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
      
      private File findCompilationUnit(String element) {
         for (String e : contexts(element)) {
            File f = compilationUnitByClass.get(e);
            if (f != null) {
               return f;
            }
         }
         return null;
      }
      
      private String resolveClassName(String name, String declaringElement, File compilationUnit) {
         String packageName = packageByElement.get(declaringElement);
         assert packageName != null;
         // try to resolve name per scope/context of enclosing elements
         for (String context : contexts(declaringElement)) {
            if (context.equals(packageName)) {
               // we need to look at imports before we try to resolve to siblings in same package
               break;
            }
            if (name.equals(getSimpleName(context))) {
               return context;
            }
            String enclosed = context + "." + name;
            if (allClasses.contains(enclosed)) {
               return enclosed;
            }
         }
         // look at imports
         int pos = name.indexOf('.');
         String namePrefix = pos == -1 ? null : name.substring(0, pos);
         String nameSuffix = pos == -1 ? null : name.substring(pos + 1);
         for (String importedType : importsByCompilationUnit.get(compilationUnit)) {
            if (namePrefix == null) {
               if (name.equals(getSimpleName(importedType))) {
                  return importedType;
               }
            } else {
               if (namePrefix.equals(getSimpleName(importedType))) {
                  return importedType + "." + nameSuffix;
               }
            }
         }
         // look at wildcard imports
         for (String importedScope : importWildcardsByCompilationUnit.get(compilationUnit)) {
            String qualified = importedScope + "." + name;
            if (allClasses.contains(qualified)) {
               return qualified;
            }
         }
         // look at other types in the same package
         if (!packageName.isEmpty()) {
            String qualified = packageName.isEmpty() ? name : packageName + "." + name;
            if (allClasses.contains(qualified)) {
               return qualified;
            }
         }
         // almost done: look at java.lang
         String qualified = "java.lang." + name;
         if (allClasses.contains(qualified)) {
            return qualified;
         }
         // final step: type could already be qualified (or be in the unnamed package)
         if (allClasses.contains(name)) {
            return name;
         }
         // could not be resolved
         return null;
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
         private final File file;
         private final ArrayDeque<String> currentScope = new ArrayDeque<>();
         private String packageName = "";
         
         AstVisitor(File file) {
            this.file = file;
         }

         public void visit() throws IOException, ParseException {
            visit(JavaParser.parse(file), null);
         }

         @Override
         public void visit(ImportDeclaration n, Void arg) {
            if (n.isStatic() || !n.isAsterisk()) {
               importsByCompilationUnit.put(file, nameToString(n.getName()));
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
   }
   
   public static void main(String args[]) throws Exception {
      if (args.length != 1) {
         System.err.printf(
               "Usage:\n"
               + "  %s file-or-directory\n"
               + "If the given file is a Java source file, it is analyzed. If it is a directory,\n"
               + "it is scanned for Java source files and package dependency cycles are printed\n"
               + "to standard out.",
               SourceDependencyAnalysis.class.getName());
         System.exit(1);
      }
      File path = new File(args[0]);
      if (!path.exists()) {
         System.err.printf(
               "The given path, %s, does not exist so could not be analyzed.", path);
         System.exit(1);
      }
      if (!path.isDirectory() && !JAVA_SOURCE_FILE_FILTER.apply(path)) {
         System.err.printf(
               "The given path, %s, is neither a directory nor a Java source file.", path);
         System.exit(1);
      }
      SourceDependencyAnalysis results =
            new Analyzer(path, p -> System.out.println("Analyzing " + p)).analyze();
      for (List<Package> cycle : results.getPackageCycles()) {
         System.out.println("Found cycle: " + cycle);
      }
   }
   
   private static CompilationUnit asCompilationUnit(File f) {
      if (f instanceof CompilationUnit) {
         return (CompilationUnit) f;
      }
      return new CompilationUnit(f);
   }

   private static Package asPackage(File f) {
      if (f instanceof Package) {
         return (Package) f;
      }
      return new Package(f);
   }

   public static class CompilationUnit extends File {
      private static final long serialVersionUID = -1385558818375328563L;

      CompilationUnit(File file) {
         super(file.getPath());
         assert JAVA_SOURCE_FILE_FILTER.apply(file);
      }
      
      @Override
      public Package getParentFile() {
         return asPackage(super.getParentFile());
      }
   }

   public static class Package extends File {
      private static final long serialVersionUID = 2154840995191666654L;

      Package(File file) {
         super(file.getPath());
         assert file.isDirectory();
      }
   }
}
