package com.bluegosling.concurrent;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.BooleanSupplier;

/**
 * An object that represents a future event. Like a {@link Future} without a value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: test
//TODO: javadoc
public interface Awaitable {
   /**
    * Awaits the future event, blocking the current thread until the event occurs.
    *
    * @throws InterruptedException if this thread is interrupted while waiting for the event
    */
   void await() throws InterruptedException;
   
   /**
    * Awaits the future event, blocking up to the specified amount of time until the event occurs.
    *
    * @param limit the maximum amount of time to wait
    * @param unit the unit of {@code limit}
    * @return true if the event occurred or false if the time limit was encountered first
    * @throws InterruptedException if this thread is interrupted while waiting for the event
    */
   boolean await(long limit, TimeUnit unit) throws InterruptedException;
   
   /**
    * Returns true if the event has occurred.
    *
    * @return true if the event has occurred; false otherwise
    */
   boolean isDone();

   /**
    * Awaits the future event, blocking the current thread in a manner that cannot be interrupted
    * until the event occurs.
    */
   default void awaitUninterruptibly() {
      boolean interrupted = false;
      try {
         while (true) {
            try {
               await();
               return;
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }
      }
   }
   
   /**
    * Awaits the future event, blocking in a manner that cannot be interrupted up to the specified
    * amount of time until the event occurs.
    *
    * @param limit the maximum amount of time to wait
    * @param unit the unit of {@code limit}
    * @return true if the event occurred or false if the time limit was encountered first
    */
   default boolean awaitUninterruptibly(long limit, TimeUnit unit) {
      boolean interrupted = false;
      long startNanos = System.nanoTime();
      long limitNanos = unit.toNanos(limit);
      try {
         while (true) {
            try {
               long spentNanos = System.nanoTime() - startNanos;
               long remaining = limitNanos - spentNanos;
               return await(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
               interrupted = true;
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }
      }
   }
   
   /**
    * Adapts a {@link Future} to the {@link Awaitable} interface.
    *
    * @param f a future
    * @return an awaitable that completes when the future completes
    */
   static Awaitable fromFuture(Future<?> f) {
      requireNonNull(f);
      if (f instanceof Awaitable) {
         return (Awaitable) f;
      } else {
         return new Awaitable() {
            @Override
            public void await() throws InterruptedException {
               try {
                  f.get();
               } catch (ExecutionException | CancellationException e) {
                  // can ignore since we only care that future finishes, not its actual disposition
               }
            }

            @Override
            public boolean await(long limit, TimeUnit unit) throws InterruptedException {
               try {
                  f.get(limit, unit);
                  return true;
               } catch (ExecutionException | CancellationException e) {
                  // can ignore since we only care that future finishes, not its actual disposition
                  return true;
               } catch (TimeoutException e) {
                  return false;
               }
            }

            @Override
            public boolean isDone() {
               return f.isDone();
            }
         };
      }
   }
   
   /**
    * Adapts a {@link CountDownLatch} to the {@link Awaitable} interface.
    *
    * @param latch a latch
    * @return an awaitable that completes when the latch opens
    */
   static Awaitable fromLatch(CountDownLatch latch) {
      requireNonNull(latch);
      return new Awaitable() {
         @Override
         public void await() throws InterruptedException {
            latch.await();
         }

         @Override
         public boolean await(long limit, TimeUnit unit) throws InterruptedException {
            return latch.await(limit, unit);
         }
         
         @Override
         public boolean isDone() {
            return latch.getCount() == 0;
         }
      };
   }
   
   /**
    * Creates an awaitable that waits for the given {@link ExecutorService} to terminate.
    *
    * @param executor an executor service
    * @return an awaitable that completes when the executor terminates
    */
   static Awaitable fromTerminatingExecutor(ExecutorService executor) {
      requireNonNull(executor);
      return new Awaitable() {
         @Override
         public void await() throws InterruptedException {
            while (true) {
               if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                  return;
               }
            }
         }

         @Override
         public boolean await(long limit, TimeUnit unit) throws InterruptedException {
            return executor.awaitTermination(limit, unit);
         }

         @Override
         public boolean isDone() {
            return executor.isTerminated();
         }
      };
   }
   
   // TODO: doc
   public static Awaitable fromIntrinsic(Object obj, BooleanSupplier complete) {
      return new Awaitable() {
         @Override
         public boolean isDone() {
            return complete.getAsBoolean();
         }
         
         @Override
         public boolean await(long limit, TimeUnit unit) throws InterruptedException {
            if (limit < 0) {
               limit = 0;
            }
            long now = System.nanoTime();
            // avoid overflow
            long deadline = now + unit.toNanos(limit);
            if (deadline < now) {
               deadline = Long.MAX_VALUE;
            }
            synchronized (obj) {
               while (!complete.getAsBoolean()) {
                  now = System.nanoTime();
                  if (now >= deadline) {
                     return false;
                  }
                  long waitNanos = deadline - now;
                  long waitMillis = TimeUnit.NANOSECONDS.toMillis(waitNanos);
                  waitNanos = waitNanos - TimeUnit.MILLISECONDS.toNanos(waitMillis);
                  assert waitNanos < 1_000_000;
                  obj.wait(waitMillis, (int) waitNanos);
               }
               return true;
            }
         }
         
         @Override
         public void await() throws InterruptedException {
            synchronized (obj) {
               while (!complete.getAsBoolean()) {
                  obj.wait();
               }
            }
         }
      };
   }
   
   // TODO: doc
   public static Awaitable fromCondition(Condition condition, Lock lock, BooleanSupplier complete) {
      return new Awaitable() {
         @Override
         public boolean isDone() {
            return complete.getAsBoolean();
         }
         
         @Override
         public boolean await(long limit, TimeUnit unit) throws InterruptedException {
            if (limit < 0) {
               limit = 0;
            }
            long now = System.nanoTime();
            // avoid overflow
            long deadline = now + unit.toNanos(limit);
            if (deadline < now) {
               deadline = Long.MAX_VALUE;
            }
            if (!lock.tryLock(limit, unit)) {
               return false;
            }
            try {
               while (!complete.getAsBoolean()) {
                  now = System.nanoTime();
                  if (now >= deadline) {
                     return false;
                  }
                  condition.await(deadline - now, TimeUnit.NANOSECONDS);
               }
               return true;
            } finally {
               lock.unlock();
            }
         }

         @Override
         public void await() throws InterruptedException {
            lock.lockInterruptibly();
            try {
               while (!complete.getAsBoolean()) {
                  condition.await();
               }
            } finally {
               lock.unlock();
            }
         }
      };
   }
}
