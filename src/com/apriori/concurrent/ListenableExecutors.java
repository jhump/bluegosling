package com.apriori.concurrent;

import static com.apriori.concurrent.ListenableFutures.completedFuture;
import static com.apriori.concurrent.ListenableFutures.failedFuture;

import com.apriori.collections.TransformingList;

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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory classes for creating instances of {@link ListenableExecutorService}. Most of the methods
 * here decorate normal {@link ExecutorService} implementations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public class ListenableExecutors {

   /**
    * A simple executor that runs each task synchronously in the same thread that submits it.
    */
   private static final Executor SAME_THREAD_EXECUTOR = new Executor() {
      @Override
      public void execute(Runnable command) {
         try {
            command.run();
         } catch (Throwable t) {
            if (t instanceof InterruptedException) {
               Thread.currentThread().interrupt();
            }
            
            try {
               Thread.currentThread().getUncaughtExceptionHandler()
                     .uncaughtException(Thread.currentThread(), t);
            } catch (Exception e) {
               // TODO: log?
            }
         }
      }
   };
   
   /**
    * Returns an executor that runs each task synchronously in the same thread that submits it.
    *
    * @return an executor that runs tasks immediately in the current thread
    */
   public static Executor sameThreadExecutor() {
      return SAME_THREAD_EXECUTOR;
   }
   
   /**
    * Returns a new executor service that runs each task synchronously in the same thread that
    * submits it. Submissions that return futures will always return completed futures.
    *
    * @return a new executor service that runs tasks immediately in the current thread
    */
   @SuppressWarnings("synthetic-access")
   public static ListenableExecutorService sameThreadExecutorService() {
      return new SameThreadExecutorService();
   }
   
   /**
    * Converts the specified service into a {@link ListenableExecutorService}. If the specified
    * service <em>is</em> already listenable, it is returned without any conversion.
    * 
    * @param executor the executor service
    * @return a listenable version of the specified service
    */
   public static ListenableExecutorService makeListenable(ExecutorService executor) {
      if (executor instanceof ListenableExecutorService) {
         return (ListenableExecutorService) executor;
      }
      return new ListenableExecutorServiceWrapper(executor);
   }

   /**
    * Converts the specified service into a {@link ListenableScheduledExecutorService}. If the
    * specified service <em>is</em> already listenable, it is returned without any conversion.
    * 
    * @param executor the scheduled executor service
    * @return a listenable version of the specified service
    */
   public static ListenableScheduledExecutorService makeListenable(
         ScheduledExecutorService executor) {
      if (executor instanceof ListenableScheduledExecutorService) {
         return (ListenableScheduledExecutorService) executor;
      }
      return new ListenableScheduledExecutorServiceWrapper(executor);
   }
   
   /**
    * Converts the specified service into a {@link ListenableScheduledExecutorService}, adding
    * scheduling capability if needed. If the specified service <em>is</em> already listenable, it
    * is returned without any conversion.
    * 
    * <p>If the specified service does not already support scheduling, this will return a new
    * service that adds that capability at the expense of adding one "scheduler" thread. (The thread
    * won't actually be started until the first scheduled task is submitted. Other tasks, submitted
    * for immediate execution, do not cause the thread to be created.) When
    * 
    * <p>The specified service should not be used other than through the returned wrapper. If it is,
    * it is possible for tasks to be accepted when the executor should be shutdown and vice versa,
    * for tasks to be rejected when the executor should still be active.
    * 
    * @param executor the executor service
    * @param waitForScheduledTasksOnShutdown if adding scheduling capability and this flag is true,
    *       the returned service will not terminate until after shutdown until all scheduled tasks
    *       have completed; if adding scheduling capability and this flag is false, the service will
    *       cancel pending scheduled tasks on shutdown; if not adding scheduling capability, this
    *       flag is ignored
    * @return a listenable version of the specified service that also provides scheduling functions
    */
   public static ListenableScheduledExecutorService makeListenableScheduled(
         ExecutorService executor, boolean waitForScheduledTasksOnShutdown) {
      if (executor instanceof ListenableScheduledExecutorService) {
         return (ListenableScheduledExecutorService) executor;
      } else if (executor instanceof ScheduledExecutorService) {
         return new ListenableScheduledExecutorServiceWrapper((ScheduledExecutorService) executor);
      }
      return new ListenableScheduledExecutorServiceSchedulingWrapper(executor,
            waitForScheduledTasksOnShutdown);
   }
   
   /**
    * Converts the specified service into a {@link ListenableScheduledExecutorService}, adding
    * scheduling capability if needed. If the specified service <em>is</em> already listenable, it
    * is returned without any conversion.
    * 
    * <p>If scheduling functionality is added, any scheduled tasks that are pending when the
    * executor is shutdown will be canceled.
    * 
    * @param executor the executor service
    * @return a listenable version of the specified service that also provides scheduling functions
    * 
    * @see #makeListenableScheduled(ExecutorService, boolean)
    */
   public static ListenableScheduledExecutorService makeListenableScheduled(
         ExecutorService executor) {
      return makeListenableScheduled(executor, false);
   }
   
   /**
    * A {@link ListenableExecutorService} that executes submitted tasks sychronously on the same
    * thread as the one that submits them.
    */
   private static class SameThreadExecutorService implements ListenableExecutorService {
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

      @SuppressWarnings("synthetic-access")
      @Override
      public void execute(Runnable command) {
         SAME_THREAD_EXECUTOR.execute(command);
      }

      @Override
      public <T> ListenableFuture<T> submit(Callable<T> task) {
         try {
            return completedFuture(task.call());
         } catch (Throwable th) {
            return failedFuture(th);
         }
      }

      @Override
      public <T> ListenableFuture<T> submit(Runnable task, T result) {
         try {
            task.run();
            return completedFuture(result);
         } catch (Throwable th) {
            return failedFuture(th);
         }
      }

      @Override
      public ListenableFuture<Void> submit(Runnable task) {
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
               results.add(ListenableFutures.<T>cancelledFuture());
               done = true;
            } else {
               results.add(submit(callable));
            }
         }
         return Collections.unmodifiableList(results);
      }
   }

   /**
    * Decorates a normal {@link ExecutorService} with the {@link ListenableExecutorService}
    * interface.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ListenableExecutorServiceWrapper implements ListenableExecutorService {
      private final ExecutorService delegate;
      
      ListenableExecutorServiceWrapper(ExecutorService delegate) {
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
      public <T> ListenableFuture<T> submit(Callable<T> task) {
         ListenableFutureTask<T> t = new ListenableFutureTask<T>(task);
         delegate.execute(t);
         return t;
      }

      @Override
      public <T> ListenableFuture<T> submit(Runnable task, T result) {
         ListenableFutureTask<T> t = new ListenableFutureTask<T>(task, result);
         delegate.execute(t);
         return t;
      }

      @Override
      public ListenableFuture<Void> submit(Runnable task) {
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
            } catch (CancellationException ignore) {
            } catch (ExecutionException ignore) {
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
            } catch (CancellationException ignore) {
            } catch (ExecutionException ignore) {
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
    * ListenableScheduledExecutorService} interface.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ListenableScheduledExecutorServiceWrapper
         extends ListenableExecutorServiceWrapper implements ListenableScheduledExecutorService {

      private final WeakHashMap<Future<?>, Runnable> wrappers =
            new WeakHashMap<Future<?>, Runnable>();
      
      ListenableScheduledExecutorServiceWrapper(ScheduledExecutorService delegate) {
         super(delegate);
      }

      @Override
      protected ScheduledExecutorService delegate() {
         return (ScheduledExecutorService) super.delegate();
      }

      @Override
      public <T> ListenableFuture<T> submit(Callable<T> task) {
         ListenableFutureTask<T> t = new ListenableFutureTask<T>(task);
         wrappers.put(delegate().submit(t), t);
         return t;
      }

      @Override
      public <T> ListenableFuture<T> submit(Runnable task, T result) {
         ListenableFutureTask<T> t = new ListenableFutureTask<T>(task, result);
         wrappers.put(delegate().submit(t), t);
         return t;
      }
      
      @Override
      public ListenableScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(delay);
         ListenableScheduledFutureTask<Void> t =
               new ListenableScheduledFutureTask<Void>(command, null, scheduledNanoTime);
         wrappers.put(delegate().schedule(t, delay, unit), t);
         return t;
      }

      @Override
      public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay,
            TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(delay);
         ListenableScheduledFutureTask<V> t =
               new ListenableScheduledFutureTask<V>(callable, scheduledNanoTime);
         wrappers.put(delegate().schedule(t, delay, unit), t);
         return t;
      }

      private void addReschedulingListener(final ListenableRepeatingFutureTask<?> future) {
         future.addListenerForEachInstance(new FutureListener<Object>() {
            @SuppressWarnings("synthetic-access") // wrappers member is private
            @Override
            public void onCompletion(ListenableFuture<? extends Object> completedFuture) {
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
         }, sameThreadExecutor());
      }
      
      @Override
      public ListenableRepeatingFuture<Void> scheduleAtFixedRate(Runnable command,
            long initialDelay, long period, TimeUnit unit) {
         return schedulePeriodic(command, initialDelay, unit,
               Reschedulers.atFixedRate(period, unit));
      }

      @Override
      public ListenableRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command,
            long initialDelay, long delay, TimeUnit unit) {
         return schedulePeriodic(command, initialDelay, unit,
               Reschedulers.withFixedDelay(delay, unit));
      }
      
      private ListenableRepeatingFuture<Void> schedulePeriodic(Runnable command,
            long initialDelay, TimeUnit unit, Rescheduler<? super Void> rescheduler) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(initialDelay);
         ListenableRepeatingFutureTask<Void> future =
               new ListenableRepeatingFutureTask<Void>(command, null, scheduledNanoTime,
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
    * Decorates a normal {@link ExecutorService} with the {@link ListenableScheduledExecutorService}
    * interface. In addition to any threads the specified service has in its pool, this wrapper
    * creates one other thread to serve as the scheduler. This other thread pulls scheduled tasks
    * from a {@link DelayQueue} and then submits them to the underlying (non-scheduled) service.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ListenableScheduledExecutorServiceSchedulingWrapper
         extends ListenableExecutorServiceWrapper implements ListenableScheduledExecutorService {

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
      
      ListenableScheduledExecutorServiceSchedulingWrapper(ExecutorService delegate,
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
      public ListenableScheduledFuture<Void> schedule(Runnable command, long delay, TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(delay);
         ListenableScheduledFutureTask<Void> future =
               new ListenableScheduledFutureTask<Void>(command, null, scheduledNanoTime);
         delayedExecute(new FutureStampedTask(future));
         return future;
      }

      @Override
      public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay,
            TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(delay);
         ListenableScheduledFutureTask<V> future =
               new ListenableScheduledFutureTask<V>(callable, scheduledNanoTime);
         delayedExecute(new FutureStampedTask(future));
         return future;
      }
      
      private void addReschedulingListener(final ListenableRepeatingFutureTask<?> future) {
         future.addListenerForEachInstance(new FutureListener<Object>() {
            @SuppressWarnings("synthetic-access") // invokes private delayedExecute
            @Override
            public void onCompletion(ListenableFuture<? extends Object> completedFuture) {
               if (!future.isDone()) {
                  synchronized (ListenableScheduledExecutorServiceSchedulingWrapper.this) {
                     if (isShutdown()) {
                        future.cancel(false);
                     } else {
                        delayedExecute(new FutureStampedTask(future));
                     }
                  }
               }
            }
         }, sameThreadExecutor());
      }

      @Override
      public ListenableRepeatingFuture<Void> scheduleAtFixedRate(Runnable command,
            long initialDelay, long period, TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(initialDelay);
         ListenableRepeatingFutureTask<Void> future =
               new ListenableRepeatingFutureTask<Void>(command, null, scheduledNanoTime,
                     Reschedulers.atFixedRate(period, unit));
         addReschedulingListener(future);
         delayedExecute(new FutureStampedTask(future));
         return future;
      }

      @Override
      public ListenableRepeatingFuture<Void> scheduleWithFixedDelay(Runnable command,
            long initialDelay, long delay, TimeUnit unit) {
         long scheduledNanoTime = System.nanoTime() + unit.toNanos(initialDelay);
         ListenableRepeatingFutureTask<Void> future =
               new ListenableRepeatingFutureTask<Void>(command, null, scheduledNanoTime,
                     Reschedulers.withFixedDelay(delay, unit));
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
         private final ListenableScheduledFutureTask<?> wrapped;
         
         @SuppressWarnings("synthetic-access") // super-class ctor is private
         FutureStampedTask(ListenableScheduledFutureTask<?> wrapped) {
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
