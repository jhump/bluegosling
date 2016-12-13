package com.bluegosling.buildgen;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.base.Predicate;

public class CompilationUnit extends File {
   private static final long serialVersionUID = 4468476116508359655L;

   static final Predicate<File> JAVA_SOURCE_FILE_FILTER =
         f -> f.isFile() && f.getName().toLowerCase().endsWith(".java");

   private final Set<JavaClass> containedClasses = new LinkedHashSet<>();
   private PackageDirectory pkg;
   
   CompilationUnit(File f) {
      super(f.getParentFile(), f.getName());
      assert f.isFile();
   }
   
   @Override
   public PackageDirectory getParentFile() {
      return pkg == null ? PackageDirectory.asPackage(super.getParentFile()) : pkg;
   }
   
   void addClass(JavaClass clazz) {
      containedClasses.add(clazz);
      if (pkg == null) {
         pkg = new PackageDirectory(super.getParentFile(), clazz.getPackage());
      }
   }
   
   public Set<JavaClass> getContainedClasses() {
      assert !containedClasses.isEmpty();
      return Collections.unmodifiableSet(containedClasses);
   }

   static CompilationUnit asCompilationUnit(File f) {
      assert JAVA_SOURCE_FILE_FILTER.apply(f);
      return new CompilationUnit(f);
   }
   
   static CompilationUnit asCompilationUnitUnknown(File f) {
      return new CompilationUnit(f);
   }
}