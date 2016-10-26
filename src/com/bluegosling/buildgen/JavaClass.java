package com.bluegosling.buildgen;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Objects;

public class JavaClass implements Comparable<JavaClass> {
   private final String className;
   private final String packageName;
   private final CompilationUnit sourceFile;
   private final File sourceRoot;
   private final JavaPackage pkg;
   
   JavaClass(String className, String packageName) {
      this(className, packageName, null, null);
   }

   JavaClass(String className, String packageName, CompilationUnit sourceFile, File sourceRoot) {
      this.className = className;
      this.packageName = packageName;
      this.sourceFile = sourceFile;
      if (sourceFile != null) {
         requireNonNull(sourceRoot);
      }
      this.sourceRoot = sourceRoot;
      this.pkg = new JavaPackage(packageName,
            sourceFile == null ? null : sourceFile.getParentFile(),
            sourceRoot);
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