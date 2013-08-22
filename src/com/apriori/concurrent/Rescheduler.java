package com.apriori.concurrent;

/**
 * Handles rescheduling a periodic task. This computes the next scheduled execution time for a
 * repeating task. An interface provides greater flexibility than does simple API where the only
 * options for repeating tasks are to run at a fixed rate or with a fixed delay. Complicated
 * recurring schedules can be implemented this way.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Rescheduler {
   /**
    * Computes the next scheduled execution time for a repeating task.
    * 
    * @param priorStartTimeNanos the start time, in {@linkplain System#nanoTime() nano-time}, of the
    *       previously scheduled occurrence of the task
    * @return the new start time, in {@linkplain System#nanoTime() nano-time}, for the next
    *       scheduled occurrence of the task
    */
   long scheduleNextStartTime(long priorStartTimeNanos);
}
