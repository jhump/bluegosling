package com.apriori.concurrent;

import static com.apriori.concurrent.FutureTuples.asPair;
import static com.apriori.concurrent.ListenableFuture.cast;
import static com.apriori.concurrent.ListenableFuture.makeListenable;

import com.apriori.possible.Fulfillable;
import com.apriori.tuples.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Lots of implementation classes, used by static and default methods of {@link ListenableFuture}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
final class ListenableFutures {
   private ListenableFutures() {
   }
   
   /**
    * Returns a copy of the specified future, as a listenable one. This specified future should
    * already be done, and the returned future will be immediately done.
    * 
    * @param future a future
    * @return a listenable version of the specified future
    */
   static <T> ListenableFuture<T> immediateCopy(Future<T> future) {
      assert future.isDone();
      // in case this thread gets interrupted, ignore it (we're not
      // actually blocking since the given future is already done)
      boolean interrupted = false;
      try {
         while (true) {
            try {
               return ListenableFuture.completedFuture(future.get());
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } catch (ExecutionException e) {
         return ListenableFuture.failedFuture(e.getCause());
      } catch (CancellationException e) {
         return ListenableFuture.cancelledFuture();
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt(); // restore interrupt status
         }
      }
   }

   /**
    * Returns a copy of the specified scheduled future, as a listenable one. This specified future
    * should already be done, and the returned future will be immediately done.
    * 
    * @param future a future
    * @return a listenable version of the specified future
    */
   static <T> ListenableScheduledFuture<T> immediateCopy(ScheduledFuture<T> future) {
      assert future.isDone();
      // in case this thread gets interrupted, ignore it (we're not
      // actually blocking since the given future is already done)
      boolean interrupted = false;
      try {
         while (true) {
            try {
               return new CompletedScheduledFuture<T>(future.get(), future);
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } catch (ExecutionException e) {
         return new FailedScheduledFuture<T>(e.getCause(), future);
      } catch (CancellationException e) {
         return new CancelledScheduledFuture<T>(future);
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt(); // restore interrupt status
         }
      }
   }
   
   /**
    * Updates a {@link AbstractListenableFuture} to have the same disposition (be it successful,
    * failed, or cancelled) as the specified future. This blocks, uninterruptibly, for the specified
    * source future to complete.
    * 
    * @param future the source future
    * @param copy updated so as to be a copy of the other
    */
   static <T> void copyFutureInto(Future<T> future, AbstractListenableFuture<T> copy) {
      boolean interrupted = false;
      try {
         while (true) {
            try {
               // if future is not done, this will block until it is
               copy.setValue(future.get());
               break;
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } catch (ExecutionException e) {
         copy.setFailure(e.getCause());
      } catch (CancellationException e) {
         copy.setCancelled();
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt(); // restore interrupt status
         }
      }
   }
   
   /**
    * Creates a defensive copy and snapshot of the given iterable.
    *
    * @param futures futures to snapshot
    * @return a copy/snapshot of the given iterable, as a list
    */
   static <T> List<ListenableFuture<? extends T>> snapshot(
         Iterable<ListenableFuture<? extends T>> futures) {
      if (futures instanceof Collection) {
         Collection<ListenableFuture<? extends T>> coll =
               (Collection<ListenableFuture<? extends T>>) futures;
         if (coll.isEmpty()) {
            return Collections.emptyList();
         }
         return new ArrayList<ListenableFuture<? extends T>>(coll);
      } else {
         List<ListenableFuture<? extends T>> ret = new ArrayList<ListenableFuture<? extends T>>();
         for (ListenableFuture<? extends T> future : futures) {
            ret.add(future);
         }
         return ret;
      }
   }
   
   /**
    * An abstract future that combines the results from other futures. General use entails calling
    * code to {@link #mark()} this future as each constituent future completes. Subclasses are
    * responsible for collecting these results and then implementing {@link #computeValue()} to
    * actually combine the results into a single value.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the combined result
    */
   static abstract class CombiningFuture<T> extends AbstractListenableFuture<T> {
      private final Collection<ListenableFuture<?>> components;
      private final AtomicInteger remaining;

      /**
       * Constructs a new combining future. The specified list should be immutable. Its size is
       * used to determine how many components must be {@linkplain #mark() marked} before a result
       * can be ready. If this combined future is cancelled, this class attempts to cancel all of
       * the underlying components as well.
       * 
       * @param components the list of component futures
       */
      CombiningFuture(Collection<ListenableFuture<?>> components) {
         this.components = components;
         remaining = new AtomicInteger(components.size());
         FutureVisitor<Object> visitor = new CombiningVisitor(this);
         for (ListenableFuture<?> future : components) {
            future.visitWhenDone(visitor);
         }
      }
      
      /**
       * Computes the combined value of this future once all components have completed.
       * 
       * @return the combined value
       */
      abstract T computeValue();

      /**
       * Marks a single result as complete. Once all components have completed, this will set the
       * value to the {@linkplain #computeValue() combined result}. 
       */
      void mark() {
         if (remaining.decrementAndGet() == 0) {
            try {
               setValue(computeValue());
            } catch (Throwable t) {
               setFailure(t);
            }
         }
      }
      
      @Override public boolean cancel(boolean mayInterrupt) {
         if (super.cancel(mayInterrupt)) {
            for (ListenableFuture<?> future : components) {
               future.cancel(mayInterrupt);
            }
            return true;
         }
         return false;
      }
   }

   /**
    * A visitor used in conjunction with {@link CombiningFuture} to produce future values that are
    * the results of combining other component futures. The general use pattern is that a visitor
    * is created for each component future and added to that component as a listener. The underlying
    * {@link Fulfillable} is fulfilled when the component calls the listener. The combined result
    * is then computed from all of the fulfilled results.
    * 
    * <p>If a component future is cancelled or fails, this visitor will mark the combined result as
    * cancelled or failed also.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the component value
    */
   static class CombiningVisitor implements FutureVisitor<Object> {
      private final CombiningFuture<?> result;

      /**
       * Creates a new visitor. This visitor is added as a listener to the component future.
       * 
       * @param component the component used as input to computing the combined result, fulfilled
       *       when the component future successfully completes
       * @param result the combined result, which is marked on successful completion of a component
       *       or set as cancelled or failed on cancelled or failed completion of a component
       */
      CombiningVisitor(CombiningFuture<?> result) {
         this.result = result;
      }
      
      @Override
      public void successful(Object o) {
         result.mark();
      }

      @Override
      public void failed(Throwable t) {
         result.setFailure(t);
      }

      @Override
      public void cancelled() {
         result.setCancelled();
      }
   }
   
   /**
    * A listenable future that wraps a non-listenable one. This creates a new thread to block until
    * the input future completes.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class ListenableFutureWrapper<T> extends AbstractListenableFuture<T> {
      final Future<T> future;
      
      ListenableFutureWrapper(Future<T> future) {
         this.future = future;
         // use a new thread that just blocks for the future and then completes the result
         new Thread(() -> { copyFutureInto(future, this); }).start();
      }
      
      @Override public boolean cancel(boolean mayInterrupt) {
         if (future.cancel(mayInterrupt)) {
            // This will happen automatically from the blocking thread, when it sees the input
            // future cancelled. But that is asynchronous, and we want this future to be cancelled
            // synchronously. So we'll do it here, too, to make sure this future is cancelled when
            // we return from this method.
            setCancelled();
            return true;
         }
         return false;
      }
   }

   /**
    * A listenable scheduled future that wraps a non-listenable one. This creates a new thread to
    * block until the input future completes.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class ListenableScheduledFutureWrapper<T> extends ListenableFutureWrapper<T>
         implements ListenableScheduledFuture<T> {
      
      ListenableScheduledFutureWrapper(ScheduledFuture<T> future) {
         super(future);
      }
      
      @Override
      public long getDelay(TimeUnit unit) {
         return ((ScheduledFuture<T>) future).getDelay(unit);
      }

      @Override
      public int compareTo(Delayed o) {
         return ((ScheduledFuture<T>) future).compareTo(o);
      }
   }
   
   /**
    * A listenable future that wraps a {@link CompletableFuture}.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class CompletableFutureWrapper<T> implements ListenableFuture<T> {
      final CompletableFuture<T> future;
      
      CompletableFutureWrapper(CompletableFuture<T> future) {
         this.future = future;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         return future.cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
         return future.isCancelled();
      }

      @Override
      public boolean isDone() {
         return future.isDone();
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
         return future.get();
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
         return future.get(timeout, unit);
      }

      @Override
      public void await() throws InterruptedException {
         try {
            future.get();
         } catch (ExecutionException e) {
         } catch (CancellationException e) {
         }
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         try {
            future.get(limit, unit);
         } catch (ExecutionException e) {
         } catch (CancellationException e) {
         } catch (TimeoutException e) {
            return false;
         }
         return true;
      }

      @Override
      public boolean isSuccessful() {
         return future.isDone() && !future.isCompletedExceptionally();
      }

      @Override
      public T getResult() {
         if (!isSuccessful()) {
            throw new IllegalStateException("future did not complete successfully");
         }
         try {
            return future.join();
         } catch (Throwable t) {
            // sadly possible thanks to asynchronous obtrudeException
            throw new IllegalStateException("future did not complete successfully");
         }
      }

      @Override
      public boolean isFailed() {
         return future.isCompletedExceptionally() && !future.isCancelled();
      }

      @Override
      public Throwable getFailure() {
         if (!isFailed()) {
            throw new IllegalStateException("future did not complete with failure");
         }
         // ugly... consider instead using CompletableFuture.handle(...) to extract failure?
         boolean interrupted = false;
         try {
            while (true) {
               try {
                  future.get();
                  // sadly possible thanks to asynchronous obtrudeValue
                  throw new IllegalStateException("future did not complete with failure");
               } catch (ExecutionException e) {
                  return e.getCause();
               } catch (InterruptedException e) {
                  interrupted = true;
               }
            }
         } finally {
            if (interrupted) Thread.currentThread().interrupt();
         }
      }

      @Override
      public void addListener(FutureListener<? super T> listener, Executor executor) {
         ListenableFuture<T> self = this;
         future.whenCompleteAsync((err, val) -> { listener.onCompletion(self); }, executor);
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         if (!future.isDone()) {
            throw new IllegalStateException("future is not yet done");
         }
         if (future.isCancelled()) {
            visitor.cancelled();
            return;
         }
         // ugly... consider instead using CompletableFuture.handle(...) to extract failure?
         boolean interrupted = false;
         try {
            while (true) {
               try {
                  visitor.successful(future.get());
                  return;
               } catch (ExecutionException e) {
                  visitor.failed(e.getCause());
                  return;
               } catch (InterruptedException e) {
                  interrupted = true;
               }
            }
         } finally {
            if (interrupted) Thread.currentThread().interrupt();
         }
      }
      
      @Override
      public CompletableFuture<T> toCompletableFuture() {
         return future;
      }
      
      @Override
      public CompletionStage<T> asCompletionStage() {
         return future;
      }
   }
   
   /**
    * Abstract base class that implements all operations that are common to all variants of
    * futures that are already finished.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static abstract class AbstractFinishedFuture<T> implements ListenableFuture<T> {
      
      AbstractFinishedFuture() {
      }
      
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         return false;
      }

      @Override
      public boolean isDone() {
         return true;
      }

      @Override
      public void addListener(final FutureListener<? super T> listener, Executor executor) {
         final ListenableFuture<T> f = this;
         FutureListenerSet.runListener(f,  listener, executor);
      }

      @Override
      public void await() throws InterruptedException {
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         return true;
      }
   }

   /**
    * A future that has already successfully completed. 
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class CompletedFuture<T> extends AbstractFinishedFuture<T> {
      private final T value;
      
      CompletedFuture(T value) {
         this.value = value;
      }
      
      @Override
      public boolean isCancelled() {
         return false;
      }

      @Override
      public T get() {
         return value;
      }

      @Override
      public T get(long timeout, TimeUnit unit) {
         return value;
      }

      @Override
      public boolean isSuccessful() {
         return true;
      }

      @Override
      public T getResult() {
         return value;
      }

      @Override
      public boolean isFailed() {
         return false;
      }

      @Override
      public Throwable getFailure() {
         throw new IllegalStateException();
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         visitor.successful(value);
      }
   }
   
   /**
    * A scheduled future that has already successfully completed. 
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class CompletedScheduledFuture<T> extends CompletedFuture<T>
         implements ListenableScheduledFuture<T> {
      private static Delayed delayed;
      
      CompletedScheduledFuture(T value, Delayed delayed) {
         super(value);
         this.delayed = delayed;
      }

      @Override
      public long getDelay(TimeUnit unit) {
         return delayed.getDelay(unit);
      }

      @Override
      public int compareTo(Delayed o) {
         return delayed.compareTo(o);
      }
   }
   
   /**
    * A future that has already failed.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FailedFuture<T> extends AbstractFinishedFuture<T> {
      private final Throwable failure;
      
      FailedFuture(Throwable failure) {
         this.failure = failure;
      }

      @Override
      public boolean isCancelled() {
         return false;
      }

      @Override
      public T get() throws ExecutionException {
         throw new ExecutionException(failure);
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws ExecutionException {
         throw new ExecutionException(failure);
      }

      @Override
      public boolean isSuccessful() {
         return false;
      }

      @Override
      public T getResult() {
         throw new IllegalStateException();
      }

      @Override
      public boolean isFailed() {
         return true;
      }

      @Override
      public Throwable getFailure() {
         return failure;
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         visitor.failed(failure);
      }
   }

   /**
    * A scheduled future that has already failed.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FailedScheduledFuture<T> extends FailedFuture<T>
         implements ListenableScheduledFuture<T> {
      private static Delayed delayed;
      
      FailedScheduledFuture(Throwable failure, Delayed delayed) {
         super(failure);
         this.delayed = delayed;
      }
      
      @Override
      public long getDelay(TimeUnit unit) {
         return delayed.getDelay(unit);
      }
      
      @Override
      public int compareTo(Delayed o) {
         return delayed.compareTo(o);
      }
   }
   
   /**
    * A future that has already been cancelled.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class CancelledFuture<T> extends AbstractFinishedFuture<T> {

      /**
       * Since this future is stateless and immutable, we don't really need more than one.
       */
      static final CancelledFuture<?> INSTANCE = new CancelledFuture<Object>();
      
      @Override
      public boolean isCancelled() {
         return true;
      }

      @Override
      public T get() throws ExecutionException {
         throw new CancellationException();
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws ExecutionException {
         throw new CancellationException();
      }

      @Override
      public boolean isSuccessful() {
         return false;
      }

      @Override
      public T getResult() {
         throw new IllegalStateException();
      }

      @Override
      public boolean isFailed() {
         return false;
      }

      @Override
      public Throwable getFailure() {
         throw new IllegalStateException();
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         visitor.cancelled();
      }
   }

   /**
    * A scheduled future that has already been cancelled.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class CancelledScheduledFuture<T> extends CancelledFuture<T>
         implements ListenableScheduledFuture<T> {
      private static Delayed delayed;

      CancelledScheduledFuture(Delayed delayed) {
         this.delayed = delayed;
      }
      
      @Override
      public long getDelay(TimeUnit unit) {
         return delayed.getDelay(unit);
      }
      
      @Override
      public int compareTo(Delayed o) {
         return delayed.compareTo(o);
      }
   }
   
   /**
    * A future that can never finish.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class UnfinishableFuture<T> implements ListenableFuture<T> {

      /**
       * Since this future is stateless and immutable, we don't really need more than one.
       */
      static final UnfinishableFuture<?> INSTANCE = new UnfinishableFuture<Object>();
      
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         return false;
      }

      @Override
      public boolean isCancelled() {
         return false;
      }

      @Override
      public boolean isDone() {
         return false;
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
         throw new UnsupportedOperationException();
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
         // For maximum compatibility, allow negative or zero duration and just throw
         // TimeoutException. But we don't support positive durations (no blocking)
         if (timeout <= 0) {
            throw new TimeoutException();
         }
         throw new UnsupportedOperationException();
      }

      @Override
      public void await() throws InterruptedException {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         // For maximum compatibility, allow negative or zero duration and just throw
         // TimeoutException. But we don't support positive durations (no blocking)
         if (limit <= 0) {
            return false;
         }
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean isSuccessful() {
         return false;
      }

      @Override
      public T getResult() {
         throw new IllegalStateException();
      }

      @Override
      public boolean isFailed() {
         return false;
      }

      @Override
      public Throwable getFailure() {
         throw new IllegalStateException();
      }

      @Override
      public void addListener(FutureListener<? super T> listener, Executor executor) {
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         throw new IllegalStateException();
      }
   }
   
   /**
    * A view of a {@link ListenableFuture} as a {@link CompletionStage}.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class ListenableCompletionStage<T> implements CompletionStage<T> {
      final ListenableFuture<T> future;
      
      ListenableCompletionStage(ListenableFuture<T> future) {
         this.future = future;
      }

      @Override
      public <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
         return thenApplyAsync(fn, SameThreadExecutor.get());
      }

      @Override
      public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
         return thenApplyAsync(fn, ForkJoinPool.commonPool());
      }

      @Override
      public <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn,
            Executor executor) {
         return proceed(future, executor, ListenableCompletionStage.<T, U>apply(fn))
               .asCompletionStage();
      }

      @Override
      public CompletionStage<Void> thenAccept(Consumer<? super T> action) {
         return thenAcceptAsync(action, SameThreadExecutor.get());
      }

      @Override
      public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
         return thenAcceptAsync(action, ForkJoinPool.commonPool());
      }

      @Override
      public CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
         return proceed(future, executor, accept(action)).asCompletionStage();
      }

      @Override
      public CompletionStage<Void> thenRun(Runnable action) {
         return thenRunAsync(action, SameThreadExecutor.get());
      }

      @Override
      public CompletionStage<Void> thenRunAsync(Runnable action) {
         return thenRunAsync(action, ForkJoinPool.commonPool());
      }

      @Override
      public CompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
         return proceed(future, executor, run(action)).asCompletionStage();
      }
      
      @Override
      public <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
         return thenCombineAsync(other, fn, SameThreadExecutor.get());
      }

      @Override
      public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn) {
         return thenCombineAsync(other, fn, ForkJoinPool.commonPool());
      }

      @Override
      public <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
            BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
         ListenableFuture<U> otherFuture = cast(makeListenable(other.toCompletableFuture()));
         return proceed(asPair(future, otherFuture), executor,
               ListenableCompletionStage.<Pair<T, U>, V>apply((pair) -> {
                  return fn.apply(pair.getFirst(), pair.getSecond());
               })).asCompletionStage();
      }

      @Override
      public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
         return thenAcceptBothAsync(other, action, SameThreadExecutor.get());
      }

      @Override
      public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action) {
         return thenAcceptBothAsync(other, action, ForkJoinPool.commonPool());
      }

      @Override
      public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
            BiConsumer<? super T, ? super U> action, Executor executor) {
         ListenableFuture<U> otherFuture = cast(makeListenable(other.toCompletableFuture()));
         return proceed(asPair(future, otherFuture), executor, accept((pair) -> {
            action.accept(pair.getFirst(), pair.getSecond());
         })).asCompletionStage();
      }

      @Override
      public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
         return runAfterBothAsync(other, action, SameThreadExecutor.get());
      }

      @Override
      public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
         return runAfterBothAsync(other, action, ForkJoinPool.commonPool());
      }

      @Override
      public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
            Executor executor) {
         ListenableFuture<?> otherFuture = makeListenable(other.toCompletableFuture());
         return proceed(asPair(future, otherFuture), executor, run(action)).asCompletionStage();
      }

      @Override
      public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other,
            Function<? super T, U> fn) {
         return applyToEitherAsync(other, fn, SameThreadExecutor.get());
      }

      @Override
      public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
            Function<? super T, U> fn) {
         return applyToEitherAsync(other, fn, ForkJoinPool.commonPool());
      }

      @Override
      public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
            Function<? super T, U> fn, Executor executor) {
         List<Thread> threads = new ArrayList<>(2);
         AbstractListenableFuture<U> newFuture = interruptingFuture(threads);
         // A handler that only runs once. We use it to process both input futures and whichever
         // completes first wins.
         AtomicBoolean done = new AtomicBoolean();
         BiConsumer<ListenableFuture<T>, AbstractListenableFuture<U>> handler = (self, next) -> {
            if (done.compareAndSet(false, true)) {
               apply(fn);
            }
         };
         proceed(future, newFuture, threads, executor, handler);
         ListenableFuture<T> otherFuture = cast(makeListenable(other.toCompletableFuture()));
         proceed(otherFuture, newFuture, threads, executor, handler);
         return newFuture.asCompletionStage();
      }

      @Override
      public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other,
            Consumer<? super T> action) {
         return acceptEitherAsync(other, action, SameThreadExecutor.get());
      }

      @Override
      public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
            Consumer<? super T> action) {
         return acceptEitherAsync(other, action, ForkJoinPool.commonPool());
      }

      @Override
      public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
            Consumer<? super T> action, Executor executor) {
         List<Thread> threads = new ArrayList<>(2);
         AbstractListenableFuture<Void> newFuture = interruptingFuture(threads);
         // A handler that only runs once. We use it to process both input futures and whichever
         // completes first wins.
         AtomicBoolean done = new AtomicBoolean();
         BiConsumer<ListenableFuture<T>, AbstractListenableFuture<Void>> handler = (self, next) -> {
            if (done.compareAndSet(false, true)) {
               accept(action);
            }
         };
         proceed(future, newFuture, threads, executor, handler);
         ListenableFuture<T> otherFuture = cast(makeListenable(other.toCompletableFuture()));
         proceed(otherFuture, newFuture, threads, executor, handler);
         return newFuture.asCompletionStage();
      }

      @Override
      public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
         return runAfterEitherAsync(other, action, SameThreadExecutor.get());
      }

      @Override
      public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
         return runAfterEitherAsync(other, action, ForkJoinPool.commonPool());
      }

      @Override
      public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
            Executor executor) {
         List<Thread> threads = new ArrayList<>(2);
         AbstractListenableFuture<Void> newFuture = interruptingFuture(threads);
         // A handler that only runs once. We use it to process both input futures and whichever
         // completes first wins.
         AtomicBoolean done = new AtomicBoolean();
         BiConsumer<ListenableFuture<Object>, AbstractListenableFuture<Void>> handler =
               (self, next) -> {
                  if (done.compareAndSet(false, true)) {
                     run(action);
                  }
               };
         proceed(cast(future), newFuture, threads, executor, handler);
         ListenableFuture<Object> otherFuture = cast(makeListenable(other.toCompletableFuture()));
         proceed(otherFuture, newFuture, threads, executor, handler);
         return newFuture.asCompletionStage();
      }

      @Override
      public <U> CompletionStage<U> thenCompose(
            Function<? super T, ? extends CompletionStage<U>> fn) {
         return thenComposeAsync(fn, SameThreadExecutor.get());
      }

      @Override
      public <U> CompletionStage<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn) {
         return thenComposeAsync(fn, ForkJoinPool.commonPool());
      }

      @Override
      public <U> CompletionStage<U> thenComposeAsync(
            Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
         List<Thread> threads = new ArrayList<>(2);
         AbstractListenableFuture<U> newFuture = interruptingFuture(threads);
         proceed(future, newFuture, threads, executor, (self, next) -> {
            if (self.isSuccessful()) {
               try {
                  ListenableFuture<U> successor =
                        makeListenable(fn.apply(self.getResult()).toCompletableFuture());
                  proceed(successor, next, threads, executor, apply(Function.identity()));
               } catch (Throwable t) {
                  next.setFailure(t);
               }
            } else if (self.isCancelled()) {
               next.setFailure(new CompletionException(new CancellationException()));
            } else {
               next.setFailure(new CompletionException(self.getFailure()));
            }
         });
         return newFuture.asCompletionStage();
      }

      @Override
      public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
         Executor executor = SameThreadExecutor.get();
         return ListenableCompletionStage.<T, T>proceed(future, executor, (self, next) -> {
            if (self.isSuccessful()) {
               next.setValue(self.getResult());
            } else {
               Throwable failure =
                     self.isCancelled() ? new CancellationException() : self.getFailure();
               try {
                  next.setValue(fn.apply(failure));
               } catch (Throwable t) {
                  next.setFailure(t);
               }
            }
         }).asCompletionStage();
      }

      @Override
      public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
         return whenCompleteAsync(action, SameThreadExecutor.get());
      }

      @Override
      public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
         return whenCompleteAsync(action, ForkJoinPool.commonPool());
      }

      @Override
      public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
            Executor executor) {
         return ListenableCompletionStage.<T, T>proceed(future, executor, (self, next) -> {
            try {
               if (self.isSuccessful()) {
                  action.accept(self.getResult(), null);
                  next.setValue(self.getResult());
               } else {
                  Throwable failure =
                        self.isCancelled() ? new CancellationException() : self.getFailure();
                  action.accept(null, failure);
                  next.setFailure(failure);
               }
            } catch (Throwable t) {
               next.setFailure(t);
            }
         }).asCompletionStage();
      }

      @Override
      public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
         return handleAsync(fn, SameThreadExecutor.get());
      }

      @Override
      public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
         return handleAsync(fn, ForkJoinPool.commonPool());
      }

      @Override
      public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
            Executor executor) {
         return ListenableCompletionStage.<T, U>proceed(future, executor, (self, next) -> {
            try {
               if (self.isSuccessful()) {
                  next.setValue(fn.apply(self.getResult(), null));
               } else {
                  Throwable failure =
                        self.isCancelled() ? new CancellationException() : self.getFailure();
                  next.setValue(fn.apply(null, failure));
               }
            } catch (Throwable t) {
               next.setFailure(t);
            }
         }).asCompletionStage();
      }

      @Override
      public CompletableFuture<T> toCompletableFuture() {
         return future.toCompletableFuture();
      }
      
      /**
       * Configures this completion stage to proceed to a successor. The returned future is the
       * basis for the next completion stage.
       *
       * @param input the input future, for the current completion stage
       * @param executor an executor used to run any logic associated with the next stage
       * @param handler handles completion of the current stage (first argument) and computation of
       *       the next (the second argument, which is programmatically completed when the next
       *       stage's logic completes)
       * @return a future that represents the next completion stage
       */
      private static <T, U> AbstractListenableFuture<U> proceed(
            ListenableFuture<T> input, Executor executor,
            BiConsumer<ListenableFuture<T>, AbstractListenableFuture<U>> handler) {
         List<Thread> threads = new ArrayList<>(1);
         AbstractListenableFuture<U> newFuture = interruptingFuture(threads);
         proceed(input, newFuture, threads, executor, handler);
         return newFuture;
      }
      
      /**
       * Returns a future that can interrupt threads in the given list if cancelled and allowed to
       * interrupt any still-running task.
       *
       * @param threads a collection of threads that are running on behalf of the returned future
       *       and thus may need to be interrupted if the returned future is cancelled (the list is
       *       not thread-safe, so interactions must synchronize on the list)
       * @return a future that can interrupt threads in the given list
       */
      private static <T> AbstractListenableFuture<T> interruptingFuture(List<Thread> threads) {
         return new AbstractListenableFuture<T>() {
            @Override protected void interrupt() {
               synchronized (threads) {
                  for (Thread th : threads) {
                     th.interrupt();
                  }
               }
            }
         };         
      }
      
      /**
       * Configures this completion stage to proceed to the specified successor.
       *
       * @param input the input future, for the current completion stage
       * @param output the future that represents the next completion stage
       * @param threads a collection of threads that are running on behalf of the next stage and
       *       thus may need to be interrupted if the returned future is cancelled (the list is
       *       not thread-safe, so interactions must synchronize on the list)
       * @param executor an executor used to run any logic associated with the next stage
       * @param handler handles completion of the current stage (first argument) and computation of
       *       the next (the second argument, which is programmatically completed when the next
       *       stage's logic completes)
       */
      private static <T, U> void proceed(ListenableFuture<T> input,
            AbstractListenableFuture<U> output, List<Thread> threads, Executor executor,
            BiConsumer<ListenableFuture<T>, AbstractListenableFuture<U>> handler) {
         input.addListener((f) -> {
            synchronized (threads) {
               if (output.isCancelled()) {
                  // if result was cancelled, don't bother completing it
                  return;
               }
               threads.add(Thread.currentThread());
            }
            try {
               handler.accept(cast(f), output);
            } finally {
               threads.remove(Thread.currentThread());
            }
         }, executor);
      }
      
      /**
       * Returns a handler that applies a function to the input future's result to compute the
       * value of the output future.
       *
       * @param fn the function to apply
       * @return a handler that accepts an input and output future
       */
      private static <T, U> BiConsumer<ListenableFuture<T>, AbstractListenableFuture<U>>
            apply(Function<? super T, ? extends U> fn) {
         return (self, next) -> {
            if (self.isSuccessful()) {
               try {
                  next.setValue(fn.apply(self.getResult()));
               } catch (Throwable t) {
                  next.setFailure(t);
               }
            } else if (self.isCancelled()) {
               next.setFailure(new CompletionException(new CancellationException()));
            } else {
               next.setFailure(new CompletionException(self.getFailure()));
            }
         };
      }
      
      /**
       * Returns a handler that lets a consumer process the input future's result before completing
       * the output future.
       *
       * @param action the consumer that will accept the input future's result
       * @return a handler that accepts an input and output future
       */
      private static <T> BiConsumer<ListenableFuture<T>, AbstractListenableFuture<Void>>
            accept(Consumer<? super T> action) {
         return (self, next) -> {
            if (self.isSuccessful()) {
               try {
                  action.accept(self.getResult());
                  next.setValue(null);
               } catch (Throwable t) {
                  next.setFailure(t);
               }
            } else if (self.isCancelled()) {
               next.setFailure(new CompletionException(new CancellationException()));
            } else {
               next.setFailure(new CompletionException(self.getFailure()));
            }
         };
      }
      
      /**
       * Returns a handler that executes the given task before completing the output future.
       *
       * @param action the task to execute
       * @return a handler that accepts an input and output future
       */
      private static <T> BiConsumer<ListenableFuture<T>, AbstractListenableFuture<Void>>
            run(Runnable action) {
         return (self, next) -> {
            if (self.isSuccessful()) {
               try {
                  action.run();
                  next.setValue(null);
               } catch (Throwable t) {
                  next.setFailure(t);
               }
            } else if (self.isCancelled()) {
               next.setFailure(new CompletionException(new CancellationException()));
            } else {
               next.setFailure(new CompletionException(self.getFailure()));
            }
         };
      }
   }
 }
