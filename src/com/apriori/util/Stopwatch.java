package com.apriori.util;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// TODO: java doc
public class Stopwatch {
   
   private final LinkedList<Long> lapNanos = new LinkedList<Long>();
   private final ReentrantReadWriteLock guard = new ReentrantReadWriteLock(true);
   private long soFar;
   private long currentBase;
   private boolean running;

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
   
   public long read(TimeUnit unit) {
      return unit.convert(read(), TimeUnit.NANOSECONDS);
   }
   
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
   
   public long[] lapResults(TimeUnit unit) {
      long laps[] = lapResults();
      for (int i = 0, len = laps.length; i < len; i++) {
         laps[i] = unit.convert(laps[i], TimeUnit.NANOSECONDS);
      }
      return laps;
   }
   
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
   
   public void reset() {
      soFar = 0;
      running = false;
      lapNanos.clear();
   }
}
