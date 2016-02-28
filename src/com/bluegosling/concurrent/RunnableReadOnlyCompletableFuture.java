package com.bluegosling.concurrent;

import com.bluegosling.vars.Variable;

import java.util.concurrent.Callable;

/**
 * A read-only completable future that represents a task. The future completes when the task is
 * executed and completes. The future completes with the result of the task or with the cause of its
 * failure.
 *
 * @param <T> the type of the future value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class RunnableReadOnlyCompletableFuture<T> extends ReadOnlyCompletableFuture<T>
      implements RunnableCompletionStageFuture<T> {
   private final Variable<Thread> runner = new Variable<>();
   private final Callable<T> task;
   
   /**
    * Creates a new future that represents the given task. Callers must subsequently arrange for
    * execution by {@linkplain #run() running} the new future.
    *
    * @param task the task that this future will run and whose result provides the value for this
    *       future
    */
   public RunnableReadOnlyCompletableFuture(Callable<T> task) {
      this.task = task;
   }

   /**
    * Runs the task associated with this future. When the task completes, the future completes with
    * its result or cause of failure. The task will run at most once. If the task is already
    * running or has already completed or if this future has been cancelled, the method will return
    * immediately.
    */
   @Override
   public void run() {
      synchronized (runner) {
         if (isDone() || runner.get() != null) {
            // already run or cancelled
            return;
         }
         runner.set(Thread.currentThread());
      }
      try {
         setValue(task.call());
      } catch (Throwable th) {
         setFailure(th);
      } finally {
         synchronized (runner) {
            runner.set(null);
         }
      }
   }
   
   /**
    * Interrupts the thread running the task that this future represents, if it is currently
    * running. If it is not running, this does nothing.
    */
   @Override
   protected void interrupt() {
      synchronized (runner) {
         Thread thread = runner.get();
         if (thread != null) {
            thread.interrupt();
         }
      }
   }
}
