package com.bluegosling.concurrent.fluent;

import com.bluegosling.concurrent.AbstractQueuedReferenceSynchronizer;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link FluentFuture} implementation that is suitable for sub-classing. Setting the value
 * (or cause of failure) is achieved by invoking protected methods ({@link #setValue(Object)},
 * {@link #setFailure(Throwable)}, and {@link #setCancelled()}).
 * 
 * <p>This provides an alternate way to complete the future compared to {@link FluentFutureTask},
 * which requires execution of code that is supplied during construction. This provides flexibility
 * but also makes the notion of interruption vague. Since the future isn't directly associated with
 * a block of code, interruption has no well-defined meaning. As such, this class provides an
 * overridable method, {@link #interrupt()}, that is invoked when the future is cancelled and
 * allowed to interrupt. To facilitate interruption of a thread that may be executing on behalf of
 * the future, the future is not considered {@linkplain #isDone() done} until after that interrupt
 * method has completed. More subtly, this means that a call to {@link #setValue(Object)} that
 * "loses the race" with a concurrent cancellation will not actually return until the cancellation
 * completes and any interruption code has executed (same for {@link #setFailure(Throwable)}).
 * 
 * <p>The blocking methods in this class ({@link #get} and {@link #await}) are safe to call from
 * a fork-join pool. When called from within a fork-join pool, these methods use a
 * {@link ManagedBlocker} instead of directly blocking.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result
 * 
 * @see FluentFutureTask
 */
public abstract class AbstractFluentFuture<T> implements FluentFuture<T> {

   /**
    * The set of listeners that will be invoked when the future completes.
    */
   private final FutureListenerSet<T> listeners = new FutureListenerSet<T>(this);
   
   /**
    * The synchronization object.
    */
   private final Sync sync = new Sync();
   
   /**
    * Invoked when the task is cancelled and allowed to interrupt a running task. This method is
    * invoked when {@code cancel(true)} is called and should perform the interruption, if such an
    * operation is supported.
    * 
    * <p>This default implementation does nothing.
    */
   protected void interrupt() {
   }
   
   @Override
   public boolean cancel(boolean mayInterruptIfRunning) {
      return sync.setCancelled(this, mayInterruptIfRunning);
   }

   /**
    * Sets the future as cancelled. This is effectively the same as {@code cancel(false)}. It is
    * defined separately so that {@link #cancel(boolean)} can be overridden but code can still use
    * this protected method to cancel without going through overridden behavior (or vice versa).
    * 
    * @return true if the future was cancelled; false if it could not be cancelled because it was
    *       already complete
    */
   protected boolean setCancelled() {
      return sync.setCancelled(this, false);
   }
   
   /**
    * Sets the future as successfully completed with the specified result.
    * 
    * @param value the future's result
    * @return true if the result was set; false if it could not be set because the future was
    *       already complete
    */
   protected boolean setValue(T value) {
      return sync.setValue(this, value);
   }
   
   /**
    * Sets the future as failed with the specified cause of failure.
    * 
    * @param failure the cause of failure
    * @return true if the result was marked as failed; false if it could not be so marked because it
    *       was already complete
    */
   protected boolean setFailure(Throwable failure) {
      return sync.setFailure(this, failure);
   }
   
   @Override
   public boolean isCancelled() {
      return sync.get() == CANCELLED;
   }

   @Override
   public boolean isDone() {
      return sync.isDone();
   }
   
   @Override
   public T getNow(T valueIfIncomplete) {
      Object state = sync.get();
      if (state == INCOMPLETE || state == CANCELLING) {
         return valueIfIncomplete;
      } else if (state instanceof Failure) {
         throw new CompletionException(((Failure) state).cause);
      } else if (state == CANCELLED) {
         throw new CancellationException();
      } else {
         @SuppressWarnings("unchecked")
         T ret = (T) state;
         return ret;
      } 
   }

   /**
    * Returns the value of the future or throws an exception if the future did not complete
    * successfully. The future must already be complete before calling this method.
    *
    * @return the result of this future
    * @throws ExecutionException if the future did not complete successfully
    * @throws CancellationException if the future was cancelled
    */
   private T reportResult() throws ExecutionException {
      Object state = sync.get();
      assert state != INCOMPLETE && state != CANCELLING;
      if (state instanceof Failure) {
         throw new ExecutionException(((Failure) state).cause);
      } else if (state == CANCELLED) {
         throw new CancellationException();
      } else {
         @SuppressWarnings("unchecked")
         T ret = (T) state;
         return ret;
      }
   }

   @Override
   public T get() throws InterruptedException, ExecutionException {
      await();
      return reportResult();
   }

   @Override
   public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
         TimeoutException {
      if (!await(timeout, unit)) {
         throw new TimeoutException();
      }
      return reportResult();
   }

   @Override
   public void addListener(FutureListener<? super T> listener, Executor executor) {
      listeners.addListener(listener, executor);
   }

   @Override
   public boolean isSuccessful() {
      Object state = sync.get();
      return state != INCOMPLETE && !(state instanceof Failure) && state != CANCELLED
            && state != CANCELLING;
   }

   @Override
   public T getResult() {
      Object state = sync.get();
      if (state == INCOMPLETE || state instanceof Failure || state == CANCELLED
            || state == CANCELLING) {
         throw new IllegalStateException();
      }
      @SuppressWarnings("unchecked")
      T ret = (T) state;
      return ret;
   }

   @Override
   public boolean isFailed() {
      return sync.get() instanceof Failure;
   }

   @Override
   public Throwable getFailure() {
      Object state = sync.get();
      if (!(state instanceof Failure)) {
         throw new IllegalStateException();
      }
      return ((Failure) state).cause;
   }

   @Override
   public void visit(FutureVisitor<? super T> visitor) {
      Object state = sync.get();
      if (state == INCOMPLETE || state == CANCELLING) {
         throw new IllegalStateException();
      }
      if (state instanceof Failure) {
         visitor.failed(((Failure) state).cause);
      } else if (state == CANCELLED) {
         visitor.cancelled();
      } else {
         @SuppressWarnings("unchecked")
         T t = (T) state;
         visitor.successful(t);
      }
   }

   @Override
   public void await() throws InterruptedException {
      if (ForkJoinTask.inForkJoinPool()) {
         ForkJoinPool.managedBlock(new Blocker(sync));
      } else {
         sync.acquireSharedInterruptibly(null);
      }
   }

   @Override
   public boolean await(long limit, TimeUnit unit) throws InterruptedException {
      if (ForkJoinTask.inForkJoinPool()) {
         ForkJoinPool.managedBlock(new TimedBlocker(sync, limit, unit));
         return isDone();
      } else {
         return sync.tryAcquireSharedNanos(null, unit.toNanos(limit));
      }
   }

   /**
    * A special marker value for a future result that indicates the future failed.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Failure {
      Throwable cause;
      
      Failure(Throwable cause) {
         this.cause = cause;
      }
   }

   /**
    * A sentinel result value that indicates the future is not actually done yet.
    */
   static final Object INCOMPLETE = new Object() {};
   
   /**
    * A sentinel result value that indicates the future was cancelled.
    */
   static final Object CANCELLED = new Object() {};
   
   /**
    * A sentinel result value that indicates the future is in the process of cancelling. Observers
    * that see this state may need to {@linkplain Sync#acquireShared(Void) wait} until the future
    * is completed (at which point its result state will be {@link #CANCELLED}).
    */
   static final Object CANCELLING = new Object() {};
   
   /**
    * The synchronizer for the future.
    * 
    * <p>Its initial state is locked. When the future completes, it is released. Attempts to get the
    * future's result must "acquire" the lock in shared mode. But this is just to block until
    * completion. After a successful acquisition, subsequent releases are unnecessary.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Sync extends AbstractQueuedReferenceSynchronizer<Object, Void> {
      private static final long serialVersionUID = -1157186986617012632L;

      Sync() {
         setState(INCOMPLETE);
      }

      Object get() {
         return super.getState();
      }
      
      /**
       * Tries to acquire the synchronizer in shared mode. This can be done freely when the future
       * completes. Threads that acquire are not required to subsequently release the sync. Awaiting
       * the completion of the future is achieved by acquiring the sync in shared mode.
       */
      @Override
      protected int tryAcquireShared(Void v) {
         return isDone() ? 1 : -1;
      }

      /**
       * Tries to release the synchronizer in shared mode. This always succeeds and signals that the
       * future has completed. This should only be called after the synchronizer's state has been
       * moved into a completed state. This is used to release any threads waiting for the future to
       * complete. Calling code must use {@link #finish(AbstractFluentFuture)} instead of
       * {@link #releaseShared(int)} to make sure that listeners are executed when the future
       * completes.
       */
      @Override
      protected boolean tryReleaseShared(Void v) {
         assert isDone();
         return true;
      }
      
      /**
       * Releases this synchronizer in shared mode and then executes listeners. The synchronizer's
       * state must first be moved into a completed state.
       *
       * @param future the associated future whose listeners are invoked
       */
      @SuppressWarnings("synthetic-access")
      private void finish(AbstractFluentFuture<?> future) {
         boolean released = releaseShared(null);
         assert released;
         future.listeners.run();
      }

      /**
       * Completes the future with a given result value.
       *
       * @param future the associated future
       * @param value the future's value
       * @return true if the operation succeeded or false if the future was already completed
       */
      @SuppressWarnings("synthetic-access")
      public <T> boolean setValue(AbstractFluentFuture<T> future, T value) {
         assert future.sync == this;
         while (true) {
            Object state = getState();
            if (state != INCOMPLETE) {
               if (state == CANCELLING) {
                  // let imminent cancellation run its course
                  acquireShared(null);
               }
               return false;
            }
            if (compareAndSetState(INCOMPLETE, value)) {
               finish(future);
               return true;
            }
         }
      }
      
      /**
       * Completes the future with a given cause of failure.
       *
       * @param future the associated future
       * @param cause the cause of the future's failure
       * @return true if the operation succeeded or false if the future was already completed
       */
      @SuppressWarnings("synthetic-access")
      public boolean setFailure(AbstractFluentFuture<?> future, Throwable cause) {
         assert future.sync == this;
         Failure failure = null;
         while (true) {
            Object state = getState();
            if (state != INCOMPLETE) {
               if (state == CANCELLING) {
                  // let imminent cancellation run its course
                  acquireShared(null);
               }
               return false;
            }
            if (failure == null) {
               // micro-optimization: delay instantiation of failure until needed; only create one
               failure = new Failure(cause);
            }
            if (compareAndSetState(INCOMPLETE, failure)) {
               finish(future);
               return true;
            }
         }
      }
      
      /**
       * Cancels the future.
       *
       * @param future the associated future
       * @param interrupt true if the future should be
       *       {@linkplain AbstractFluentFuture#interrupt() interrupted}
       * @return true if the operation succeeded or false if the future was already completed
       */
      @SuppressWarnings("synthetic-access")
      public boolean setCancelled(AbstractFluentFuture<?> future, boolean interrupt) {
         assert future.sync == this;
         
         while (true) {
            Object state = getState();
            if (state != INCOMPLETE) {
               if (state == CANCELLING) {
                  // let imminent cancellation run its course
                  acquireShared(null);
               }
               return false;
            }
            if (compareAndSetState(INCOMPLETE, CANCELLING)) {
               try {
                  if (interrupt) {
                     // if this misbehaves, we'll propagate the exception
                     // to whoever invoked cancel(boolean) or setCancelled()
                     future.interrupt();
                  }
               } finally {
                  setState(CANCELLED);
                  finish(future);
               }
               return true;
            }
         }
      }
      
      /**
       * Returns true if the sync is in a completed state.
       */
      public boolean isDone() {
         Object state = getState();
         return state != INCOMPLETE && state != CANCELLING;
      }
   }
   
   private static class Blocker implements ManagedBlocker {
      private final Sync sync;
      
      Blocker(Sync sync) {
         this.sync = sync;
      }
      
      @Override
      public boolean isReleasable() {
         return sync.isDone();
      }

      @Override
      public boolean block() throws InterruptedException {
         sync.acquireSharedInterruptibly(null);
         return true;
      }
   }
   
   private static class TimedBlocker implements ManagedBlocker {

      private final Sync sync; 
      private final long deadline;
      
      TimedBlocker(Sync sync, long timeout, TimeUnit unit) {
         this.sync = sync;
               
         long now = System.nanoTime();
         long end = now + unit.toNanos(Math.max(0, timeout));
         // avoid overflow
         if (end < now) {
            end = Long.MAX_VALUE;
         }
         this.deadline = end;
      }

      @Override
      public boolean block() throws InterruptedException {
         sync.tryAcquireSharedNanos(null, deadline - System.nanoTime());
         return true;
      }

      @Override
      public boolean isReleasable() {
         return sync.isDone() || System.nanoTime() >= deadline;
      }
   }
}
