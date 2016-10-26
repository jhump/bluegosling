package com.bluegosling.reflect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

// TODO: doc
// TODO: tests
public final class CallingClass {
   private CallingClass() {
   }
   
   public static class StackFrame {
      
      @SuppressWarnings("unused") // used to construct a null sentinel value of type Method
      private static void nullMethod() {
      }
      
      private static Method NULL_SENTINEL;
      static {
         try {
            NULL_SENTINEL = StackFrame.class.getDeclaredMethod("nullMethod");
         } catch (NoSuchMethodException | SecurityException e) {
            throw new AssertionError(e);
         }
      }
      
      private final StackTraceElement st;
      private final Class<?> clazz;
      private Method method = NULL_SENTINEL;
      
      StackFrame(StackTraceElement st, Class<?> clazz) {
         this.st = st;
         this.clazz = clazz;
      }
      
      public Class<?> getLocationClass() {
         return clazz;
      }
      
      public String getLocationClassName() {
         return st.getClassName();
      }
      
      public Method getLocationMethod() {
         if (method == null) {
            Collection<Method> matches = Members.findMethods(clazz, st.getMethodName());
            method = matches.size() == 1 ? method = matches.iterator().next() : NULL_SENTINEL;
         }
         return method == NULL_SENTINEL ? null : method;
      }
      
      public String getLocationMethodName() {
         return st.getMethodName();
      }
      
      public String getSourceFileName() {
         return st.getFileName();
      }
      
      public int getSourceLineNumber() {
         return st.getLineNumber();
      }
   }
   
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
      return computeCaller(additionalStackFramesBack + 1, (ste, cls) -> cls);
   }
   
   public static Class<?> getCaller(int additionalStackFramesBack,
         Iterable<? extends ClassLoader> classLoaders) {
      if (additionalStackFramesBack < 0) {
         throw new IllegalArgumentException("Stack frame distance must be non-negative");
      }
      return computeCaller(additionalStackFramesBack + 1, classLoaders, (ste, cls) -> cls);
   }

   public static StackFrame getCallerStackFrame() {
      return getCallerStackFrame(1);
   }
   
   public static StackFrame getCallerStackFrame(Iterable<? extends ClassLoader> classLoaders) {
      return getCallerStackFrame(1, classLoaders);
   }
   
   public static StackFrame getCallerStackFrame(int additionalStackFramesBack) {
      if (additionalStackFramesBack < 0) {
         throw new IllegalArgumentException("Stack frame distance must be non-negative");
      }
      return computeCaller(additionalStackFramesBack + 1, StackFrame::new);
   }
   
   public static StackFrame getCallerStackFrame(int additionalStackFramesBack,
         Iterable<? extends ClassLoader> classLoaders) {
      if (additionalStackFramesBack < 0) {
         throw new IllegalArgumentException("Stack frame distance must be non-negative");
      }
      return computeCaller(additionalStackFramesBack + 1, classLoaders, StackFrame::new);
   }

   private static <T> T computeCaller(int additionalStackFramesBack,
         BiFunction<StackTraceElement, Class<?>, T> fn) {
      ClassLoader cl1 = Thread.currentThread().getContextClassLoader();
      ClassLoader cl2 = CallingClass.class.getClassLoader();
      Iterable<ClassLoader> cls;
      if (cl1 == cl2 || cl2 == null) {
         cls = Collections.singleton(cl1);
      } else if (cl1 == null) {
         cls = Collections.singleton(cl2);
      } else {
         cls = Arrays.asList(cl1, cl2);
      }
      return computeCaller(additionalStackFramesBack + 1, cls, fn);
   }

   private static <T> T computeCaller(int additionalStackFramesBack,
         Iterable<? extends ClassLoader> classLoaders,
         BiFunction<StackTraceElement, Class<?>, T> fn) {
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
            if (cl == null) {
               return fn.apply(stackTrace[idx], Class.forName(callerName));
            } else {
               return fn.apply(stackTrace[idx], cl.loadClass(callerName));
            }
         } catch (ClassNotFoundException e) {
            // go on to the next class loader
         }
      }
      // Not found!
      return fn.apply(stackTrace[idx], null);
   }
}
