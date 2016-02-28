package com.bluegosling.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Additional helper methods for creating and using instances of {@link CompletableFuture}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class CompletableFutures {
   private CompletableFutures() {
   }
   
   /**
    * Returns a copy of the given future that completes with the same result or failure as the given
    * completable future. However, the given future cannot be modified (completed or obtruded) via
    * methods on the returned future (hence making it effectively read-only).
    *
    * @param f a completable future
    * @return a read-only future that mirrors the given future
    * 
    * @see ReadOnlyCompletableFuture
    */
   public static <T> ReadOnlyCompletableFuture<T> readOnly(CompletableFuture<T> f) {
      ReadOnlyCompletableFuture<T> ret = new ReadOnlyCompletableFuture<>();
      f.whenComplete((t, th) -> {
         if (th != null) {
            ret.setFailure(th);
         } else {
            ret.setValue(t);
         }
      });
      return ret;
   }
   
   /**
    * Like {@link CompletableFuture#completedFuture(Object)}, except that it returns an immediately
    * failed future, instead of one that is immediately successful.
    *
    * @param failure the cause of the future's failure
    * @return an immediately failed future
    */
   public static <T> CompletableFuture<T> failedFuture(Throwable failure) {
      CompletableFuture<T> ret = new CompletableFuture<T>();
      ret.completeExceptionally(failure);
      return ret;
   }

   /**
    * Like {@link CompletableFuture#completedFuture(Object)}, except that it returns an immediately
    * cancelled future, instead of one that is immediately successful.
    *
    * @return an immediately cancelled future
    */
   public static <T> CompletableFuture<T> cancelledFuture() {
      CompletableFuture<T> ret = new CompletableFuture<T>();
      ret.cancel(false);
      return ret;
   }

   /**
    * Returns a {@link CompletableFuture} that completes when the given task completes. The task is
    * run asynchronously in the {@link ForkJoinPool#commonPool()}.
    * 
    * <p>Unlike {@link CompletableFuture#runAsync(Runnable)}, if the returned future is cancelled
    * and allowed to interrupt, it <em>will</em> attempt to interrupt the task if it is still
    * running.
    *
    * @param r the task to run
    * @return a future that completes when the task finishes
    */
   public static CompletableFuture<Void> runInterruptiblyAsync(Runnable r) {
      return runInterruptiblyAsync(r, ForkJoinPool.commonPool());
   }

   /**
    * Returns a {@link CompletableFuture} that completes when the given task completes.
    * 
    * <p>Unlike {@link CompletableFuture#runAsync(Runnable, Executor)}, if the returned future is
    * cancelled and allowed to interrupt, it <em>will</em> attempt to interrupt the task if it is
    * still running.
    *
    * @param r the task to run
    * @param executor the executor that is used to run the task asynchronously
    * @return a future that completes when the task finishes
    */
   public static CompletableFuture<Void> runInterruptiblyAsync(Runnable r, Executor executor) {
      return callInterruptiblyAsync(Executors.callable(r, null), executor);
   }

   /**
    * Calls the given task asynchronously, returning a future that represents its completion. The
    * task is run asynchronously in the {@link ForkJoinPool#commonPool()}.
    * 
    * <p>This is like {@link CompletableFuture#supplyAsync(Supplier)} except that it takes a
    * {@link Callable}, which is allowed to throw checked exceptions.
    *
    *
    * @param c the task to call
    * @return a future that completes with the value computed by the given task
    */
   public static <T> CompletableFuture<T> callAsync(Callable<T> c) {
      return callAsync(c, ForkJoinPool.commonPool());
   }

   /**
    * Calls the given task asynchronously, returning a future that represents its completion.
    * 
    * <p>This is like {@link CompletableFuture#supplyAsync(Supplier, Executor)} except that it takes
    * a {@link Callable}, which is allowed to throw checked exceptions.
    *
    *
    * @param c the task to call
    * @param executor the executor that is used to run the task asynchronously
    * @return a future that completes with the value computed by the given task
    */
   public static <T> CompletableFuture<T> callAsync(Callable<T> c, Executor executor) {
      CompletableFuture<T> cf = new CompletableFuture<>();
      executor.execute(() -> {
         try {
            cf.complete(c.call());
         } catch (Throwable th) {
            cf.completeExceptionally(th);
         }
      });
      return cf;
   }

   /**
    * Calls the given task asynchronously, returning a future that represents its completion. The
    * task is run asynchronously in the {@link ForkJoinPool#commonPool()}.
    * 
    * <p>Unlike {@link #callAsync(Callable)}, if the returned future is cancelled and allowed to
    * interrupt, it <em>will</em> attempt to interrupt the task if it is still running.
    *
    * @param c the task to call
    * @return a future that completes with the value computed by the given task
    */
   public static <T> CompletableFuture<T> callInterruptiblyAsync(Callable<T> c) {
      return callInterruptiblyAsync(c, ForkJoinPool.commonPool());
   }

   /**
    * Calls the given task asynchronously, returning a future that represents its completion.
    * 
    * <p>Unlike {@link #callAsync(Callable, Executor)}, if the returned future is cancelled and
    * allowed to interrupt, it <em>will</em> attempt to interrupt the task if it is still running.
    *
    * @param c the task to call
    * @param executor the executor that is used to run the task asynchronously
    * @return a future that completes with the value computed by the given task
    */
   public static <T> CompletableFuture<T> callInterruptiblyAsync(Callable<T> c, Executor executor) {
      RunnableCompletableFuture<T> ret = new RunnableCompletableFuture<>(c);
      executor.execute(ret);
      return ret;
   }
}
