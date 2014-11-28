package com.apriori.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An object that represents a future event.
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

   default void awaitUninterruptibly() {
      boolean interrupted = false;
      while (true) {
         try {
            await();
            break;
         } catch (InterruptedException e) {
            interrupted = true;
         }
      }
      if (interrupted) {
         Thread.currentThread().interrupt();
      }
   }
   
   default boolean awaitUninterruptibly(long limit, TimeUnit unit) {
      boolean ret;
      boolean interrupted = false;
      long startNanos = System.nanoTime();
      long limitNanos = unit.toNanos(limit);
      while (true) {
         try {
            long spentNanos = System.nanoTime() - startNanos;
            long remaining = limitNanos - spentNanos;
            ret = await(remaining, TimeUnit.NANOSECONDS);
            break;
         } catch (InterruptedException e) {
            interrupted = true;
         }
      }
      if (interrupted) {
         Thread.currentThread().interrupt();
      }
      return ret;
   }
   
   static Awaitable fromFuture(Future<?> f) {
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
   
   static Awaitable fromLatch(CountDownLatch latch) {
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
}
