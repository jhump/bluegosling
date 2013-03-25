package com.apriori.apt;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

// TODO: doc!
public final class ProcessingEnvironments {
   private ProcessingEnvironments() {}
   
   private static final ThreadLocal<ProcessingEnvironment> threadLocal =
         new InheritableThreadLocal<ProcessingEnvironment>();

   public static void reset() {
      ProcessingEnvironment env = threadLocal.get();
      if (env == null) {
         throw new IllegalStateException("ProcessingEnvironment has not been setup on this thread");
      }
      threadLocal.remove();
   }
   
   public static void setup(ProcessingEnvironment env) {
      if (env == null) {
         throw new NullPointerException();
      }
      ProcessingEnvironment previousEnv = threadLocal.get();
      if (previousEnv != null && previousEnv != env) {
         throw new IllegalStateException("ProcessingEnvironment has already been setup on this thread");
      }
      threadLocal.set(env);
   }
   
   public static ProcessingEnvironment get() {
      ProcessingEnvironment env = threadLocal.get();
      if (env == null) {
         throw new IllegalStateException("ProcessingEnvironment has not been setup on this thread");
      }
      return env;
   }
   
   public static Elements elements() {
      return get().getElementUtils();
   }
   
   public static Types types() {
      return get().getTypeUtils();
   }
}
