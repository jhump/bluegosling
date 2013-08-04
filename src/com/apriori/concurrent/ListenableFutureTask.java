package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//TODO: javadoc
//TODO: test
public class ListenableFutureTask<T> extends FutureTask<T> implements ListenableFuture<T> {

   private final Lock completionLock = new ReentrantLock();
   private boolean complete;
   private Throwable failure;
   private FutureListenerSet<T> listeners = new FutureListenerSet<T>(this);
   
   public ListenableFutureTask(Callable<T> callable) {
      super(callable);
   }

   public ListenableFutureTask(Runnable runnable, T result) {
      super(runnable, result);
   }

   @Override
   public void addListener(FutureListener<? super T> listener, Executor executor) {
      completionLock.lock();
      try {
         if (!complete) {
            listeners.addListener(listener, executor);
            return;
         }
      } finally {
         completionLock.unlock();
      }
      // if we get here, future is complete so run listener immediately
      FutureListenerSet.runListener(this, listener, executor);
   }
   
   @Override
   protected void done() {
      FutureListenerSet<T> toExecute;
      completionLock.lock();
      toExecute = listeners;
      complete = true;
      if (!isCancelled()) {
         checkForFailure();
      }
      listeners = null;
      completionLock.unlock();
      toExecute.runListeners();
   }
   
   private void checkForFailure() {
      boolean interrupted = false;
      while (true) {
         try {
            get();
            break;
         } catch (ExecutionException e) {
            failure = e.getCause();
            break;
         } catch (InterruptedException e) {
            interrupted = true;
         }
      }
      if (interrupted) {
         Thread.currentThread().interrupt();
      }
   }

   @Override
   public boolean isSuccessful() {
      return isDone() && !isFailed() && !isCancelled();
   }

   @Override
   public T getResult() {
      if (!isSuccessful()) {
         throw new IllegalArgumentException();
      }
      try {
         return get();
      } catch (Throwable wtf) {
         // future is complete and successful, so this should never happen
         throw new AssertionError();
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
