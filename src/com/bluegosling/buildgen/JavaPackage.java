package com.bluegosling.buildgen;

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class JavaPackage implements Comparable<JavaPackage> {
   private final String packageName;
   private final Set<PackageDirectory> directories;
   
   JavaPackage(String packageName) {
      this.packageName = packageName;
      this.directories = null;
   }

   JavaPackage(String packageName, Iterable<PackageDirectory> directories) {
      this.packageName = packageName;
      this.directories = ImmutableSet.copyOf(directories);
   }
   
   public String getPackageName() {
      return packageName;
   }
   
   public Set<PackageDirectory> getPackageDirectories() {
      if (directories == null) {
         throw new IllegalStateException("External packages have no associated directories");
      }
      return directories;
   }
   
   public boolean hasDirectories() {
      return directories != null;
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof JavaPackage) {
         JavaPackage other = (JavaPackage) o;
         return packageName.equals(other.packageName)
               && Objects.equals(directories, other.directories);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      return Objects.hash(packageName, directories);
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