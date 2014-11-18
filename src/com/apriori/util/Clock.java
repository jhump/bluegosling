package com.apriori.util;

import java.util.concurrent.TimeUnit;

// TODO: javadoc
public interface Clock {
   
   long currentTimeMillis();
   
   long nanoTime();
   
   default void sleep(long duration, TimeUnit unit) throws InterruptedException {
      if (duration < 0) {
         throw new IllegalArgumentException();
      }
      sleepUntilNanoTime(nanoTime() + unit.toNanos(duration));
   }

   default void sleepUntilMillis(long wakeTimeMillis) throws InterruptedException {
      long sleepMillis = wakeTimeMillis - currentTimeMillis();
      if (sleepMillis > 0) {
         sleep(sleepMillis, TimeUnit.MILLISECONDS);
      }
   }
   
   void sleepUntilNanoTime(long wakeNanoTime) throws InterruptedException;
   
   default void uninterruptedSleep(long duration, TimeUnit unit) {
      if (duration < 0) {
         throw new IllegalArgumentException();
      }
      uninterruptedSleepUntilNanoTime(nanoTime() + unit.toNanos(duration));
   }

   default void uninterruptedSleepUntilMillis(long wakeTimeMillis) {
      long sleepMillis = wakeTimeMillis - currentTimeMillis();
      if (sleepMillis > 0) {
         uninterruptedSleep(sleepMillis, TimeUnit.MILLISECONDS);
      }
   }

   default void uninterruptedSleepUntilNanoTime(long wakeNanoTime) {
      boolean interrupted = false;
      try {
         while (true) {
            try {
               sleepUntilNanoTime(wakeNanoTime);
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
}
