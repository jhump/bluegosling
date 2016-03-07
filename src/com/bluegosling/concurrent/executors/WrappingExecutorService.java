package com.bluegosling.concurrent.executors;

import com.bluegosling.concurrent.fluent.FluentExecutorService;
import com.bluegosling.concurrent.fluent.FluentFuture;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * An executor service that provides a way to wrap tasks that are executed. Wrappers can perform a
 * range of cross-cutting concerns before and after delegating to the wrapped task.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
public abstract class WrappingExecutorService extends WrappingExecutor
      implements FluentExecutorService {

   /**
    * Constructs a new executor service that delegates to the given executor service.
    *
    * @param delegate an executor service
    */
   protected WrappingExecutorService(ExecutorService delegate) {
      super(FluentExecutorService.makeFluent(delegate));
   }
   
   /**
    * Returns the underlying executor service.
    *
    * @return the underlying executor service
    */
   @Override
   protected FluentExecutorService delegate() {
      return (FluentExecutorService) super.delegate();
   }
   
   /**
    * Wraps the given task. This is called for each task executed.
    *
    * @param c a task
    * @return a wrapper around the given task that will be executed in its place
    */
   protected abstract <T> Callable<T> wrap(Callable<T> c);
   
   /**
    * Wraps the given collection of tasks by calling {@link #wrap(Callable)} for each element in the
    * given collection.
    *
    * @param coll a collection of tasks
    * @return a new collection with the results of wrapping each task
    */
   protected <T, C extends Callable<T>> Collection<Callable<T>> wrap(Collection<C> coll) {
      return coll.stream().map(this::wrap).collect(Collectors.toList());
   }
   
   /**
    * Wraps the given task by first adapting it to a {@link Executors#callable(Runnable) Callable}
    * and then wrapping via {@link #wrap(Callable)}.
    */
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
   public <T> FluentFuture<T> submit(Callable<T> task) {
      return delegate().submit(wrap(task));
   }

   @Override
   public <T> FluentFuture<T> submit(Runnable task, T result) {
      return delegate().submit(wrap(Executors.callable(task, result)));
   }

   @Override
   public FluentFuture<Void> submit(Runnable task) {
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
