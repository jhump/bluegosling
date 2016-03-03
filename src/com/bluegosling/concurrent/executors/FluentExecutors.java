package com.bluegosling.concurrent.executors;

import static com.bluegosling.concurrent.futures.fluent.FluentFuture.cancelledFuture;
import static com.bluegosling.concurrent.futures.fluent.FluentFuture.completedFuture;
import static com.bluegosling.concurrent.futures.fluent.FluentFuture.failedFuture;

import com.bluegosling.collections.views.TransformingList;
import com.bluegosling.concurrent.FutureListener;
import com.bluegosling.concurrent.Scheduled;
import com.bluegosling.concurrent.futures.fluent.FluentFuture;
import com.bluegosling.concurrent.futures.fluent.FluentFutureTask;
import com.bluegosling.concurrent.futures.fluent.FluentRepeatingFuture;
import com.bluegosling.concurrent.futures.fluent.FluentRepeatingFutureTask;
import com.bluegosling.concurrent.futures.fluent.FluentScheduledFuture;
import com.bluegosling.concurrent.futures.fluent.FluentScheduledFutureTask;
import com.bluegosling.concurrent.scheduler.Rescheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory classes for creating instances of {@link FluentExecutorService}. Most of the methods
 * here decorate normal {@link ExecutorService} implementations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
final class FluentExecutors {
   private FluentExecutors() {
   }

   /**
    * A {@link FluentExecutorService} that executes submitted tasks synchronously on the same
    * thread as the one that submits them.
    */
   static class SameThreadExecutorService implements FluentExecutorService {
      private volatile boolean shutdown;
      
      @Override
      public void shutdown() {
         shutdown = true;
      }

      @Override
      public List<Runnable> shutdownNow() {
         shutdown = true;
         return Collections.emptyList();
      }

      @Override
      public boolean isShutdown() {
         return shutdown;
      }

      @Override
      public boolean isTerminated() {
         return shutdown;
      }

      @Override
      public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
         return true;
      }

      @Override
      public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException {
         Throwable failure = null;
         for (Callable<T> callable : tasks) {
            try {
               return callable.call();
            } catch (Throwable th) {
               failure = th;
            }
         }
         throw new ExecutionException(failure);
      }

      @Override
      public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws ExecutionException, TimeoutException {
         long deadline = System.nanoTime() + unit.toNanos(timeout);
         Throwable failure = null;
         for (Callable<T> callable : tasks) {
            if (System.nanoTime() >= deadline) {
               throw new TimeoutException();
            }
            try {
               return callable.call();
            } catch (Throwable th) {
               failure = th;
            }
         }
         throw new ExecutionException(failure);
      }

      @Override
      public void execute(Runnable command) {
         SameThreadExecutor.get().execute(command);
      }

      @Override
      public <T> FluentFuture<T> submit(Callable<T> task) {
         try {
            return completedFuture(task.call());
         } catch (Throwable th) {
            return failedFuture(th);
         }
      }

      @Override
      public <T> FluentFuture<T> submit(Runnable task, T result) {
         try {
            task.run();
            return completedFuture(result);
         } catch (Throwable th) {
            return failedFuture(th);
         }
      }

      @Override
      public FluentFuture<Void> submit(Runnable task) {
         try {
            task.run();
            return completedFuture(null);
         } catch (Throwable th) {
            return failedFuture(th);
         }
      }

      @Override
      public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
         ArrayList<Future<T>> results = new ArrayList<Future<T>>(tasks.size());
         for (Callable<T> callable : tasks) {
            results.add(submit(callable));
         }
         return Collections.unmodifiableList(results);
      }

      @Override
      public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit) {
         ArrayList<Future<T>> results = new ArrayList<Future<T>>(tasks.size());
         long deadline = System.nanoTime() + unit.toNanos(timeout);
         boolean done = false;
         for (Callable<T> callable : tasks) {
            if (done || System.nanoTime() >= deadline) {
               results.add(cancelledFuture());
               done = true;
            } else {
               results.add(submit(callable));
            }
         }
         return Collections.unmodifiableList(results);
      }
   }

   /**
    * Decorates a normal {@link ExecutorService} with the {@link FluentExecutorService}
    * interface.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FluentExecutorServiceWrapper implements FluentExecutorService {
      private final ExecutorService delegate;
      
      FluentExecutorServiceWrapper(ExecutorService delegate) {
         this.delegate = delegate;
      }
      
      protected ExecutorService delegate() {
         return delegate;
      }

      @Override
      public void execute(Runnable command) {
         delegate.execute(command);
      }

      @Override
      public void shutdown() {
         delegate.shutdown();
      }

      @Override
      public List<Runnable> shutdownNow() {
         return delegate.shutdownNow();
      }

      @Override
      public boolean isShutdown() {
         return delegate.isShutdown();
      }

      @Override
      public boolean isTerminated() {
         return delegate.isTerminated();
      }

      @Override
      public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
         return delegate.awaitTermination(timeout, unit);
      }

      @Override
      public <T> FluentFuture<T> submit(Callable<T> task) {
         FluentFutureTask<T> t = new FluentFutureTask<T>(task);
         delegate.execute(t);
         return t;
      }

      @Override
      public <T> FluentFuture<T> submit(Runnable task, T result) {
         FluentFutureTask<T> t = new FluentFutureTask<T>(task, result);
         delegate.execute(t);
         return t;
      }

      @Override
      public FluentFuture<Void> submit(Runnable task) {
         return submit(task, null);
      }

      @Override
      public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
         List<Future<T>> results = new ArrayList<Future<T>>(tasks.size());
         for (Callable<T> task : tasks) {
            results.add(submit(task));
         }
         for (Future<T> future : results) {
            try {
               future.get();
            } catch (CancellationException | ExecutionException ignore) {
            }
         }
         return results;
      }

      @Override
      public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException {
         long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
         List<Future<T>> results = new ArrayList<Future<T>>(tasks.size());
         for (Callable<T> task : tasks) {
            results.add(submit(task));
         }
         boolean timedOut = false;
         for (Future<T> future : results) {
            try {
               if (timedOut) {
                  future.cancel(true);
               } else {
                  long nanosLeft = deadlineNanos - System.nanoTime();
                  future.get(nanosLeft, TimeUnit.NANOSECONDS);
               }
            } catch (TimeoutException e) {
               future.cancel(true);
               timedOut = true;
            } catch (CancellationException | ExecutionException ignore) {
            }
         }
         return results;
      }

      @Override
      public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
            ExecutionException {
         return delegate.invokeAny(tasks);
      }

      @Override
      public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
         return delegate.invokeAny(tasks, timeout, unit);
      }
   }
   
   /**
    * Decorates a normal {@link ScheduledExecutorService} with the {@link
    * FluentScheduledExecutorService} interface.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FluentScheduledExecutorServiceWrapper
         extends FluentExecutorServiceWrapper implements FluentScheduledExecutorService {

      private final WeakHashMap<Future<?>, Runnable> wrappers =
            new WeakHashMap<Future<?>, Runnable>();
      
      FluentScheduledExecutorServiceWrapper(ScheduledExecutorService delegate) {
         super(delegate);
      }

      @Override
      protected ScheduledExecutorService delegate() {
         return (ScheduledExecutorService) super.delegate();
      }

      @Override
      public <T> FluentFuture<T> submit(Callable<T> task) {
         FluentFutureTask<T> t = new FluentFutureTask<T>(task);
         wrappers.put(delegate().submit(t), t);
         return t;
      }

      @Override
      public <T> FluentFuture<T> submit(Runnable task, T result) {
         FluentFutureTask<T> t = new FluentFutureTask<T>(task, result);
         wrappers.put(delegate().submit(t), t);
         return t;
      }
      
      @Override
      public FluentScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(delay);
         FluentScheduledFutureTask<Void> t =
               new FluentScheduledFutureTask<Void>(command, null, scheduledNanoTime);
         wrappers.put(delegate().schedule(t, delay, unit), t);
         return t;
      }

      @Override
      public <V> FluentScheduledFuture<V> schedule(Callable<V> callable, long delay,
            TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(delay);
         FluentScheduledFutureTask<V> t =
               new FluentScheduledFutureTask<V>(callable, scheduledNanoTime);
         wrappers.put(delegate().schedule(t, delay, unit), t);
         return t;
      }

      private void addReschedulingListener(final FluentRepeatingFutureTask<?> future) {
         future.addListenerForEachInstance(new FutureListener<Object>() {
            @SuppressWarnings("synthetic-access") // wrappers member is private
            @Override
            public void onCompletion(FluentFuture<? extends Object> completedFuture) {
               if (!future.isDone()) {
                  if (isShutdown()) {
                     future.cancel(false);
                  } else {
                     long newScheduledTime = future.getScheduledNanoTime();
                     try {
                        wrappers.put(delegate().schedule(future,
                              newScheduledTime - System.nanoTime(), TimeUnit.NANOSECONDS), future);
                     } catch (RejectedExecutionException e) {
                        future.cancel(false);
                     }
                  }
               }
            }
         }, SameThreadExecutor.get());
      }
      
      @Override
      public FluentRepeatingFuture<Void> scheduleAtFixedRate(Runnable command,
            long initialDelay, long period, TimeUnit unit) {
         return schedulePeriodic(command, initialDelay, unit,
               Rescheduler.atFixedRate(period, unit));
      }

      @Override
      public FluentRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command,
            long initialDelay, long delay, TimeUnit unit) {
         return schedulePeriodic(command, initialDelay, unit,
               Rescheduler.withFixedDelay(delay, unit));
      }
      
      private FluentRepeatingFuture<Void> schedulePeriodic(Runnable command,
            long initialDelay, TimeUnit unit, Rescheduler<? super Void> rescheduler) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(initialDelay);
         FluentRepeatingFutureTask<Void> future =
               new FluentRepeatingFutureTask<Void>(command, null, scheduledNanoTime,
                     rescheduler);
         addReschedulingListener(future);
         wrappers.put(delegate().schedule(future, initialDelay, unit), future);
         return future;
      }
      
      @Override
      public void execute(Runnable r) {
         wrappers.put(delegate().submit(r), r);
      }
      
      @Override
      public List<Runnable> shutdownNow() {
         List<Runnable> tasks = delegate().shutdownNow();
         return new TransformingList.ReadOnly<Runnable, Runnable>(tasks,
               (input) -> {
                  Runnable unwrapped = wrappers.get(input);
                  return unwrapped != null ? unwrapped : input;
               });
      }
   }
   
   /**
    * Decorates a normal {@link ExecutorService} with the {@link FluentScheduledExecutorService}
    * interface. In addition to any threads the specified service has in its pool, this wrapper
    * creates one other thread to serve as the scheduler. This other thread pulls scheduled tasks
    * from a {@link DelayQueue} and then submits them to the underlying (non-scheduled) service.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FluentScheduledExecutorServiceSchedulingWrapper
         extends FluentExecutorServiceWrapper implements FluentScheduledExecutorService {

      private final boolean waitForScheduledTasksOnShutdown;
      private final AtomicReference<Thread> scheduler = new AtomicReference<Thread>();
      private final DelayQueue<AbstractStampedTask> scheduleQueue =
            new DelayQueue<AbstractStampedTask>();
      private final Runnable shutdownTask = new Runnable() {
         @Override public void run() {
            delegate().shutdown();
         }
      };
      private boolean isShutdown;
      
      FluentScheduledExecutorServiceSchedulingWrapper(ExecutorService delegate,
            boolean waitForScheduledTasksOnShutdown) {
         super(delegate);
         this.waitForScheduledTasksOnShutdown = waitForScheduledTasksOnShutdown;
      }
      
      private void reject() {
         throw new RejectedExecutionException();
      }

      private void startSchedulerThread() {
         Thread newScheduler = new Thread() {
            @SuppressWarnings("synthetic-access") // accesses private members of enclosing class
            @Override public void run() {
               while (true) {
                  try {
                     AbstractStampedTask task = scheduleQueue.take();
                     doExecute(task);
                     if (task == shutdownTask) {
                        return;
                     }
                  }
                  catch (InterruptedException e) {
                     // ignore
                  }
               }
            }
         };
         // we don't bother starting thread if we lost race to set the scheduler
         if (scheduler.compareAndSet(null, newScheduler)) {
            newScheduler.start();
         }
      }
      
      private synchronized void delayedExecute(AbstractStampedTask task) {
         if (isShutdown()) {
            reject();
         } else {
            if (scheduler.get() == null) {
               startSchedulerThread();
            }
            scheduleQueue.add(task);
         }
      }
      
      @Override
      public synchronized void execute(Runnable task) {
         if (task instanceof Delayed) {
            if (task instanceof Scheduled) {
               delayedExecute(
                     new ScheduledStampedTask(((Scheduled) task).getScheduledNanoTime(), task));
            } else {
               delayedExecute(
                     new ScheduledStampedTask(((Delayed) task).getDelay(TimeUnit.NANOSECONDS)
                           + System.nanoTime(), task));
            }
            return;
         }
         
         if (isShutdown()) {
            reject();
         } else {
            doExecute(task);
         }
      }
      
      private void doExecute(Runnable task) {
         delegate().execute(task);
      }
      
      @Override
      public FluentScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(delay);
         FluentScheduledFutureTask<Void> future =
               new FluentScheduledFutureTask<Void>(command, null, scheduledNanoTime);
         delayedExecute(new FutureStampedTask(future));
         return future;
      }

      @Override
      public <V> FluentScheduledFuture<V> schedule(Callable<V> callable, long delay,
            TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(delay);
         FluentScheduledFutureTask<V> future =
               new FluentScheduledFutureTask<V>(callable, scheduledNanoTime);
         delayedExecute(new FutureStampedTask(future));
         return future;
      }
      
      private void addReschedulingListener(final FluentRepeatingFutureTask<?> future) {
         future.addListenerForEachInstance(new FutureListener<Object>() {
            @SuppressWarnings("synthetic-access") // invokes private delayedExecute
            @Override
            public void onCompletion(FluentFuture<? extends Object> completedFuture) {
               if (!future.isDone()) {
                  synchronized (FluentScheduledExecutorServiceSchedulingWrapper.this) {
                     if (isShutdown()) {
                        future.cancel(false);
                     } else {
                        delayedExecute(new FutureStampedTask(future));
                     }
                  }
               }
            }
         }, SameThreadExecutor.get());
      }

      @Override
      public FluentRepeatingFuture<Void> scheduleAtFixedRate(Runnable command,
            long initialDelay, long period, TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(initialDelay);
         FluentRepeatingFutureTask<Void> future =
               new FluentRepeatingFutureTask<Void>(command, null, scheduledNanoTime,
                     Rescheduler.atFixedRate(period, unit));
         addReschedulingListener(future);
         delayedExecute(new FutureStampedTask(future));
         return future;
      }

      @Override
      public FluentRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command,
            long initialDelay, long delay, TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(initialDelay);
         FluentRepeatingFutureTask<Void> future =
               new FluentRepeatingFutureTask<Void>(command, null, scheduledNanoTime,
                     Rescheduler.withFixedDelay(delay, unit));
         addReschedulingListener(future);
         delayedExecute(new FutureStampedTask(future));
         return future;
      }
      
      private void stopSchedulerThread() {
         if (!isShutdown()) {
            // should never happen, but we want to throw in case it ever does
            throw new IllegalStateException();
         }
         
         Thread schedulerThread = scheduler.get();
         if (schedulerThread == null || !schedulerThread.isAlive()) {
            // no thread in need of stopping
            return;
         }
         
         Scheduled max = null;
         for (Scheduled s : scheduleQueue) {
            if (max == null || max.compareTo(s) < 0) {
               max = s;
            }
         }
         long scheduledNanoTime = max == null ? System.nanoTime() // immediately
               : max.getScheduledNanoTime() + 1; // schedule after other tasks are initiated
         scheduleQueue.add(new ScheduledStampedTask(scheduledNanoTime, shutdownTask));
      }

      private List<Runnable> drainDelayQueue() {
         List<Runnable> tasks = new ArrayList<Runnable>(scheduleQueue.size());
         scheduleQueue.drainTo(tasks);
         // DelayQueue only drains tasks whose delay has expired, so now we need to use Iterator
         // to extract all of the other items from it
         while (!scheduleQueue.isEmpty()) {
            try {
               Iterator<? extends Runnable> iter = scheduleQueue.iterator();
               if (iter.hasNext()) {
                  Runnable r = iter.next();
                  if (scheduleQueue.remove(r)) {
                     tasks.add(r);
                  }
               }
            } catch (ConcurrentModificationException ignore) {
            }
         }
         for (ListIterator<Runnable> listIter = tasks.listIterator(); listIter.hasNext();) {
            // every item from the scheduleQueue is an AbstractStampedTask
            AbstractStampedTask task = (AbstractStampedTask) listIter.next();
            Runnable r = task.unwrap();
            if (r == shutdownTask) {
               // this isn't a user task, so we can filter it out
               listIter.remove();
            } else {
               if (r instanceof Future) {
                  ((Future<?>) r).cancel(false);
               }
               // return unwrapped tasks
               listIter.set(r);
            }
         }
         return tasks;
      }
      
      @Override
      public void shutdown() {
         if (setShutdown()) {
            if (waitForScheduledTasksOnShutdown) {
               // schedules shutdown of delegate after existing scheduled tasks run
               stopSchedulerThread();
            } else {
               // cancel everything that is scheduled but not yet submitted
               drainDelayQueue();
               stopSchedulerThread();
               delegate().shutdown();
            }
         }
      }
      
      @Override
      public List<Runnable> shutdownNow() {
         // no need to look at this method's return value since, even if already shutdown, we need
         // to proceed just in case this call to shutdownNow() follows a call to shutdown()
         setShutdown();
         
         List<Runnable> scheduled = drainDelayQueue();
         stopSchedulerThread();
         List<Runnable> unscheduled = delegate().shutdownNow();
         ArrayList<Runnable> results =
               new ArrayList<Runnable>(scheduled.size() + unscheduled.size());
         results.addAll(scheduled);
         results.addAll(unscheduled);
         return results;
      }

      private synchronized boolean setShutdown() {
         if (isShutdown) {
            return false;
         }
         return isShutdown = true;
      }
      
      @Override
      public synchronized boolean isShutdown() {
         return isShutdown;
      }
      
      private abstract static class AbstractStampedTask implements Scheduled, Runnable {
         private static final AtomicLong stampSequence = new AtomicLong();
         
         private final long stamp = stampSequence.incrementAndGet();
         
         @Override
         public int compareTo(Delayed o) {
            int result = Scheduled.COMPARATOR.compare(this,  o);
            if (result == 0 && o instanceof AbstractStampedTask) {
               long otherStamp = ((AbstractStampedTask) o).stamp;
               result = stamp > otherStamp ? 1 : (stamp < otherStamp ? -1 : 0); 
            }
            return result;
         }
         
         public abstract Runnable unwrap();
      }
      
      private static class FutureStampedTask extends AbstractStampedTask {
         private final FluentScheduledFutureTask<?> wrapped;
         
         @SuppressWarnings("synthetic-access") // super-class ctor is private
         FutureStampedTask(FluentScheduledFutureTask<?> wrapped) {
            this.wrapped = wrapped;
         }

         @Override
         public long getScheduledNanoTime() {
            return wrapped.getScheduledNanoTime();
         }

         @Override
         public long getDelay(TimeUnit unit) {
            return wrapped.getDelay(unit);
         }

         @Override
         public void run() {
            wrapped.run();
         }

         @Override
         public Runnable unwrap() {
            return wrapped;
         }
      }

      private static class ScheduledStampedTask extends AbstractStampedTask {
         private final long scheduledNanoTime;
         private final Runnable task;
         
         @SuppressWarnings("synthetic-access") // super-class ctor is private
         ScheduledStampedTask(long scheduledNanoTime, Runnable task) {
            this.scheduledNanoTime = scheduledNanoTime;
            this.task = task;
         }
         
         @Override
         public long getScheduledNanoTime() {
            return scheduledNanoTime;
         }

         @Override
         public long getDelay(TimeUnit unit) {
            return unit.convert(scheduledNanoTime - System.nanoTime(), TimeUnit.NANOSECONDS);
         }

         @Override
         public void run() {
            task.run();
         }

         @Override
         public Runnable unwrap() {
            return task;
         }
      }
   }
}
