package com.apriori.concurrent;

import java.util.Date;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * Numerous utility methods related to blocking using {@link ManagedBlocker}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class ManagedBlockers {
   private ManagedBlockers() {
   }

   /** A sentinel value used to represent {@code null}. */
   static final Object NULL_SENTINEL = new Object();
   
   /** A sentinel value used to represent a timeout condition. */
   static final Object TIMEOUT_SENTINEL = new Object();

   /**
    * Like a {@link Supplier}, except allowed to throw an {@link InterruptedException}.
    *
    * @param <T> the type of value supplied
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public static interface InterruptibleSupplier<T> {
      T get() throws InterruptedException;
   }

   /**
    * Similar to {@link InterruptibleSupplier}, but accepts a time limit argument and can throw
    * a {@link TimeoutException} if the given time limit elapses before the result value is
    * available.
    * 
    * @param <T> the type of value supplied
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public static interface TimedInterruptibleSupplier<T> {
      T get(long nanosTimeLimit) throws InterruptedException, TimeoutException;
   }

   /**
    * A function that waits up to a given limit for some event. The function returns true if the
    * event happens before the given time limit elapses; false otherwise.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @FunctionalInterface
   public static interface TimedInterruptibleWait {
      boolean get(long nanosTimeLimit) throws InterruptedException;
   }
   
   /**
    * Blocks using a {@link ManagedBlocker} in a non-interruptible way. If the thread is interrupted
    * while blocking, it will be ignored and blocking will continue. If interrupted, the interrupt
    * status on the thread is restored before this method returns.
    *
    * @param blocker the blocker used to block the current thread
    */
   public static void blockUninterruptibly(ManagedBlocker blocker) {
      boolean interrupted = false;
      while (true) {
         try {
            ForkJoinPool.managedBlock(blocker);
            break;
         } catch (InterruptedException e) {
            interrupted = true;
         }
      }
      if (interrupted) {
         Thread.currentThread().interrupt();
      }
   }

   /**
    * Performs a managed block to get a value from the given supplier. The given supplier is allowed
    * (expected actually) to block.
    *
    * @param supplier a supplier that blocks to produce a value
    * @return the value returned from the given supplier
    * @throws InterruptedException if the current thread is interrupted while waiting for the result
    */
   public static <T> T managedBlockFor(InterruptibleSupplier<T> supplier)
         throws InterruptedException {
      SupplierBlocker<T> blocker = new SupplierBlocker<>(supplier);
      ForkJoinPool.managedBlock(blocker);
      return blocker.get();
   }

   /**
    * Performs an uninterruptible managed block to get a value from the given supplier. The given
    * supplier is allowed (expected actually) to block.
    *
    * @param supplier a supplier that blocks to produce a value
    * @return the value returned from the given supplier
    */
   public static <T> T managedBlockUninterruptiblyFor(InterruptibleSupplier<T> supplier) {
      SupplierBlocker<T> blocker = new SupplierBlocker<>(supplier);
      blockUninterruptibly(blocker);
      return blocker.get();
   }
   
   /**
    * Performs a managed block, up to a given time limit, to get a value from the given supplier.
    * The given supplier is allowed (expected actually) to block.
    * 
    * <p>The given supplier is expected to handle the time limit itself by respecting the argument
    * passed to it. That argument will be the number of nanoseconds it has to complete. It should
    * throw a {@link TimeoutException} if it cannot return a result in time.
    *
    * @param supplier a supplier that blocks to produce a value
    * @param timeLimit the time limit to wait for the value
    * @param unit the unit of the given time limit
    * @return the value returned from the given supplier
    * @throws InterruptedException if the current thread is interrupted while waiting for the result
    * @throws TimeoutException if the given time limit elapses before a result is returned
    */
   public static <T> T managedTimedBlockFor(TimedInterruptibleSupplier<T> supplier, long timeLimit,
         TimeUnit unit) throws InterruptedException, TimeoutException {
      TimedSupplierBlocker<T> blocker = new TimedSupplierBlocker<>(supplier, timeLimit, unit);
      ForkJoinPool.managedBlock(blocker);
      return blocker.get();
   }

   /**
    * Performs a managed block, up to a given time limit, for some event. The given function should
    * return true if the event occurs or false if the time limit elapsed first.
    * 
    * <p>The given function is expected to handle the time limit itself by respecting the argument
    * passed to it. That argument will be the number of nanoseconds it has to complete. It should
    * return false if that elapses before the event for which it is waiting.
    *
    * @param waiter a function that waits until an event occurs
    * @param timeLimit the time limit to wait for the value
    * @param unit the unit of the given time limit
    * @return true if the event occurred or false if the time limit elapsed first
    * @throws InterruptedException if the current thread is interrupted while waiting for the result
    */
   public static boolean managedTimedWait(TimedInterruptibleWait waiter, long timeLimit,
         TimeUnit unit) throws InterruptedException {
      TimedSupplierBlocker<Boolean> blocker =
            new TimedSupplierBlocker<>(waiter::get, timeLimit, unit);
      ForkJoinPool.managedBlock(blocker);
      return !blocker.timedOut();
   }
   
   /**
    * Awaits an {@link Awaitable} using a {@link ManagedBlocker}. This effectively calls
    * {@code awaitable.await()}, but in a managed way that can be invoked from a
    * {@link ForkJoinPool}.
    *
    * @param awaitable an awaitable
    * @throws InterruptedException if the current thread is interrupted while waiting
    * @see #makeManaged(Awaitable)
    */
   public static void managedAwait(Awaitable awaitable) throws InterruptedException {
      managedBlockFor(() -> { awaitable.await(); return NULL_SENTINEL; });
   }

   /**
    * Awaits an {@link Awaitable} up to the given time limit using a {@link ManagedBlocker}. This
    * effectively calls {@code awaitable.await(timeLimit, unit)}, but in a managed way that can be
    * invoked from a {@link ForkJoinPool}.
    *
    * @param awaitable an awaitable
    * @param timeLimit the limit on the time to wait
    * @param unit the unit of the given time limit
    * @return true if the awaitable completed or false if the time limit elapsed first
    * @throws InterruptedException if the current thread is interrupted while waiting
    * @see #makeManaged(Awaitable)
    */
   public static boolean managedAwait(Awaitable awaitable, long timeLimit, TimeUnit unit)
         throws InterruptedException {
      return managedTimedWait(nanos -> awaitable.await(nanos, TimeUnit.NANOSECONDS),
            timeLimit, unit);
   }

   /**
    * Awaits an {@link Awaitable} in an uninterruptible way, using a {@link ManagedBlocker}. This
    * effectively calls {@code awaitable.awaitUninterruptibly()}, but in a managed way that can be
    * invoked from a {@link ForkJoinPool}.
    *
    * @param awaitable an awaitable
    * @see #makeManaged(Awaitable)
    */
   public static void managedAwaitUninterruptibly(Awaitable awaitable) {
      managedBlockUninterruptiblyFor(
            () -> { awaitable.awaitUninterruptibly(); return NULL_SENTINEL; });
   }
   
   /**
    * Acquires a lock using a {@link ManagedBlocker}. This effectively calls {@code lock.lock()},
    * but in a managed way that can be invoked from a {@link ForkJoinPool}.
    *
    * @param lock a lock
    * @see #makeManaged(Lock)
    */
   public static void managedLock(Lock lock) {
      managedBlockUninterruptiblyFor(() -> { lock.lock(); return NULL_SENTINEL; });
   }

   /**
    * Acquires a lock, allowing interruptions, using a {@link ManagedBlocker}. This effectively
    * calls {@code lock.lockInterruptibly()}, but in a managed way that can be invoked from a
    * {@link ForkJoinPool}.
    *
    * @param lock a lock
    * @throws InterruptedException if the current thread is interrupted while waiting to acquire
    * @see #makeManaged(Lock)
    */
   public static void managedLockInterruptibly(Lock lock) throws InterruptedException {
      managedBlockFor(() -> { lock.lockInterruptibly(); return NULL_SENTINEL; });
   }

   /**
    * Acquires a lock, waiting up to the given time limit, using a {@link ManagedBlocker}. This
    * effectively calls {@code lock.tryLock(timeLimit, unit)}, but in a managed way that can be
    * invoked from a {@link ForkJoinPool}.
    *
    * @param lock a lock
    * @param timeLimit the limit on the time to wait for acquisition
    * @param unit the unit of the given time limit
    * @return true if the lock was acquired or false if the time limit elapsed first
    * @throws InterruptedException if the current thread is interrupted while waiting to acquire
    * @see #makeManaged(Lock)
    */
   public static boolean managedTryLock(Lock lock, long timeLimit, TimeUnit unit)
         throws InterruptedException {
      return managedTimedWait(nanos -> lock.tryLock(nanos, TimeUnit.NANOSECONDS), timeLimit, unit);
   }

   /**
    * Awaits a {@link Condition} using a {@link ManagedBlocker}. This effectively calls
    * {@code condition.await()}, but in a managed way that can be invoked from a
    * {@link ForkJoinPool}.
    *
    * @param condition a condition
    * @throws InterruptedException if the current thread is interrupted while waiting
    * @see #makeManaged(Condition)
    */
   public static void managedConditionAwait(Condition condition) throws InterruptedException {
      managedBlockFor(() -> { condition.await(); return NULL_SENTINEL; });
   }

   /**
    * Awaits a {@link Condition} up to the given time limit using a {@link ManagedBlocker}. This
    * effectively calls {@code condition.await(timeLimit, unit)}, but in a managed way that can be
    * invoked from a {@link ForkJoinPool}.
    *
    * @param condition a condition
    * @param timeLimit the limit on the time to wait
    * @param unit the unit of the given time limit
    * @return true if the condition signaled or false if the time limit elapsed first
    * @throws InterruptedException if the current thread is interrupted while waiting
    * @see #makeManaged(Condition)
    */
   public static boolean managedConditionAwait(Condition condition, long timeLimit, TimeUnit unit)
         throws InterruptedException {
      return managedTimedWait(nanos -> condition.await(nanos, TimeUnit.NANOSECONDS),
            timeLimit, unit);
   }
   
   /**
    * Awaits a {@link Condition} in an uninterruptible way, using a {@link ManagedBlocker}. This
    * effectively calls {@code condition.awaitUninterruptibly()}, but in a managed way that can be
    * invoked from a {@link ForkJoinPool}.
    *
    * @param condition a condition
    * @see #makeManaged(Condition)
    */
   public static void managedConditionAwaitUninterruptibly(Condition condition) {
      managedBlockUninterruptiblyFor(
            () -> { condition.awaitUninterruptibly(); return NULL_SENTINEL; });
   }

   /**
    * Returns a version of the given awaitable that uses {@link ManagedBlocker}s for all blocking
    * operations.
    *
    * @param awaitable an awaitable
    * @return an awaitable that delegates all calls to the given awaitable, except for using
    *       {@link ManagedBlocker}s for any blocking operations
    */
   public static Awaitable makeManaged(Awaitable awaitable) {
      if (awaitable instanceof ManagedAwaitable) {
         return awaitable;
      }
      return new ManagedAwaitable(awaitable);
   }
   
   /**
    * Returns a version of the given lock that uses {@link ManagedBlocker}s for all blocking
    * operations. Any conditions {@linkplain Lock#newCondition() created} by the returned lock also
    * use {@link ManagedBlocker}s for blocking operations.
    *
    * @param lock a lock
    * @return an lock that delegates all calls to the given lock, except for using
    *       {@link ManagedBlocker}s for any blocking operations
    * @see #makeManaged(Condition)
    */
   public static Lock makeManaged(Lock lock) {
      if (lock instanceof ManagedLock) {
         return lock;
      }
      return new ManagedLock(lock);
   }
   
   /**
    * Returns a version of the given condition that uses {@link ManagedBlocker}s for all blocking
    * operations.
    *
    * @param condition a condition
    * @return an condition that delegates all calls to the given condition, except for using
    *       {@link ManagedBlocker}s for any blocking operations
    */
   public static Condition makeManaged(Condition condition) {
      if (condition instanceof ManagedCondition) {
         return condition;
      }
      return new ManagedCondition(condition);
   }

   /**
    * A blocker that invokes a given supplier and retains the supplied value.
    *
    * @param <T> the type of value supplied
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class SupplierBlocker<T> implements ManagedBlocker {
      private final InterruptibleSupplier<T> supplier;
      private T result;
      
      SupplierBlocker(InterruptibleSupplier<T> supplier) {
         this.supplier = supplier;
      }
      
      T get() {
         return result != NULL_SENTINEL ? result : null;
      }
      
      @SuppressWarnings("unchecked")
      @Override
      public boolean block() throws InterruptedException {
         if (result != null) {
            return true;
         }
         T t = supplier.get();
         result = t == null ? (T) NULL_SENTINEL : t;
         return true;
      }

      @Override
      public boolean isReleasable() {
         return result != null;
      }
   }      

   /**
    * A blocker that invokes a given supplier and retains the supplied value.
    *
    * @param <T> the type of value supplied
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class TimedSupplierBlocker<T> implements ManagedBlocker {
      private final TimedInterruptibleSupplier<T> supplier;
      private final long deadline;
      private T result;
      
      TimedSupplierBlocker(TimedInterruptibleSupplier<T> supplier, long timeLimit, TimeUnit unit) {
         this.supplier = supplier;
         long now = System.nanoTime();
         if (timeLimit < 0) {
            timeLimit = 0;
         }
         long nowPlusTimeLimit = now + unit.toNanos(timeLimit);
         // handle overflow
         this.deadline = nowPlusTimeLimit < now ? Long.MAX_VALUE : nowPlusTimeLimit;
      }
      
      T get() throws TimeoutException {
         if (result == TIMEOUT_SENTINEL) {
            throw new TimeoutException();
         }
         return result != NULL_SENTINEL ? result : null;
      }
      
      boolean timedOut() {
         return result == TIMEOUT_SENTINEL;
      }
      
      @SuppressWarnings("unchecked")
      @Override
      public boolean block() throws InterruptedException {
         if (result != null) {
            return true;
         }
         try {
            T t = supplier.get(deadline - System.nanoTime());
            result = t == null ? (T) NULL_SENTINEL : t;
         } catch (TimeoutException e) {
            result = (T) TIMEOUT_SENTINEL;
         }
         return true;
      }

      @SuppressWarnings("unchecked")
      @Override
      public boolean isReleasable() {
         if (result != null) {
            return true;
         }
         if (System.nanoTime() >= deadline) {
            result = (T) TIMEOUT_SENTINEL;
            return true;
         }
         return false;
      }
   }
   
   /**
    * An awaitable implementation that delegates all actions to a given awaitable except for
    * blocking operations, which are wrapped in {@link ManagedBlocker}s.
    * 
    * @see ManagedBlockers#makeManaged(Awaitable)
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ManagedAwaitable implements Awaitable {
      private final Awaitable awaitable;

      ManagedAwaitable(Awaitable awaitable) {
         this.awaitable = awaitable;
      }
      
      @Override
      public void await() throws InterruptedException {
         managedAwait(awaitable);
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         return managedAwait(awaitable, limit, unit);
      }

      @Override
      public void awaitUninterruptibly() {
         managedAwaitUninterruptibly(awaitable);
      }

      @Override
      public boolean isDone() {
         return awaitable.isDone();
      }
   }
   
   /**
    * A lock implementation that delegates all actions to a given lock except for blocking
    * operations, which are wrapped in {@link ManagedBlocker}s.
    * 
    * @see ManagedBlockers#makeManaged(Lock)
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ManagedLock implements Lock {
      private final Lock lock;
      
      ManagedLock(Lock lock) {
         this.lock = lock;
      }
      
      @Override
      public void lock() {
         managedLock(lock);
      }

      @Override
      public void lockInterruptibly() throws InterruptedException {
         managedLockInterruptibly(lock);
      }

      @Override
      public boolean tryLock() {
         return lock.tryLock();
      }

      @Override
      public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
         return managedTryLock(lock, time, unit);
      }

      @Override
      public void unlock() {
         lock.unlock();
      }

      @Override
      public Condition newCondition() {
         return makeManaged(lock.newCondition());
      }
   }
   
   /**
    * A condition implementation that delegates all actions to a given condition except for blocking
    * operations, which are wrapped in {@link ManagedBlocker}s.
    * 
    * @see ManagedBlockers#makeManaged(Condition)
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ManagedCondition implements Condition {
      private final Condition condition;
      
      ManagedCondition(Condition condition) {
         this.condition = condition;
      }
      
      @Override
      public void await() throws InterruptedException {
         managedConditionAwait(condition);
      }

      @Override
      public boolean await(long time, TimeUnit unit) throws InterruptedException {
         return managedConditionAwait(condition, time, unit);
      }

      @Override
      public void awaitUninterruptibly() {
         managedConditionAwaitUninterruptibly(condition);
      }

      @Override
      public long awaitNanos(long nanosTimeout) throws InterruptedException {
         long now = System.nanoTime();
         if (nanosTimeout < 0) {
            nanosTimeout = 0;
         }
         long deadline = now + nanosTimeout;
         // avoid overflow
         if (deadline < now) {
            deadline = Long.MAX_VALUE;
         }
         await(nanosTimeout, TimeUnit.NANOSECONDS);
         return deadline - System.nanoTime();
      }

      @Override
      public boolean awaitUntil(Date deadline) throws InterruptedException {
         return await(deadline.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
      }

      @Override
      public void signal() {
         condition.signal();
      }

      @Override
      public void signalAll() {
         condition.signalAll();
      }
   }
}