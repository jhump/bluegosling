package com.apriori.concurrent.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * A utility class that enables access to {@link Unsafe}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class UnsafeUtils {
   private UnsafeUtils() {
   }

   private static final Unsafe INSTANCE;

   public static Unsafe getUnsafe() {
      return INSTANCE;
   }

   static {
      Unsafe theUnsafe;
      try {
         Field field = Unsafe.class.getDeclaredField("theUnsafe");
         field.setAccessible(true);
         theUnsafe = (Unsafe) field.get(null);
      } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
         throw new ExceptionInInitializerError(e);
      }
      INSTANCE = theUnsafe;
   }
}
