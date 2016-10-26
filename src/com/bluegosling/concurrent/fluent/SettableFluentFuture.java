package com.bluegosling.concurrent.fluent;

import com.bluegosling.vars.Variable;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future whose result is set programmatically. This exposes protected API in
 * {@link AbstractFluentFuture} as public API.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result
 */
public class SettableFluentFuture<T> extends AbstractFluentFuture<T> {

   @Override
   public boolean setValue(T result) {
      return super.setValue(result);
   }

   @Override
   public boolean setFailure(Throwable failure) {
      return super.setFailure(failure);
   }
   
   /**
    * Returns a view of this future as a {@link RunnableFluentFuture} that executes the given
    * task when run. If the task completes successfully before the future's value (or cause of
    * failure) is otherwise set, the future's value is set to {@code null}.
    * 
    * <p>The returned future can be cancelled and may interrupt the given task if it is running.
    * Calling {@link #cancel(boolean)} directly on the underlying {@link SettableFluentFuture}, however,
    * will not interrupt the given task.
    *
    * @param runnable the task to run
    * @return a view of this settable future as a runnable future
    * 
    * @see SettableRunnableFluentFuture
    */
   public RunnableFluentFuture<T> asRunnableFuture(Runnable runnable) {
      return asRunnableFuture(runnable, null);
   }

   /**
    * Returns a view of this future as a {@link RunnableFluentFuture} that executes the given
    * task when run. If the task completes successfully before the future's value (or cause of
    * failure) is otherwise set, the future's value is set to the specified value.
    * 
    * <p>The returned future can be cancelled and may interrupt the given task if it is running.
    * Calling {@link #cancel(boolean)} directly on the underlying {@link SettableFluentFuture}, however,
    * will not interrupt the given task.
    *
    * @param runnable the task to run
    * @param result the value of this future upon successful completion of the task
    * @return a view of this settable future as a runnable future
    * 
    * @see SettableRunnableFluentFuture
    */
   public RunnableFluentFuture<T> asRunnableFuture(Runnable runnable, T result) {
      return asRunnableFuture(Executors.callable(runnable, result));
   }
   
   /**
    * Returns a view of this future as a {@link RunnableFluentFuture} that performs the given
    * computation when run. If the task completes successfully before the future's value (or cause
    * of failure) is otherwise set, the future's value is set to the result of that computation.
    * 
    * <p>The returned future can be cancelled and may interrupt the given task if it is running.
    * Calling {@link #cancel(boolean)} directly on the underlying {@link SettableFluentFuture}, however,
    * will not interrupt the given task.
    *
    * @param callable the computation to perform
    * @return a view of this settable future as a runnable future
    * 
    * @see SettableRunnableFluentFuture
    */   public RunnableFluentFuture<T> asRunnableFuture(Callable<T> callable) {
      return new RunnableView(callable);
   }

   /**
    * A view of the future and a task as a {@link RunnableFluentFuture}. When the task is run,
    * the future completes.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class RunnableView implements RunnableFluentFuture<T> {
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
         return SettableFluentFuture.this.isSuccessful();
      }

      @Override
      public T getResult() {
         return SettableFluentFuture.this.getResult();
      }

      @Override
      public boolean isFailed() {
         return SettableFluentFuture.this.isFailed();
      }

      @Override
      public Throwable getFailure() {
         return SettableFluentFuture.this.getFailure();
      }

      @Override
      public void addListener(FutureListener<? super T> listener, Executor executor) {
         SettableFluentFuture.this.addListener(listener, executor);         
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         SettableFluentFuture.this.visit(visitor);
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
         return SettableFluentFuture.this.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
         return SettableFluentFuture.this.isCancelled();
      }

      @Override
      public boolean isDone() {
         return SettableFluentFuture.this.isDone();
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
         return SettableFluentFuture.this.get();
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
         return SettableFluentFuture.this.get(timeout, unit);
      }

      @Override
      public void await() throws InterruptedException {
         SettableFluentFuture.this.await();         
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         return SettableFluentFuture.this.await(limit, unit);
      }
   }
}
