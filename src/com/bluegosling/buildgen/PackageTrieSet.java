package com.bluegosling.buildgen;

class PackageTrieSet {
   private final PackageTrieMap<Boolean> map;
   
   PackageTrieSet() {
      this.map = new PackageTrieMap<>();
   }

   PackageTrieSet(char nameComponentSeparator) {
      this.map = new PackageTrieMap<>(nameComponentSeparator);
   }

   void add(String packagePattern) {
      map.put(packagePattern, Boolean.TRUE);
   }

   boolean contains(String packageName) {
      return map.get(packageName) != null;
   }
   
   String findPackage(String qualifiedName) {
      return map.findPackage(qualifiedName);
   }
}
