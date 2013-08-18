package com.apriori.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An executor service that returns {@link ListenableFuture}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface ListenableExecutorService extends ExecutorService {
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be listenable.
    */
   @Override <T> ListenableFuture<T> submit(Callable<T> task);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be listenable.
    */
   @Override <T> ListenableFuture<T> submit(Runnable task, T result);

   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to be listenable.
    */
   @Override ListenableFuture<Void> submit(Runnable task);

   /**
    * {@inheritDoc}
    * 
    * <p>Every instance in the returned list will implement {@link ListenableFuture}.
    */
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
         throws InterruptedException;

   /**
    * {@inheritDoc}
    * 
    * <p>Every instance in the returned list will implement {@link ListenableFuture}.
    */
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
         long timeout, TimeUnit unit) throws InterruptedException;
}