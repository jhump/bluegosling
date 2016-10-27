package com.bluegosling.buildgen;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class CompilationUnit extends File {
   private static final long serialVersionUID = 4468476116508359655L;

   private final Set<JavaClass> containedClasses = new LinkedHashSet<>();
   private JavaPackage pkg;
   
   CompilationUnit(File f) {
      super(f.getParentFile(), f.getName());
      assert f.isFile();
   }
   
   @Override
   public PackageDirectory getParentFile() {
      return pkg == null ? SourceDependencyAnalyzer.asPackage(super.getParentFile()) : pkg.getPackageDirectory();
   }
   
   void addClass(JavaClass clazz) {
      containedClasses.add(clazz);
      if (pkg == null) {
         pkg = clazz.getPackage();
      }
   }
   
   public Set<JavaClass> getContainedClasses() {
      assert !containedClasses.isEmpty();
      return Collections.unmodifiableSet(containedClasses);
   }
}