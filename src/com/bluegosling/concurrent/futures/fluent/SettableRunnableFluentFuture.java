package com.bluegosling.concurrent.futures.fluent;

import com.bluegosling.concurrent.unsafe.UnsafeReferenceFieldUpdater;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * A {@link RunnableFluentFuture runnable} future that is also settable. Setting the value or
 * cause of failure before execution of the associated task begins will prevent execution, much like
 * cancelling other forms of runnable futures prevents task execution. Once the task runs and the
 * value, or cause of failure, is set, subsequent attempts to set the value or cause of failure will
 * not succeed.
 * 
 * <p>Unlike its superclass, {@link SettableFluentFuture}, canceling this kind of future can interrupt a
 * running task. 
 *
 * @param <T> the type of future value
 * @see SettableFluentFuture#asRunnableFuture
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class SettableRunnableFluentFuture<T> extends SettableFluentFuture<T>
      implements RunnableFluentFuture<T> {

   @SuppressWarnings("rawtypes")
   public static final UnsafeReferenceFieldUpdater<SettableRunnableFluentFuture, Thread> runnerUpdater =
         new UnsafeReferenceFieldUpdater<>(SettableRunnableFluentFuture.class, Thread.class, "runner");
   
   private final Callable<T> task;
   private volatile Thread runner;
   
   /**
    * Constructs a new future that will run the given task. If the task completes successfully,
    * the future will complete with a {@code null} value.
    *
    * @param task the task to run
    */
   public SettableRunnableFluentFuture(Runnable task) {
      this(task, null);
   }

   /**
    * Constructs a new future that will run the given task. If the task completes successfully,
    * the future will complete with the specified value.
    *
    * @param task the task to run
    * @param result the value of the future upon successful completion of the task
    */
   public SettableRunnableFluentFuture(Runnable task, T result) {
      this(Executors.callable(task, result));
   }
   
   /**
    * Constructs a new future that will run the given computation.
    *
    * @param task the computation to run, the result of which will be future's result upon
    *       successful completion
    */
   public SettableRunnableFluentFuture(Callable<T> task) {
      if (task == null) {
         throw new NullPointerException();
      }
      this.task = task;
   }
   
   @Override
   public void run() {
      if (isDone() || !runnerUpdater.compareAndSet(this, null, Thread.currentThread())) {
         return;
      }
      try {
         setValue(task.call());
      } catch (Throwable t) {
         setFailure(t);
      } finally {
         runner = null;
      }
   }
   
   @Override
   protected void interrupt() {
      Thread th = runner;
      if (th != null) {
         th.interrupt();
      }
   }
}
