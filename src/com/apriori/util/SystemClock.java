package com.apriori.util;

import java.util.concurrent.locks.LockSupport;



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
   public void sleepUntilNanoTime(long wakeNanoTime) throws InterruptedException {
      // LockSupport.parkNanos(long) seems to be more precise than Thread.sleep(long,int),
      // so we use that instead.
      while (true) {
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         long sleepNanos = wakeNanoTime - nanoTime();
         if (sleepNanos <= 0) {
            return;
         }
         LockSupport.parkNanos(sleepNanos);
      }
   }
}