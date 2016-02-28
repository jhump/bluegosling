package com.bluegosling.concurrent;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link CompletableFuture} that provides a mechanism for interrupting an in-progress computation
 * when the future is cancelled.
 *
 * @param <T> the type of the future value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class InterruptibleCompletableFuture<T> extends CompletableFuture<T>
      implements CompletionStageFuture<T> {
   
   @Override
   public boolean cancel(boolean mayInterruptIfRunning) {
      boolean ret = super.cancel(mayInterruptIfRunning);
      if (ret && mayInterruptIfRunning) {
         interrupt();
      }
      return ret;
   }
   
   /**
    * Invoked when the task is cancelled and allowed to interrupt a running task. This method is
    * invoked when {@code cancel(true)} is called and should perform the interruption, if such an
    * operation is supported.
    * 
    * <p>This default implementation does nothing.
    */
   protected void interrupt() {
   }
}
