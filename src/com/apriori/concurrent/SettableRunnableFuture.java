package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

// TODO: javadoc
// TODO: tests
public class SettableRunnableFuture<T> extends SettableFuture<T>
      implements RunnableListenableFuture<T> {

   @SuppressWarnings("rawtypes")
   public static final AtomicReferenceFieldUpdater<SettableRunnableFuture, Thread> runnerUpdater =
         AtomicReferenceFieldUpdater.newUpdater(SettableRunnableFuture.class, Thread.class,
               "runner");
   
   private final Callable<T> task;
   private volatile Thread runner;
   
   public SettableRunnableFuture(Runnable task) {
      this(task, null);
   }
   
   public SettableRunnableFuture(Runnable task, T result) {
      this(Executors.callable(task, result));
   }
   
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
