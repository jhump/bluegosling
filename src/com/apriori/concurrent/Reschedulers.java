package com.apriori.concurrent;

import java.util.concurrent.TimeUnit;

//TODO: javadoc
//TODO: tests
public final class Reschedulers {
   private Reschedulers() {
   }

   public static Rescheduler atFixedRate(long period, TimeUnit unit) {
      return new RescheduleAtFixedRate(unit.toNanos(period), false);
   }

   public static Rescheduler atFixedRateSkippingMissedOccurrences(long period, TimeUnit unit) {
      return new RescheduleAtFixedRate(unit.toNanos(period), true);
   }

   public static long getFixedRatePeriodNanos(Rescheduler rescheduler) {
      return rescheduler instanceof RescheduleAtFixedRate
            ? ((RescheduleAtFixedRate) rescheduler).periodNanos : 0;
   }

   public static Rescheduler withFixedDelay(long delay, TimeUnit unit) {
      return new RescheduleWithFixedDelay(unit.toNanos(delay));
   }

   public static long getFixedDelayNanos(Rescheduler rescheduler) {
      return rescheduler instanceof RescheduleWithFixedDelay
            ? ((RescheduleWithFixedDelay) rescheduler).delayNanos : 0;
   }

   private static class RescheduleAtFixedRate implements Rescheduler {
      final long periodNanos;
      private final boolean skipMissedOccurrences;
      
      RescheduleAtFixedRate(long periodNanos, boolean skipMissedOccurrences) {
         if (periodNanos <= 0) {
            throw new IllegalArgumentException();
         }
         this.periodNanos = periodNanos;
         this.skipMissedOccurrences = skipMissedOccurrences;
      }
      
      @Override
      public long scheduleNextStartTime(long priorStartTimeNanos) {
         return computeNextStartTime(priorStartTimeNanos, System.nanoTime());
      }
      
      long computeNextStartTime(long priorStartTimeNanos, long now) {
         long nextStartTimeNanos = priorStartTimeNanos + periodNanos;
         if (skipMissedOccurrences) {
            long behind = now - nextStartTimeNanos;
            if (behind > periodNanos) {
               // we missed some executions, just skip them
               long numMissed = behind / periodNanos;
               nextStartTimeNanos += numMissed * periodNanos;
            }
         }
         return nextStartTimeNanos;
      }
   }

   private static class RescheduleWithFixedDelay implements Rescheduler {
      final long delayNanos;
      
      RescheduleWithFixedDelay(long delayNanos) {
         if (delayNanos <= 0) {
            throw new IllegalArgumentException();
         }
         this.delayNanos = delayNanos;
      }
      
      @Override
      public long scheduleNextStartTime(long priorStartTimeNanos) {
         return System.nanoTime() + delayNanos;
      }
   }
}
