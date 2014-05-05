package com.apriori.concurrent.scheduler;

import com.apriori.concurrent.ListenableRepeatingFuture;

//TODO: javadoc
//TODO: tests
final class Reschedulers {
   private Reschedulers() {
   }

   static class RescheduleAtFixedRate implements Rescheduler<Object> {
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
      public long computeNextStartTime(ListenableRepeatingFuture<? extends Object> future,
            long priorStartTimeNanos) {
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

   static class RescheduleWithFixedDelay implements Rescheduler<Object> {
      final long delayNanos;
      
      RescheduleWithFixedDelay(long delayNanos) {
         if (delayNanos <= 0) {
            throw new IllegalArgumentException();
         }
         this.delayNanos = delayNanos;
      }
      
      @Override
      public long computeNextStartTime(ListenableRepeatingFuture<? extends Object> future,
            long priorStartTimeNanos) {
         return System.nanoTime() + delayNanos;
      }
   }
}
