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
public class Stopwatch {
   
   private final LinkedList<Long> lapNanos = new LinkedList<Long>();
   private final ReentrantReadWriteLock guard = new ReentrantReadWriteLock(true);
   private long soFar;
   private long currentBase;
   private boolean running;

   /**
    * Starts the stopwatch. Once started, it is "running". Time that elapses while the clock
    * is running can be queried via {@link #read()}.
    * 
    * <p>If the stopwatch is already running, this method does nothing.
    */
   public void start() {
      guard.writeLock().lock();
      try {
         if (!running) {
            lapNanos.clear();
            currentBase = System.nanoTime();
            running = true;
         }
      } finally {
         guard.writeLock().unlock();
      }
   }

   /**
    * Stops the stopwatch. Once stopped, it is no longer "running". Time that elapses while the
    * watch is stopped will not be measured and will not impact queries.
    * 
    * @see #reset()
    */
   public void stop() {
      guard.writeLock().lock();
      try {
         if (running) {
            soFar += System.nanoTime() - currentBase;
            running = false;
         }
      } finally {
         guard.writeLock().unlock();
      }
   }

   /**
    * Records the stopwatch's elapsed time as a lap measurement and resets the elapsed time. If the
    * stopwatch is stopped when this method is called, it will still be stopped when this method
    * returns.
    */
   public void lap() {
      guard.writeLock().lock();
      try {
         if (running) {
            long now = System.nanoTime();
            lapNanos.add(now - currentBase + soFar);
            currentBase = now;
         } else {
            lapNanos.add(soFar);
         }
         soFar = 0;
      } finally {
         guard.writeLock().unlock();
      }
   }
   
   /**
    * Queries for the elapsed time measured by the stopwatch, in milliseconds. This returns the
    * total elapsed time during which the stopwatch was "running" since the last call to either
    * {@link #lap()} or {@link #reset()}.
    * 
    * @return the elapsed time, in milliseconds
    */
   public long read() {
      guard.readLock().lock();
      try {
         if (!running) {
            return soFar;
         } else {
            return System.nanoTime() - currentBase + soFar;
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
    * Queries for recorded lap times, in milliseconds. The returned array will have one value for
    * each call to {@link #lap()}. If {@link #lap()} hasn't been called then the array will be
    * empty.
    * 
    * @return an array of lap times, in milliseconds
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
    * milliseconds. If no laps have been recorded then {@link Double#NaN} is returned.
    * 
    * @return the average of lap results, in milliseconds
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
    */
   public void reset() {
      guard.writeLock().lock();
      try {
         soFar = 0;
         running = false;
         lapNanos.clear();
      } finally {
         guard.writeLock().unlock();
      }
   }
}
