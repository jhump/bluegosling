package com.bluegosling.buildgen;

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
      assert pkg != null;
      return pkg;
   }
}