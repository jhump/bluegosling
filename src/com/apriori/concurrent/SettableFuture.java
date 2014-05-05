package com.apriori.concurrent;

import com.apriori.possible.AbstractDynamicPossible;
import com.apriori.possible.Fulfillable;
import com.apriori.util.Throwables;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future whose result is set programmatically. This exposes protected API in
 * {@link AbstractListenableFuture} as public API. It also provides a view of the future as an
 * instance of {@link Fulfillable}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result
 */
public class SettableFuture<T> extends AbstractListenableFuture<T> {

   @Override
   public boolean setValue(T result) {
      return super.setValue(result);
   }

   @Override
   public boolean setFailure(Throwable failure) {
      return super.setFailure(failure);
   }
   
   public RunnableListenableFuture<T> asRunnableFuture(Runnable runnable) {
      return asRunnableFuture(runnable, null);
   }

   public RunnableListenableFuture<T> asRunnableFuture(Runnable runnable, T result) {
      return asRunnableFuture(() -> { runnable.run(); return result; });
   }
   
   public RunnableListenableFuture<T> asRunnableFuture(Callable<T> callable) {
      return new RunnableView(callable);
   }

   /**
    * Returns a view of this future as a {@link Fulfillable}. Fulfilling the returned object will
    * successfully complete the future.
    * 
    * @return a view of this future as a {@link Fulfillable}
    */
   public Fulfillable<T> asFulfillable() {
      return new FutureFulfillable();
   }
   
   /**
    * A view of the future as a {@link Fulfillable}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class FutureFulfillable extends AbstractDynamicPossible<T> implements Fulfillable<T> {
      
      FutureFulfillable() {
      }
      
      @Override
      public boolean isPresent() {
         return isSuccessful();
      }
      
      @Override
      public boolean fulfill(T value) {
         return setValue(value);
      }

      @Override
      public T get() {
         if (!isDone() || isCancelled()) {
            throw new NoSuchElementException("not yet fulfilled");
         } else if (isFailed()) {
            throw Throwables.withCause(new NoSuchElementException("failed to fulfill"),
                  getFailure());
         }
         return getResult();
      }
         
      @Override
      public Set<T> asSet() {
         // once completed, the future is immutable
         if (isDone()) {
            return isSuccessful() ? Collections.singleton(getResult()) : Collections.emptySet();
         }
         return super.asSet();
      }
   }

   /**
    * A view of the future and a task as a {@link RunnableListenableFuture}. When the task is run,
    * the future completes.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class RunnableView implements RunnableListenableFuture<T> {
      private final Thread runner[] = new Thread[1];
      private final Callable<T> task;
      private boolean hasBeenRun;
      
      RunnableView(Callable<T> task) {
         this.task = task;
      }

      @Override
      public void run() {
         synchronized (runner) {
            if (isDone() || hasBeenRun) {
               return;
            }
            runner[0] = Thread.currentThread();
            hasBeenRun = true;
         }
         try {
            setValue(task.call());
         } catch (Throwable t) {
            setFailure(t);
         } finally {
            synchronized (runner) {
               runner[0] = null;
            }
         }
      }

      @Override
      public boolean isSuccessful() {
         return SettableFuture.this.isSuccessful();
      }

      @Override
      public T getResult() {
         return SettableFuture.this.getResult();
      }

      @Override
      public boolean isFailed() {
         return SettableFuture.this.isFailed();
      }

      @Override
      public Throwable getFailure() {
         return SettableFuture.this.getFailure();
      }

      @Override
      public void addListener(FutureListener<? super T> listener, Executor executor) {
         SettableFuture.this.addListener(listener, executor);         
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         SettableFuture.this.visit(visitor);
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         if (mayInterruptIfRunning) {
            synchronized (runner) {
               Thread th = runner[0];
               if (th != null) {
                  th.interrupt();
               }
            }
         }
         return SettableFuture.this.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
         return SettableFuture.this.isCancelled();
      }

      @Override
      public boolean isDone() {
         return SettableFuture.this.isDone();
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
         return SettableFuture.this.get();
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
         return SettableFuture.this.get(timeout, unit);
      }

      @Override
      public void await() throws InterruptedException {
         SettableFuture.this.await();         
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         return SettableFuture.this.await(limit, unit);
      }
   }
}
