package com.apriori.concurrent;

import static java.util.Objects.requireNonNull;

import com.apriori.util.Clock;
import com.apriori.util.SystemClock;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

// TODO: javadoc
// TODO: tests
public class RateLimiter {
   
   private static final long NANOS_PER_SEC = TimeUnit.SECONDS.toNanos(1);
   private static final long NANOS_PER_MILLI = TimeUnit.MILLISECONDS.toNanos(1);
   
   private final Clock clock;

   private long storedPermits;
   private long asOfNanos;
   
   private double rateNanosPerPermit;
   private long maxStoredPermits;
   private double jitter;
   
   public RateLimiter(double ratePermitsPerSecond) {
      this(ratePermitsPerSecond, (long) Math.max(ratePermitsPerSecond, 1));
   }

   public RateLimiter(double ratePermitsPerSecond, long maxStoredPermits) {
      this(ratePermitsPerSecond, maxStoredPermits, maxStoredPermits);
   }

   public RateLimiter(double ratePermitsPerSecond, long maxStoredPermits, long initialPermits) {
      this(ratePermitsPerSecond, maxStoredPermits, initialPermits, 0);
   }
   
   public RateLimiter(double ratePermitsPerSecond, long maxStoredPermits, long initialPermits,
         double jitter) {
      this(ratePermitsPerSecond, maxStoredPermits, initialPermits, jitter, SystemClock.INSTANCE);
   }
   
   public RateLimiter(double ratePermitsPerSecond, long maxStoredPermits, long initialPermits,
         double jitter, Clock clock) {
      this.clock = requireNonNull(clock, "clock");

      if (initialPermits < 0) {
         throw new IllegalArgumentException("initial permits must be >= 0");
      }
      this.storedPermits = initialPermits;
      this.asOfNanos = clock.nanoTime();

      setRate(ratePermitsPerSecond);
      setMaxPermits(maxStoredPermits);
      setJitter(jitter);
      
   }
   
   public void acquire() {
      acquire(1);
   }
   
   public void acquire(long permits) {
      if (permits < 1) {
         throw new IllegalArgumentException();
      }
      long acquireCompleteNanos;
      lock();
      try {
         acquireCompleteNanos = makeReservation(permits, -1);
      } finally {
         unlock();
      }
      clock.uninterruptedSleepUntilNanoTime(acquireCompleteNanos);
   }
   
   public void acquireInterruptibly() throws InterruptedException {
      acquireInterruptibly(1);
   }

   public void acquireInterruptibly(int permits) throws InterruptedException {
      if (permits < 1) {
         throw new IllegalArgumentException();
      }
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      long acquireCompleteNanos;
      lock();
      try {
         acquireCompleteNanos = makeReservation(permits, -1);
      } finally {
         unlock();
      }
      clock.sleepUntilNanoTime(acquireCompleteNanos);
   }

   public boolean tryAcquire() {
      return tryAcquire(1);
   }

   public boolean tryAcquire(long permits) {
      if (permits < 1) {
         throw new IllegalArgumentException();
      }
      long acquireCompleteNanos;
      lock();
      try {
         acquireCompleteNanos = makeReservation(permits, 0);
         if (acquireCompleteNanos == -1) {
            return false;
         }
      } finally {
         unlock();
      }
      return acquireCompleteNanos != -1;
   }
   
   public boolean tryAcquire(long timeLimit, TimeUnit unit)
         throws InterruptedException {
      return tryAcquire(1, timeLimit, unit);
   }

   public boolean tryAcquire(long permits, long timeLimit, TimeUnit unit)
         throws InterruptedException {
      if (permits < 1 || timeLimit < 0) {
         throw new IllegalArgumentException();
      }
      long maxWaitNanos = unit.toNanos(timeLimit);
      long acquireCompleteNanos;
      lock();
      try {
         acquireCompleteNanos = makeReservation(permits, maxWaitNanos);
      } finally {
         unlock();
      }
      if (acquireCompleteNanos == -1) {
         return false;
      }
      clock.sleepUntilNanoTime(acquireCompleteNanos);
      return true;
   }
   
   public synchronized double getRate() {
      return NANOS_PER_SEC / rateNanosPerPermit;
   }

   public synchronized void setRate(double permitsPerSecond) {
      if (permitsPerSecond <= 0) {
         throw new IllegalArgumentException("rate must be > 0 permits per second");
      }
      double nanosPerPermit = NANOS_PER_SEC / permitsPerSecond;
      if (nanosPerPermit < 1.0) {
         throw new IllegalArgumentException("rate must be <= one billion permits per second");
      }
      this.rateNanosPerPermit = nanosPerPermit;
   }

   public synchronized long getMaxPermits() {
      return maxStoredPermits;
   }

   public synchronized void setMaxPermits(long maxStoredPermits) {
      if (maxStoredPermits <= 0) {
         throw new IllegalArgumentException("max stored permits must be > 0");
      }
      this.maxStoredPermits = maxStoredPermits;
   }

   public synchronized double getJitter() {
      return jitter;
   }

   public synchronized void setJitter(double jitter) {
      if (jitter < 0.0 || jitter > 1.0) {
         throw new IllegalArgumentException("jitter must be between 0.0 and 1.0, inclusive");
      }
      this.jitter = jitter;
   }
   
   /**
    * Makes a reservation for the given number of permits, unless it would result in waiting longer
    * than the given wait threshold.
    *
    * @param permits the number of permits to reserve
    * @param maxWaitNanos the maximum number of nanoseconds the caller is willing to wait for the
    *       permits or -1 if there is no wait limit
    * @return the time, in {@linkplain System#nanoTime() nanos}, when the permits become available
    *       or -1 if no reservation was made because the wait limit would have been exceeded
    */
   private long makeReservation(long permits, long maxWaitNanos) {
      // Give ourselves an extra millisecond for margin of error. Otherwise, we get weird effects
      // with very high rates but max bucket size of 1 where the permit should have been in the
      // bucket but we missed it due to scheduling inaccuracy.
      final long now = clock.nanoTime() - NANOS_PER_MILLI;
      final double rate = rateNanosPerPermit;
      long asOf = asOfNanos;
      long stored = storedPermits;
      double jtr = jitter;
      if (asOf <= now) {
         // last time stamp is in the past, so we compute the number of permits as of now
         long max = maxStoredPermits;
         long delta = now - asOf;
         double jrate = jtr == 0 ? rate : computeJitteredRate(rate, jtr, delta);
         long newPermits = (long) (delta / jrate);
         stored = Math.max(stored, Math.min(max, stored + newPermits));
         asOf = asOf + (long) (newPermits * jrate);
         if (stored >= permits) {
            // we already have enough permits to satisfy the request!
            this.asOfNanos = asOf;
            this.storedPermits = stored - permits;
            return now;
         }
         // fall through to below to figure out how long to wait
      } else {
         assert stored == 0;
      }
      
      long shortage = permits - stored;
      long waitTime;
      if (jtr == 0) {
         waitTime = (long) (shortage * rate);
      } else {
         double jrate = computeJitteredRate(rate, jtr, shortage * rate);
         waitTime = (long) (shortage * jrate);
      }
      if (maxWaitNanos < 0 || maxWaitNanos > waitTime) {
         asOf += waitTime;
         this.asOfNanos = asOf;
         this.storedPermits = 0;
         return asOf;
      }
      return -1;
   }
   
   private double computeJitteredRate(double rate, double jtr, double timePeriodNanos) {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      double timePeriodSeconds = timePeriodNanos / NANOS_PER_SEC;
      double j = jtr * Math.pow(r.nextDouble(), timePeriodSeconds);
      if (r.nextBoolean()) {
         j = -j;
      }
      return rate * (1.0 + j);
   }

//   private final AtomicBoolean lock = new AtomicBoolean();
//   
//   private void lock() {
//      while (true) {
//         if (lock.compareAndSet(false, true)) {
//            return;
//         }
//      }
//   }
//
//   private void unlock() {
//      lock.set(false);
//   }

   private final Lock lock = new Lock();
   
   private void lock() {
      lock.acquire(0);
   }
   
   private void unlock() {
      lock.release(0);
   }

   /** A simple non-reentrant lock. */
   private static class Lock extends AbstractQueuedSynchronizer {
      private static final long serialVersionUID = 6064513427789707295L;

      Lock() {
      }
      
      @Override
      protected boolean tryAcquire(int i) {
         return compareAndSetState(0, 1);
      }
      
      @Override
      protected boolean tryRelease(int i) {
         setState(0);
         return true;
      }
   }
}
