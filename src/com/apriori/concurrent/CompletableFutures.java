package com.apriori.concurrent;

import com.apriori.vars.Variable;

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
      // kind of ugly, but...
      Variable<Thread> runner = new Variable<>();
      // make a future that knows how to cancel the task if it is running
      CompletableFuture<T> cf = new InterruptibleCompletableFuture<>(runner);
      // the actual task we execute: invokes the callable and completes the future
      executor.execute(() -> {
         synchronized (runner) {
            if (cf.isDone()) {
               // no need to do anything if task is done
               // (e.g. asynchronously cancelled or completed)
               return; 
            }
            runner.set(Thread.currentThread());
         }
         try {
            cf.complete(c.call());
         } catch (Throwable t) {
            cf.completeExceptionally(t);
         } finally {
            synchronized (runner) {
               runner.clear();
            }
         }
      });
      return cf;
   }
   
   /**
    * A sub-class of {@link CompletableFuture} that can interrupt its corresponding task if it's
    * still running.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {
      private final Variable<Thread> runner;
      
      /**
       * Creates a new future. The given variable references the thread that is running the
       * corresponding task. Access to it should be synchronized (using the variable's intrinsic
       * lock).
       *
       * @param runner the variable that either references the thread running the task or references
       *    {@code null} if no thread is currently running it
       */
      InterruptibleCompletableFuture(Variable<Thread> runner) {
         this.runner = runner;
      }
      
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
   }
}
