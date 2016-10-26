package com.bluegosling.time;

import java.util.concurrent.TimeUnit;
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

   private void sleepNanos(long nanos) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      if (nanos <= 0) {
         return;
      }
      // LockSupport.parkNanos(long) seems to be more precise than Thread.sleep(long,int),
      // so we use that instead.
      long wakeNanoTime = nanos + nanoTime();
      while (true) {
         LockSupport.parkNanos(nanos);
         nanos = wakeNanoTime - nanoTime();
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         if (nanos <= 0) {
            return;
         }
      }
   }
   
   @Override
   public void sleep(long duration, TimeUnit unit) throws InterruptedException {
      if (duration < 0) {
         throw new IllegalArgumentException();
      }
      sleepNanos(unit.toNanos(duration));
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

   private void uninterruptedSleepNanos(long nanos) {
      if (nanos <= 0) {
         return;
      }
      // LockSupport.parkNanos(long) seems to be more precise than Thread.sleep(long,int),
      // so we use that instead.
      long wakeNanoTime = nanos + nanoTime();
      while (true) {
         LockSupport.parkNanos(nanos);
         nanos = wakeNanoTime - nanoTime();
         if (nanos <= 0) {
            return;
         }
      }
   }
   
   @Override
   public void uninterruptedSleep(long duration, TimeUnit unit) {
      if (duration < 0) {
         throw new IllegalArgumentException();
      }
      uninterruptedSleepNanos(unit.toNanos(duration));
   }

   @Override
   public void uninterruptedSleepUntilNanoTime(long wakeNanoTime) {
      // LockSupport.parkNanos(long) seems to be more precise than Thread.sleep(long,int),
      // so we use that instead.
      while (true) {
         long sleepNanos = wakeNanoTime - nanoTime();
         if (sleepNanos <= 0) {
            return;
         }
         LockSupport.parkNanos(sleepNanos);
      }
   }
}
