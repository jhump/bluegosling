package com.apriori.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// TODO: javadoc
public interface ListenableExecutorService extends ExecutorService {
   @Override <T> ListenableFuture<T> submit(Callable<T> task);
   @Override <T> ListenableFuture<T> submit(Runnable task, T result);
   @Override ListenableFuture<Void> submit(Runnable task);
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
         throws InterruptedException;
   @Override <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
         long timeout, TimeUnit unit) throws InterruptedException;
}