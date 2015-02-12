package com.apriori.concurrent;

import com.apriori.collections.TransformingCollection;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//TODO: doc
//TODO: tests
public abstract class WrappingExecutorService extends WrappingExecutor
      implements ListenableExecutorService {

   protected WrappingExecutorService(ExecutorService delegate) {
      super(ListenableExecutorService.makeListenable(delegate));
   }
   
   @Override
   protected ListenableExecutorService delegate() {
      return (ListenableExecutorService) super.delegate();
   }
   
   protected abstract <T> Callable<T> wrap(Callable<T> c);
   
   protected <T, C extends Callable<T>> Collection<Callable<T>> wrap(Collection<C> coll) {
      return new TransformingCollection<C, Callable<T>>(coll, this::wrap);
   }
   
   @Override
   protected Runnable wrap(Runnable r) {
      return () -> {
         try {
            wrap(Executors.callable(r)).call();
         } catch (RuntimeException | Error e) {
            throw e;
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      };
   }

   @Override
   public void shutdown() {
      delegate().shutdown();
   }

   @Override
   public List<Runnable> shutdownNow() {
      return delegate().shutdownNow();
   }

   @Override
   public boolean isShutdown() {
      return delegate().isShutdown();
   }

   @Override
   public boolean isTerminated() {
      return delegate().isTerminated();
   }

   @Override
   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return delegate().awaitTermination(timeout, unit);
   }

   @Override
   public <T> ListenableFuture<T> submit(Callable<T> task) {
      return delegate().submit(wrap(task));
   }

   @Override
   public <T> ListenableFuture<T> submit(Runnable task, T result) {
      return delegate().submit(wrap(Executors.callable(task, result)));
   }

   @Override
   public ListenableFuture<Void> submit(Runnable task) {
      return delegate().submit(wrap(Executors.callable(task, null)));
   }

   @Override
   public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
         throws InterruptedException {
      return delegate().invokeAll(wrap(tasks));
   }

   @Override
   public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
         TimeUnit unit) throws InterruptedException {
      return delegate().invokeAll(wrap(tasks), timeout, unit);
   }

   @Override
   public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
         ExecutionException {
      return delegate().invokeAny(wrap(tasks));
   }

   @Override
   public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
         throws InterruptedException, ExecutionException, TimeoutException {
      return delegate().invokeAny(wrap(tasks), timeout, unit);
   }
}
