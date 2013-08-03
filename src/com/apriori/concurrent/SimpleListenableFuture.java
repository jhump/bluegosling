package com.apriori.concurrent;

import com.apriori.util.Fulfillable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//TODO: javadoc
//TODO: test
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
   
   protected boolean setCancelled() {
      return doIfNotDone(new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void run() {
            cancelled = true;
         }
      });
   }
   
   protected boolean setValue(final T t) {
      return doIfNotDone(new Runnable() {
         @SuppressWarnings("synthetic-access")
         @Override
         public void run() {
            SimpleListenableFuture.this.t = t;
         }
      });
   }
   
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
      return getValue();
   }

   @Override
   public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
         TimeoutException {
      if (!done) {
         long startNanos = System.nanoTime();
         lock.lock();
         try {
            long spent = System.nanoTime() - startNanos;
            long nanosLeft = unit.toNanos(timeout) - spent;
            while (!done) {
               if (nanosLeft <= 0) throw new TimeoutException();
               nanosLeft = complete.awaitNanos(nanosLeft);
            }
         } finally {
            lock.unlock();
         }
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

   @SuppressWarnings("synthetic-access") // accesses private members of enclosing class
   public Fulfillable<T> asFullfillable() {
      return new Fulfillable<T>() {

         @Override
         public boolean isFulfilled() {
            return done && failure == null;
         }

         @Override
         public boolean fulfill(T value) {
            return setValue(value);
         }

         @Override
         public T get() {
            if (!done) {
               throw new IllegalStateException("not yet fulfilled");
            } else if (failure != null) {
               throw new IllegalStateException("failed to fulfill", failure);
            }
            return t;
         }

         @Override
         public T getOr(T other) {
            return done && failure == null ? t : other;
         }

         @Override
         public Set<T> asSet() {
            return done && failure == null ? Collections.singleton(t) : Collections.<T>emptySet();
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
}
