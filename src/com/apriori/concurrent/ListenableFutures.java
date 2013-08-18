package com.apriori.concurrent;

import static com.apriori.concurrent.FutureListeners.forVisitor;
import static com.apriori.concurrent.ListenableExecutors.sameThreadExecutor;

import com.apriori.possible.Fulfillable;
import com.apriori.possible.Fulfillables;
import com.apriori.util.Function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lots of goodies related to {@link ListenableFuture}s. These include factory methods for creating
 * already-completed futures, for wrapping normal futures and executors in listenable versions, and
 * cool patterns for asynchronously processing futures using listeners (like transforming future
 * values, joining multiple values into one, etc.).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
//TODO: javadoc
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
    * Updates a {@link SimpleListenableFuture} to have the same disposition (be it successful,
    * failed, or cancelled) as the specified future. This blocks, uninterruptibly, for the specified
    * source future to complete.
    * 
    * @param future the source future
    * @param copy updated so as to be a copy of the other
    */
   static <T> void copyFutureInto(Future<T> future, SimpleListenableFuture<T> copy) {
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
   public static <T> ListenableFuture<T> makeListenable(final Future<T> future) {
      if (future instanceof ListenableFuture) {
         return (ListenableFuture<T>) future;
      }
      if (future.isDone()) {
         // can get the value immediately
         return copy(future);
      }
      return new ListenableFutureWrapper<T>(future);
   }

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
    * Adds a listener that visits the specified future using a {@linkplain
    * ListenableExecutors#sameThreadExecutor() same thread executor}. This is such a common pattern
    * for listeners that this utility method is provided to reduce the boiler-plate involved in
    * otherwise directly calling {@code future.addListener(...)}.
    * 
    * @param future the listenable future that will be visited
    * @param visitor the visitor that will be called once the future completes
    */
   public static <T> void addCallback(ListenableFuture<T> future,
         FutureVisitor<? super T> visitor) {
      future.addListener(forVisitor(visitor), sameThreadExecutor());
   }
   
   public static <T> ListenableFuture<T> chain(ListenableFuture<?> future, final Callable<T> task,
         Executor executor) {
      return chain(future, new Function<Object, T>() {
         @Override public T apply(Object unused) {
            try {
               return task.call();
            } catch (Exception e) {
               throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
            }
         }
      }, executor);
   }

   public static <T> ListenableFuture<T> chain(ListenableFuture<?> future, final Runnable task,
         final T result, Executor executor) {
      return chain(future, new Function<Object, T>() {
         @Override public T apply(Object unused) {
            task.run();
            return result;
         }
      }, executor);
   }

   public static ListenableFuture<Void> chain(ListenableFuture<?> future, Runnable task,
         Executor executor) {
      return chain(future, task, null, executor);
   }

   public static <T, U> ListenableFuture<U> chain(ListenableFuture<T> future,
         final Function<? super T, ? extends U> function, Executor executor) {
      final SimpleListenableFuture<U> result = new SimpleListenableFuture<U>();
      future.addListener(forVisitor(new FutureVisitor<T>() {
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
      }), executor);
      return result;
   }

   public static <T, U> ListenableFuture<U> transform(final ListenableFuture<T> future,
         final Function<? super T, ? extends U> function) {
      final SimpleListenableFuture<U> result = new SimpleListenableFuture<U>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               future.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      addCallback(future, new FutureVisitor<T>() {
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

   public static <T> ListenableFuture<List<T>> join(ListenableFuture<? extends T>... futures) {
      return join(Arrays.asList(futures));
   }

   public static <T> ListenableFuture<List<T>> join(
         final Iterable<ListenableFuture<? extends T>> futures) {
      List<ListenableFuture<? extends T>> futureList;
      if (futures instanceof List) {
         futureList = (List<ListenableFuture<? extends T>>) futures;
      } else {
         if (futures instanceof Collection) {
            futureList = new ArrayList<ListenableFuture<? extends T>>(
                  (Collection<ListenableFuture<? extends T>>) futures);
         } else {
            futureList = new ArrayList<ListenableFuture<? extends T>>();
            for (ListenableFuture<? extends T> future : futures) {
               futureList.add(future);
            }
         }
      }
      if (futureList.isEmpty()) {
         return completedFuture(Collections.<T>emptyList());
      }
      final int len = futureList.size();
      if (len == 1) {
         return transform(futureList.get(0), new Function<T, List<T>>() {
            @Override
            public List<T> apply(T input) {
               return Collections.singletonList(input);
            }
         });
      }
      final List<Fulfillable<T>> resolved = new ArrayList<Fulfillable<T>>(len);
      for (int i = 0; i < len; i++) {
         resolved.add(Fulfillables.<T>create());
      }
      @SuppressWarnings({"unchecked", "rawtypes"}) // java generics not expressive enough
      CombiningFuture<List<T>> result = new CombiningFuture<List<T>>((Collection)futureList) {
         @Override List<T> computeValue() {
            List<T> list = new ArrayList<T>(len);
            for (Fulfillable<T> f : resolved) {
               list.add(f.get());
            }
            return list;
         }
      };
      for (int i = 0; i < len; i++) {
         futureList.get(i).addListener(
               forVisitor(new CombiningVisitor<T>(resolved.get(i), result)),
               sameThreadExecutor());
      }
      return result;
   }
   
   public static <T, U, V> ListenableFuture<V> combine(
         ListenableFuture<T> future1, ListenableFuture<U> future2,
         final Function.Bivariate<? super T, ? super U, ? extends V> function) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-args array is safe here
      CombiningFuture<V> result = new CombiningFuture<V>(Arrays.asList(future1, future2)) {
         @Override V computeValue() {
            return function.apply(t.get(), u.get());
         }
      };
      future1.addListener(forVisitor(new CombiningVisitor<T>(t, result)), sameThreadExecutor());
      future2.addListener(forVisitor(new CombiningVisitor<U>(u, result)), sameThreadExecutor());
      return result;
   }

   public static <T, U, V, W> ListenableFuture<W> combine(ListenableFuture<T> future1,
         ListenableFuture<U> future2, ListenableFuture<V> future3,
         final Function.Trivariate<? super T, ? super U, ? super V, ? extends W> function) {
      final Fulfillable<T> t = Fulfillables.create();
      final Fulfillable<U> u = Fulfillables.create();
      final Fulfillable<V> v = Fulfillables.create();
      @SuppressWarnings("unchecked") // generic var-args array is safe here
      CombiningFuture<W> result = new CombiningFuture<W>(Arrays.asList(future1, future2, future3)) {
         @Override W computeValue() {
            return function.apply(t.get(), u.get(), v.get());
         }
      };
      future1.addListener(forVisitor(new CombiningVisitor<T>(t, result)), sameThreadExecutor());
      future2.addListener(forVisitor(new CombiningVisitor<U>(u, result)), sameThreadExecutor());
      future3.addListener(forVisitor(new CombiningVisitor<V>(v, result)), sameThreadExecutor());
      return result;
   }
   
   public static <T> ListenableFuture<T> dereference(
         final ListenableFuture<? extends ListenableFuture<T>> future) {
      final SimpleListenableFuture<T> result = new SimpleListenableFuture<T>() {
         @Override public boolean cancel(boolean mayInterrupt) {
            if (super.setCancelled()) {
               future.cancel(mayInterrupt);
               return true;
            }
            return false;
         }
      };
      future.addListener(forVisitor(new FutureVisitor<ListenableFuture<T>>() {
         @Override
         public void successful(ListenableFuture<T> value) {
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
   static abstract class CombiningFuture<T> extends SimpleListenableFuture<T> {
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
   static class CombiningVisitor<T> implements FutureVisitor<T> {
      private final Fulfillable<T> component;
      private final CombiningFuture<?> result;

      /**
       * Creates a new visitor. This visitor is added as a listener to the component future.
       * 
       * @param component the component used as input to computing the combined result, fulfilled
       *       when the component future successfully completes
       * @param result the combined result, which is marked on successful completion of a component
       *       or set as cancelled or failed on cancelled or failed completion of a component
       */
      CombiningVisitor(Fulfillable<T> component, CombiningFuture<?> result) {
         this.component = component;
         this.result = result;
      }
      
      @Override
      public void successful(T t) {
         if (component.fulfill(t)) {
            result.mark();
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
   }
   
   private static class ListenableFutureWrapper<T> extends SimpleListenableFuture<T> {
      final Future<T> future;
      
      ListenableFutureWrapper(Future<T> future) {
         this.future = future;
         // use a new thread that just blocks for the future and then completes the result
         new Thread() {
            @Override public void run() {
               copyFutureInto(ListenableFutureWrapper.this.future,  ListenableFutureWrapper.this);
            }
         }.start();
      }
      
      @Override public boolean cancel(boolean mayInterrupt) {
         if (future.cancel(mayInterrupt)) {
            setCancelled();
            return true;
         }
         return false;
      }
   }

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
   
   private static class CompletedFuture<T> implements ListenableFuture<T> {
      private final T value;
      
      CompletedFuture(T value) {
         this.value = value;
      }
      
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
         return true;
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
      public void addListener(final FutureListener<? super T> listener, Executor executor) {
         final ListenableFuture<T> f = this;
         FutureListenerSet.runListener(f,  listener, executor);
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         visitor.successful(value);
      }

      @Override
      public void await() throws InterruptedException {
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         return true;
      }
   }
   
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
   
   private static class FailedFuture<T> implements ListenableFuture<T> {
      private final Throwable failure;
      
      FailedFuture(Throwable failure) {
         this.failure = failure;
      }

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
         return true;
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
      public void addListener(final FutureListener<? super T> listener, Executor executor) {
         final ListenableFuture<T> f = this;
         FutureListenerSet.runListener(f,  listener, executor);
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         visitor.failed(failure);
      }
      
      @Override
      public void await() throws InterruptedException {
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         return true;
      }
   }

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
   
   private static class CancelledFuture<T> implements ListenableFuture<T> {

      /**
       * Since this future is stateless and immutable, we don't really need more than one.
       */
      static final CancelledFuture<?> INSTANCE = new CancelledFuture<Object>();
      
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         return false;
      }

      @Override
      public boolean isCancelled() {
         return true;
      }

      @Override
      public boolean isDone() {
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
      public void addListener(final FutureListener<? super T> listener, Executor executor) {
         final ListenableFuture<T> f = this;
         FutureListenerSet.runListener(f,  listener, executor);
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         visitor.cancelled();
      }
      
      @Override
      public void await() throws InterruptedException {
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         return true;
      }
   }

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
}
