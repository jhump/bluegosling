package com.apriori.concurrent;

import static com.apriori.concurrent.FutureListener.forVisitor;
import static com.apriori.concurrent.ListenableExecutors.sameThreadExecutor;

import com.apriori.concurrent.ListenableFutures.CombiningFuture;
import com.apriori.possible.Holder;
import com.apriori.util.TriFunction;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The future to end all futures. This provides API improvements over the standard {@link Future} in
 * several categories:
 * <ol>
 * <li><strong>Callbacks</strong>: This allows for real asynchronous processing of futures and
 * is what gives this future its name. You can add listeners that are invoked when the future
 * completes.</li>
 * <li><strong>Blocking</strong>: This interface extends {@link Awaitable}, giving you more API
 * choices for blocking until the future completes (that do not require catching {@link
 * ExecutionException}, {@link CancellationException}, or {@link TimeoutException}).</li>
 * <li><strong>Inspecting</strong>: Numerous new methods are provided for inspecting the result of
 * a completed future, none of which require a {@code try/catch} block ({@link #isSuccessful()},
 * {@link #getResult()}, {@link #isFailed()}, {@link #getFailure()}, and
 * {@link #visit(FutureVisitor)}). All of these new methods are non-blocking and are intended to
 * assist with implementing listeners and with writing asynchronous code. Many will throw an
 * {@link IllegalStateException} if invoked before the future is done.</li>
 * </ol>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the future value
 */
public interface ListenableFuture<T> extends Future<T>, Cancellable, Awaitable {
   /**
    * Returns true if the future is done and was successful.
    * 
    * @return true if the future is done and was successful; false otherwise
    */
   boolean isSuccessful();
   
   /**
    * Gets the result of a successful future. This method can only be called when the future is
    * done and was successful.
    * 
    * @return the result
    * @throws IllegalArgumentException if the future is not complete or was not successful
    */
   T getResult();
   
   /**
    * Returns true if the future is done and failed.
    * 
    * @return true if the future is done and failed; false otherwise
    */
   boolean isFailed();
   
   /**
    * Gets the cause of failure for a future. This method can only be called when the future is done
    * and has failed.
    * 
    * @return the cause of failure
    * @throws IllegalArgumentException if the future is not complete or did not fail
    */
   Throwable getFailure();
   
   /**
    * Adds a listener that will be called when the future completes. The listener will be invoked
    * using the specified executor. If the future is already complete when the listener is added,
    * the listener will be immediately invoked. If the listener can be called synchronously (e.g.
    * it will complete very quickly and not block) then consider using {@link
    * ListenableExecutors#sameThreadExecutor()}.
    * 
    * @param listener the listener
    * @param executor the executor used when calling the listener
    */
   void addListener(FutureListener<? super T> listener, Executor executor);
   
   /**
    * Invokes applicable methods on the specified visitor, depending on the disposition of this
    * future. The future must be complete in order to be visited.
    * 
    * @param visitor the visitor
    * @throws IllegalStateException if the future is not complete
    */
   void visit(FutureVisitor<? super T> visitor);
   
   /**
    * Casts a future to a super-type. All methods on the future only return instances of the
    * parameterized type, none accept them as arguments. Since the target type is a super-type of
    * the future's original type, all instances returned by the original future are instances of
    * the target type. So the cast is safe thanks.
    *
    * @param future a future
    * @return the same future, but with its type parameter as a super-type of the original
    */
   static <T, U extends T> ListenableFuture<T> cast(ListenableFuture<U> future) {
      // co-variance makes it safe since all operations return a T, none take a T
      @SuppressWarnings("unchecked")
      ListenableFuture<T> cast = (ListenableFuture<T>) future;
      return cast;
   }

   /**
    * Adds a listener that visits this future using a {@linkplain
    * ListenableExecutors#sameThreadExecutor() same thread executor} when it completes.
    * 
    * @param visitor the visitor that will be called once this future completes
    */
   default void visitWhenDone(FutureVisitor<? super T> visitor) {
      addListener(forVisitor(visitor), sameThreadExecutor());
   }
   
   /**
    * Chains the specified task to the completion of this future. The specified task will be
    * initiated once the specified future completes successfully. The returned future will complete
    * successfully once both this future and chained task have completed. The returned future will
    * fail if this future fails (in which case the task is never invoked) or if the task throws an
    * exception.
    * 
    * <p>If this future is cancelled then the returned future will also be cancelled. But not vice
    * versa, so canceling the returned future will <em>not</em> cause this future to be cancelled.
    *
    * @param task a task that should execute when this future completes
    * @param executor the executor used to run the task
    * @return a future whose value will be the result of the specified task
    */
   default <U> ListenableFuture<U> chainTo(Callable<U> task, Executor executor) {
      Holder<Thread> taskThread = Holder.create();
      AbstractListenableFuture<U> result = new AbstractListenableFuture<U>() {
         @Override protected void interrupt() {
            // must use synchronization to make sure the interrupt can't happen
            // after the task is no longer running on that thread
            synchronized (taskThread) {
               Thread thread = taskThread.get();
               if (thread != null) {
                  thread.interrupt();
               }
            }
         }
      };
      addListener(forVisitor(new FutureVisitor<Object>() {
         @Override
         public void successful(Object o) {
            synchronized (taskThread) {
               taskThread.set(Thread.currentThread());
            }
            try {
               if (!result.isCancelled()) {
                  result.setValue(task.call());
               }
            } catch (Throwable th) {
               result.setFailure(th);
            } finally {
               synchronized (taskThread) {
                  taskThread.set(null);
               }
            }
         }
   
         @Override
         public void failed(Throwable t) {
            result.setFailure(t);
         }
         
         @Override
         public void cancelled() {
            result.setCancelled();
         }
      }), executor);
      return result;
   }

   /**
    * Chains the specified task to the completion of this future. The specified task will be
    * initiated once the specified future completes successfully. The returned future will complete
    * successfully once both this future and chained task have completed. The returned future will
    * fail if this future fails (in which case the task is never invoked) or if the task throws an
    * exception.
    * 
    * <p>If this future is cancelled then the returned future will also be cancelled. But not vice
    * versa, so canceling the returned future will <em>not</em> cause this future to be cancelled.
    *
    * @param task a task that should execute when the future completes
    * @param result the result of the task when it completes
    * @param executor the executor used to run the task
    * @return a future that will complete with the specified result when the specified task
    *       completes
    */
   default <U> ListenableFuture<U> chainTo(Runnable task, U result, Executor executor) {
      return chainTo(Executors.callable(task, result), executor);
   }

   /**
    * Chains the specified task to the completion of this future. The specified task will be
    * initiated once the specified future completes successfully. The returned future will complete
    * successfully once both this future and chained task have completed. The returned future will
    * fail if this future fails (in which case the task is never invoked) or if the task throws an
    * exception.
    * 
    * <p>If this future is cancelled then the returned future will also be cancelled. But not vice
    * versa, so canceling the returned future will <em>not</em> cause this future to be cancelled.
    *
    * @param task a task that should execute when the future completes
    * @param executor the executor used to run the task
    * @return a future that will complete with a {@code null} value when the specified task
    *       completes
    */
   default ListenableFuture<Void> chainTo(Runnable task, Executor executor) {
      return chainTo(task, null, executor);
   }

   /**
    * Chains the specified task to the completion of this future. The specified task will be
    * initiated once the specified future completes successfully. The returned future will complete
    * successfully once both this future and chained task have completed. The returned future will
    * fail if this future fails (in which case the task is never invoked) or if the task throws an
    * exception.
    * 
    * <p>If this future is cancelled then the returned future will also be cancelled. But not vice
    * versa, so canceling the returned future will <em>not</em> cause this future to be cancelled.
    *
    * @param task a task whose input is the result of the specified future
    * @param executor the executor used to run the task
    * @return a future whose value will be the result of the specified task
    */
   default <U> ListenableFuture<U> chainTo(Function<? super T, ? extends U> task,
         Executor executor) {
      return chainTo(() -> task.apply(getResult()), executor);
   }

   /**
    * Transforms the result of this future using the specified function. This is similar to
    * using {@link #chainTo(Function, Executor)} except that no executor is specified. The function
    * will be executed in the same thread that completes the future, so it should run quickly and be
    * safe to run from <em>any</em> thread. The function cannot be interrupted. The returned future
    * will fail if this future fails (in which case the given function is never invoked) or if the
    * function throws an exception.
    * 
    * <p>Also unlike using {@code chainTo(...)}, the returned future's cancellation status is kept
    * in sync with this future. So if the returned future is cancelled, this will also be cancelled
    * (if it is not yet done).
    *
    * @param function a function used to compute the transformed value
    * @return a future that is the result of applying the function to this future value
    */
   default <U> ListenableFuture<U> transform(Function<? super T, ? extends U> function) {
      ListenableFuture<T> self = this;
      AbstractListenableFuture<U> result = new AbstractListenableFuture<U>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               self.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      visitWhenDone(new FutureVisitor<T>() {
         @Override
         public void successful(T t) {
            try {
               result.setValue(function.apply(t));
            } catch (Throwable th) {
               result.setFailure(th);
            }
         }
   
         @Override
         public void failed(Throwable t) {
            result.setFailure(t);
         }
         
         @Override
         public void cancelled() {
            result.setCancelled();
         }
      });
      return result;
   }

   /**
    * Transforms this future using the specified function. The given function is applied, even if
    * this future fails. Since the function takes the future as an input, not just its resulting
    * value, the function can then inspect the future to see whether it completed successfully or
    * not. The function is not invoked until the given future is done, so it is safe for it call
    * {@link #getResult()}, {@link #getFailure()}, or {@link #visit(FutureVisitor)}.
    * 
    * <p>The returned future's cancellation status is kept in sync with the given future. So if the
    * returned future is cancelled, this future is also cancelled (if it is not yet done).
    * Similarly, if this future gets cancelled then the returned future will also be cancelled. The
    * function will not be invoked if this future is cancelled.
    *
    * @param function a function used to compute the transformed value
    * @return a future that is the result of applying the function to this future after it completes
    */
   default <U> ListenableFuture<U> transformFuture(
         Function<? super ListenableFuture<T>, ? extends U> function) {
      ListenableFuture<T> self = this;
      AbstractListenableFuture<U> result = new AbstractListenableFuture<U>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               self.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      addListener(new FutureListener<T>() {
         @Override
         public void onCompletion(ListenableFuture<? extends T> completedFuture) {
            if (completedFuture.isCancelled()) {
               result.setCancelled();
            }
            try {
               ListenableFuture<T> castFuture = cast(completedFuture);
               result.setValue(function.apply(castFuture));
            } catch (Throwable t) {
               result.setFailure(t);
            }
         }
      }, sameThreadExecutor());
      return result;
   }

   /**
    * Transforms any exception from this future using the specified function. This is nearly
    * identical to {@link #transform(ListenableFuture, Function)}, except that the function is
    * applied to this future's exception if it fails, <em>not</em> to the future's value if it
    * succeeds. So if this future completes successfully then the given function is never invoked. 
    * 
    * <p>The returned future's cancellation status is kept in sync with the given future. So if the
    * returned future is cancelled, this future is also cancelled (if it is not yet done).
    * Similarly, if this future gets cancelled then the returned future will also be cancelled. The
    * function will not be invoked if this future is cancelled.
    *
    * @param function a function used to compute a new value from an exception
    * @return a future that, if it fails, will have an exception that is the result of applying the
    *       given function to this future's exception
    */
   default ListenableFuture<T> transformException(
         Function<Throwable, ? extends Throwable> function) {
      ListenableFuture<T> self = this;
      AbstractListenableFuture<T> result = new AbstractListenableFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               self.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      visitWhenDone(new FutureVisitor<T>() {
         @Override
         public void successful(T t) {
            result.setValue(t);
         }
   
         @Override
         public void failed(Throwable t) {
            try {
               result.setFailure(function.apply(t));
            } catch (Throwable th) {
               result.setFailure(th);
            }
         }
         
         @Override
         public void cancelled() {
            result.setCancelled();
         }
      });
      return result;
   }

   /**
    * Recovers a future value by applying the specified function to any exception from this future.
    * This is similar to {@link #transform(ListenableFuture, Function)}, except that the function is
    * applied to the future's exception if it fails, instead of being applied to the future's value
    * if it succeeds. If this future completes successfully then the given function is never
    * invoked and the returned future completes with the same value. If this future fails, the
    * function is invoked to compute a value for the returned future. The returned future only fails
    * if that function throws an exception.
    * 
    * <p>The returned future's cancellation status is kept in sync with the given future. So if the
    * returned future is cancelled, this future is also cancelled (if it is not yet done).
    * Similarly, if this future gets cancelled then the returned future will also be cancelled. The
    * function will not be invoked if this future is cancelled.
    *
    * @param function a function used to compute the transformed exception
    * @return a future whose result will be that of this future, or if this future fails then
    *       then the result of applying a function to its cause of failure
    */ 
   default ListenableFuture<T> recover(Function<Throwable, ? extends T> function) {
      return transformFuture((future) ->
            future.isSuccessful() ? future.getResult() : function.apply(future.getFailure()));
   }
   
   /**
    * Combines this future with another by applying a function. The value of the returned future is
    * the result of applying the specified function to the values of the two futures. The returned
    * future will complete successfully when both input futures complete successfully. The returned
    * future will fail if either of the input futures fails (in which case the function is not
    * invoked) or if the given function throws an exception. 
    *
    * <p>The returned future's cancellation status will be kept in sync with the both input futures.
    * So if the returned future is cancelled, any unfinished input futures will also be cancelled.
    * If either of the input futures is cancelled, the returned future will also be cancelled.
    * 
    * @param other another future
    * @param function a function that combines two input values into one result
    * @return the future result of applying the function to this future value and the other one
    */
   default <U, V> ListenableFuture<V> combineWith(ListenableFuture<U> other,
         BiFunction<? super T, ? super U, ? extends V> function) {
      ListenableFuture<T> self = this;
      CombiningFuture<V> result = new CombiningFuture<V>(Arrays.asList(self, other)) {
         @Override V computeValue() {
            return function.apply(self.getResult(), other.getResult());
         }
      };
      return result;
   }

   /**
    * Combines this future with two other futures by applying a function. The value of the returned
    * future is the result of applying the specified function to the values of the three futures.
    * The returned future will complete successfully when all three input futures complete
    * successfully. The returned future will fail if any of the input futures fails (in which case
    * the function is not invoked) or if the given function throws an exception. 
    *
    * <p>The returned future's cancellation status will be kept in sync with the three input
    * futures. So if the returned future is cancelled, any unfinished input futures will also be
    * cancelled. If any of the input futures is cancelled, the returned future will also be
    * cancelled.
    * 
    * @param other1 another future
    * @param other2 yet another future
    * @param function a function that combines three input values into one result
    * @return the future result of applying the function to this future value the other two
    */
   default <U, V, W> ListenableFuture<W> combineWith(ListenableFuture<U> other1,
         ListenableFuture<V> other2,
         TriFunction<? super T, ? super U, ? super V, ? extends W> function) {
      ListenableFuture<T> self = this;
      CombiningFuture<W> result = new CombiningFuture<W>(Arrays.asList(self, other1, other2)) {
         @Override W computeValue() {
            return function.apply(self.getResult(), other1.getResult(), other2.getResult());
         }
      };
      return result;
   }
}
