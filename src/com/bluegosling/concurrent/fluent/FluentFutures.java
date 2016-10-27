package com.bluegosling.concurrent.fluent;

import static com.bluegosling.concurrent.fluent.FluentFuture.*;
//import static com.bluegosling.concurrent.fluent.FluentFuture.cast;
//import static com.bluegosling.concurrent.fluent.FluentFuture.makeFluent;

import com.bluegosling.collections.MoreIterables;
import com.bluegosling.concurrent.SameThreadExecutor;
import com.bluegosling.concurrent.futures.CompletionStageFuture;
import com.bluegosling.tuples.Pair;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
//import java.util.concurrent.CancellationException;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CompletionException;
//import java.util.concurrent.CompletionStage;
//import java.util.concurrent.Delayed;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ForkJoinPool;
//import java.util.concurrent.Future;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Lots of implementation classes, used by static and default methods of {@link FluentFuture}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
final class FluentFutures {
   private FluentFutures() {
   }
   
   /**
    * Returns a copy of the specified future, as a fluent one. This specified future should
    * already be done, and the returned future will be immediately done.
    * 
    * @param future a future
    * @return a fluent version of the specified future
    */
   static <T> FluentFuture<T> immediateCopy(Future<T> future) {
      assert future.isDone();
      // in case this thread gets interrupted, ignore it (we're not
      // actually blocking since the given future is already done)
      boolean interrupted = false;
      try {
         while (true) {
            try {
               return FluentFuture.completedFuture(future.get());
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } catch (ExecutionException e) {
         return FluentFuture.failedFuture(e.getCause());
      } catch (CancellationException e) {
         return FluentFuture.cancelledFuture();
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt(); // restore interrupt status
         }
      }
   }

   /**
    * Returns a copy of the specified scheduled future, as a fluent one. This specified future
    * should already be done, and the returned future will be immediately done.
    * 
    * @param future a future
    * @return a fluent version of the specified future
    */
   static <T> FluentScheduledFuture<T> immediateCopy(ScheduledFuture<T> future) {
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
    * Updates a {@link AbstractFluentFuture} to have the same disposition (be it successful,
    * failed, or cancelled) as the specified future. This blocks, uninterruptibly, for the specified
    * source future to complete.
    * 
    * @param future the source future
    * @param copy updated so as to be a copy of the other
    */
   static <T> void copyFutureInto(Future<T> future, AbstractFluentFuture<T> copy) {
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
    * Creates a defensive copy of the given iterable.
    *
    * @param values values to snapshot
    * @return a snapshot of the given iterable, as a list
    */
   static <T> List<T> snapshot(Iterable<? extends T> values) {
      ArrayList<T> ret = new ArrayList<T>(MoreIterables.trySize(values).orElse(16));
      Iterables.addAll(ret, values);
      return Collections.unmodifiableList(ret);
   }
   
   /**
    * A fluent future that wraps a listenable one. This can implement all abstract methods in terms
    * of {@link ListenableFuture} by delegation.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FluentListenableFutureWrapper<T> implements FluentFuture<T> {
      final ListenableFuture<T> future;
      
      FluentListenableFutureWrapper(ListenableFuture<T> future) {
         this.future = future;
      }

      @Override
      public void addListener(Runnable listener, Executor executor) {
         future.addListener(listener, executor);
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
      public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
         return future.get(timeout, unit);
      }

      @Override
      public void await() throws InterruptedException {
         try {
            get();
         } catch (ExecutionException | CancellationException e) {
            // ignored
         }
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         try {
            get(limit, unit);
            return true;
         } catch (TimeoutException e) {
            return false;
         } catch (ExecutionException | CancellationException e) {
            return true;
         }
      }

      @Override
      public boolean isSuccessful() {
         if (!future.isDone() || future.isCancelled()) {
            return false;
         }
         try {
            Uninterruptibles.getUninterruptibly(future);
            return true;
         } catch (ExecutionException | CancellationException e) {
            return false;
         }
      }

      @Override
      public T getResult() {
         if (!future.isDone()) {
            throw new IllegalStateException("future is not done");
         }
         try {
            return Uninterruptibles.getUninterruptibly(future);
         } catch (ExecutionException | CancellationException e) {
            throw new IllegalStateException("future did not complete successfully");
         }
      }

      @Override
      public boolean isFailed() {
         if (!future.isDone() || future.isCancelled()) {
            return false;
         }
         try {
            Uninterruptibles.getUninterruptibly(future);
            return false;
         } catch (CancellationException e) {
            return false;
         } catch (ExecutionException e) {
            return true;
         }
      }

      @Override
      public Throwable getFailure() {
         if (!future.isDone()) {
            throw new IllegalStateException("future is not done");
         }
         try {
            Uninterruptibles.getUninterruptibly(future);
            throw new IllegalStateException("future completed successfully");
         } catch (CancellationException e) {
            throw new IllegalStateException("future was cancelled");
         } catch (ExecutionException e) {
            return e.getCause();
         }
      }

      @Override
      public void addListener(FutureListener<? super T> listener, Executor executor) {
         addListener(() -> listener.onCompletion(this), executor);
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         if (!future.isDone()) {
            throw new IllegalStateException("future is not done");
         }
         try {
            visitor.successful(Uninterruptibles.getUninterruptibly(future));
         } catch (ExecutionException e) {
            visitor.failed(e.getCause());
         } catch (CancellationException e) {
            visitor.cancelled();
         }
      }
   }
   
   /**
    * A fluent scheduled future that wraps a non-fluent one. This creates a new thread to
    * block until the input future completes.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FluentListenableScheduledFutureWrapper<T> extends FluentListenableFutureWrapper<T>
         implements FluentScheduledFuture<T> {
      
      FluentListenableScheduledFutureWrapper(ListenableScheduledFuture<T> future) {
         super(future);
      }
      
      @Override
      public long getDelay(TimeUnit unit) {
         return ((ListenableScheduledFuture<T>) future).getDelay(unit);
      }

      @Override
      public int compareTo(Delayed o) {
         return ((ListenableScheduledFuture<T>) future).compareTo(o);
      }
   }
   
   /**
    * A fluent future that wraps a non-fluent one. This creates a new thread to block until
    * the input future completes.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FluentFutureWrapper<T> extends AbstractFluentFuture<T> {
      final Future<T> future;
      
      FluentFutureWrapper(Future<T> future) {
         this.future = future;
         // use a new thread that just blocks for the future and then completes the result
         String threadName = "FluentFuture.makeFluent-"
                  + Integer.toHexString(System.identityHashCode(future));
         new Thread(() -> copyFutureInto(future, this), threadName).start();
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
    * A fluent scheduled future that wraps a non-fluent one. This creates a new thread to
    * block until the input future completes.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FluentScheduledFutureWrapper<T> extends FluentFutureWrapper<T>
         implements FluentScheduledFuture<T> {
      
      FluentScheduledFutureWrapper(ScheduledFuture<T> future) {
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
    * A fluent future that wraps a {@link CompletionStage}.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class CompletionStageWrapper<T> implements FluentFuture<T> {
      @SuppressWarnings("rawtypes")
      private static final AtomicReferenceFieldUpdater<CompletionStageWrapper, CompletableFuture>
      cfUpdater = AtomicReferenceFieldUpdater.newUpdater(
            CompletionStageWrapper.class, CompletableFuture.class, "cf");
      
      private final CompletionStage<T> stage;
      private volatile CompletableFuture<T> cf;
      
      CompletionStageWrapper(CompletionStage<T> stage) {
         this.stage = stage;
      }
      
      private CompletableFuture<T> future() {
         while (true) {
            CompletableFuture<T> future = cf;
            if (future != null) {
               return future;
            }
            future = stage.toCompletableFuture();
            if (cfUpdater.compareAndSet(this, null, future)) {
               return future;
            }
         }
      }
      
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         return future().cancel(mayInterruptIfRunning);
      }

      @Override
      public boolean isCancelled() {
         return future().isCancelled();
      }

      @Override
      public boolean isDone() {
         return future().isDone();
      }

      @Override
      public T get() throws InterruptedException, ExecutionException {
         return future().get();
      }

      @Override
      public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
         return future().get(timeout, unit);
      }

      @Override
      public void await() throws InterruptedException {
         try {
            future().get();
         } catch (ExecutionException | CancellationException e) {
         }
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         try {
            future().get(limit, unit);
         } catch (ExecutionException | CancellationException e) {
         } catch (TimeoutException e) {
            return false;
         }
         return true;
      }

      @Override
      public boolean isSuccessful() {
         if (!future().isDone()) {
            return false;
         }
         try {
            getResult();
            return true;
         } catch (IllegalStateException e) {
            return false;
         }
      }

      @Override
      public T getResult() {
         if (!isDone()) {
            throw new IllegalStateException("future has not yet completed");
         }
         boolean interrupted = false;
         try {
            while (true) {
               try {
                  return future().get();
               } catch (ExecutionException | CancellationException e) {
                  throw new IllegalStateException("future did not complete successfully");
               } catch (InterruptedException e) {
                  interrupted = true;
               }
            }
         } finally {
            if (interrupted) Thread.currentThread().interrupt();
         }
      }

      @Override
      public boolean isFailed() {
         if (!future().isDone()) {
            return false;
         }
         try {
            getFailure();
            return true;
         } catch (IllegalStateException e) {
            return false;
         }
      }

      @Override
      public Throwable getFailure() {
         if (!isDone()) {
            throw new IllegalStateException("future has not yet completed");
         }
         boolean interrupted = false;
         try {
            while (true) {
               try {
                  future().get();
                  throw new IllegalStateException("future did not complete with a failure");
               } catch (CancellationException e) {
                  throw new IllegalStateException("future did not complete with a failure");
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
         stage.whenCompleteAsync((err, val) -> listener.onCompletion(this), executor);
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         if (!future().isDone()) {
            throw new IllegalStateException("future is not yet done");
         }
         if (future().isCancelled()) {
            visitor.cancelled();
            return;
         }
         boolean interrupted = false;
         try {
            while (true) {
               try {
                  visitor.successful(future().get());
                  return;
               } catch (ExecutionException e) {
                  visitor.failed(e.getCause());
                  return;
               } catch (CancellationException e) {
                  visitor.cancelled();
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
      public CompletionStage<T> asCompletionStage() {
         return stage;
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
   private static abstract class AbstractFinishedFuture<T> implements FluentFuture<T> {
      
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
         final FluentFuture<T> f = this;
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
    * Abstract base class for futures with a known result but that incur a fixed delay before that
    * result is becomes available and the future completes. If no listeners are added to the future
    * then no actual scheduling of deferred completion is necessary. Instead, various methods on the
    * future will try to complete the future if it's not already done and its completion is due. But
    * if listeners are added, then completion is scheduled (using a static
    * {@link ScheduledExecutorService} with a fixed size pool) so that the listener can be invoked
    * in a timely manner once the delay has passed.
    *
    * @param <T> the type of the future value
    * 
    * @see AbstractDeferredFuture#doComplete()
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static abstract class AbstractDeferredFuture<T> extends AbstractFluentFuture<T> {

      /**
       * Tracks thread numbers, for naming threads used by the static thread pool below.
       */
      private static final AtomicLong THREAD_ID = new AtomicLong();
      
      /**
       * A static thread pool used to schedule completion of a future, used if and only if a
       * listener is added to a future.
       */
      private static final ScheduledExecutorService SCHEDULER =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2,
                  r -> {
                     Thread th = new Thread(r);
                     th.setDaemon(true);
                     th.setName("DeferredFuture-completion-scheduler-"
                           + THREAD_ID.incrementAndGet());
                     return th;
                  }); 

      /**
       * Absolute time in {@linkplain System#nanoTime() system nanos} when this future completes.
       */
      private final long completionTimeNanos;
      
      /**
       * If true, then completion has been scheduled (because a listener was added).
       */
      private final AtomicBoolean scheduled = new AtomicBoolean();

      /**
       * Constructs a new future that will complete after the given delay.
       *
       * @param delay the delay until the future completes
       * @param unit the unit of the given delay
       */
      AbstractDeferredFuture(long delay, TimeUnit unit) {
         this.completionTimeNanos = System.nanoTime() + unit.toNanos(delay);
      }

      @Override
      public boolean isDone() {
         return maybeComplete();
      }

      @Override
      public void await() throws InterruptedException {
         if (scheduled.get()) {
            super.await();
            return;
         }
         
         long maxWaitTime = completionTimeNanos - System.nanoTime();
         if (!await(maxWaitTime, TimeUnit.NANOSECONDS)) {
            doComplete();
         }
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         if (scheduled.get()) {
            return super.await(limit, unit);
         }
         
         long limitNanos = unit.toNanos(limit);
         long maxWaitTime = Math.min(completionTimeNanos - System.nanoTime(), limitNanos);
         if (super.await(maxWaitTime, TimeUnit.NANOSECONDS)) {
            return true;
         }
         return tryComplete();
      }
      
      /**
       * Tries to complete the future if completion is due. Returns true if the completion was due
       * and {@link #doComplete()} was called or false otherwise.
       *
       * @return true if the completion was due
       */
      private boolean tryComplete() {
         if (System.nanoTime() > completionTimeNanos) {
            doComplete();
            return true;
         }
         return false;
      }
      
      /**
       * Completes this future. This method should call one of the following methods in order to
       * complete the future:
       * <ul>
       * <li>{@link #setValue(Object)}</li>
       * <li>{@link #setFailure(Throwable)}</li>
       * <li>{@link #setCancelled()}</li>
       * <li>{@link #cancel(boolean)}</li>
       * </ul>
       */
      abstract void doComplete();
      
      /**
       * Determines if this future is complete and actually completes the future if not already done
       * but completion is due. Returns true if the future is complete upon return.
       *
       * @return true if the future is complete
       */
      private boolean maybeComplete() {
         if (super.isDone()) {
            return true;
         }
         return scheduled.get() ? false : tryComplete();
      }

      @Override
      public boolean isCancelled() {
         maybeComplete();
         return super.isCancelled();
      }

      @Override
      public void addListener(FutureListener<? super T> listener, Executor executor) {
         if (!maybeComplete() && scheduled.compareAndSet(false, true)) {
            long waitNanos = completionTimeNanos - System.nanoTime();
            ScheduledFuture<?> f =
                  SCHEDULER.schedule(this::doComplete, waitNanos, TimeUnit.NANOSECONDS);
            this.addListener(future -> f.cancel(false), SameThreadExecutor.get());
         }
         super.addListener(listener, executor);
      }

      @Override
      public boolean isSuccessful() {
         maybeComplete();
         return super.isSuccessful();
      }

      @Override
      public T getResult() {
         maybeComplete();
         return super.getResult();
      }

      @Override
      public boolean isFailed() {
         maybeComplete();
         return super.isFailed();
      }

      @Override
      public Throwable getFailure() {
         maybeComplete();
         return super.getFailure();
      }

      @Override
      public void visit(FutureVisitor<? super T> visitor) {
         maybeComplete();
         super.visit(visitor);
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
         implements FluentScheduledFuture<T> {
      private final Delayed delayed;
      
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
         implements FluentScheduledFuture<T> {
      private final Delayed delayed;
      
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
         implements FluentScheduledFuture<T> {
      private final Delayed delayed;

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
    * A future which completes with a given value after a given amount of time. This future always
    * completes successfully unless it is {@linkplain #cancel(boolean) cancelled} before the delay
    * elapses.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class DeferredFuture<T> extends AbstractDeferredFuture<T> {
      private final T value;
      
      DeferredFuture(T value, long delay, TimeUnit unit) {
         super(delay, unit);
         this.value = value;
      }

      @Override
      void doComplete() {
         this.setValue(value);
      }
   }
   
   /**
    * A future which fails after a given amount of time. This future always completes with the given
    * cause of failure unless it is {@linkplain #cancel(boolean) cancelled} before the delay
    * elapses.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class DeferredFailedFuture<T> extends AbstractDeferredFuture<T> {
      private final Throwable failure;
      
      DeferredFailedFuture(Throwable failure, long delay, TimeUnit unit) {
         super(delay, unit);
         this.failure = failure;
      }

      @Override
      void doComplete() {
         this.setFailure(failure);
      }
   }

   /**
    * A future which is cancelled after a given amount of time. This future always completes due to
    * a cancellation. It may complete before the delay elapses if it gets
    * {@linkplain #cancel(boolean) cancelled}.
    *
    * @param <T> the type of future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class DeferredCancelledFuture<T> extends AbstractDeferredFuture<T> {
      DeferredCancelledFuture(long delay, TimeUnit unit) {
         super(delay, unit);
      }

      @Override
      void doComplete() {
         this.setCancelled();
      }
   }

   /**
    * A future that can never finish.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class UnfinishableFuture<T> implements FluentFuture<T> {

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
    * A view of a {@link FluentFuture} as a {@link CompletionStage}.
    *
    * @param <T> the type of the future value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class FluentCompletionStage<T> implements CompletionStageFuture<T> {
      final FluentFuture<T> future;
      
      FluentCompletionStage(FluentFuture<T> future) {
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
         return proceed(future, executor, FluentCompletionStage.<T, U>apply(fn))
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
         FluentFuture<U> otherFuture = cast(makeFluent(other.toCompletableFuture()));
         return proceed(combine(future, otherFuture), executor,
               FluentCompletionStage.<Pair<T, U>, V>apply((pair) -> {
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
         FluentFuture<U> otherFuture = cast(makeFluent(other.toCompletableFuture()));
         return proceed(combine(future, otherFuture), executor, accept((pair) -> {
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
         FluentFuture<?> otherFuture = makeFluent(other.toCompletableFuture());
         return proceed(combine(future, otherFuture), executor, run(action)).asCompletionStage();
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
         AbstractFluentFuture<U> newFuture = interruptingFuture(threads);
         // A handler that only runs once. We use it to process both input futures and whichever
         // completes first wins.
         AtomicBoolean done = new AtomicBoolean();
         BiConsumer<FluentFuture<T>, AbstractFluentFuture<U>> handler = (self, next) -> {
            if (done.compareAndSet(false, true)) {
               apply(fn);
            }
         };
         proceed(future, newFuture, threads, executor, handler);
         FluentFuture<T> otherFuture = cast(makeFluent(other.toCompletableFuture()));
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
         AbstractFluentFuture<Void> newFuture = interruptingFuture(threads);
         // A handler that only runs once. We use it to process both input futures and whichever
         // completes first wins.
         AtomicBoolean done = new AtomicBoolean();
         BiConsumer<FluentFuture<T>, AbstractFluentFuture<Void>> handler = (self, next) -> {
            if (done.compareAndSet(false, true)) {
               accept(action);
            }
         };
         proceed(future, newFuture, threads, executor, handler);
         FluentFuture<T> otherFuture = cast(makeFluent(other.toCompletableFuture()));
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
         AbstractFluentFuture<Void> newFuture = interruptingFuture(threads);
         // A handler that only runs once. We use it to process both input futures and whichever
         // completes first wins.
         AtomicBoolean done = new AtomicBoolean();
         BiConsumer<FluentFuture<Object>, AbstractFluentFuture<Void>> handler =
               (self, next) -> {
                  if (done.compareAndSet(false, true)) {
                     run(action);
                  }
               };
         proceed(cast(future), newFuture, threads, executor, handler);
         FluentFuture<Object> otherFuture = cast(makeFluent(other.toCompletableFuture()));
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
         AbstractFluentFuture<U> newFuture = interruptingFuture(threads);
         proceed(future, newFuture, threads, executor, (self, next) -> {
            if (self.isSuccessful()) {
               try {
                  FluentFuture<U> successor =
                        makeFluent(fn.apply(self.getResult()).toCompletableFuture());
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
         return FluentCompletionStage.<T, T>proceed(future, executor, (self, next) -> {
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
         return FluentCompletionStage.<T, T>proceed(future, executor, (self, next) -> {
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
         return FluentCompletionStage.<T, U>proceed(future, executor, (self, next) -> {
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
         CompletableFuture<T> ret = new CompletableFuture<T>() {
            @Override public boolean cancel(boolean mayInterrupt) {
               // cancel this future when the completable view gets cancelled
               return super.cancel(mayInterrupt) && future.cancel(mayInterrupt);
            }
         };
         future.visitWhenDone(new FutureVisitor<T>() {
            @Override
            public void successful(T result) {
               ret.complete(result);
            }

            @Override
            public void failed(Throwable failure) {
               ret.completeExceptionally(failure);
            }

            @Override
            public void cancelled() {
               ret.cancel(false);
            }
         });
         return ret;
      }
      
      private static <T, U> FluentFuture<Pair<T, U>> combine(FluentFuture<? extends T> futureT,
            FluentFuture<? extends U> futureU) {
         return futureT.combineWith(futureU, (t, u) -> Pair.<T, U>of(t, u));
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
      private static <T, U> AbstractFluentFuture<U> proceed(
            FluentFuture<T> input, Executor executor,
            BiConsumer<FluentFuture<T>, AbstractFluentFuture<U>> handler) {
         List<Thread> threads = new ArrayList<>(1);
         AbstractFluentFuture<U> newFuture = interruptingFuture(threads);
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
      private static <T> AbstractFluentFuture<T> interruptingFuture(List<Thread> threads) {
         return new AbstractFluentFuture<T>() {
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
      private static <T, U> void proceed(FluentFuture<T> input,
            AbstractFluentFuture<U> output, List<Thread> threads, Executor executor,
            BiConsumer<FluentFuture<T>, AbstractFluentFuture<U>> handler) {
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
      private static <T, U> BiConsumer<FluentFuture<T>, AbstractFluentFuture<U>>
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
      private static <T> BiConsumer<FluentFuture<T>, AbstractFluentFuture<Void>>
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
      private static <T> BiConsumer<FluentFuture<T>, AbstractFluentFuture<Void>>
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
   }
 }
