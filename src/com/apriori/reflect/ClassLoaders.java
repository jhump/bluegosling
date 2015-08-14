package com.apriori.reflect;


public final class ClassLoaders {
   private ClassLoaders() {
   }
   
   /**
    * Returns true if the second classloader can be found in the first classloader's delegation
    * chain. Equivalent to the inaccessible: first.isAncestor(second).
    */
   public static boolean isAncestor(ClassLoader first, ClassLoader second) {
      ClassLoader acl = first;
      do {
         acl = acl.getParent();
         if (second == acl) {
            return true;
         }
      } while (acl != null);
      return false;
   }
}
