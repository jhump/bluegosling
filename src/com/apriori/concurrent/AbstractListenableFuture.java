package com.apriori.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link ListenableFuture} implementation that is suitable for sub-classing. Setting the value
 * (or cause of failure) is achieved by invoking protected methods ({@link #setValue(Object)},
 * {@link #setFailure(Throwable)}, and {@link #setCancelled()}).
 * 
 * <p>This provides an alternate way to complete the future compared to {@link ListenableFutureTask},
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
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result
 * 
 * @see ListenableFutureTask
 */
public abstract class AbstractListenableFuture<T> implements ListenableFuture<T> {

   /**
    * The set of listeners that will be invoked when the future completes.
    */
   private final FutureListenerSet<T> listeners = new FutureListenerSet<T>(this);
   
   /**
    * The synchronization object.
    */
   private final Sync sync = new Sync();
   
   /**
    * Runs all listeners. Any listeners added after this is called will be invoked immediately,
    * since the future is already complete.
    */
   private void runListeners() {
      listeners.run();
   }
   
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
      return sync.isCancelled();
   }

   @Override
   public boolean isDone() {
      return sync.isDone();
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
      Completion state = sync.getState();
      assert state != null && state != CANCELLING;
      if (state instanceof Success) {
         @SuppressWarnings("unchecked")
         T ret = (T) ((Success) state).result;
         return ret;
      } else if (state instanceof Failure) {
         throw new ExecutionException(((Failure) state).cause);
      } else if (state == CANCELLED) {
         throw new CancellationException();
      }
      throw new AssertionError("invalid completion state: " + state);
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
      return sync.isSuccessful();
   }

   @Override
   public T getResult() {
      Completion state = sync.getState();
      if (!(state instanceof Success)) {
         throw new IllegalStateException();
      }
      @SuppressWarnings("unchecked")
      T ret = (T) ((Success) state).result;
      return ret;
   }

   @Override
   public boolean isFailed() {
      return sync.isFailed();
   }

   @Override
   public Throwable getFailure() {
      Completion state = sync.getState();
      if (!(state instanceof Failure)) {
         throw new IllegalStateException();
      }
      return ((Failure) state).cause;
   }

   @Override
   public void visit(FutureVisitor<? super T> visitor) {
      Completion state = sync.getState();
      if (state == null || state == CANCELLING) {
         throw new IllegalStateException();
      }
      if (state instanceof Success) {
         @SuppressWarnings("unchecked")
         T t = (T) ((Success) state).result;
         visitor.successful(t);
      } else if (state instanceof Failure) {
         visitor.failed(((Failure) state).cause);
      } else if (state == CANCELLED) {
         visitor.cancelled();
      } else {
         throw new AssertionError("unrecognized completion state: " + state);
      }
   }

   @Override
   public void await() throws InterruptedException {
      sync.acquireSharedInterruptibly(null);
   }

   @Override
   public boolean await(long limit, TimeUnit unit) throws InterruptedException {
      return sync.tryAcquireSharedNanos(null, unit.toNanos(limit));
   }
   
   private static interface Completion {
   }

   private static class Failure implements Completion {
      Throwable cause;
      
      Failure(Throwable cause) {
         this.cause = cause;
      }
   }

   private static class Success implements Completion {
      Object result;
      
      Success(Object result) {
         this.result = result;
      }
   }

   private static final Completion CANCELLED = new Completion() {};
   private static final Completion CANCELLING = new Completion() {};
   
   /**
    * The synchronizer for the future.
    * 
    * <p>Its initial state is locked. When the future completes, it is released. Attempts to get the
    * future's result must acquire the lock in shared mode, but only to block until completion.
    * After that, no further releases are necessary.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Sync extends AbstractQueuedReferenceSynchronizer<Completion, Void> {
      private static final long serialVersionUID = -1157186986617012632L;

      Sync() {
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
       * complete. Calling code must use {@link #finish(AbstractListenableFuture)} instead of
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
      private void finish(AbstractListenableFuture<?> future) {
         boolean released = releaseShared(null);
         assert released;
         future.runListeners();
      }

      /**
       * Completes the future with a given result value.
       *
       * @param future the associated future
       * @param value the future's value
       * @return true if the operation succeeded or false if the future was already completed
       */
      @SuppressWarnings("synthetic-access")
      public <T> boolean setValue(AbstractListenableFuture<T> future, T value) {
         assert future.sync == this;
         Success success = null;
         while (true) {
            Completion state = getState();
            if (state != null) {
               if (state == CANCELLING) {
                  // let imminent cancellation run its course
                  acquireShared(null);
               }
               return false;
            }
            if (success == null) {
               success = new Success(value);
            }
            if (compareAndSetState(state, success)) {
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
      public boolean setFailure(AbstractListenableFuture<?> future, Throwable cause) {
         assert future.sync == this;
         Failure failure = null;
         while (true) {
            Completion state = getState();
            if (state != null) {
               if (state == CANCELLING) {
                  // let imminent cancellation run its course
                  acquireShared(null);
               }
               return false;
            }
            if (failure == null) {
               failure = new Failure(cause);
            }
            if (compareAndSetState(state, failure)) {
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
       *       {@linkplain AbstractListenableFuture#interrupt() interrupted}
       * @return true if the operation succeeded or false if the future was already completed
       */
      @SuppressWarnings("synthetic-access")
      public boolean setCancelled(AbstractListenableFuture<?> future, boolean interrupt) {
         assert future.sync == this;
         
         while (true) {
            Completion state = getState();
            if (state != null) {
               if (state == CANCELLING) {
                  // let imminent cancellation run its course
                  acquireShared(null);
               }
               return false;
            }
            if (compareAndSetState(state, CANCELLING)) {
               try {
                  if (interrupt) {
                     // if this misbehaves, we'll propagate the exception
                     // to whoever invoked cancel(...) or setCancelled()
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
      @SuppressWarnings("synthetic-access")
      public boolean isDone() {
         Completion state = getState();
         return state != null && state != CANCELLING;
      }
      
      /**
       * Returns true if the future is completed and was cancelled.
       */
      @SuppressWarnings("synthetic-access")
      public boolean isCancelled() {
         return getState() == CANCELLED;
      }

      /**
       * Returns true if the future completed successfully.
       */
      public boolean isSuccessful() {
         return getState() instanceof Success;
      }

      /**
       * Returns true if the future is completed and failed.
       */
      public boolean isFailed() {
         return getState() instanceof Failure;
      }
   }
}
