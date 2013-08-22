package com.apriori.util;

import java.util.concurrent.TimeUnit;

// TODO: javadoc
// TODO: tests
public enum SystemClock implements Clock {

   INSTANCE;
   
   @Override
   public long currentTimeMillis() {
      return System.currentTimeMillis();
   }

   @Override
   public long nanoTime() {
      return System.nanoTime();
   }

   @Override
   public void sleep(long duration, TimeUnit unit) {
      if (duration < 0) {
         throw new IllegalArgumentException();
      }
      sleepUntilNanoTime(nanoTime() + unit.toNanos(duration));
   }

   @Override
   public void sleepUntilMillis(long wakeTimeMillis) {
      long sleepMillis = wakeTimeMillis - currentTimeMillis();
      if (sleepMillis > 0) {
         sleep(sleepMillis, TimeUnit.MILLISECONDS);
      }
   }

   @Override
   public void sleepUntilNanoTime(long wakeNanoTime) {
      boolean interrupted = false;
      try {
         while (true) {
            long sleepNanos = wakeNanoTime - nanoTime();
            if (sleepNanos <= 0) {
               break;
            }
            try {
               Thread.sleep(sleepNanos / 1000000, (int) (sleepNanos % 1000000));
               break;
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