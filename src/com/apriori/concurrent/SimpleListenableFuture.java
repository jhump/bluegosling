package com.apriori.concurrent;

import com.apriori.possible.Fulfillable;
import com.apriori.possible.Possible;
import com.apriori.possible.Reference;
import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link ListenableFuture} implementation that is suitable for sub-classing. Setting the value
 * (or cause of failure) is achieved by invoking protected methods ({@link #setValue(Object)},
 * {@link #setFailure(Throwable)}, and {@link #setCancelled()}).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result
 * 
 * @see ListenableFutureTask
 */
public class SimpleListenableFuture<T> implements ListenableFuture<T> {

   private final Lock lock = new ReentrantLock();
   private final Condition complete = lock.newCondition();
   private FutureListenerSet<T> listeners = new FutureListenerSet<T>(this);
   private volatile T t;
   private volatile Throwable failure;
   private volatile boolean cancelled;
   private volatile boolean done;
   
   private boolean doIfNotDone(Runnable r) {
      FutureListenerSet<T> toExecute;
      lock.lock();
      try {
         if (done) {
            return false;
         }
         r.run();
         // done is set last - for perfect consistency when reading volatile values, done
         // must be set to true for the future to be considered "complete" (even if cancelled,
         // failure, or t fields are set -- if done isn't true, it's not yet complete)
         done = true;
         complete.signalAll();
         toExecute = listeners;
         listeners = null;
      } finally {
         lock.unlock();
      }
      toExecute.runListeners();
      return true;
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
   public boolean cancel(final boolean mayInterruptIfRunning) {
      return doIfNotDone(new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void run() {
            cancelled = true;
            if (mayInterruptIfRunning) {
               interrupt();
            }
         }
      });
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
      return doIfNotDone(new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void run() {
            cancelled = true;
         }
      });
   }
   
   /**
    * Sets the future as successfully completed with the specified result.
    * 
    * @param t the future's result
    * @return true if the result was set; false if it could not be set because the future was
    *       already complete
    */
   protected boolean setValue(final T t) {
      return doIfNotDone(new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void run() {
            SimpleListenableFuture.this.t = t;
         }
      });
   }
   
   /**
    * Sets the future as failed with the specified cause of failure.
    * 
    * @param failure the cause of failure
    * @return true if the result was marked as failed; false if it could not be so marked because it
    *       was already complete
    */
   protected boolean setFailure(final Throwable failure) {
      return doIfNotDone(new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void run() {
            SimpleListenableFuture.this.failure = failure;
         }
      });
   }
   
   @Override
   public boolean isCancelled() {
      return done && cancelled;
   }

   @Override
   public boolean isDone() {
      return done;
   }
   
   private T getValue() throws ExecutionException {
      if (failure != null) {
         throw new ExecutionException(failure);
      } else if (cancelled) {
         throw new CancellationException();
      } else {
         return t;
      }
   }

   @Override
   public T get() throws InterruptedException, ExecutionException {
      await();
      return getValue();
   }

   @Override
   public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
         TimeoutException {
      if (!await(timeout, unit)) {
         throw new TimeoutException();
      }
      return getValue();
   }

   @Override
   public void addListener(FutureListener<? super T> listener, Executor executor) {
      lock.lock();
      try {
         if (!done) {
            listeners.addListener(listener, executor);
            return;
         }
      } finally {
         lock.unlock();
      }
      // if we get here, future is complete so run listener immediately
      FutureListenerSet.runListener(this, listener, executor);
   }

   /**
    * Returns a view of this future as a {@link Fulfillable}. Fulfilling the returned object will
    * successfully complete the future.
    * 
    * @return a view of this future as a {@link Fulfillable}
    */
   @SuppressWarnings("synthetic-access") // accesses private members of enclosing class
   protected Fulfillable<T> asFulfillable() {
      return new Fulfillable<T>() {

         @Override
         public boolean isPresent() {
            return done && !cancelled && failure == null;
         }

         @Override
         public boolean fulfill(T value) {
            return setValue(value);
         }

         @Override
         public T get() {
            if (!done || cancelled) {
               throw new IllegalStateException("not yet fulfilled");
            } else if (failure != null) {
               throw new IllegalStateException("failed to fulfill", failure);
            }
            return t;
         }

         @Override
         public T getOr(T alternate) {
            return isPresent() ? t : alternate;
         }

         @Override
         public <X extends Throwable> T getOrThrow(X throwable) throws X {
            if (isPresent()) {
               return t;
            }
            throw throwable;
         }

         @Override
         public Possible<T> or(Possible<T> alternate) {
            return isPresent() ? this : alternate;
         }

         @Override
         public <U> Possible<U> transform(Function<? super T, ? extends U> function) {
            return isPresent() ? Reference.<U>setTo(function.apply(t)) : Reference.<U>unset();
         }

         @Override
         public Possible<T> filter(Predicate<? super T> predicate) {
            return isPresent() && predicate.test(t) ? this : Reference.<T>unset();
         }
         
         @Override
         public Set<T> asSet() {
            // since it can never be unset, we can just return a singleton set if
            // the value has been fulfilled
            if (isPresent()) {
               return Collections.singleton(t);
            }
            // otherwise, we return a view that is empty but will become a singleton
            // set once the value is fulfilled
            return new AbstractSet<T>() {
               @Override
               public Iterator<T> iterator() {
                  return new Iterator<T>() {
                     boolean consumed;
                     
                     @Override
                     public boolean hasNext() {
                        // This could possibly return true after returning false, but that should
                        // be harmless. Since a fulfillable can only be set once and cannot be
                        // cleared, it can never return false after returning true.
                        return isPresent() && !consumed;
                     }

                     @Override
                     public T next() {
                        if (consumed || !isPresent()) {
                           throw new NoSuchElementException();
                        }
                        consumed = true;
                        return get();
                     }

                     @Override
                     public void remove() {
                        throw new UnsupportedOperationException();
                     }
                  };
               }

               @Override
               public int size() {
                  return isPresent() ? 1 : 0;
               }
            };
         }

         @Override
         public <R> R visit(Possible.Visitor<T, R> visitor) {
            return isPresent() ? visitor.present(t) : visitor.absent();
         }
      };
   }

   @Override
   public boolean isSuccessful() {
      return done && !cancelled && failure == null;
   }

   @Override
   public T getResult() {
      if (!isSuccessful()) {
         throw new IllegalStateException();
      }
      return t;
   }

   @Override
   public boolean isFailed() {
      return done && failure != null;
   }

   @Override
   public Throwable getFailure() {
      if (!isFailed()) {
         throw new IllegalStateException();
      }
      return failure;
   }

   @Override
   public void visit(FutureVisitor<? super T> visitor) {
      if (!done) {
         throw new IllegalStateException();
      }
      if (cancelled) {
         visitor.cancelled();
      } else if (failure != null) {
         visitor.failed(failure);
      } else {
         visitor.successful(t);
      }
   }

   @Override
   public void await() throws InterruptedException {
      if (!done) {
         lock.lock();
         try {
            while (!done) {
               complete.await();
            }
         } finally {
            lock.unlock();
         }
      }
   }

   @Override
   public boolean await(long limit, TimeUnit unit) throws InterruptedException {
      if (!done) {
         long startNanos = System.nanoTime();
         long limitNanos = unit.toNanos(limit);
         lock.lock();
         try {
            long spentNanos = System.nanoTime() - startNanos;
            long nanosLeft = limitNanos - spentNanos;
            while (!done) {
               if (nanosLeft <= 0) {
                  return false;
               }
               nanosLeft = complete.awaitNanos(nanosLeft);
            }
         } finally {
            lock.unlock();
         }
      }
      return true;
   }
}
