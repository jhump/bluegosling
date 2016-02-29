package com.bluegosling.concurrent.executors;

import com.bluegosling.concurrent.executors.CompletableExecutors.CompletableExecutorServiceWrapper;
import com.bluegosling.concurrent.executors.CompletableExecutors.SameThreadExecutorService;
import com.bluegosling.concurrent.futures.CompletableFutures;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An executor service that returns {@link CompletableFuture}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface CompletableExecutorService extends ExecutorService {
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be completable.
    */
   @Override <T> CompletableFuture<T> submit(Callable<T> task);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be completable.
    */
   @Override <T> CompletableFuture<T> submit(Runnable task, T result);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be completable.
    */
   @Override CompletableFuture<Void> submit(Runnable task);

   /**
    * {@inheritDoc}
    * 
    * <p>Every instance in the returned list will be an instance of {@link CompletableFuture}.
    */
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
         throws InterruptedException;

   /**
    * {@inheritDoc}
    * 
    * <p>Every instance in the returned list will be an instance of {@link CompletableFuture}.
    */
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
         long timeout, TimeUnit unit) throws InterruptedException;

   /**
    * Returns a new executor service that runs each task synchronously in the same thread that
    * submits it. Submissions that return futures will always return completed futures.
    *
    * @return a new executor service that runs tasks immediately in the current thread
    */
   static CompletableExecutorService sameThreadExecutorService() {
      return new SameThreadExecutorService();
   }

   /**
    * Converts the specified service into a {@link CompletableExecutorService}. If the specified
    * service <em>is</em> already completable, it is returned without any conversion.
    * 
    * <p>Instances of {@link CompletableFuture} returned from the executor's various {@code submit}
    * methods differ from typical completable futures in that a call to {@code future.cancel(true)}
    * <em>will</em> attempt to interrupt the thread that is running the associated task. (See
    * {@link CompletableFutures#callInterruptiblyAsync(Callable, Executor)}.)
    * 
    * @param executor the executor service
    * @return a completable version of the specified service
    */
   static CompletableExecutorService makeCompletable(ExecutorService executor) {
      if (executor instanceof CompletableExecutorService) {
         return (CompletableExecutorService) executor;
      }
      return new CompletableExecutorServiceWrapper(executor);
   }
}
