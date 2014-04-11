package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future task that implements the {@link ListenableFuture} interface. This builds on the
 * {@link FutureTask} implementation provided by the Java runtime library and extends it with
 * additional API.
 * 
 * <p>Another implementation of the {@link ListenableFuture} interface that doesn't necessarily
 * represent a runnable task is {@link AbstractListenableFuture}. That other class also implements
 * some of the new API more efficiently since it doesn't operate within constraints inherited from
 * {@link FutureTask} (which was designed to provide a more simplistic API).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of result produced by the future task
 * 
 * @see AbstractListenableFuture
 */
public class ListenableFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

   private Throwable failure;
   private final FutureListenerSet<T> listeners = new FutureListenerSet<T>(this);
   
   /**
    * Constructs a new task for the specified callable. When {@linkplain #run() run}, the callable
    * is invoked and the future completes.
    * 
    * @param callable the task that produces the future result
    */
   public ListenableFutureTask(Callable<T> callable) {
      super(callable);
   }

   /**
    * Constructs a new task for the specified command and result. When {@linkplain #run() run}, the
    * runnable is invoked and the future completes.
    * 
    * @param runnable the task that is executed
    * @param result the future result
    */
   public ListenableFutureTask(Runnable runnable, T result) {
      super(runnable, result);
   }

   @Override
   public void addListener(FutureListener<? super T> listener, Executor executor) {
      listeners.addListener(listener, executor);
   }
   
   @Override
   protected void done() {
      if (!isCancelled()) {
         checkForFailure();
      }
      listeners.run();
   }
   
   private void checkForFailure() {
      assert isDone() && !isCancelled();
      boolean interrupted = false;
      try {
         while (true) {
            try {
               get();
               return;
            } catch (ExecutionException e) {
               failure = e.getCause();
               return;
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }
      }
   }

   @Override
   public boolean isSuccessful() {
      return isDone() && !isFailed() && !isCancelled();
   }

   @Override
   public T getResult() {
      if (!isSuccessful()) {
         throw new IllegalStateException();
      }
      boolean interrupted = false;
      try {
         while (true) {
            try {
               return get();
            } catch (ExecutionException e) {
               throw new AssertionError();
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }
      }
   }

   @Override
   public boolean isFailed() {
      return failure != null;
   }

   @Override
   public Throwable getFailure() {
      if (failure == null) {
         throw new IllegalStateException();
      }
      return failure;
   }

   @Override
   public void visit(FutureVisitor<? super T> visitor) {
      if (!isDone()) {
         throw new IllegalStateException();
      }
      if (failure != null) {
         visitor.failed(failure);
      } else if (isCancelled()) {
         visitor.cancelled();
      } else {
         visitor.successful(getResult());
      }
   }

   @Override
   public void await() throws InterruptedException {
      try {
         get();
      } catch (ExecutionException unused) {
      } catch (CancellationException unused) {
      }
   }

   @Override
   public boolean await(long limit, TimeUnit unit) throws InterruptedException {
      try {
         get(limit, unit);
      } catch (ExecutionException unused) {
      } catch (CancellationException unused) {
      } catch (TimeoutException e) {
         // icky that timing out and returning false requires an exception be thrown and caught :(
         return false;
      }
      return true;
   }
}
