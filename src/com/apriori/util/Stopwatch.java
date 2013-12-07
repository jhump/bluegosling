package com.apriori.util;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A utility for measuring elapsed time. It can be stopped and restarted and the resulting elapsed
 * time will represent time elapsed during periods when the stopwatch was "running". It also has a
 * "lap" function so that data can be collected and retained for multiple iterations or laps.
 * 
 * <p>This class is threadsafe, so a single instance could be shared by multiple threads if need be.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class Stopwatch {
   
   private final Clock clock;
   private final LinkedList<Long> lapNanos = new LinkedList<Long>();
   private final ReentrantReadWriteLock guard = new ReentrantReadWriteLock(true);
   private long soFar;
   private long currentBase;
   private boolean running;
   
   /**
    * Constructs a new stopwatch. This uses the system clock.
    */
   public Stopwatch() {
      this(SystemClock.INSTANCE);
   }

   /**
    * Constructs a new stopwatch using the specified clock. This is suitable for mocking/testing.
    * 
    * @param clock the clock used to measure elapsed time
    */
   public Stopwatch(Clock clock) {
      this.clock = clock;
   }

   /**
    * Starts the stopwatch. Once started, it is "running". Time that elapses while the clock
    * is running can be queried via {@link #read()}.
    * 
    * <p>If the stopwatch is already running, this method does nothing.
    * 
    * @return {@code this}, for method chaining
    */
   public Stopwatch start() {
      guard.writeLock().lock();
      try {
         if (!running) {
            currentBase = clock.nanoTime();
            running = true;
         }
      } finally {
         guard.writeLock().unlock();
      }
      return this;
   }

   /**
    * Stops the stopwatch. Once stopped, it is no longer "running". Time that elapses while the
    * watch is stopped will not be measured and will not impact queries.
    * 
    * @return {@code this}, for method chaining
    * 
    * @see #reset()
    */
   public Stopwatch stop() {
      guard.writeLock().lock();
      try {
         if (running) {
            soFar += clock.nanoTime() - currentBase;
            running = false;
         }
      } finally {
         guard.writeLock().unlock();
      }
      return this;
   }

   /**
    * Records the stopwatch's elapsed time as a lap measurement and resets the elapsed time. If the
    * stopwatch is stopped when this method is called, it will still be stopped when this method
    * returns.
    * 
    * @return {@code this}, for method chaining
    */
   public Stopwatch lap() {
      guard.writeLock().lock();
      try {
         if (running) {
            long now = clock.nanoTime();
            lapNanos.add(now - currentBase + soFar);
            currentBase = now;
         } else {
            lapNanos.add(soFar);
         }
         soFar = 0;
      } finally {
         guard.writeLock().unlock();
      }
      return this;
   }
   
   /**
    * Queries for the elapsed time measured by the stopwatch, in nanoseconds. This returns the
    * total elapsed time during which the stopwatch was "running" since the last call to either
    * {@link #lap()} or {@link #reset()}.
    * 
    * @return the elapsed time, in nanoseconds
    */
   public long read() {
      guard.readLock().lock();
      try {
         if (!running) {
            return soFar;
         } else {
            return clock.nanoTime() - currentBase + soFar;
         }
      } finally {
         guard.readLock().unlock();
      }
   }
   
   /**
    * Queries for the elapased time measured by the stopwatch, in the specified unit.
    * 
    * @param unit the unit in which measured elapsed time is returned
    * @return the elapsed time, in the specified unit
    * 
    * @see #read()
    */
   public long read(TimeUnit unit) {
      return unit.convert(read(), TimeUnit.NANOSECONDS);
   }

   /**
    * Queries for recorded lap times, in nanoseconds. The returned array will have one value for
    * each call to {@link #lap()}. If {@link #lap()} hasn't been called then the array will be
    * empty.
    * 
    * @return an array of lap times, in nanoseconds
    */
   public long[] lapResults() {
      guard.readLock().lock();
      try {
         long laps[] = new long[lapNanos.size()];
         int i = 0;
         for (long lap : lapNanos) {
            laps[i++] = lap;
         }
         return laps;
      } finally {
         guard.readLock().unlock();
      }
   }
   
   /**
    * Queries for recorded lap times, in the specified unit.
    * 
    * @param unit the unit in which measured lap times are returned
    * @return an array of lap times, in the specified unit
    * 
    * @see #lapResults()
    */
   public long[] lapResults(TimeUnit unit) {
      long laps[] = lapResults();
      for (int i = 0, len = laps.length; i < len; i++) {
         laps[i] = unit.convert(laps[i], TimeUnit.NANOSECONDS);
      }
      return laps;
   }

   /**
    * Convenience method that computes the average of {@linkplain #lapResults() lap results}, in
    * nanoseconds. If no laps have been recorded then {@link Double#NaN} is returned.
    * 
    * @return the average of lap results, in nanoseconds
    */
   public double lapAverage() {
      long sum = 0;
      int count = 0;
      for (long lap : lapResults()) {
         count++;
         sum += lap;
      }
      if (count == 0) {
         return Double.NaN;
      }
      return (sum * 1.0) / count;
   }
   
   /**
    * Convenience method that computes the average of {@linkplain #lapResults() lap results}, in the
    * specified unit.
    * 
    * @param unit the unit in which measured lap times are returned
    * @return the average of lap results, in the specified unit
    * 
    * @see #lapAverage()
    */
   public double lapAverage(TimeUnit unit) {
      long sum = 0;
      int count = 0;
      for (long lap : lapResults(unit)) {
         count++;
         sum += lap;
      }
      if (count == 0) {
         return Double.NaN;
      }
      return (sum * 1.0) / count;
   }
   
   /**
    * Completely resets the stopwatch. After this, the stopwatch will be stopped and all
    * collected measurements (elapsed time and lap times) will be cleared.
    * 
    * @return {@code this}, for method chaining
    */
   public Stopwatch reset() {
      guard.writeLock().lock();
      try {
         soFar = 0;
         running = false;
         lapNanos.clear();
      } finally {
         guard.writeLock().unlock();
      }
      return this;
   }
   
   @Override
   public String toString() {
      long nanos = read();
      if (nanos < TimeUnit.MICROSECONDS.toNanos(2)) {
         return "" + nanos + "ns";
      } else if (nanos < TimeUnit.MILLISECONDS.toNanos(2)) {
         return String.format("%1.3fus", nanos / 1000.0);
      } else if (nanos < TimeUnit.SECONDS.toNanos(2)) {
         return String.format("%1.3fms", nanos / 1000000.0);
      } else if (nanos <= TimeUnit.SECONDS.toNanos(90)) {
         return String.format("%1.3fsec", nanos / 1000000000.0);
      } else if (nanos <= TimeUnit.MINUTES.toNanos(90)) {
         long min = TimeUnit.NANOSECONDS.toMinutes(nanos);
         long sec = TimeUnit.NANOSECONDS.toSeconds(nanos) % 60;
         return String.format("%d:%d", min, sec);
      } else {
         long hr = TimeUnit.NANOSECONDS.toHours(nanos);
         long min = TimeUnit.NANOSECONDS.toMinutes(nanos) % 60;
         long sec = TimeUnit.NANOSECONDS.toSeconds(nanos) % 60;
         return String.format("%d:%d:%d", hr, min, sec);
      }
   }
}
