package com.bluegosling.concurrent.fluent;

import static com.bluegosling.concurrent.FutureListener.forVisitor;
import static com.bluegosling.concurrent.fluent.FluentFutures.immediateCopy;
import static com.bluegosling.concurrent.fluent.FluentFutures.snapshot;

import com.bluegosling.concurrent.Awaitable;
import com.bluegosling.concurrent.Cancellable;
import com.bluegosling.concurrent.FutureListener;
import com.bluegosling.concurrent.FutureVisitor;
import com.bluegosling.concurrent.executors.SameThreadExecutor;
import com.bluegosling.concurrent.fluent.FluentFutures.CancelledFuture;
import com.bluegosling.concurrent.fluent.FluentFutures.CompletedFuture;
import com.bluegosling.concurrent.fluent.FluentFutures.CompletionStageWrapper;
import com.bluegosling.concurrent.fluent.FluentFutures.DeferredCancelledFuture;
import com.bluegosling.concurrent.fluent.FluentFutures.DeferredFailedFuture;
import com.bluegosling.concurrent.fluent.FluentFutures.DeferredFuture;
import com.bluegosling.concurrent.fluent.FluentFutures.FailedFuture;
import com.bluegosling.concurrent.fluent.FluentFutures.FluentCompletionStage;
import com.bluegosling.concurrent.fluent.FluentFutures.FluentFutureWrapper;
import com.bluegosling.concurrent.fluent.FluentFutures.FluentScheduledFutureWrapper;
import com.bluegosling.concurrent.fluent.FluentFutures.UnfinishableFuture;
import com.bluegosling.concurrent.futures.CompletionStageFuture;
import com.bluegosling.function.TriFunction;
import com.bluegosling.vars.VariableBoolean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
 * <li><strong>Monadic</strong>: This future includes numerous useful default methods, all built on
 * top of the listener primitive, that allow it to be used like a monad, chaining additional
 * operations and applying numerous kinds of transformations.
 * </ol>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the future value
 */
// TODO: more tests
public interface FluentFuture<T> extends Future<T>, Cancellable, Awaitable {
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
    * @throws IllegalArgumentException if the future is not complete, was successful, or was
    *       cancelled
    */
   Throwable getFailure();
   
   /**
    * Adds a listener that will be called when the future completes. The listener will be invoked
    * using the specified executor. If the future is already complete when the listener is added,
    * the listener will be immediately invoked. If the listener can be called synchronously (e.g.
    * it will complete very quickly and not block) then consider using a {@link SameThreadExecutor}.
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
    * Casts a future to a super-type. The compiler enforces on invariance on generic types, but this
    * method allows futures to be treated as covariant. Since the target type is a super-type of
    * the future's original type, all instances returned by the original future are also instances
    * of the target type. This combined with the fact that no methods accept the target type means
    * that the covariance is type-safe.
    *
    * @param future a future
    * @return the same future, but with its type parameter as a super-type of the original
    */
   static <T, U extends T> FluentFuture<T> cast(FluentFuture<U> future) {
      // co-variance makes it safe since all operations return a T, none take a T
      @SuppressWarnings("unchecked")
      FluentFuture<T> cast = (FluentFuture<T>) future;
      return cast;
   }
   
   /**
    * Gets the value of the future or a given default value if the future is not yet complete. If
    * the future is complete but finished with a failure or due to cancellation, this method will
    * throw an exception.
    *
    * @param valueIfIncomplete the value to return if the future is not yet complete
    * @return the future's value or the given value if the future is not yet complete
    * @throws CancellationException if the future was cancelled
    * @throws CompletionException if the future finished with a failure (the cause of the thrown
    *       exception will be the cause of the failure)
    */
   default T getNow(T valueIfIncomplete) {
      if (!isDone()) {
         return valueIfIncomplete;
      } else if (isSuccessful()) {
         return getResult();
      } else if (isFailed()) {
         throw new CompletionException(getFailure());
      } else {
         //assert isCancelled();
         throw new CancellationException();
      }
   }
   
   /**
    * Adds a listener that visits this future using a {@link SameThreadExecutor} when it completes.
    * 
    * @param visitor the visitor that will be called once this future completes
    */
   default void visitWhenDone(FutureVisitor<? super T> visitor) {
      addListener(forVisitor(visitor), SameThreadExecutor.get());
   }
   
   /**
    * Chains the specified task to the completion of this future. The specified task will be
    * initiated once this future completes successfully. The returned future will complete
    * successfully once both this future and the chained task have completed. The returned future
    * will fail if this future fails (in which case the task is never invoked) or if the task throws
    * an exception.
    * 
    * <p>If this future is cancelled then the returned future will also be cancelled. But not vice
    * versa, so canceling the returned future will <em>not</em> cause this future to be cancelled.
    *
    * @param task a task that should execute when this future completes
    * @param executor the executor used to run the task
    * @return a future whose value will be the result of the specified task
    */
   default <U> FluentFuture<U> chainTo(Callable<U> task, Executor executor) {
      SettableRunnableFluentFuture<U> result = new SettableRunnableFluentFuture<U>(task);
      addListener(forVisitor(new FutureVisitor<Object>() {
         @Override
         public void successful(Object o) {
            result.run();
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
    * initiated once this future completes successfully. The returned future will complete
    * successfully once both this future and the chained task have completed. The returned future
    * will fail if this future fails (in which case the task is never invoked) or if the task throws
    * an exception.
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
   default <U> FluentFuture<U> chainTo(Runnable task, U result, Executor executor) {
      return chainTo(Executors.callable(task, result), executor);
   }

   /**
    * Chains the specified task to the completion of this future. The specified task will be
    * initiated once this future completes successfully. The returned future will complete
    * successfully once both this future and the chained task have completed. The returned future
    * will fail if this future fails (in which case the task is never invoked) or if the task throws
    * an exception.
    * 
    * <p>If this future is cancelled then the returned future will also be cancelled. But not vice
    * versa, so canceling the returned future will <em>not</em> cause this future to be cancelled.
    *
    * @param task a task that should execute when the future completes
    * @param executor the executor used to run the task
    * @return a future that will complete with a {@code null} value when the specified task
    *       completes
    */
   default FluentFuture<Void> chainTo(Runnable task, Executor executor) {
      return chainTo(task, null, executor);
   }

   /**
    * Chains the specified task to the completion of this future. The specified task will be
    * initiated once this future completes successfully. The returned future will complete
    * successfully once both this future and the chained task have completed. The returned future
    * will fail if this future fails (in which case the task is never invoked) or if the task throws
    * an exception.
    * 
    * <p>If this future is cancelled then the returned future will also be cancelled. But not vice
    * versa, so canceling the returned future will <em>not</em> cause this future to be cancelled.
    *
    * @param task a task whose input is the result of the specified future
    * @param executor the executor used to run the task
    * @return a future whose value will be the result of the specified task
    */
   default <U> FluentFuture<U> chainTo(Function<? super T, ? extends U> task,
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
   default <U> FluentFuture<U> map(Function<? super T, ? extends U> function) {
      FluentFuture<T> self = this;
      AbstractFluentFuture<U> result = new AbstractFluentFuture<U>() {
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
               if (!result.isDone()) {
                  result.setValue(function.apply(t));
               }
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
   default <U> FluentFuture<U> flatMap(
         Function<? super T, ? extends FluentFuture<U>> function) {
      return dereference(map(function));
   }
   
   /**
    * Transforms this future using the specified function. The given function is applied, even if
    * this future fails. Since the function takes the future as an input, not just its resulting
    * value, the function can then inspect the future to see whether it completed successfully or
    * not. The function is not invoked until the given future is done, so it is safe for it to call
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
   default <U> FluentFuture<U> mapFuture(
         Function<? super FluentFuture<T>, ? extends U> function) {
      FluentFuture<T> self = this;
      AbstractFluentFuture<U> result = new AbstractFluentFuture<U>() {
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
         public void onCompletion(FluentFuture<? extends T> completedFuture) {
            if (completedFuture.isCancelled()) {
               result.setCancelled();
            }
            try {
               if (!result.isDone()) {
                  result.setValue(function.apply(cast(completedFuture)));
               }
            } catch (Throwable t) {
               result.setFailure(t);
            }
         }
      }, SameThreadExecutor.get());
      return result;
   }

   /**
    * Transforms any exception from this future using the specified function. This is nearly
    * identical to {@link #map(Function)}, except that the function is applied to this future's
    * exception if it fails, <em>not</em> to the future's value if it succeeds. So if this future
    * completes successfully then the given function is never invoked. 
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
   default FluentFuture<T> mapException(
         Function<Throwable, ? extends Throwable> function) {
      FluentFuture<T> self = this;
      AbstractFluentFuture<T> result = new AbstractFluentFuture<T>() {
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
               if (!result.isDone()) {
                  result.setFailure(function.apply(t));
               }
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
    * This is similar to {@link #map(Function)}, except that the function is applied to the future's
    * exception if it fails, instead of being applied to the future's value if it succeeds. If this
    * future completes successfully then the given function is never invoked and the returned future
    * completes with the same value. If this future fails, the function is invoked to compute a
    * value for the returned future. The returned future only fails if that function throws an
    * exception.
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
   default FluentFuture<T> recover(Function<Throwable, ? extends T> function) {
      return mapFuture((future) ->
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
   default <U, V> FluentFuture<V> combineWith(FluentFuture<U> other,
         BiFunction<? super T, ? super U, ? extends V> function) {
      FluentFuture<T> self = this;
      CombiningFluentFuture<V> result = new CombiningFluentFuture<V>(Arrays.asList(self, other)) {
         @Override protected V computeValue() {
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
   default <U, V, W> FluentFuture<W> combineWith(FluentFuture<U> other1,
         FluentFuture<V> other2,
         TriFunction<? super T, ? super U, ? super V, ? extends W> function) {
      FluentFuture<T> self = this;
      CombiningFluentFuture<W> result = new CombiningFluentFuture<W>(Arrays.asList(self, other1, other2)) {
         @Override protected W computeValue() {
            return function.apply(self.getResult(), other1.getResult(), other2.getResult());
         }
      };
      return result;
   }

   /**
    * Returns a view of this future as a {@link CompletionStage}.
    *
    * @return a view of this future as a completion stage
    */
   default CompletionStage<T> asCompletionStage() {
      if (this instanceof CompletionStageFuture) {
         Future<T> self = this; // we need intermediate upcast to Future to make compiler happy;
                                // otherwise, it thinks the cast below is unchecked
         return (CompletionStageFuture<T>) self;
      }
      return new FluentCompletionStage<>(this);
   }

   /**
    * Converts the given completion stage into a {@link FluentFuture}.
    *
    * @param stage a completion stage
    * @return a view of the stage as a future
    */
   static <T> FluentFuture<T> fromCompletionStage(CompletionStage<T> stage) {
      if (stage instanceof FluentCompletionStage) {
         // unwrap if we can
         return ((FluentCompletionStage<T>) stage).future;
      }
      return new CompletionStageWrapper<T>(stage); 
   }
   
   /**
    * Converts the specified future into a {@link FluentFuture}. If the specified future
    * <em>is</em> a {@link FluentFuture}, it is returned without any conversion. Note that if
    * the specified future is not yet done and is not a {@link CompletableFuture}, conversion
    * requires creating a new thread that simply blocks until the specified future completes,
    * at which time the returned fluent future is also completed (asynchronously).
    * 
    * <p>The returned future's cancellation status will be kept in sync with the specified future.
    * So if the returned future is cancelled, so too will the underlying future be cancelled, and
    * vice versa.
    * 
    * @param future the future
    * @return a fluent version of the specified future
    */
   static <T> FluentFuture<T> makeFluent(Future<T> future) {
      if (future instanceof ScheduledFuture) {
         return makeFluent((ScheduledFuture<T>) future);
      }
      if (future instanceof FluentFuture) {
         return (FluentFuture<T>) future;
      }
      if (future instanceof CompletableFuture) {
         return fromCompletionStage((CompletableFuture<T>) future);
      }
      if (future.isDone()) {
         // can get the value immediately
         return immediateCopy(future);
      }
      return new FluentFutureWrapper<T>(future);
   }
   
   /**
    * Converts the specified future into a {@link FluentScheduledFuture}. If the specified
    * future <em>is</em> a {@link FluentScheduledFuture}, it is returned without any conversion.
    * Note that if the specified future is not yet done, conversion requires creating a new thread.
    * The thread simply blocks until the specified future completes, at which time the returned
    * fluent future is also completed (asynchronously).
    * 
    * <p>The returned future's cancellation status will be kept in sync with the specified future.
    * So if the returned future is cancelled, so too will the underlying future be cancelled, and
    * vice versa.
    *
    * @param future the scheduled future
    * @return a fluent version of the specified future
    */
   static <T> FluentScheduledFuture<T> makeFluent(ScheduledFuture<T> future) {
      if (future instanceof FluentScheduledFuture) {
         return (FluentScheduledFuture<T>) future;
      }
      if (future.isDone()) {
         // can get the value immediately
         return immediateCopy(future);
      }
      return new FluentScheduledFutureWrapper<T>(future);
   }

   /**
    * Returns a future that has already successfully completed with the specified value.
    * 
    * @param value the future result
    * @return a future that is immediately done
    */
   static <T> FluentFuture<T> completedFuture(final T value) {
      return new CompletedFuture<T>(value);
   }
   
   /**
    * Returns a future that has already failed due to the specified cause.
    * 
    * @param failure the cause of future failure
    * @return a future that is immediately done
    */
   static <T> FluentFuture<T> failedFuture(final Throwable failure) {
      return new FailedFuture<T>(failure);
   }

   /**
    * Returns a future that has already been cancelled.
    * 
    * @return a future that is immediately done
    */
   @SuppressWarnings("unchecked") // CancelledFuture is stateless and immutable, so cast is safe
   static <T> FluentFuture<T> cancelledFuture() {
      return (CancelledFuture<T>) CancelledFuture.INSTANCE;
   }
   
   /**
    * Returns a future that will complete successfully with the given value after the given delay.
    * This future may not complete successfully if it is {@linkplain #cancel(boolean) cancelled}
    * before the delay elapses.
    * 
    * @param value the future value
    * @param delay the time after which the future completes
    * @param unit the unit for the given delay
    * @return a future that will complete successfully with the given value after the given delay
    */
   static <T> FluentFuture<T> deferredFuture(T value, long delay, TimeUnit unit) {
      return new DeferredFuture<>(value, delay, unit);
   }
   
   /**
    * Returns a future that will fail with the given cause after the given delay. This future may
    * not fail with the given cause if it is {@linkplain #cancel(boolean) cancelled} before the
    * delay elapses.
    * 
    * @param failure the cause of future failure
    * @param delay the time after which the future completes
    * @param unit the unit for the given delay
    * @return a future that will fail with the given cause after the given delay
    */
   static <T> FluentFuture<T> deferredFailedFuture(Throwable failure, long delay,
         TimeUnit unit) {
      return new DeferredFailedFuture<>(failure, delay, unit);
   }

   /**
    * Returns a future that will be cancelled after the given delay. This future may complete before
    * the given delay elapses if it is {@linkplain #cancel(boolean) cancelled}.
    * 
    * @param delay the time after which the future is cancelled
    * @param unit the unit for the given delay
    * @return a future that will be cancelled after the given delay
    */
   static <T> FluentFuture<T> deferredCancelledFuture(long delay, TimeUnit unit) {
      return new DeferredCancelledFuture<>(delay, unit);
   }

   /**
    * Returns a future that will never finish. This can be useful in some circumstances for
    * asynchronous functional idioms, and also for testing.
    * 
    * <p>The returned future cannot be used with blocking calls, since they would never return or
    * always timeout. So calls to both forms of {@link FluentFuture#get} and both forms of
    * {@link FluentFuture#await} all throw {@link UnsupportedOperationException}.
    * 
    * <p>Additionally, the future cannot be cancelled (as that would implicitly finish it). So calls
    * to {@link #cancel(boolean)} always return false but don't actually cause the future to become
    * {@linkplain #isCancelled() cancelled}.
    *
    * @return a future that will never finish
    */
   @SuppressWarnings("unchecked") // UnfinishableFuture is stateless and immutable, so cast is safe
   static <T> FluentFuture<T> unfinishableFuture() {
      return (FluentFuture<T>) UnfinishableFuture.INSTANCE;
   }
   
   /**
    * Joins multiple futures into one future list. The returned future will complete successfully
    * once all constituent futures complete successfully. The returned future will fail if any of
    * the constituent futures fails.
    * 
    * <p>The returned future's cancellation status will be kept in sync with the constituent
    * futures. So if the returned future is cancelled, any unfinished constituent futures will also
    * be cancelled. If any of the constituent futures is cancelled, the returned future will also
    * be cancelled.
    *
    * @param futures the future values
    * @return a future list whose elements are the values from the specified futures
    */
   @SafeVarargs
   static <T> FluentFuture<List<T>> join(FluentFuture<? extends T>... futures) {
      return join(Arrays.asList(futures));
   }

   /**
    * Joins a list of futures into one future list. The returned future will complete successfully
    * once all constituent futures complete successfully. The returned future will fail if any of
    * the constituent futures fails. 
    * 
    * <p>The returned future's cancellation status will be kept in sync with the constituent
    * futures. So if the returned future is cancelled, any unfinished constituent futures will also
    * be cancelled. If any of the constituent futures is cancelled, the returned future will also
    * be cancelled.
    *
    * @param futures the future values
    * @return a future list whose elements are the values from the specified futures
    */
   static <T> FluentFuture<List<T>> join(
         final Iterable<? extends FluentFuture<? extends T>> futures) {
      List<FluentFuture<? extends T>> futureList = snapshot(futures);
      if (futureList.isEmpty()) {
         return completedFuture(Collections.emptyList());
      }
      final int len = futureList.size();
      if (len == 1) {
         return futureList.get(0).map(o -> Collections.singletonList(o));
      }
      @SuppressWarnings({"unchecked", "rawtypes"}) // java generics not expressive enough
      CombiningFluentFuture<List<T>> result = new CombiningFluentFuture<List<T>>((Collection) futureList) {
         @Override protected List<T> computeValue() {
            List<T> list = new ArrayList<T>(len);
            for (FluentFuture<? extends T> future : futureList) {
               list.add(future.getResult());
            }
            return list;
         }
      };
      return result;
   }

   /**
    * Returns a future that represents the first future to complete out of the given futures.
    * The returned future could be a failed future if the first future to complete fails. When an
    * input future is cancelled, it is ignored unless <em>all</em> of the given futures are
    * cancelled, in which case the returned future will also be cancelled.
    * 
    * <p>If the returned future is cancelled, it will in turn result in all of the input futures
    * being cancelled.
    * 
    * <p>Note that this method does not block for completion of any future. It instead returns a
    * new future that is completed upon the first given future completing.
    *
    * @param futures an array of futures
    * @return a future that represents the first of the given futures to complete
    * @throws IllegalArgumentException if the given array of futures is empty
    */
   @SafeVarargs
   static <T> FluentFuture<T> firstOf(FluentFuture<? extends T>... futures) {
      return firstOf(Arrays.asList(futures));
   }

   /**
    * Returns a future that represents the first future to complete out of the given collection of
    * futures. The returned future could be a failed future if the first future to complete fails.
    * When an input future is cancelled, it is ignored unless <em>all</em> of the given futures are
    * cancelled, in which case the returned future will also be cancelled.
    * 
    * <p>If the returned future is cancelled, it will in turn result in all of the input futures
    * being cancelled.
    * 
    * <p>Note that this method does not block for completion of any future. It instead returns a
    * new future that is completed upon the first given future completing.
    *
    * @param futures a collection of futures
    * @return a future that represents the first of the given futures to complete
    * @throws IllegalArgumentException if the given collection of futures is empty
    */
   static <T> FluentFuture<T> firstOf(Iterable<FluentFuture<? extends T>> futures) {
      final List<FluentFuture<? extends T>> futureList = snapshot(futures);
      if (futureList.isEmpty()) {
         throw new IllegalArgumentException("must supply at least one future");
      } else if (futureList.size() == 1) {
         // co-variance makes it safe since all operations return a T, none take a T
         @SuppressWarnings("unchecked")
         FluentFuture<T> theOne = (FluentFuture<T>) futureList.get(0);
         return theOne;
      }
      final AbstractFluentFuture<T> result = new AbstractFluentFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.cancel(mayInterrupt)) {
               for (FluentFuture<?> future : futureList) {
                  future.cancel(mayInterrupt);
               }
               return true;
            }
            return false;
         }
      };
      final AtomicInteger remaining = new AtomicInteger(futureList.size());
      FutureVisitor<T> visitor = new FutureVisitor<T>() {
         @Override
         public void successful(T value) {
            result.setValue(value);
            remaining.decrementAndGet();
         }

         @Override
         public void failed(Throwable failure) {
            result.setFailure(failure);
            remaining.decrementAndGet();
         }

         @Override
         public void cancelled() {
            if (remaining.decrementAndGet() == 0) {
               // If last one is cancelled, then we need to cancel the result. Otherwise,
               // we let one of the remaining futures provide the result's value.
               result.setCancelled();
            }
         }
      };
      for (FluentFuture<? extends T> future : futureList) {
         future.visitWhenDone(visitor);
      }
      return result;
   }

   /**
    * Returns a future that represents the first future to successfully complete out of the given
    * futures. The returned future will only failed if <em>all</em> of the given futures fail. When
    * an input future is cancelled, it is ignored unless <em>all</em> of the given futures are
    * cancelled, in which case the returned future will also be cancelled. If all input futures
    * either fail or are cancelled, then the returned future will either fail or be cancelled
    * depending on the disposition of the last input future to complete.
    * 
    * <p>If the returned future is cancelled, it will in turn result in all of the input futures
    * being cancelled.
    * 
    * <p>Note that this method does not block for completion of any future. It instead returns a
    * new future that is completed upon the first given future successfully completing.
    *
    * @param futures an array of futures
    * @return a future that represents the first of the given futures to complete successfully
    * @throws IllegalArgumentException if the given array of futures is empty
    */
   @SafeVarargs
   static <T> FluentFuture<T> firstSuccessfulOf(
         FluentFuture<? extends T>... futures) {
      return firstSuccessfulOf(Arrays.asList(futures));
   }

   /**
    * Returns a future that represents the first future to successfully complete out of the given
    * collection of futures. The returned future will only failed if <em>all</em> of the given
    * futures fail. When an input future is cancelled, it is ignored unless <em>all</em> of the
    * given futures are cancelled, in which case the returned future will also be cancelled. If all
    * input futures either fail or are cancelled, then the returned future will either fail or be
    * cancelled depending on the disposition of the last input future to complete.
    * 
    * <p>If the returned future is cancelled, it will in turn result in all of the input futures
    * being cancelled.
    * 
    * <p>Note that this method does not block for completion of any future. It instead returns a
    * new future that is completed upon the first given future successfully completing.
    *
    * @param futures a collection of futures
    * @return a future that represents the first of the given futures to complete successfully
    * @throws IllegalArgumentException if the given collection of futures is empty
    */
   static <T> FluentFuture<T> firstSuccessfulOf(
         Iterable<FluentFuture<? extends T>> futures) {
      final List<FluentFuture<? extends T>> futureList = snapshot(futures);
      if (futureList.isEmpty()) {
         throw new IllegalArgumentException("must supply at least one future");
      } else if (futureList.size() == 1) {
         return cast(futureList.get(0));
      }
      final AbstractFluentFuture<T> result = new AbstractFluentFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.cancel(mayInterrupt)) {
               for (FluentFuture<?> future : futureList) {
                  future.cancel(mayInterrupt);
               }
               return true;
            }
            return false;
         }
      };
      final AtomicInteger remaining = new AtomicInteger(futureList.size());
      FutureVisitor<T> visitor = new FutureVisitor<T>() {
         @Override
         public void successful(T value) {
            result.setValue(value);
            remaining.decrementAndGet();
         }

         @Override
         public void failed(Throwable failure) {
            if (remaining.decrementAndGet() == 0) {
               // Only if last one fails do we mark the whole result as failed.
               result.setFailure(failure);
            }
         }

         @Override
         public void cancelled() {
            if (remaining.decrementAndGet() == 0) {
               // If last one is cancelled, then we need to cancel the result. Otherwise,
               // we let one of the remaining futures provide the result's value.
               result.setCancelled();
            }
         }
      };
      for (FluentFuture<? extends T> future : futureList) {
         future.visitWhenDone(visitor);
      }
      return result;
   }

   /**
    * Dereferences a future future. The returned future will complete successfully when the
    * specified future and its value complete. The future will fail if either the input future or
    * its value fails. 
    *
    * <p>The returned future's cancellation status will be kept in sync with the input futures. So
    * if the returned future is cancelled, the input future or its value will also be cancelled. If
    * the input future or its value is cancelled, the returned future will also be cancelled.
    *
    * @param future the future future
    * @return a future value that represents the value of the future future
    */
   static <T> FluentFuture<T> dereference(
         FluentFuture<? extends FluentFuture<T>> future) {
      AtomicReference<FluentFuture<?>> outstanding = new AtomicReference<>(future);
      // visible thanks to always written before volatile write and read after volatile read
      VariableBoolean shouldInterrupt = new VariableBoolean();
      AbstractFluentFuture<T> result = new AbstractFluentFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               shouldInterrupt.set(mayInterrupt);
               // If there's a race here and the input future completes before we can cancel it,
               // then the listener below will execute. If we get here first, then the null value
               // signals the listener to cancel the future's value. If we get here second, then
               // outstanding has already been set to the future's value. That way, we've made
               // certain that the cancellation correctly propagates.
               FluentFuture<?> toCancel = outstanding.getAndSet(null);
               if (toCancel != null) {
                  toCancel.cancel(mayInterrupt);
               }
               return true;
            }
            return false;
         }
      };
      future.visitWhenDone(new FutureVisitor<FluentFuture<T>>() {
         @Override
         public void successful(FluentFuture<T> value) {
            if (outstanding.getAndSet(value) == null) {
               // result already cancelled, so also cancel this value
               if (value != null) {
                  value.cancel(shouldInterrupt.get());
               }
               return;
            }
            if (value == null) {
               result.setFailure(new NullPointerException());
               return;
            }
            value.visitWhenDone(new FutureVisitor<T>() {
               @Override
               public void successful(T t) {
                  result.setValue(t);
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
}
