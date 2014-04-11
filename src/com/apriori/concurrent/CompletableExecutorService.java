package com.apriori.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// TODO:javadoc
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
    * <p>Every instance in the returned list will implement {@link CompletableFuture}.
    */
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
         throws InterruptedException;

   /**
    * {@inheritDoc}
    * 
    * <p>Every instance in the returned list will implement {@link CompletableFuture}.
    */
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
         long timeout, TimeUnit unit) throws InterruptedException;}
