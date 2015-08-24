package com.apriori.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;

/**
 * An executor service that uses threads from another executor but imposes limits on parallelism
 * and the number of threads used for its tasks.
 * 
 * <p>This allows for creating numerous thread pools with their own limits, but having a separate
 * constraint on the total number of threads, too. For example, you could create four
 * thread-limiting executors for four sub-systems, each allowing up to 20 threads. These executors
 * are created atop a thread pool with a maximum of 50 threads. So each sub-system can try to use
 * up to 20 threads, but we've constrained the total across all four sub-systems to just 50, not 80.
 *
 * <p>Note: since this executor must maintain its own set of queues, independent of the underlying
 * executor's main work queue (for constraining its level of parallelism), it is possible for tasks
 * to be accepted by this executor but then later rejected by the underlying executor. Care should
 * be exercised to coordinate the life cycle of the underlying executor and the sources of task
 * submissions.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
// TODO: can this be made lock-free? or closer to it (e.g. reduce contention over queue)?
// TODO: batching -- let a virtual thread run several sequential tasks before yielding
public class ThreadLimitingExecutorService extends AbstractExecutorService
      implements ListenableExecutorService {

   final Executor executor;
   final int threadLimit;
   final Object lock = new Object(); // guards the next three members
   final Queue<Runnable> queue = new LinkedList<>();
   final Set<VirtualThread> activeThreads = new HashSet<>();
   boolean shutdown;
   
   public ThreadLimitingExecutorService(Executor executor, int threadLimit) {
      this.executor = executor;
      this.threadLimit = threadLimit;
   }

   @Override
   public void shutdown() {
      synchronized (lock) {
         shutdown = true;
      }
   }

   @Override
   public List<Runnable> shutdownNow() {
      List<Runnable> aborted;
      synchronized (lock) {
         shutdown = true;
         aborted = new ArrayList<>(queue);
         queue.clear();
         for (VirtualThread thread : activeThreads) {
            thread.interrupt();
         }
      }
      for (Runnable r : aborted) {
         if (r instanceof Future) {
            ((Future<?>) r).cancel(false);
         } else if (r instanceof Cancellable) {
            ((Cancellable) r).cancel(false);
         }
      }
      return aborted;
   }

   @Override
   public boolean isShutdown() {
      synchronized (lock) {
         return shutdown;
      }
   }

   @Override
   public boolean isTerminated() {
      synchronized (lock) {
         return shutdown && activeThreads.isEmpty();
      }
   }

   @Override
   public boolean awaitTermination(long timeout, TimeUnit unit)
         throws InterruptedException {
      if (timeout < 0) {
         throw new IllegalArgumentException();
      }
      if (unit == null) {
         throw new NullPointerException();
      }
      long deadline = System.nanoTime() + unit.toNanos(timeout);
      synchronized (lock) {
         while (true) {
            if (isTerminated()) {
               return true;
            }
            long nanosLeft = deadline - System.nanoTime();
            if (nanosLeft <= 0) {
               return false;
            }
            long millis = TimeUnit.NANOSECONDS.toMillis(nanosLeft);
            long nanos = nanosLeft - TimeUnit.MILLISECONDS.toNanos(millis);
            // clamp instead of overflow if the time we are trying to wait is too big
            if (millis > Integer.MAX_VALUE) {
               millis = Integer.MAX_VALUE;
            }
            lock.wait((int) millis, (int) nanos);
         }
      }
   }

   @Override
   public void execute(Runnable command) {
      VirtualThread t;
      synchronized (lock) {
         if (shutdown) {
            throw new RejectedExecutionException();
         }
         if (activeThreads.size() == threadLimit) {
            queue.add(command);
            return;
         } else {
            t = new VirtualThread();
            activeThreads.add(t);
         }
      }
      t.run(command);
   }
   
   void removeAndCleanupThread(VirtualThread thread) {
      List<Runnable> outstanding = removeThread(thread);
      // VirtualThreads only remove themselves while queue is not empty when they encounter
      // RejectedExecutionExceptions. If that happened to last thread, just cancel all outstanding
      // tasks. (No need to be holding lock while doing this.)
      abort(outstanding);
   }
   
   List<Runnable> removeThread(VirtualThread thread) {
      synchronized (lock) {
         activeThreads.remove(thread);
         if (isTerminated()) {
            return Collections.emptyList();
         }
         // time to terminate the service
         List<Runnable> outstanding = new ArrayList<>(queue);
         queue.clear();
         // notify any threads waiting for termination
         lock.notifyAll();
         return outstanding;
      }
   }
   
   Runnable pollNext(VirtualThread runner) {
      List<Runnable> outstanding;
      synchronized (lock) {
         Runnable next = queue.poll();
         if (next != null) {
            return next;
         }
         outstanding = removeThread(runner);
      }
      abort(outstanding);
      return null;
   }
   
   private static void abort(List<Runnable> aborted) {
      for (Runnable r : aborted) {
         if (r instanceof Future) {
            ((Future<?>) r).cancel(false);
         } else if (r instanceof Cancellable) {
            ((Cancellable) r).cancel(false);
         }
      }
   }
   
   @Override
   protected <T> RunnableFuture<T> newTaskFor(Callable<T> task) {
      return new SettableRunnableFuture<>(task);
   }

   @Override
   protected <T> RunnableFuture<T> newTaskFor(Runnable task, T result) {
      return new SettableRunnableFuture<>(task, result);
   }

   @Override
   public <T> ListenableFuture<T> submit(Callable<T> task) {
      return (ListenableFuture<T>) super.submit(task);
   }

   @Override
   public <T> ListenableFuture<T> submit(Runnable task, T result) {
      return (ListenableFuture<T>) super.submit(task, result);
   }

   @Override
   public ListenableFuture<Void> submit(Runnable task) {
      return (ListenableFuture<Void>) super.<Void>submit(task, null);
   }
   
   private class VirtualThread {
      private boolean interrupted;
      private Thread current;
      
      VirtualThread() {
      }
      
      void run(Runnable r) {
         synchronized (this) {
            if (!interrupted) {
               try {
                  executor.execute(() -> {
                     synchronized (this) {
                        current = Thread.currentThread();
                     }
                     try {
                        r.run();
                     } finally {
                        synchronized (this) {
                           current = null;
                        }
                        runNext();
                     }
                  });
                  return;
               } catch (RejectedExecutionException ree) {
                  rejectTask(r, ree);
               }
            }
         }
         // We defer this handling to outside of synchronized block since the method we're invoking
         // will acquire the enclosing object's lock. But acquisition order must always be the
         // enclosing object's lock first, before the VirtualThread's monitor. Otherwise, deadlock
         // could occur. 
         removeAndCleanupThread(this);
      }
      
      private void rejectTask(Runnable failed, Throwable t) {
         if (failed instanceof AbstractListenableFuture) {
            ((AbstractListenableFuture<?>) failed).setFailure(t);
         } else if (failed instanceof CompletableFuture) {
            ((CompletableFuture<?>) failed).completeExceptionally(t);
         } else if (failed instanceof Future) {
            ((Future<?>) failed).cancel(false);
         } else if (failed instanceof Cancellable) {
            ((Cancellable) failed).cancel(false);
         }
      }
      
      private void runNext() {
         Runnable next = pollNext(this);
         if (next != null) {
            run(next);
         }
      }
      
      synchronized void interrupt() {
         interrupted = true;
         if (current != null) {
            current.interrupt();
         }
      }
   }
}
