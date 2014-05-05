package com.apriori.concurrent.scheduler;

import com.apriori.concurrent.FutureListener;
import com.apriori.concurrent.FutureVisitor;
import com.apriori.concurrent.ListenableFuture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link RepeatingScheduledTask} that is backed by a
 * {@link ScheduledTaskDefinition}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned by each occurrence of the task
 */
class RepeatingScheduledTaskImpl<V> implements RepeatingScheduledTask<V> {
   
   private final ScheduledTaskDefinition<V> taskDef;
   private final AtomicReference<CountDownLatch> awaitLatch =
         new AtomicReference<CountDownLatch>();
   
   RepeatingScheduledTaskImpl(ScheduledTaskDefinition<V> taskDef) {
      this.taskDef = taskDef;
   }
   
   /**
    * Gets a latch that will open when this repeating task is finished.
    * 
    * @return a latch that will open when this repeating task is finished
    */
   private CountDownLatch getAwaitLatch() {
      CountDownLatch latch = awaitLatch.get();
      if (latch != null) {
         // already set
         return latch;
      }
      latch = new CountDownLatch(1);
      if (!awaitLatch.compareAndSet(null, latch)) {
         // lost race, now already set
         return awaitLatch.get();
      }
      // we just set the latch, so now we need the listener so that it opens at the right time
      final CountDownLatch finalLatch = latch;
      taskDef.addListener(new ScheduledTaskListener<V>() {
         @Override
         public void taskCompleted(ScheduledTask<? extends V> task) {
            if (task.taskDefinition().isFinished()) {
               finalLatch.countDown();
            }
         }
      });
      return latch;
   }
   
   @Override
   public ScheduledTaskDefinition<V> taskDefinition() {
      return taskDef;
   }

   private Delayed getDelayed() {
      Delayed d = taskDef.current();
      if (d == null) {
         d = taskDef.latest();
         if (d == null) {
            throw new IllegalStateException();
         }
      }
      return d;
   }
   
   @Override
   public long getDelay(TimeUnit unit) {
      return getDelayed().getDelay(unit);
   }

   @Override
   public int compareTo(Delayed o) {
      return getDelayed().compareTo(o);
   }

   @Override
   public boolean cancel(boolean mayInterruptIfRunning) {
      return taskDef.cancel(mayInterruptIfRunning);
   }

   @Override
   public boolean isCancelled() {
      return taskDef.isCancelled();
   }

   @Override
   public boolean isDone() {
      return taskDef.isFinished();
   }
   
   @Override
   public V get() throws InterruptedException, ExecutionException {
      await();
      return taskDef.latest().get();
   }

   @Override
   public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
         TimeoutException {
      if (!await(timeout, unit)) {
         throw new TimeoutException();
      }
      return taskDef.latest().get();
   }
   
   @Override
   public V getMostRecentResult() {
      ScheduledTask<V> task = taskDef.latest();
      if (task == null) {
         throw new IllegalStateException();
      }
      return task.getResult();
   }

   @Override
   public void addListenerForEachInstance(final FutureListener<? super V> listener,
         Executor executor) {
      final ListenableFuture<V> self = this; 
      taskDef.addListener(new ScheduledTaskListener<V>() {
         @Override
         public void taskCompleted(ScheduledTask<? extends V> task) {
            listener.onCompletion(self);
         }
      });
   }

   @Override
   public int executionCount() {
      return taskDef.executionCount();
   }
   
   @Override
   public boolean isSuccessful() {
      ScheduledTask<V> task;
      return isDone() && (task = taskDef.latest()) != null && task.isSuccessful();
   }

   @Override
   public V getResult() {
      ScheduledTask<V> task;
      if (!isDone() || (task = taskDef.latest()) == null) {
         throw new IllegalStateException();
      }
      return task.getResult();
   }

   @Override
   public boolean isFailed() {
      ScheduledTask<V> task;
      return isDone() && (task = taskDef.latest()) != null && task.isFailed();
   }

   @Override
   public Throwable getFailure() {
      ScheduledTask<V> task;
      if (!isDone() || (task = taskDef.latest()) == null) {
         throw new IllegalStateException();
      }
      return task.getFailure();
   }

   @Override
   public void addListener(final FutureListener<? super V> listener, Executor executor) {
      final ListenableFuture<V> self = this; 
      taskDef.addListener(new ScheduledTaskListener<V>() {
         @Override
         public void taskCompleted(ScheduledTask<? extends V> task) {
            if (isDone()) {
               listener.onCompletion(self);
            }
         }
      });
   }

   @Override
   public void visit(FutureVisitor<? super V> visitor) {
      if (!isDone()) {
         throw new IllegalStateException();
      }
      if (isCancelled()) {
         visitor.cancelled();
      } else {
         ScheduledTask<V> task = taskDef.latest();
         task.visit(visitor);
      }
   }
   
   @Override
   public void await() throws InterruptedException {
      getAwaitLatch().await();
   }

   @Override
   public boolean await(long limit, TimeUnit unit) throws InterruptedException {
      return getAwaitLatch().await(limit, unit);
   }
}
