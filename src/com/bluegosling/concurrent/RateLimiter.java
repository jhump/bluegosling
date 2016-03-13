package com.bluegosling.concurrent;

import static java.util.Objects.requireNonNull;

import com.bluegosling.util.Clock;
import com.bluegosling.util.SystemClock;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * A synchronizer that can be used to limit the rate, or frequency, of an operation. This uses a
 * leaky bucket algorithm to control the rate.
 * 
 * <p>This is effectively just like Guava's {@link com.google.common.util.concurrent.RateLimiter}
 * except with several additional features:
 * <ol>
 * <li>This version allows control over the bucket size (e.g. maximum number of stored permits).
 * </li>
 * <li>This version allows creation of the limiter with a specific initial bucket size, which can be
 * be greater than the steady-state bucket size. This allows an initial burst that is larger than
 * allowed in steady-state operations.</li>
 * <li>This version provides a jitter parameter, which adds a small amount of randomness to the
 * delay between permits being added to the bucket. Note that using jitter will reduce performance
 * slightly. For typical rates, this should not be an issue. But for extremely high rates (100s of
 * thousands per second or millions per second), this can reduce the actual observed rate due to
 * contention over the limiter while computing jitter.</li>
 * <li>This version provides interruptible acquisition methods. (Guava's version uses
 * uninterruptible blocking when an acquirer needs to wait for a permit.)</li>
 * <li>Finally, this version provides methods for forcibly taking tokens from the bucket or putting
 * them back. This can be used for reconciliation when the number of permits an operation needs is
 * not known apriori. In such a case, the operation acquires an estimated number of permits. On
 * completion, the operation has measured the actual number of permits needed. It can then push
 * tokens back into the bucket (if the estimate was too high) or remove tokens (if the estimate was
 * too low).</li>
 * </ol>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public class RateLimiter {
   
   private static final long NANOS_PER_SEC = TimeUnit.SECONDS.toNanos(1);
   private static final long NANOS_PER_MICRO = TimeUnit.MICROSECONDS.toNanos(1);
   
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
      long acquireCompleteNanos = makeReservation(permits, -1);
      clock.uninterruptedSleep(acquireCompleteNanos, TimeUnit.NANOSECONDS);
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
      long acquireCompleteNanos = makeReservation(permits, -1);
      try {
         clock.sleep(acquireCompleteNanos, TimeUnit.NANOSECONDS);
      } catch (InterruptedException e) {
         putBack(permits);
         throw e;
      }
   }

   public boolean tryAcquire() {
      return tryAcquire(1);
   }

   public boolean tryAcquire(long permits) {
      if (permits < 1) {
         throw new IllegalArgumentException();
      }
      long acquireCompleteNanos = makeReservation(permits, 0);
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
      long acquireCompleteNanos = makeReservation(permits, maxWaitNanos);
      if (acquireCompleteNanos == -1) {
         return false;
      }
      try {
         clock.sleep(acquireCompleteNanos, TimeUnit.NANOSECONDS);
      } catch (InterruptedException e) {
         putBack(permits);
         throw e;
      }
      return true;
   }
   
   public synchronized void putBack(long permits) {
      if (permits < 0) {
         throw new IllegalArgumentException();
      }
      if (permits == 0) {
         return;
      }
      this.storedPermits += permits;
   }

   public synchronized void forceTake(long permits) {
      if (permits < 0) {
         throw new IllegalArgumentException();
      }
      if (permits == 0) {
         return;
      }
      this.storedPermits -= permits;
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
    * @return the duration to wait, in {@linkplain System#nanoTime() nanos}, until the permits
    *       become available or -1 if no reservation was made because the wait limit would have been
    *       exceeded
    */
   private synchronized long makeReservation(long permits, long maxWaitNanos) {
      final long realNow = clock.nanoTime();
      // Give ourselves an extra microsecond for margin of error. Otherwise, we get weird effects
      // with very high rates and max bucket size of 1, where the permit should have been in the
      // bucket but we missed it due to scheduling inaccuracy.
      final long now = realNow - NANOS_PER_MICRO;
      final double rate = rateNanosPerPermit;
      long asOf = asOfNanos;
      long stored = storedPermits;
      double jtr = jitter;
      
      if (now > asOf) {
         // last time stamp is in the past, so we compute the number of permits as of now
         long newPermits;
         if (jtr == 0) {
            newPermits = (long) ((now - asOf) / rate);
            asOf = now;
         } else {
            long delta = now - asOf;
            double jrate = computeJitteredRate(rate, jtr, delta);
            newPermits = (long) (delta / jrate);
            asOf = asOf + (long) (newPermits * jrate); 
         }
         stored = Math.max(stored, Math.min(maxStoredPermits, stored + newPermits));
         if (stored >= permits) {
            this.asOfNanos = asOf;
            this.storedPermits = stored - permits;
            return 0;
         }
         // fall through to below to figure out how long to wait
      } else if (stored >= permits) {
         this.storedPermits = stored - permits;
         return 0;
      }
      
      // compute how long caller must wait for the requested permits
      long shortage = permits - stored;
      long extraTime;
      if (jtr == 0) {
         extraTime = (long) (shortage * rate);
      } else {
         double jrate = computeJitteredRate(rate, jtr, shortage * rate);
         extraTime = (long) (shortage * jrate);
      }
      asOf += extraTime;
      long waitTime = Math.max(0, asOf - realNow); 
      if (maxWaitNanos < 0 || maxWaitNanos >= waitTime) {
         this.asOfNanos = asOf;
         this.storedPermits = 0;
         return waitTime;
      }
      return -1;
   }
   
   private static double computeJitteredRate(double rate, double jtr, double timePeriodNanos) {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      double timePeriodSeconds = timePeriodNanos / NANOS_PER_SEC;
      double j = jtr * Math.pow(r.nextDouble(), timePeriodSeconds);
      if (r.nextBoolean()) {
         j = -j;
      }
      return rate * (1.0 + j);
   }
}
