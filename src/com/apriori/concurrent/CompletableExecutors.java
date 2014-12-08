package com.apriori.concurrent;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.apriori.vars.Variable;

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
    * Like {@link CompletableFuture#completedFuture(Object)}, except that it returns an immediately
    * failed future, instead of one that is immediately successful.
    *
    * @param failure the cause of the future's failure
    * @return an immediately failed future
    */
   static <T> CompletableFuture<T> failedFuture(Throwable failure) {
      CompletableFuture<T> ret = new CompletableFuture<T>();
      ret.completeExceptionally(failure);
      return ret;
   }
   
   /**
    * A {@link ListenableExecutorService} that executes submitted tasks synchronously on the same
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
            return failedFuture(th);
         }
      }

      @Override
      public <T> CompletableFuture<T> submit(Runnable task, T result) {
         try {
            task.run();
            return completedFuture(result);
         } catch (Throwable th) {
            return failedFuture(th);
         }
      }

      @Override
      public CompletableFuture<Void> submit(Runnable task) {
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
               results.add(ListenableFuture.cancelledFuture());
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
         // CompletableFuture has no factory method for a Callable. It also ignores requests
         // to interrupt the task if it is running. So we have to do something a little different
         // to get the right semantics (this is a bit hideous...)
         Variable<Thread> runner = new Variable<>();
         // make a future that knows how to cancel the task if it is running
         CompletableFuture<T> future = new CompletableFuture<T>() {
            @Override public boolean cancel(boolean mayInterrupt) {
               boolean ret = super.cancel(mayInterrupt);
               if (ret && mayInterrupt) {
                  synchronized (runner) {
                     Thread th = runner.get();
                     if (th != null) {
                        th.interrupt();
                     }
                  }
               }
               return ret;
            }
         };
         // the actual task we execute: invokes the callable and completes the future
         Runnable r = () -> {
            synchronized (runner) {
               if (future.isDone()) {
                  // no need to do anything if task is done
                  // (e.g. asynchronously cancelled or completed)
                  return; 
               }
               runner.set(Thread.currentThread());
            }
            try {
               future.complete(task.call());
            } catch (Throwable t) {
               future.completeExceptionally(t);
            } finally {
               synchronized (runner) {
                  runner.clear();
               }
            }
         };
         
         // finally, submit the task and return the future
         delegate.execute(r);
         return future;
      }

      @Override
      public <T> CompletableFuture<T> submit(Runnable task, T result) {
         // we use submit(Callable) instead of CompletableFuture.runAsync or
         // CompletableFuture.supplyAsync so we can get cancel/interrupt functionality
         return submit(Executors.callable(task, result));
      }

      @Override
      public CompletableFuture<Void> submit(Runnable task) {
         // we use submit(Callable) instead of CompletableFuture.runAsync or
         // CompletableFuture.supplyAsync so we can get cancel/interrupt functionality
         return submit(Executors.callable(task, null));
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
}
