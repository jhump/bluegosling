package com.apriori.concurrent;

import static com.apriori.concurrent.FutureListener.forVisitor;
import static com.apriori.concurrent.ListenableExecutors.sameThreadExecutor;
import static com.apriori.concurrent.ListenableFuture.cast;

import com.apriori.possible.Fulfillable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lots of goodies related to {@link ListenableFuture}s. These include factory methods for creating
 * already-completed futures, for wrapping normal futures and executors in listenable versions, and
 * cool patterns for asynchronously processing futures using listeners (like transforming future
 * values, joining multiple values into one, etc.).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
public final class ListenableFutures {
   private ListenableFutures() {
   }
   
   /**
    * Returns a copy of the specified future, as a listenable one. This method will block if the
    * specified future is not already done.
    * 
    * @param future a future
    * @return a listenable version of the specified future (once it completes)
    */
   static <T> ListenableFuture<T> copy(Future<T> future) {
      boolean interrupted = false;
      try {
         while (true) {
            try {
               // if future is not done, this will block until it is
               return completedFuture(future.get());
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } catch (ExecutionException e) {
         return failedFuture(e.getCause());
      } catch (CancellationException e) {
         return cancelledFuture();
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt(); // restore interrupt status
         }
      }
   }

   /**
    * Returns a copy of the specified scheduled future, as a listenable one. This method will block
    * if the specified future is not already done.
    * 
    * @param future a future
    * @return a listenable version of the specified future (once it completes)
    */
   static <T> ListenableScheduledFuture<T> copy(ScheduledFuture<T> future) {
      boolean interrupted = false;
      try {
         while (true) {
            try {
               // if future is not done, this will block until it is
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
    * Converts the specified future into a {@link ListenableFuture}. If the specified future
    * <em>is</em> a {@link ListenableFuture}, it is returned without any conversion. Note that if
    * the specified future is not yet done, conversion requires creating a new thread. The thread
    * simply blocks until the specified future completes, at which time the returned listenable
    * future is also completed (asynchronously).
    * 
    * <p>The returned future's cancellation status will be kept in sync with the specified future.
    * So if the returned future is cancelled, so too will the underlying future be cancelled, and
    * vice versa.
    * 
    * @param future the future
    * @return a listenable version of the specified future
    */
   public static <T> ListenableFuture<T> makeListenable(Future<T> future) {
      if (future instanceof ListenableFuture) {
         return (ListenableFuture<T>) future;
      }
      if (future.isDone()) {
         // can get the value immediately
         return copy(future);
      }
      return new ListenableFutureWrapper<T>(future);
   }

   /**
    * Converts the specified future into a {@link ListenableScheduledFuture}. If the specified
    * future <em>is</em> a {@link ListenableScheduledFuture}, it is returned without any conversion.
    * Note that if the specified future is not yet done, conversion requires creating a new thread.
    * The thread simply blocks until the specified future completes, at which time the returned
    * listenable future is also completed (asynchronously).
    * 
    * <p>The returned future's cancellation status will be kept in sync with the specified future.
    * So if the returned future is cancelled, so too will the underlying future be cancelled, and
    * vice versa.
    *
    * @param future the scheduled future
    * @return a listenable version of the specified future
    */
   public static <T> ListenableScheduledFuture<T> makeListenable(final ScheduledFuture<T> future) {
      if (future instanceof ListenableScheduledFuture) {
         return (ListenableScheduledFuture<T>) future;
      }
      if (future.isDone()) {
         // can get the value immediately
         return copy(future);
      }
      return new ListenableScheduledFutureWrapper<T>(future);
   }

   /**
    * Returns a future that has already successfully completed with the specified value.
    * 
    * @param value the future result
    * @return a future that is immediately done
    */
   public static <T> ListenableFuture<T> completedFuture(final T value) {
      return new CompletedFuture<T>(value);
   }
   
   /**
    * Returns a future that has already failed due to the specified cause.
    * 
    * @param failure the cause of future failure
    * @return a future that is immediately done
    */
   public static <T> ListenableFuture<T> failedFuture(final Throwable failure) {
      return new FailedFuture<T>(failure);
   }

   /**
    * Returns a future that has already been cancelled.
    * 
    * @return a future that is immediately done
    */
   @SuppressWarnings("unchecked") // CancelledFuture is stateless and immutable, so cast is safe
   public static <T> ListenableFuture<T> cancelledFuture() {
      return (CancelledFuture<T>) CancelledFuture.INSTANCE;
   }

   /**
    * Returns a future that will never finish. This can be useful in some circumstances for
    * asynchronous functional idioms, and also for testing.
    * 
    * <p>The returned future cannot be used with blocking calls, since they would never return or
    * always timeout. Additionally, the future cannot be cancelled. So calls to
    * {@link ListenableFuture#cancel(boolean)}, both variants of {@link ListenableFuture#get}, and
    * both variants of {@link ListenableFuture#await} all throw {@link UnsupportedOperationException}.
    *
    * @return a future that will never finish
    */
   @SuppressWarnings("unchecked") // UnfinishableFuture is stateless and immutable, so cast is safe
   public static <T> ListenableFuture<T> unfinishableFuture() {
      return (ListenableFuture<T>) UnfinishableFuture.INSTANCE;
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
   public static <T> ListenableFuture<List<T>> join(ListenableFuture<? extends T>... futures) {
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
   public static <T> ListenableFuture<List<T>> join(
         final Iterable<ListenableFuture<? extends T>> futures) {
      List<ListenableFuture<? extends T>> futureList = snapshot(futures);
      if (futureList.isEmpty()) {
         return completedFuture(Collections.emptyList());
      }
      final int len = futureList.size();
      if (len == 1) {
         return futureList.get(0).transform((o) -> Collections.singletonList(o));
      }
      @SuppressWarnings({"unchecked", "rawtypes"}) // java generics not expressive enough
      CombiningFuture<List<T>> result = new CombiningFuture<List<T>>((Collection) futureList) {
         @Override List<T> computeValue() {
            List<T> list = new ArrayList<T>(len);
            for (ListenableFuture<? extends T> future : futureList) {
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
   public static <T> ListenableFuture<T> firstOf(ListenableFuture<? extends T>... futures) {
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
   public static <T> ListenableFuture<T> firstOf(Iterable<ListenableFuture<? extends T>> futures) {
      final List<ListenableFuture<? extends T>> futureList = snapshot(futures);
      if (futureList.isEmpty()) {
         throw new IllegalArgumentException("must supply at least one future");
      } else if (futureList.size() == 1) {
         // co-variance makes it safe since all operations return a T, none take a T
         @SuppressWarnings("unchecked")
         ListenableFuture<T> theOne = (ListenableFuture<T>) futureList.get(0);
         return theOne;
      }
      final AbstractListenableFuture<T> result = new AbstractListenableFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.cancel(mayInterrupt)) {
               for (ListenableFuture<?> future : futureList) {
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
      for (ListenableFuture<? extends T> future : futureList) {
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
   public static <T> ListenableFuture<T> firstSuccessfulOf(
         ListenableFuture<? extends T>... futures) {
      return firstOf(Arrays.asList(futures));
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
   public static <T> ListenableFuture<T> firstSuccessfulOf(
         Iterable<ListenableFuture<? extends T>> futures) {
      final List<ListenableFuture<? extends T>> futureList = snapshot(futures);
      if (futureList.isEmpty()) {
         throw new IllegalArgumentException("must supply at least one future");
      } else if (futureList.size() == 1) {
         return cast(futureList.get(0));
      }
      final AbstractListenableFuture<T> result = new AbstractListenableFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.cancel(mayInterrupt)) {
               for (ListenableFuture<?> future : futureList) {
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
      for (ListenableFuture<? extends T> future : futureList) {
         future.visitWhenDone(visitor);
      }
      return result;
   }

   private static <T> List<ListenableFuture<? extends T>> snapshot(
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
   public static <T> ListenableFuture<T> dereference(
         final ListenableFuture<? extends ListenableFuture<T>> future) {
      final AtomicReference<ListenableFuture<?>> outstanding =
            new AtomicReference<ListenableFuture<?>>(future);
      final AtomicBoolean shouldInterrupt = new AtomicBoolean();
      final AbstractListenableFuture<T> result = new AbstractListenableFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               shouldInterrupt.set(mayInterrupt);
               // If there's a race here and the input future completes before we can cancel it,
               // then the listener below will execute. If we get here first, then the null value
               // signals the listener to cancel the future's value. If we get here second, then
               // outstanding has already been set to the future's value. That way, we've made
               // certain that the cancellation correctly propagates.
               ListenableFuture<?> toCancel = outstanding.getAndSet(null);
               if (toCancel != null) {
                  toCancel.cancel(mayInterrupt);
               }
               return true;
            }
            return false;
         }
      };
      future.addListener(forVisitor(new FutureVisitor<ListenableFuture<T>>() {
         @Override
         public void successful(ListenableFuture<T> value) {
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
            value.addListener(forVisitor(new FutureVisitor<T>() {
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
            }), sameThreadExecutor());
         }
   
         @Override
         public void failed(Throwable t) {
            result.setFailure(t);
         }
         
         @Override
         public void cancelled() {
            result.setCancelled();
         }
      }), sameThreadExecutor());
      return result;
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
   private static class ListenableFutureWrapper<T> extends AbstractListenableFuture<T> {
      final Future<T> future;
      
      ListenableFutureWrapper(Future<T> future) {
         this.future = future;
         // use a new thread that just blocks for the future and then completes the result
         new Thread() {
            @Override public void run() {
               copyFutureInto(ListenableFutureWrapper.this.future, ListenableFutureWrapper.this);
            }
         }.start();
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
   private static class ListenableScheduledFutureWrapper<T> extends ListenableFutureWrapper<T>
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
   private static class CompletedFuture<T> extends AbstractFinishedFuture<T> {
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
   private static class CompletedScheduledFuture<T> extends CompletedFuture<T>
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
   private static class FailedFuture<T> extends AbstractFinishedFuture<T> {
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
   private static class FailedScheduledFuture<T> extends FailedFuture<T>
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
   private static class CancelledFuture<T> extends AbstractFinishedFuture<T> {

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
   private static class CancelledScheduledFuture<T> extends CancelledFuture<T>
         implements ListenableScheduledFuture<T> {
      private static Delayed delayed;

      @SuppressWarnings("synthetic-access") // super-class ctor is private
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
   private static class UnfinishableFuture<T> implements ListenableFuture<T> {

      /**
       * Since this future is stateless and immutable, we don't really need more than one.
       */
      static final UnfinishableFuture<?> INSTANCE = new UnfinishableFuture<Object>();
      
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         throw new UnsupportedOperationException();
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
         throw new UnsupportedOperationException();
      }

      @Override
      public void await() throws InterruptedException {
         throw new UnsupportedOperationException();
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
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
}
