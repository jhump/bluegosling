package com.apriori.util;

import java.util.concurrent.TimeUnit;

// TODO: javadoc
// TODO: tests
public class FakeClock implements Clock {
   
   private long nanoTime;

   @Override
   public long currentTimeMillis() {
      return TimeUnit.NANOSECONDS.toMillis(nanoTime);
   }

   @Override
   public long nanoTime() {
      return nanoTime;
   }

   @Override
   public void sleep(long duration, TimeUnit unit) {
      if (duration < 0) {
         throw new IllegalArgumentException();
      }
      nanoTime += unit.toNanos(duration);
   }

   @Override
   public void sleepUntilMillis(long wakeTimeMillis) {
      long newNanoTime = TimeUnit.MILLISECONDS.toNanos(wakeTimeMillis);
      if (newNanoTime > nanoTime) {
         nanoTime = newNanoTime;
      }
   }

   @Override
   public void sleepUntilNanoTime(long wakeNanoTime) {
      if (wakeNanoTime > nanoTime) {
         nanoTime = wakeNanoTime;
      }
   }
}
