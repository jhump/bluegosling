package com.apriori.concurrent;

import java.util.concurrent.TimeUnit;

//TODO: test
//TODO: javadoc
public final class Awaitables {
   private Awaitables() {
   }
   
   public static void awaitUninterruptibly(Awaitable a) {
      boolean interrupted = false;
      while (true) {
         try {
            a.await();
            break;
         } catch (InterruptedException e) {
            interrupted = true;
         }
      }
      if (interrupted) {
         Thread.currentThread().interrupt();
      }
   }
   
   public static boolean awaitUninterruptibly(Awaitable a, long limit, TimeUnit unit) {
      boolean ret;
      boolean interrupted = false;
      long startNanos = System.nanoTime();
      long limitNanos = unit.toNanos(limit);
      while (true) {
         try {
            long spentNanos = System.nanoTime() - startNanos;
            long remaining = limitNanos - spentNanos;
            ret = a.await(remaining, TimeUnit.NANOSECONDS);
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
}
