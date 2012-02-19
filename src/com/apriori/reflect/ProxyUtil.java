package com.apriori.reflect;

/**
 * Utility methods for working with proxies and implementing invocation handlers.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ProxyUtil {
   /**
    * Prevents instantiation.
    */
   private ProxyUtil() {}

   /**
    * Determines a "null" return value to use for a method. For most methods this will simply be
    * {@code null}. But of mehods that return primitives, we need to choose a suitable primitive
    * value to avoid a {@code NullPointerException}.
    * 
    * @param clazz the return type of the method
    * @return the "null" value that method should return
    */
   public static Object getNullReturnValue(Class<?> clazz) {
      if (!clazz.isPrimitive()) {
         return null;
      }
      else if (clazz == int.class) {
         return 0;
      }
      else if (clazz == short.class) {
         return (short) 0;
      }
      else if (clazz == byte.class) {
         return (byte) 0;
      }
      else if (clazz == char.class) {
         return '\u0000';
      }
      else if (clazz == long.class) {
         return 0L;
      }
      else if (clazz == double.class) {
         return 0.0;
      }
      else if (clazz == float.class) {
         return 0.0F;
      }
      else if (clazz == boolean.class) {
         return false;
      }
      else if (clazz == void.class) {
         return null;
      }
      else {
         // this should never happen...
         throw new IllegalArgumentException(
               "Suppressing exceptions could not determine return value for "
                     + clazz.getName());
      }
   }
}
