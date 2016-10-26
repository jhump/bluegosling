package com.bluegosling.concurrent.executors;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.bluegosling.concurrent.SameThreadExecutor;
import com.bluegosling.concurrent.fluent.FluentExecutorService;
import com.bluegosling.concurrent.futures.CompletableFutures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementations that back the static methods in {@link CompletableExecutorService}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
final class CompletableExecutors {
   private CompletableExecutors() {
   }
   
   /**
    * A {@link FluentExecutorService} that executes submitted tasks synchronously on the same
    * thread as the one that submits them.
    */
   static class SameThreadExecutorService implements CompletableExecutorService {
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
      public <T> CompletableFuture<T> submit(Callable<T> task) {
         try {
            return completedFuture(task.call());
         } catch (Throwable th) {
            return CompletableFutures.failedFuture(th);
         }
      }

      @Override
      public <T> CompletableFuture<T> submit(Runnable task, T result) {
         try {
            task.run();
            return completedFuture(result);
         } catch (Throwable th) {
            return CompletableFutures.failedFuture(th);
         }
      }

      @Override
      public CompletableFuture<Void> submit(Runnable task) {
         try {
            task.run();
            return completedFuture(null);
         } catch (Throwable th) {
            return CompletableFutures.failedFuture(th);
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
               results.add(CompletableFutures.cancelledFuture());
               done = true;
            } else {
               results.add(submit(callable));
            }
         }
         return Collections.unmodifiableList(results);
      }
   }

   /**
    * Decorates a normal {@link ExecutorService} with the {@link CompletableExecutorService}
    * interface.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class CompletableExecutorServiceWrapper implements CompletableExecutorService {
      private final ExecutorService delegate;
      
      CompletableExecutorServiceWrapper(ExecutorService delegate) {
         this.delegate = delegate;
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
      public <T> CompletableFuture<T> submit(Callable<T> task) {
         return CompletableFutures.callInterruptiblyAsync(task, this);
      }

      @Override
      public <T> CompletableFuture<T> submit(Runnable task, T result) {
         return submit(Executors.callable(task, result));
      }

      @Override
      public CompletableFuture<Void> submit(Runnable task) {
         return CompletableFutures.runInterruptiblyAsync(task, this);
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
}
