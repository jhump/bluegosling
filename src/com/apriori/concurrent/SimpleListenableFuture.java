package com.apriori.concurrent;

import com.apriori.tuples.Pair;
import com.apriori.util.Fulfillable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
   private List<Pair<Runnable, Executor>> listeners = new LinkedList<Pair<Runnable, Executor>>();
   private volatile T t;
   private volatile Throwable failure;
   private volatile boolean cancelled;
   private volatile boolean done;
   
   private boolean doIfNotDone(Runnable r) {
      List<Pair<Runnable, Executor>> toExecute;
      lock.lock();
      try {
         if (done) {
            return false;
         }
         done = true;
         r.run();
         complete.signalAll();
         toExecute = listeners;
         listeners = null;
      } finally {
         lock.unlock();
      }
      for (Pair<Runnable, Executor> listener : toExecute) {
         try {
            listener.getSecond().execute(listener.getFirst());
         } catch (RuntimeException e) {
            // TODO: log?
         }
      }
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
      return cancelled;
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
   public void addListener(Runnable listener, Executor executor) {
      lock.lock();
      try {
         if (!done) {
            listeners.add(Pair.create(listener, executor));
            return;
         }
      } finally {
         lock.unlock();
      }
      // if we get here, future is complete so run listener immediately
      executor.execute(listener);
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
}
