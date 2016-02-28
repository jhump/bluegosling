package com.bluegosling.concurrent.scheduler;

import com.bluegosling.concurrent.ListenableRepeatingFuture;

import java.util.concurrent.TimeUnit;

/**
 * Handles rescheduling a periodic task. This computes the next scheduled execution time for a
 * repeating task. An interface provides greater flexibility than does simple API where the only
 * options for repeating tasks are to run at a fixed rate or with a fixed delay. Complicated
 * recurring schedules can be implemented this way.
 *
 * @param <T> the type of future value for which a subsequent calculation may be rescheduled
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@FunctionalInterface
public interface Rescheduler<T> {
   /**
    * Computes the next scheduled execution time for a repeating task, in system nano-time.
    * 
    * @param future the task that completed and is being rescheduled
    * @param priorStartTimeNanos the start time, in {@linkplain System#nanoTime() nano-time}, of the
    *       previously scheduled occurrence of the task
    * @return the new start time, in {@linkplain System#nanoTime() nano-time}, for the next
    *       scheduled occurrence of the task
    */
   long computeNextStartTime(ListenableRepeatingFuture<? extends T> future,
         long priorStartTimeNanos);

   /**
    * Returns the fixed delay, in nanoseconds, used by the given rescheduler. If the given
    * rescheduler was not created with {@link #withFixedDelay} then this returns zero.
    *
    * @param rescheduler a rescheduler
    * @return the fixed delay, in nanoseconds, used by the given rescheduler or zero if it does
    *    not reschedule tasks with a fixed delay
    */
   static long getFixedDelayNanos(Rescheduler<?> rescheduler) {
      return rescheduler instanceof Reschedulers.RescheduleWithFixedDelay
            ? ((Reschedulers.RescheduleWithFixedDelay) rescheduler).delayNanos : 0;
   }

   /**
    * Creates a rescheduler that schedules tasks with a fixed delay between the end of one task
    * and the beginning of the next.
    *
    * @param delay the delay between tasks
    * @param unit the delay unit
    * @return a rescheduler that schedules tasks with a fixed delay between tasks
    */
   static Rescheduler<Object> withFixedDelay(long delay, TimeUnit unit) {
      return new Reschedulers.RescheduleWithFixedDelay(unit.toNanos(delay));
   }

   /**
    * Returns the fixed period, in nanoseconds, used by the given rescheduler. If the given
    * rescheduler was not created with {@link #atFixedRate} or
    * {@link #atFixedRateSkippingMissedOccurrences} then this returns zero.
    *
    * @param rescheduler a rescheduler
    * @return the fixed period, in nanoseconds, used by the given rescheduler or zero if it does
    *    not reschedule tasks at a fixed rate
    */
   static long getFixedRatePeriodNanos(Rescheduler<?> rescheduler) {
      return rescheduler instanceof Reschedulers.RescheduleAtFixedRate
            ? ((Reschedulers.RescheduleAtFixedRate) rescheduler).periodNanos : 0;
   }

   /**
    * Creates a rescheduler that schedules tasks at a fixed rate. If invocations of the task run
    * too slowly and fall behind the fixed rate, they may be scheduled more frequently so the task
    * can catch up.
    *
    * @param period the period between task starts
    * @param unit the delay unit
    * @return a rescheduler that schedules tasks at a fixed rate
    */
   static Rescheduler<Object> atFixedRateSkippingMissedOccurrences(long period, TimeUnit unit) {
      return new Reschedulers.RescheduleAtFixedRate(unit.toNanos(period), true);
   }

   /**
    * Creates a rescheduler that schedules tasks at a fixed rate. If invocations of the task run
    * too slowly and fall behind the fixed rate, any missed invocations will be skipped, and the
    * schedule will not be allowed to catch up.
    *
    * @param period the period between task starts
    * @param unit the delay unit
    * @return a rescheduler that schedules tasks at a fixed rate
    */
   static Rescheduler<Object> atFixedRate(long period, TimeUnit unit) {
      return new Reschedulers.RescheduleAtFixedRate(unit.toNanos(period), false);
   }
}
