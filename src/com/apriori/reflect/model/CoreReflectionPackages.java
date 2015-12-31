package com.apriori.reflect.model;

import com.apriori.reflect.PackageScanner;
import com.apriori.reflect.PackageScanner.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;

/**
 * Utility functions for performing a package scan (via reflection over the class path), for
 * discovering packages and associated classes.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
final class CoreReflectionPackages {
   /** Lazy holder pattern, for thread-safe initialization of the package scan. */
   private static class ScanResultHolder {
      static final ScanResult SCAN_RESULTS = new PackageScanner().includeAllDefaults().start();
   }
   
   static ScanResult get() {
      return ScanResultHolder.SCAN_RESULTS;
   }
   
   private CoreReflectionPackages() {
   }
   
   public static boolean doesPackageExist(String packageName) {
      return get().includesPackage(packageName);
   }
   
   public static Package getPackage(String packageName) {
      Package pkg = get().getPackage(packageName);
      return pkg == null ? Package.getPackage(packageName) : pkg;
   }
   
   public static List<Element> getTopLevelTypesAsElements(String packageName) {
      Set<Class<?>> classes = get().getClassesFound(packageName);
      if (classes.isEmpty()) {
         return Collections.emptyList();
      }
      List<Element> results = new ArrayList<>(classes.size());
      for (Class<?> cl : classes) {
         if (!cl.isAnonymousClass() && !cl.isLocalClass() && !cl.isMemberClass()) {
            results.add(new CoreReflectionTypeElement(cl));
         }
      }
      return Collections.unmodifiableList(results);
   }
}
