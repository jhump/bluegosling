package com.bluegosling.concurrent;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.bluegosling.util.Clock;
import com.bluegosling.util.SystemClock;
import com.google.common.util.concurrent.ForwardingFuture;
import com.google.common.util.concurrent.ForwardingListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

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
 * <li>This version provides additional methods for reserving permits without blocking to actually
 * acquire them and also for scheduling tasks on permit acquisition instead of blocking. This
 * means it can be readily used in non-blocking constructs.</li>
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
// TODO: tests
public class RateLimiter {
   private static final Duration ZERO = Duration.of(0, TimeUnit.NANOSECONDS);
   private static final long NANOS_PER_SEC = TimeUnit.SECONDS.toNanos(1);
   private static final long NANOS_PER_MICRO = TimeUnit.MICROSECONDS.toNanos(1);
   
   private final Clock clock;

   private long storedPermits;
   private long asOfNanos;
   
   private double rateNanosPerPermit;
   private long maxStoredPermits;
   private double jitter;
   
   /**
    * Creates a new limiter with the given number of permits allowed per second. The rate limiter
    * will allow storing up to one second's worth of permits, to allow for bursts. If the
    * limiter allows fewer than one permit per second, the burst capacity is just a single permit.
    * The limiter will initially be "full", allowing an initial burst for up to one second's worth
    * of activity.
    * 
    * @param ratePermitsPerSecond the allowed number of permits per second
    * @throws IllegalArgumentException if the rate is non-positive or {@linkplain Double#NaN NaN} 
    */
   public RateLimiter(double ratePermitsPerSecond) {
      this(ratePermitsPerSecond, (long) Math.max(ratePermitsPerSecond, 1));
   }

   /**
    * Creates a new limiter with the given number of permits allowed per second and maximum
    * capacity. The maximum capacity is the limit to which unused permits can accrue, which allows
    * for bursts. The limiter will initially be "full", allowing an initial burst for up to one
    * second's worth of activity.
    * 
    * @param ratePermitsPerSecond the allowed number of permits per second
    * @param maxStoredPermits the maximum capacity for bursts
    * @throws IllegalArgumentException if the rate is non-positive or {@linkplain Double#NaN NaN}
    *       or if the given maximum capacity is non-positive 
    */
   public RateLimiter(double ratePermitsPerSecond, long maxStoredPermits) {
      this(ratePermitsPerSecond, maxStoredPermits, maxStoredPermits);
   }

   /**
    * Creates a new limiter with the given number of permits allowed per second, maximum capacity,
    * and initial capacity. The maximum capacity is the limit to which unused permits can accrue,
    * which allows for bursts. The initial capacity can allow an initial burst immediately after
    * construction, up to the given number of permits.
    * 
    * @param ratePermitsPerSecond the allowed number of permits per second
    * @param maxStoredPermits the maximum capacity for bursts
    * @param initialPermits the initial capacity, allowing for an initial burst if greater than zero
    * @throws IllegalArgumentException if the rate is non-positive or {@linkplain Double#NaN NaN}
    *       or if the given maximum or initial capacity numbers are non-positive 
    */
   public RateLimiter(double ratePermitsPerSecond, long maxStoredPermits, long initialPermits) {
      this(ratePermitsPerSecond, maxStoredPermits, initialPermits, 0);
   }
   
   /**
    * Creates a new "jittered" limiter with the given number of permits allowed per second,
    * maximum capacity, and initial capacity. The maximum capacity is the limit to which unused
    * permits can accrue, which allows for bursts. The initial capacity can allow an initial burst
    * immediately after construction, up to the given number of permits.
    * 
    * <p>The amount of jitter must be between 0 and 1 (inclusive). It adds randomness to permit
    * acquisition. So a limiter that allows up to 10 permits per second with a jitter of 1.0 may
    * allow as few as zero permits in some seconds and up to 20 permits in others. With a jitter of
    * 0.1, it would allow as few as nine permits on some seconds and up to 11 in others.
    * 
    * @param ratePermitsPerSecond the allowed number of permits per second
    * @param maxStoredPermits the maximum capacity for bursts
    * @param initialPermits the initial capacity, allowing for an initial burst if greater than zero
    * @param jitter the amount of jitter
    * @throws IllegalArgumentException if the rate is non-positive or {@linkplain Double#NaN NaN},
    *       if the given maximum or initial capacity numbers are non-positive, or if the given
    *       amount of jitter is less than zero or greater than one 
    */
   public RateLimiter(double ratePermitsPerSecond, long maxStoredPermits, long initialPermits,
         double jitter) {
      this(ratePermitsPerSecond, maxStoredPermits, initialPermits, jitter, SystemClock.INSTANCE);
   }
   
   /**
    * Creates a new "jittered" limiter that uses the given clock and the given number of permits
    * allowed per second, maximum capacity, and initial capacity. The maximum capacity is the limit
    * to which unused permits can accrue, which allows for bursts. The initial capacity can allow an
    * initial burst immediately after construction, up to the given number of permits.
    * 
    * <p>The amount of jitter must be between 0 and 1 (inclusive). It adds randomness to permit
    * acquisition. So a limiter that allows up to 10 permits per second with a jitter of 1.0 may
    * allow as few as zero permits in some seconds and up to 20 permits in others. With a jitter of
    * 0.1, it would allow as few as nine permits on some seconds and up to 11 in others.
    * 
    * <p>The clock, which by default is a {@link SystemClock}, can be used to override accounting
    * of time. This can be used to control how acquisitions block until a permit is ready. Note that
    * is has no impact on calls that do not block, like {@link #reserve()} or
    * {@link #onAcquire(Callable, ScheduledExecutorService)}.
    * 
    * @param ratePermitsPerSecond the allowed number of permits per second
    * @param maxStoredPermits the maximum capacity for bursts
    * @param initialPermits the initial capacity, allowing for an initial burst if greater than zero
    * @param jitter the amount of jitter
    * @param clock the clock used to account for time and block until permits are ready
    * @throws IllegalArgumentException if the rate is non-positive or {@linkplain Double#NaN NaN},
    *       if the given maximum or initial capacity numbers are non-positive, or if the given
    *       amount of jitter is less than zero or greater than one 
    */
   public RateLimiter(double ratePermitsPerSecond, long maxStoredPermits, long initialPermits,
         double jitter, Clock clock) {
      this.clock = requireNonNull(clock, "clock");

      checkArgument(initialPermits >= 0, "initial permits, %s, should be >= 0", initialPermits);
      this.storedPermits = initialPermits;
      this.asOfNanos = clock.nanoTime();

      setRate(ratePermitsPerSecond);
      setMaxPermits(maxStoredPermits);
      setJitter(jitter);
   }
   
   /**
    * Acquires a permit, blocking if one is not available. This method is shorthand for
    * {@code limiter.acquire(1)}.
    */
   public void acquire() {
      acquire(1);
   }
   
   /**
    * Acquires the given number of permits, blocking if they are not all immediately available.
    * 
    * @param permits the number of permits being acquired
    * @throws IllegalArgumentException if the given number of permits is non-positive
    */
   public void acquire(long permits) {
      checkArgument(permits >= 1, "permits, %s, should be >= 1", permits);
      long acquireCompleteNanos = makeReservation(permits, -1);
      clock.uninterruptedSleep(acquireCompleteNanos, TimeUnit.NANOSECONDS);
   }
   
   /**
    * Acquires a permit, blocking interruptibly if one is not available. This method is shorthand
    * for {@code limiter.acquireInterruptibly(1)}.
    * 
    * <p>If the permit is reserved and this thread is interrupted while waiting, the permit is
    * {@linkplain #putBack(long) put back}.
    * 
    * @throws InterruptedException if the current thread is interrupted while waiting for the permit
    */
   public void acquireInterruptibly() throws InterruptedException {
      acquireInterruptibly(1);
   }

   /**
    * Acquires the given number of permits, blocking interruptibly if they are not all immediately
    * available.
    * 
    * <p>If the permits are reserved and this thread is interrupted while waiting for them, the
    * permits are {@linkplain #putBack(long) put back}.
    * 
    * @param permits the number of permits being acquired
    * @throws InterruptedException if the current thread is interrupted while waiting for the
    *       permits to become available
    * @throws IllegalArgumentException if the given number of permits is non-positive
    */
   public void acquireInterruptibly(int permits) throws InterruptedException {
      checkArgument(permits >= 1, "permits, %s, should be >= 1", permits);
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

   /**
    * Tries to acquires a single permit immediately, returning false if a permit is not available.
    * This method is shorthand for {@code limiter.tryAcquire(1)}.
    * 
    * @return true if the permit was successfully acquired or false if one was not available
    */
   public boolean tryAcquire() {
      return tryAcquire(1);
   }

   /**
    * Tries to acquires a the given number of permits immediately, returning false if the permits
    * are not all available.
    * 
    * @param permits the number of permits being acquired
    * @return true if the permits were successfully acquired or false if they were not all available
    * @throws IllegalArgumentException if the given number of permits is non-positive
    */
   public boolean tryAcquire(long permits) {
      checkArgument(permits >= 1, "permits, %s, should be >= 1", permits);
      long acquireCompleteNanos = makeReservation(permits, 0);
      return acquireCompleteNanos != -1;
   }
   
   /**
    * Tries to acquire a permit, waiting up to the given time limit if one is not immediately
    * available. This method is shorthand for {@code limiter.tryAcquire(1, timeLimit, unit)}.
    * 
    * <p>Note that if the permit will not be available in time, this method does not block and will
    * instead return immediately.
    * 
    * <p>If the permit is reserved and this thread is interrupted while waiting for it, the
    * permit is {@linkplain #putBack(long) put back}.
    * 
    * @param timeLimit the limit of time to wait for the permit to become available
    * @param unit the unit of the time limit
    * @return true if the permit was successfully acquired or false if they will not be available
    *       before the given time limit expires
    * @throws InterruptedException if the current thread is interrupted while waiting for the
    *       permit to become available
    * @throws IllegalArgumentException if the given time limit is negative
    */
   public boolean tryAcquire(long timeLimit, TimeUnit unit)
         throws InterruptedException {
      return tryAcquire(1, timeLimit, unit);
   }

   /**
    * Tries to acquire the given number of permits, waiting up to the given time limit if they are
    * not all immediately available.
    * 
    * <p>Note that if the permits will not be available in time, this method does not block and will
    * instead return immediately.
    * 
    * <p>If the permits are reserved and this thread is interrupted while waiting for them, the
    * permits are {@linkplain #putBack(long) put back}.
    * 
    * @param permits the number of permits being acquired
    * @param timeLimit the limit of time to wait for the permits to become available
    * @param unit the unit of the time limit
    * @return true if the permits were successfully acquired or false if they will not be available
    *       before the given time limit expires
    * @throws InterruptedException if the current thread is interrupted while waiting for the
    *       permits to become available
    * @throws IllegalArgumentException if the given number of permits is non-positive or if the
    *       given time limit is negative
    */
   public boolean tryAcquire(long permits, long timeLimit, TimeUnit unit)
         throws InterruptedException {
      checkArgument(permits >= 1, "permits, %s, should be >= 1", permits);
      checkArgument(timeLimit >= 0, "time limit, %s, should be >= 0", timeLimit);
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
   
   /**
    * Reserves a permit, returning the duration between now and the time the permit becomes
    * available. This method is shorthand for {@code limiter.reserve(1)}.
    * 
    * <p>This does not block, expecting the caller to block or to schedule an action in the
    * future after the returned duration elapses.
    * 
    * @return the duration between now and the moment the reserved permit becomes available
    */
   public Duration reserve() {
      return reserve(1);
   }

   /**
    * Reserves the given number of permits, returning the duration between now and the time the
    * permits become available. This does not block, expecting the caller to block or to schedule
    * an action in the future after the returned duration elapses.
    * 
    * @param permits the number of permits being reserved
    * @return the duration between now and the moment the reserved permits become available
    * @throws IllegalArgumentException if the given number of permits is non-positive
    */
   public Duration reserve(long permits) {
      checkArgument(permits >= 1, "permits, %s, should be >= 1", permits);
      long acquireCompleteNanos = makeReservation(permits, -1);
      return Duration.of(acquireCompleteNanos, TimeUnit.NANOSECONDS);
   }

   /**
    * Reserves a permit if the duration between now and when the permit becomes available is less
    * than or equal to the given time limit. This method is shorthand for
    * {@code limiter.tryReserve(1, timeLimit, unit)}.
    * 
    * <p>This does not block, expecting the caller to block or to schedule an action in the
    * future after the returned duration elapses.
    * 
    * @param timeLimit the limit of time for the permits to become available
    * @param unit the unit of the time limit
    * @return the duration between now and the moment the reserved permit becomes available or
    *       {@code null} if that duration would be greater than the given time limit
    * @throws IllegalArgumentException if the given time limit is negative
    */
   public Duration tryReserve(long timeLimit, TimeUnit unit) {
      return tryReserve(1, timeLimit, unit);
   }

   /**
    * Reserves a permit if the duration between now and when the permit becomes available is less
    * than or equal to the given time limit. This method is shorthand for
    * {@code limiter.tryReserve(1, timeLimit, unit)}.
    * 
    * <p>This does not block, expecting the caller to block or to schedule an action in the
    * future after the returned duration elapses.
    * 
    * @param permits the number of permits being reserved
    * @param timeLimit the limit of time for the permits to become available
    * @param unit the unit of the time limit
    * @return the duration between now and the moment the reserved permit becomes available or
    *       {@code null} if that duration would be greater than the given time limit
    * @throws IllegalArgumentException if the given number of permits is non-positive or if the
    *       given time limit is negative
    */
   public Duration tryReserve(long permits, long timeLimit, TimeUnit unit) {
      checkArgument(permits >= 1, "permits, %s, should be >= 1", permits);
      long acquireCompleteNanos = makeReservation(permits, unit.toNanos(timeLimit));
      return acquireCompleteNanos == -1
            ? null
            : Duration.of(acquireCompleteNanos, TimeUnit.NANOSECONDS);
   }
   
   /**
    * Acquires a permit and schedules the given task to be called when acquired. This method
    * reserves the permit and then uses the given executor to schedule the task for when the permit
    * becomes available. This method is shorthand for {@code limiter.onAcquire(1, task, scheduler)}.
    * 
    * <p>Note that if the returned future is cancelled before the task is called, the reserved
    * permit is {@linkplain #putBack(long) put back}.
    * 
    * <p>If the given executor is a {@link ListeningScheduledExecutorService} then the returned
    * future will actually implement {@link ListenableScheduledFuture}.
    * 
    * @param task the task that is called
    * @param scheduler the executor used to schedule the task
    * @return a future that will complete when the task completes
    */
   public <T> ScheduledFuture<T> onAcquire(Callable<T> task, ScheduledExecutorService scheduler) {
      return onAcquire(1, task, scheduler);
   }
   
   /**
    * Acquires the given number of permits and schedules the given task to be called when acquired.
    * This method reserves the permits and then uses the given executor to schedule the task for
    * when the permits become available.
    * 
    * <p>Note that if the returned future is cancelled before the task is called, the reserved
    * permits are {@linkplain #putBack(long) put back}.
    * 
    * <p>If the given executor is a {@link ListeningScheduledExecutorService} then the returned
    * future will actually implement {@link ListenableScheduledFuture}.
    * 
    * @param permits the number of permits being acquired
    * @param task the task that is called
    * @param scheduler the executor used to schedule the task
    * @return a future that will complete when the task completes
    * @throws IllegalArgumentException if the given number of permits is non-positive
    */
   public <T> ScheduledFuture<T> onAcquire(long permits, Callable<T> task,
         ScheduledExecutorService scheduler) {
      checkArgument(permits >= 1, "permits, %s, should be >= 1", permits);
      requireNonNull(task, "task");
      requireNonNull(scheduler, "scheduler");
      long acquireCompleteNanos = makeReservation(permits, -1);
      AtomicReference<TaskState> state = new AtomicReference<>(TaskState.NOT_STARTED);
      ScheduledFuture<T> f = scheduler.schedule(() -> {
         if (state.compareAndSet(TaskState.NOT_STARTED, TaskState.STARTED)) {
            return task.call();
         } else {
            // if we get here then future has been cancelled, so return value doesn't really matter
            return null;
         }
      }, acquireCompleteNanos, TimeUnit.NANOSECONDS);
      return wrap(f, state, permits);
   }
   
   /**
    * The state of a task scheduled to be executed on acquisition of a permit.
    * @see RateLimiter#onAcquire(long, Callable, ScheduledExecutorService)
    */
   private static enum TaskState {
      NOT_STARTED, STARTED, CANCELLED
   }
   
   /**
    * Wraps the given future with one that will automatically put back the given permits if
    * cancelled before the scheduled task has started. If the given future implements
    * {@link ListenableScheduledFuture} then so will the returned wrapper.
    * 
    * The returned future just delegates all method calls to the given future. But it will also
    * put back the given number of permits if the task is
    * {@linkplain ScheduledFuture#cancel(boolean) cancelled} before starting.
    * 
    * @param future the future to wrap
    * @param taskState the state of the task, used to determine whether it has started or not
    * @param permits the number of permits to return on cancellation
    * @return a future that will return the given number of permits if cancelled before the task
    *       has started
    */
   private <T> ScheduledFuture<T> wrap(ScheduledFuture<T> future,
         AtomicReference<TaskState> taskState, long permits) {
      if (future instanceof ListenableScheduledFuture) {
         return new CancellingListenableFuture<>(
               (ListenableScheduledFuture<T>) future, taskState, permits);
      } else {
         return new CancellingFuture<>(future, taskState, permits);
      }
   }

   /**
    * Acquires a permit and schedules the given task to be run when acquired. This method reserves
    * the permit and then uses the given executor to schedule the task for when the permit becomes
    * available. This method is shorthand for {@code limiter.onAcquire(1, task, scheduler)}.
    * 
    * <p>Note that if the returned future is cancelled before the task is run, the reserved permit
    * is {@linkplain #putBack(long) put back}.
    * 
    * <p>If the given executor is a {@link ListeningScheduledExecutorService} then the returned
    * future will actually implement {@link ListenableScheduledFuture}.
    * 
    * @param task the task that is run
    * @param scheduler the executor used to schedule the task
    * @return a future that will complete when the task completes
    */
   public ScheduledFuture<Void> onAcquire(Runnable task, ScheduledExecutorService scheduler) {
      return onAcquire(1, Executors.callable(task, null), scheduler);
   }

   /**
    * Acquires the given number of permits and schedules the given task to be run when acquired.
    * This method reserves the permits and then uses the given executor to schedule the task for
    * when the permits become available.
    * 
    * <p>Note that if the returned future is cancelled before the task is run, the reserved permits
    * are {@linkplain #putBack(long) put back}.
    * 
    * <p>If the given executor is a {@link ListeningScheduledExecutorService} then the returned
    * future will actually implement {@link ListenableScheduledFuture}.
    * 
    * @param permits the number of permits being acquired
    * @param task the task that is run
    * @param scheduler the executor used to schedule the task
    * @return a future that will complete when the task completes
    * @throws IllegalArgumentException if the given number of permits is non-positive
    */
   public ScheduledFuture<Void> onAcquire(long permits, Runnable task,
         ScheduledExecutorService scheduler) {
      return onAcquire(permits, Executors.callable(task, null), scheduler);
   }

   /**
    * Returns the given number of permits. This simply adds them to the current number of stored
    * permits.
    * 
    * <p>There is no enforced requirement that the caller has first acquired the permits, but that
    * is the expected and correct usage. Otherwise, use of this method may cause this limiter to
    * allow acquisition of permits at a rate that is greater than the currently configured rate.
    * 
    * <p>Frequent use of this method may cause the limiter to allow more bursty acquisition patterns
    * than expected. The expected use is to return permits that go unused. This could be because the
    * unit of work that needed the permits is cancelled or it could be for scenarios where the
    * number of permits required for a task is not unknown up-front. In the latter case, an estimate
    * for the number of permits is acquired. If the actual work needed less than the estimate, the
    * unused permits can be put back. (Similarly, if the actual work needed more than the estimate,
    * the extra permits can be {@linkplain #forceTake(long) taken forcibly} to reconcile.)
    * 
    * <p>Use of this method can change the "fairness" of acquisitions. For example, if the limiter
    * allows just one permit per second and ten tasks each acquire a permit, they will end up
    * running with an interval of one second between. If one of these tasks is cancelled such that
    * its permit is returned, its reservation is still taken and effectively remains empty. To
    * compensate, the returned permit will allow any subsequent acquisition to succeed immediately.
    * Since the acquisition could occur before the reserved point in time, this would appear to be
    * a momentary burst (an extra task running in the same second as another) that is reconciled by
    * the coming gap for the actual reserved moment.
    * 
    * @param permits the number of permits to return
    * @throws IllegalArgumentException if the given number of permits is negative
    */
   public synchronized void putBack(long permits) {
      checkArgument(permits >= 0, "permits, %s, should be >= 0", permits);
      if (permits == 0) {
         return;
      }
      this.storedPermits += permits;
   }

   /**
    * Forcibly takes permits without waiting to acquire them. This method always succeeds, so if
    * more permits are taken than actually available, the "stored balance" becomes negative. This
    * will delay subsequent acquisitions as if the taker had actually reserved all of the taken
    * permits.
    * 
    * @param permits the number of permits to take
    * @throws IllegalArgumentException if the given number of permits is negative
    */
   public synchronized void forceTake(long permits) {
      checkArgument(permits >= 0, "permits, %s, should be >= 0", permits);
      if (permits == 0) {
         return;
      }
      this.storedPermits -= permits;
   }
   
   /**
    * Takes all permits currently available, returning the count of taken permits.
    * 
    * @return the number of permits taken, which can be zero of there were no permits available
    */
   public synchronized long takeAll() {
      long ret = storedPermits;
      if (ret <= 0) {
         return 0;
      }
      storedPermits = 0;
      return ret;
   }
   
   /**
    * Returns the number of permits currently available. Not that this method is racy since the
    * value can be changing due to concurrent acquisitions. This method is intended for
    * instrumentation and monitoring purposes.
    * 
    * @return the number of permits immediately available
    */
   public synchronized long permitsAvailable() {
      return Math.max(0, storedPermits);
   }

   /**
    * Returns the duration of the current acquisition backlog. The duration represents the time
    * between now and the moment the next successful acquisition would actually execute. Note that
    * this method is racy since the value can be changing due to concurrent acquisitions. This
    * method is intended for instrumentation and monitoring purposes.
    * 
    * @return the duration between now and the moment the next successful acquisition would execute
    */
   public synchronized Duration backlogDuration() {
      if (storedPermits > 0) {
         return ZERO;
      }
      long shortage = storedPermits + 1;
      long now = clock.nanoTime();
      long availableWhen = asOfNanos + (long)(shortage * rateNanosPerPermit);
      return Duration.of(Math.max(0, availableWhen - now), TimeUnit.NANOSECONDS);
   }

   /**
    * Gets the current allowed rate of permit acquisition, in permits per second.
    * 
    * @return the current allowed rate of permit acquisition
    */
   public synchronized double getRate() {
      return NANOS_PER_SEC / rateNanosPerPermit;
   }

   /**
    * Sets the allowed rate of permit acquisition, in permits per second.
    * 
    * @param permitsPerSecond the new allowed rate of permit acquisition
    * @throws IllegalArgumentException if the rate is non-positive or {@linkplain Double#NaN NaN} 
    */
   public synchronized void setRate(double permitsPerSecond) {
      checkArgument(!Double.isNaN(permitsPerSecond), "rate should be a valid number, not NaN");
      checkArgument(permitsPerSecond > 0,
            "rate, %s, should be > 0 permits per second", permitsPerSecond);
      this.rateNanosPerPermit = NANOS_PER_SEC / permitsPerSecond;
   }

   /**
    * Gets the current maximum capacity of permits, for bursts.
    * 
    * @return the maximum capacity of permits
    */
   public synchronized long getMaxPermits() {
      return maxStoredPermits;
   }

   /**
    * Sets the maximum capacity of permits, for bursts.
    * 
    * @param maxStoredPermits the new maximum capacity of permits
    * @throws IllegalArgumentException if the capacity is non-positive 
    */
   public synchronized void setMaxPermits(long maxStoredPermits) {
      checkArgument(maxStoredPermits > 0,
            "max stored permits, %s, should be > 0", maxStoredPermits);
      this.maxStoredPermits = maxStoredPermits;
   }

   /**
    * Gets the current jitter amount. The returned value will be between 0.0 and 1.0 where 0.0 means
    * no randomness or jitter and 1.0 is the maximum jitter allowed.
    * 
    * @return the current jitter amount
    */
   public synchronized double getJitter() {
      return jitter;
   }

   /**
    * Sets the jitter amount. The value must be between 0.0 and 1.0 where 0.0 means no randomness or
    * jitter and 1.0 is the maximum jitter allowed.
    * 
    * @param jitter the new jitter amount
    * @throws IllegalArgumentException if the given amount of jitter is less than zero or greater
    *       than one 
    */
   public synchronized void setJitter(double jitter) {
      checkArgument(jitter >= 0.0 && jitter <= 1.0,
            "jitter, %s, must be between 0.0 and 1.0 (inclusive)", jitter);
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
      /*
       * NB: This is lossy and could result in inaccurate rate limiting for extremely high rates
       * (e.g. close to or exceeding one billion permits per second). This should be okay since the
       * realistic limits allowed are much less than that, simply due to typical CPU clock speeds
       * and the number of cycles required to acquire a permit as well as due to possible lock
       * contention (if many threads are trying to concurrently acquire permits).
       * 
       * To address this inaccuracy, asOfNanos should allow non-integral values (e.g. use a double
       * or perhaps fixed-precision with 64 bits after the decimal by representing it as two longs). 
       */

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
         long newStored = Math.max(stored, Math.min(maxStoredPermits, stored + newPermits));
         stored = newStored < stored ? Long.MAX_VALUE : newStored;
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
      // TODO: this methodology does not really work for very small time periods (e.g. much smaller
      // than one second)
      ThreadLocalRandom r = ThreadLocalRandom.current();
      double timePeriodSeconds = timePeriodNanos / NANOS_PER_SEC;
      double j = jtr * Math.pow(r.nextDouble() * 2 - 1, timePeriodSeconds);
      return rate * (1.0 + j);
   }

   /**
    * A {@link ListenableScheduledFuture} that returns permits if cancelled before the associated
    * task starts.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of future result
    */
   private class CancellingListenableFuture<T> extends ForwardingListenableFuture<T>
   implements ListenableScheduledFuture<T> {
      private final ListenableScheduledFuture<T> delegate;
      private final AtomicReference<TaskState> taskState;
      private final long permits;
      
      CancellingListenableFuture(ListenableScheduledFuture<T> delegate,
            AtomicReference<TaskState> taskState, long permits) {
         this.delegate = delegate;
         this.taskState = taskState;
         this.permits = permits;
      }

      @Override
      protected ListenableScheduledFuture<T> delegate() {
         return delegate;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         boolean ret = super.cancel(mayInterruptIfRunning);
         if (ret && taskState.compareAndSet(TaskState.NOT_STARTED, TaskState.CANCELLED)) {
            // CAS succeeded means task hasn't started
            putBack(permits);
         }
         return ret;
      }

      @Override
      public long getDelay(TimeUnit unit) {
         return delegate.getDelay(unit);
      }

      @Override
      public int compareTo(Delayed o) {
         return delegate.compareTo(o);
      }
   }
   
   /**
    * A scheduled future that returns permits if cancelled before the associated
    * task starts.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of future result
    */
   private class CancellingFuture<T> extends ForwardingFuture<T> implements ScheduledFuture<T> {
      private final ScheduledFuture<T> delegate;
      private final AtomicReference<TaskState> taskState;
      private final long permits;
      
      CancellingFuture(ScheduledFuture<T> delegate, AtomicReference<TaskState> taskState,
            long permits) {
         this.delegate = delegate;
         this.taskState = taskState;
         this.permits = permits;
      }

      @Override
      protected ScheduledFuture<T> delegate() {
         return delegate;
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         boolean ret = super.cancel(mayInterruptIfRunning);
         if (ret && taskState.compareAndSet(TaskState.NOT_STARTED, TaskState.CANCELLED)) {
            // CAS succeeded means task hasn't started
            putBack(permits);
         }
         return ret;
      }

      @Override
      public long getDelay(TimeUnit unit) {
         return delegate.getDelay(unit);
      }

      @Override
      public int compareTo(Delayed o) {
         return delegate.compareTo(o);
      }
   }
}

