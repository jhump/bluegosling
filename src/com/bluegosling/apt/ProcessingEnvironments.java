package com.bluegosling.apt;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Static helper methods related to the current processing environment.
 * 
 * <p>The current environment must be {@linkplain #setup(ProcessingEnvironment) setup} for the
 * current thread. After that, processing code running on that same thread can conveniently
 * access the environment and the {@link Elements} and {@link Types} utility interfaces using static
 * methods on this class.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class ProcessingEnvironments {
   private ProcessingEnvironments() {}
   
   private static final ThreadLocal<ProcessingEnvironment> threadLocal =
         new InheritableThreadLocal<ProcessingEnvironment>();

   /**
    * Resets the current thread, clearing thread-local state related to the current environment.
    * This can only be called after the state has been {@link #setup} on this thread.
    * 
    * @throws IllegalStateException if the environment has not been setup on this thread
    */
   public static void reset() {
      ProcessingEnvironment env = threadLocal.get();
      if (env == null) {
         throw new IllegalStateException("ProcessingEnvironment has not been setup on this thread");
      }
      threadLocal.remove();
   }
   
   /**
    * Sets up the environment for the current thread. This initializes thread-local state so that
    * the environment can be conveniently accessed from other static methods.
    *
    * @param env the current processing environment
    * @throws IllegalStateException if the environment has already been setup on this thread
    */
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
   
   /**
    * Gets the environment for the current thread.
    * 
    * @return the processing environment for the current thread
    * @throws IllegalStateException if the environment has not been setup on this thread
    */
   public static ProcessingEnvironment get() {
      ProcessingEnvironment env = threadLocal.get();
      if (env == null) {
         throw new IllegalStateException("ProcessingEnvironment has not been setup on this thread");
      }
      return env;
   }
   
   /**
    * Gets the current implementation of utility methods relating to elements. This convenience
    * method is short for {@code ProcessingEnvironments.get().getElementUtils()}.
    *
    * @return the implementation of element utility methods for the current thread
    * @throws IllegalStateException if the environment has not been setup on this thread
    */
   public static Elements elements() {
      return get().getElementUtils();
   }
   
   /**
    * Gets the current implementation of utility methods relating to types. This convenience
    * method is short for {@code ProcessingEnvironments.get().getTypeUtils()}.
    *
    * @return the implementation of type utility methods for the current thread
    * @throws IllegalStateException if the environment has not been setup on this thread
    */
   public static Types types() {
      return get().getTypeUtils();
   }
}
