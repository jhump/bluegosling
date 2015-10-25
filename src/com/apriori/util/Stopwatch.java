package com.apriori.util;

import com.apriori.concurrent.unsafe.UnsafeReferenceFieldUpdater;

import java.util.concurrent.TimeUnit;

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

   private static final UnsafeReferenceFieldUpdater<Stopwatch, State> stateUpdater =
         new UnsafeReferenceFieldUpdater<>(Stopwatch.class, State.class, "state");

   private static final long[] EMPTY_LAPS = new long[0];

   /**
    * A simple linked list of laps. The head of the list is the most recently recorded lap, and the
    * tail is the first recorded.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Lap {
      // fields are mutable but must be read-only after we've CAS'ed the list into the
      // stopwatch's state
      long lapNanos;
      Lap next;
      int index;
      
      Lap() {
      }
      
      void setNext(Lap next) {
         this.next = next;
         this.index = next == null ? 0 : next.index + 1;
      }
   }

   /**
    * The state of the stopwatch.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class State {
      // fields are mutable but must be read-only after we've CAS'ed into the stopwatch's state
      long soFar;
      long currentBase;
      boolean running;
      Lap lapHead;
      
      State() {
      }
   }
   
   private final Clock clock;
   private volatile State state;
   
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
      State newState = null;
      while (true) {
         State st = state;
         if (st != null && st.running) {
            return this;
         }
         if (newState == null) {
            newState = new State();
         }
         newState.soFar = st == null ? 0 : st.soFar;
         newState.currentBase = clock.nanoTime();
         newState.running = true;
         newState.lapHead = st == null ? null : st.lapHead;
         if (stateUpdater.compareAndSet(this, st, newState)) {
            return this;
         }
      }
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
      State newState = null;
      while (true) {
         State st = state;
         if (st == null || !st.running) {
            return this;
         }
         if (newState == null) {
            newState = new State();
         }
         newState.soFar = st.soFar + clock.nanoTime() - st.currentBase;
         newState.lapHead = st.lapHead;
         if (stateUpdater.compareAndSet(this, st, newState)) {
            return this;
         }
      }
   }

   /**
    * Returns true if the stopwatch is currently running. It is running if {@link #start()} has
    * been called without a subsequent call to {@link #stop()} or {@link #reset()}.
    * 
    * <p>While not running, elapsed time does not accumulate into the stopwatch's
    * {@linkplain #read() value}.
    *
    * @return true if the stopwatch is running
    */
   public boolean isRunning() {
      State st = state;
      return st != null && state.running;
   }
   
   /**
    * Records the stopwatch's elapsed time as a lap measurement and resets the elapsed time. If the
    * stopwatch is stopped when this method is called, it will still be stopped when this method
    * returns.
    * 
    * @return {@code this}, for method chaining
    */
   public Stopwatch lap() {
      State newState = new State();
      Lap newLap = new Lap();
      while (true) {
         State st = state;
         if (st == null) {
            newLap.lapNanos = 0;
            newLap.setNext(null);

            newState.soFar = 0;
            newState.currentBase = 0;
            newState.running = false;
            newState.lapHead = newLap;
         } else if (!st.running) {
            newLap.lapNanos = st.soFar;
            newLap.setNext(st.lapHead);

            newState.soFar = 0;
            newState.currentBase = 0;
            newState.running = false;
            newState.lapHead = newLap;
         } else {
            long now = clock.nanoTime();
            newLap.lapNanos = now - st.currentBase + st.soFar;
            newLap.setNext(st.lapHead);

            newState.soFar = st.soFar;
            newState.currentBase = now;
            newState.running = true;
            newState.lapHead = newLap;
         }
         
         if (stateUpdater.compareAndSet(this, st, newState)) {
            return this;
         }
      }
   }
   
   /**
    * Queries for the elapsed time measured by the stopwatch, in nanoseconds. This returns the
    * total elapsed time during which the stopwatch was "running" since the last call to either
    * {@link #lap()} or {@link #reset()}.
    * 
    * @return the elapsed time, in nanoseconds
    */
   public long read() {
      State st = state;
      if (st == null) {
         return 0;
      } else if (!st.running) {
         return st.soFar;
      } else {
         return clock.nanoTime() - st.currentBase + st.soFar;
      }
   }
   
   /**
    * Queries for the elapsed time measured by the stopwatch, in the specified unit.
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
      State st = state;
      Lap l = st == null ? null : st.lapHead;
      if (l == null) {
         return EMPTY_LAPS;
      }
      long laps[] = new long[l.index + 1];
      while (l != null) {
         laps[l.index] = l.lapNanos;
         l = l.next;
      }
      return laps;
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
      State st = state;
      if (st == null) {
         return Double.NaN;
      }
      long sum = 0;
      int count = 0;
      for (Lap l = st.lapHead; l != null; l = l.next) {
         count++;
         sum += l.lapNanos;
      }
      if (count == 0) {
         return Double.NaN;
      }
      return ((double) sum) / count;
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
      State st = state;
      if (st == null) {
         return Double.NaN;
      }
      long sum = 0;
      int count = 0;
      for (Lap l = st.lapHead; l != null; l = l.next) {
         count++;
         sum += unit.convert(l.lapNanos, TimeUnit.NANOSECONDS);
      }
      if (count == 0) {
         return Double.NaN;
      }
      return ((double) sum) / count;
   }
   
   /**
    * Completely resets the stopwatch. After this, the stopwatch will be stopped and all
    * collected measurements (elapsed time and lap times) will be cleared.
    * 
    * @return {@code this}, for method chaining
    */
   public Stopwatch reset() {
      state = null;
      return this;
   }
   
   @Override
   public String toString() {
      long nanos = read();
      if (nanos < TimeUnit.MICROSECONDS.toNanos(2)) {
         return "" + nanos + "ns";
      } else if (nanos < TimeUnit.MILLISECONDS.toNanos(2)) {
         return String.format("%1.3fus", nanos / 1_000.0);
      } else if (nanos < TimeUnit.SECONDS.toNanos(2)) {
         return String.format("%1.3fms", nanos / 1_000_000.0);
      } else if (nanos <= TimeUnit.SECONDS.toNanos(90)) {
         return String.format("%1.3fsec", nanos / 1_000_000_000.0);
      } else if (nanos <= TimeUnit.MINUTES.toNanos(90)) {
         long min = TimeUnit.NANOSECONDS.toMinutes(nanos);
         long sec = TimeUnit.NANOSECONDS.toSeconds(nanos) % 60;
         return String.format("%d:%02d", min, sec);
      } else {
         long hr = TimeUnit.NANOSECONDS.toHours(nanos);
         long min = TimeUnit.NANOSECONDS.toMinutes(nanos) % 60;
         long sec = TimeUnit.NANOSECONDS.toSeconds(nanos) % 60;
         return String.format("%d:%02d:%02d", hr, min, sec);
      }
   }
}
