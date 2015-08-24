package com.apriori.concurrent.scheduler;

import com.apriori.concurrent.ListenableRepeatingFuture;

/**
 * Utility class with typical {@link Rescheduler} implementations.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
final class Reschedulers {
   private Reschedulers() {
   }

   /**
    * Schedules subsequent invocations at a fixed rate. If invocations execute too slowly and fall
    * behind, then they can be scheduled more frequently to catch up. If configured to skip missed
    * occurrences however, subsequent invocations are scheduled at the fixed rate and the schedule
    * will not be able to catch up.  
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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

   /**
    * Schedules subsequent invocations with a fixed delay between the end of one task and the
    * beginning of the next one.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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
