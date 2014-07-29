package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A {@link RunnableListenableFuture runnable} future that is also settable. Setting the value or
 * cause of failure before execution of the associated task begins will prevent execution, much like
 * cancelling other forms of runnable futures prevents task execution. Once the task runs and the
 * value, or cause of failure, is set, subsequent attempts to set the value or cause of failure will
 * not succeed.
 * 
 * <p>Unlike its superclass, {@link SettableFuture}, cancelling this kind of future can interrupt a
 * running task. 
 *
 * @param <T> the type of future value
 * @see SettableFuture#asRunnableFuture
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class SettableRunnableFuture<T> extends SettableFuture<T>
      implements RunnableListenableFuture<T> {

   @SuppressWarnings("rawtypes")
   public static final AtomicReferenceFieldUpdater<SettableRunnableFuture, Thread> runnerUpdater =
         AtomicReferenceFieldUpdater.newUpdater(SettableRunnableFuture.class, Thread.class,
               "runner");
   
   private final Callable<T> task;
   private volatile Thread runner;
   
   /**
    * Constructs a new future that will run the given task. If the task completes successfully,
    * the future will complete with a {@code null} value.
    *
    * @param task the task to run
    */
   public SettableRunnableFuture(Runnable task) {
      this(task, null);
   }

   /**
    * Constructs a new future that will run the given task. If the task completes successfully,
    * the future will complete with the specified value.
    *
    * @param task the task to run
    * @param result the value of the future upon successful completion of the task
    */
   public SettableRunnableFuture(Runnable task, T result) {
      this(Executors.callable(task, result));
   }
   
   /**
    * Constructs a new future that will run the given computation.
    *
    * @param task the computation to run, the result of which will be future's result upon
    *       successful completion
    */
   public SettableRunnableFuture(Callable<T> task) {
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
