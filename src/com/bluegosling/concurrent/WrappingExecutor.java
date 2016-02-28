package com.bluegosling.concurrent;

import java.util.concurrent.Executor;

/**
 * An executor that provides a way to wrap tasks that are executed. Wrappers can perform a range of
 * cross-cutting concerns before and after delegating to the wrapped task.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class WrappingExecutor implements Executor {

   private final Executor delegate;
   
   /**
    * Constructs a new executor that delegates to the given executor.
    *
    * @param delegate an executor
    */
   protected WrappingExecutor(Executor delegate) {
      this.delegate = delegate;
   }
   
   /**
    * Returns the underlying executor.
    *
    * @return the underlying executor
    */
   protected Executor delegate() {
      return delegate;
   }
   
   /**
    * Wraps the given task. This is called for each task executed.
    *
    * @param r a task
    * @return a wrapper around the given task that will be executed in its place
    */
   protected abstract Runnable wrap(Runnable r);

   @Override
   public void execute(Runnable command) {
      delegate.execute(wrap(command));
   }
}
