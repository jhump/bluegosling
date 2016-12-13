package com.bluegosling.buildgen;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Objects;

public class JavaClass implements Comparable<JavaClass> {
   private final String className;
   private final JavaPackage pkg;
   private final String packageName;
   private final CompilationUnit sourceFile;
   private final File sourceRoot;
   
   JavaClass(String className, String packageName) {
      this(className, new JavaPackage(packageName), null, null);
   }

   JavaClass(String className, JavaPackage pkg, CompilationUnit sourceFile, File sourceRoot) {
      this.className = className;
      this.pkg = pkg;
      this.packageName = pkg.getPackageName();
      this.sourceFile = sourceFile;
      if (sourceFile != null) {
         requireNonNull(sourceRoot);
      }
      this.sourceRoot = sourceRoot;
   }

   public String getClassName() {
      return className;
   }

   public JavaPackage getPackage() {
      return pkg;
   }

   public CompilationUnit getSourceFile() {
      return sourceFile;
   }
   
   public File getSourceRoot() {
      return sourceRoot;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof JavaClass) {
         JavaClass other = (JavaClass) o;
         return className.equals(other.className)
               && packageName.equals(other.packageName)
               && Objects.equals(sourceFile, other.sourceFile)
               && Objects.equals(sourceRoot, other.sourceRoot);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      return Objects.hash(className, packageName, sourceFile, sourceRoot);
   }

   @Override
   public int compareTo(JavaClass o) {
      return className.compareTo(o.className);
   }
   
   @Override
   public String toString() {
      return className;
   }
}