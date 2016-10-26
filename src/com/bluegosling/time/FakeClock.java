package com.bluegosling.time;

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
   public void sleepUntilNanoTime(long wakeNanoTime) {
      if (wakeNanoTime > nanoTime) {
         nanoTime = wakeNanoTime;
      }
   }
}
