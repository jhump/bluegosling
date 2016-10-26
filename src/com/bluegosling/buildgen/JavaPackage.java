package com.bluegosling.buildgen;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Objects;

public class JavaPackage implements Comparable<JavaPackage> {
   private final String packageName;
   private final PackageDirectory directory;
   private final File sourceRoot;
   
   JavaPackage(String packageName) {
      this(packageName, null, null);
   }

   JavaPackage(String packageName, PackageDirectory directory, File sourceRoot) {
      this.packageName = packageName;
      this.directory = directory == null ? null : new PackageDirectory(directory, this);
      if (directory != null) {
         requireNonNull(sourceRoot);
      }
      this.sourceRoot = sourceRoot;
   }

   public String getPackageName() {
      return packageName;
   }
   
   public PackageDirectory getPackageDirectory() {
      return directory;
   }
   
   public File getSourceRoot() {
      return sourceRoot;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof JavaPackage) {
         JavaPackage other = (JavaPackage) o;
         return packageName.equals(other.packageName)
               && Objects.equals(directory, other.directory)
               && Objects.equals(sourceRoot, other.sourceRoot);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      return Objects.hash(packageName, directory, sourceRoot);
   }

   @Override
   public int compareTo(JavaPackage o) {
      return packageName.compareTo(o.packageName);
   }
   
   @Override
   public String toString() {
      return packageName;
   }
}