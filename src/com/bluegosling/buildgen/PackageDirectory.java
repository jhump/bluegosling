package com.bluegosling.buildgen;

import static java.util.Objects.requireNonNull;

import java.io.File;

public class PackageDirectory extends File {
   private static final long serialVersionUID = 5861936417155122896L;

   private final JavaPackage pkg;
   
   public PackageDirectory(File f) {
      this(f, null);
   }
   
   public PackageDirectory(File f, JavaPackage pkg) {
      super(f.getParentFile(), f.getName());
      assert f.isDirectory();
      this.pkg = pkg;
   }
   
   public JavaPackage asJavaPackage() {
      return requireNonNull(pkg);
   }
   
   static PackageDirectory asPackage(File f) {
      return new PackageDirectory(f);
   }
   
   static PackageDirectory of(File f) {
      return f.isDirectory() ? asPackage(f) : asPackage(f.getParentFile());
   }
}