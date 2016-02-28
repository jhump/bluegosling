package com.bluegosling.concurrent;

import com.bluegosling.possible.AbstractDynamicPossible;
import com.bluegosling.possible.Fulfillable;
import com.bluegosling.util.Throwables;
import com.bluegosling.vars.Variable;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
   
   /**
    * Returns a view of this future as a {@link RunnableListenableFuture} that executes the given
    * task when run. If the task completes successfully before the future's value (or cause of
    * failure) is otherwise set, the future's value is set to {@code null}.
    * 
    * <p>The returned future can be cancelled and may interrupt the given task if it is running.
    * Calling {@link #cancel(boolean)} directly on the underlying {@link SettableFuture}, however,
    * will not interrupt the given task.
    *
    * @param runnable the task to run
    * @return a view of this settable future as a runnable future
    * 
    * @see SettableRunnableFuture
    */
   public RunnableListenableFuture<T> asRunnableFuture(Runnable runnable) {
      return asRunnableFuture(runnable, null);
   }

   /**
    * Returns a view of this future as a {@link RunnableListenableFuture} that executes the given
    * task when run. If the task completes successfully before the future's value (or cause of
    * failure) is otherwise set, the future's value is set to the specified value.
    * 
    * <p>The returned future can be cancelled and may interrupt the given task if it is running.
    * Calling {@link #cancel(boolean)} directly on the underlying {@link SettableFuture}, however,
    * will not interrupt the given task.
    *
    * @param runnable the task to run
    * @param result the value of this future upon successful completion of the task
    * @return a view of this settable future as a runnable future
    * 
    * @see SettableRunnableFuture
    */
   public RunnableListenableFuture<T> asRunnableFuture(Runnable runnable, T result) {
      return asRunnableFuture(Executors.callable(runnable, result));
   }
   
   /**
    * Returns a view of this future as a {@link RunnableListenableFuture} that performs the given
    * computation when run. If the task completes successfully before the future's value (or cause
    * of failure) is otherwise set, the future's value is set to the result of that computation.
    * 
    * <p>The returned future can be cancelled and may interrupt the given task if it is running.
    * Calling {@link #cancel(boolean)} directly on the underlying {@link SettableFuture}, however,
    * will not interrupt the given task.
    *
    * @param callable the computation to perform
    * @return a view of this settable future as a runnable future
    * 
    * @see SettableRunnableFuture
    */   public RunnableListenableFuture<T> asRunnableFuture(Callable<T> callable) {
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
      private final Variable<Thread> runner = new Variable<>();
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
            runner.set(Thread.currentThread());
            hasBeenRun = true;
         }
         try {
            setValue(task.call());
         } catch (Throwable t) {
            setFailure(t);
         } finally {
            synchronized (runner) {
               runner.set(null);
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
               Thread th = runner.get();
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
