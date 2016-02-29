package com.bluegosling.concurrent.futures;

import com.bluegosling.vars.Variable;

import java.util.concurrent.Callable;

/**
 * A completable future that represents a task. The future completes when the task is executed and
 * completes. The future completes with the result of the task or with the cause of its failure.
 *
 * @param <T> the type of the future value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class RunnableCompletableFuture<T> extends InterruptibleCompletableFuture<T>
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
   public RunnableCompletableFuture(Callable<T> task) {
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
         complete(task.call());
      } catch (Throwable th) {
         completeExceptionally(th);
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
