package com.bluegosling.buildgen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

class PackageTrieMap<V> {
   private final char nameComponentSeparator;
   private final Map<String, PackageTrieMap<V>> children;
   private V value;
   
   PackageTrieMap() {
      this('.');
   }

   PackageTrieMap(char nameComponentSeparator) {
      this.nameComponentSeparator = nameComponentSeparator;
      children = new HashMap<>();
   }


   PackageTrieMap(char nameComponentSeparator, Map<String, PackageTrieMap<V>> children, V value) {
      this.nameComponentSeparator = nameComponentSeparator;
      this.children = children;
      this.value = value;
   }
   
   void put(String packagePattern, V value) {
      put(asComponents(packagePattern), value);
   }
   
   private void put(Iterator<String> nameComponents, V value) {
      if (!nameComponents.hasNext()) {
         this.value = value;
         return;
      }
      children.computeIfAbsent(nameComponents.next(), k -> new PackageTrieMap<>())
         .put(nameComponents, value);
   }
   
   PackageTrieMap<V> asUnmodifiable() {
      return new PackageTrieMap<V>(nameComponentSeparator, children, value) {
         @Override void put(String packagePattern, V value) {
            throw new UnsupportedOperationException();
         }
      };
   }
   
   PackageTrieMap<V> deepCopy() {
      HashMap<String, PackageTrieMap<V>> childrenCopy = new HashMap<>(children.size() * 4 / 3);
      children.forEach((k, v) -> childrenCopy.put(k, v.deepCopy()));
      return new PackageTrieMap<>(nameComponentSeparator, childrenCopy, value);
   }
   
   V get(String packageName) {
      return get(asComponents(packageName));
   }

   private V get(Iterator<String> nameComponents) {
      if (!nameComponents.hasNext()) {
         return value;
      }
      String n = nameComponents.next();
      PackageTrieMap<V> child = children.get(n);
      if (child == null) {
         // if exact child not found, see if there's a wildcard
         child = children.get("*");
         if (child == null) {
            return value;
         }
      }
      V ret = child.get(nameComponents);
      return ret == null ? value : ret;
   }

   String findPackage(String qualifiedName) {
      return findPackage(asComponents(qualifiedName));
   }
   
   private String findPackage(Iterator<String> nameComponents) {
      StringBuilder sb = new StringBuilder();
      findPackage(nameComponents, sb);
      return sb.toString();
   }
   
   private void findPackage(Iterator<String> nameComponents, StringBuilder sb) {
      if (!nameComponents.hasNext()) {
         return;
      }
      String c = nameComponents.next();
      PackageTrieMap<V> child = children.get(c);
      if (child == null) {
         return;
      }
      if (sb.length() != 0) {
         sb.append(nameComponentSeparator);
      }
      sb.append(c);
      child.findPackage(nameComponents, sb);
   }
   
   Iterator<String> asComponents(String string) {
      return new Iterator<String>() {
         int pos = 0;
         
         @Override
         public boolean hasNext() {
            return pos < string.length();
         }

         @Override
         public String next() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            int nextPos = string.indexOf(nameComponentSeparator, pos);
            if (nextPos == -1) {
               nextPos = string.length();
            }
            String ret = string.substring(pos, nextPos);
            pos = nextPos + 1;
            return ret;
         }
      };
   }
}