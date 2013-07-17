package com.apriori.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// TODO: javadoc
public interface ListenableFuture<T> extends Future<T>, Cancellable {
   public void addListener(Runnable listener, Executor executor);
   
   public interface ExecutorService extends java.util.concurrent.ExecutorService {
      @Override public <T> ListenableFuture<T> submit(Callable<T> task);
      @Override public <T> ListenableFuture<T> submit(Runnable task, T result);
      @Override public ListenableFuture<?> submit(Runnable task);
      @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException;
      @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
            long timeout, TimeUnit unit) throws InterruptedException;
   }
}
