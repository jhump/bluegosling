package com.apriori.util;

import java.util.Collections;

// TODO: doc
// TODO: tests
public final class CallingClass {
   
   public static Class<?> getCaller() {
      return getCaller(1);
   }
   
   public static Class<?> getCaller(Iterable<? extends ClassLoader> classLoaders) {
      return getCaller(1, classLoaders);
   }
   
   public static Class<?> getCaller(int additionalStackFramesBack) {
      if (additionalStackFramesBack < 0) {
         throw new IllegalArgumentException("Stack frame distance must be non-negative");
      }
      return getCaller(additionalStackFramesBack + 1,
            Collections.singletonList(Thread.currentThread().getContextClassLoader()));
   }
   
   public static Class<?> getCaller(int additionalStackFramesBack,
         Iterable<? extends ClassLoader> classLoaders) {
      if (additionalStackFramesBack < 0) {
         throw new IllegalArgumentException("Stack frame distance must be non-negative");
      }
      StackTraceElement stackTrace[] = Thread.currentThread().getStackTrace();
      // Top of the stack is this method. Next item is this method's caller. So we start with the
      // third item (index = 2) since that is the caller we're interested in.
      int idx = additionalStackFramesBack + 2;
      if (idx >= stackTrace.length) {
         throw new IllegalArgumentException(
               "Stack frame distance is greater than actual stack height");
      }
      String callerName = stackTrace[idx].getClassName();
      for (ClassLoader cl : classLoaders) {
         try {
            return cl.loadClass(callerName);
         } catch (ClassNotFoundException e) {
            // go on to the next class loader
         }
      }
      // Not found!
      return null;
   }
}
