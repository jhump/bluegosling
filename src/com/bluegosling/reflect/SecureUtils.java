package com.bluegosling.reflect;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

/**
 * Several utility methods for checking security for reflective access. These methods consult a
 * {@link SecurityManager}, if one is installed, to check access.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class SecureUtils {
   private SecureUtils() {
   }

   /*
    * The following methods come from sun.reflect.misc.ReflectUtil. We include implementations
    * here to limit reliance on sun.* code.
    */

   /**
    * Checks package access on the given class.
    *
    * If it is a {@link Proxy#isProxyClass(java.lang.Class)} that implements a non-public
    * interface (i.e. may be in a non-restricted package), also check the package access on the
    * proxy interfaces.
    */
   public static void checkPackageAccess(Class<?> clazz) {
      checkPackageAccess(clazz.getName());
      if (isNonPublicProxyClass(clazz)) {
         checkProxyPackageAccess(clazz);
      }
   }

   /**
    * Checks package access on the given classname. This method is typically called when the
    * Class instance is not available and the caller attempts to load a class on behalf the true
    * caller (application).
    */
   public static void checkPackageAccess(String name) {
      SecurityManager s = System.getSecurityManager();
      if (s != null) {
         String cname = name.replace('/', '.');
         if (cname.startsWith("[")) {
            int b = cname.lastIndexOf('[') + 2;
            if (b > 1 && b < cname.length()) {
               cname = cname.substring(b);
            }
         }
         int i = cname.lastIndexOf('.');
         if (i != -1) {
            s.checkPackageAccess(cname.substring(0, i));
         }
      }
   }

   /**
    * Check package access on the proxy interfaces that the given proxy class implements.
    *
    * @param clazz Proxy class object
    */
   private static void checkProxyPackageAccess(Class<?> clazz) {
      SecurityManager s = System.getSecurityManager();
      if (s != null) {
         // check proxy interfaces if the given class is a proxy class
         if (Proxy.isProxyClass(clazz)) {
            for (Class<?> intf : clazz.getInterfaces()) {
               checkPackageAccess(intf);
            }
         }
      }
   }

   /**
    * Test if the given class is a proxy class that implements non-public interface. Such proxy
    * class may be in a non-restricted package that bypasses checkPackageAccess.
    */
   private static boolean isNonPublicProxyClass(Class<?> cls) {
      return Proxy.isProxyClass(cls) && containsNonPublicInterfaces(cls);
   }

   private static boolean containsNonPublicInterfaces(Class<?> cls) {
      for (Class<?> iface : cls.getInterfaces()) {
         if (!Modifier.isPublic(iface.getModifiers())) {
            return true;
         }
         if (containsNonPublicInterfaces(iface)) {
            return true;
         }
      }
      return false;
   }
}
