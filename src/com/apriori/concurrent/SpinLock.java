package com.apriori.concurrent;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

/**
 * A non-reentrant and unfair lock that "spins" while waiting to lock. The lock will regularly
 * {@linkplain Thread#yield() yield} if it's spinning too much. Because the only wait mechanism is
 * spinning, and no queues are used, the order of lock acquisition is non-deterministic (and thus
 * unfair, possibly resulting in starvation if highly contended).
 * 
 * <p>Timed lock operations are discouraged unless the wait times are measured in microseconds or
 * smaller, since spinning for long periods of time is wasteful of CPU compute capacity. For that
 * reason, this lock should only be used when the critical sections in which it is held are
 * very short/fast. Due to the use of spinning with periodic yields, timeout precision when
 * attempting a timed acquisition is greatly limited.
 * 
 * <p>This lock is non-reentrant and will deadlock if re-entrance is accidentally attempted. This
 * lock does not have an exclusive owner thread, so it can be locked in one thread and then unlocked
 * in another. However, this is discouraged since it implies that the duration for which the lock is
 * held is non-deterministic. (See previous paragraph, about using this to guard very short/fast
 * critical sections.)
 * 
 * <p>Unlike lock acquisitions, awaiting a {@link Condition} created by this lock does not use
 * spinning. Threads awaiting conditions are enqueued and will be notified in FIFO order by calls to
 * {@link Condition#signal()}.
 * 
 * <p>Like normal locks, awaiting or signaling a condition requires that the lock be locked. But
 * since the lock has no concept of an "owner", it cannot be verified that it is the awaiting or
 * signaling thread that has locked it.
 * 
 * <p>Awaiting a condition will unlock the lock before parking the awaiting thread. If that unlock
 * operation fails, an {@link IllegalMonitorStateException} is thrown. Signaling a condition just
 * does a best effort check that the lock is locked. If the lock is not locked at the onset of the
 * operation, an {@link IllegalMonitorStateException} is thrown. But if the lock is concurrently
 * unlocked by another thread, awaiting threads are still signaled.
 * 
 * @see NonReentrantLock
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class SpinLock implements Lock {

   final AtomicBoolean locked = new AtomicBoolean();

   /**
    * Constructs a new spin lock.
    */
   public SpinLock() {
   }
   
   @Override
   public void lock() {
      while (true) {
         for (int i = 0; i < 10_000; i++) {
            if (locked.compareAndSet(false, true)) {
               return;
            }
         }
         Thread.yield();
      }
   }

   @Override
   public void lockInterruptibly() throws InterruptedException {
      while (true) {
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         for (int i = 0; i < 10_000; i++) {
            if (locked.compareAndSet(false, true)) {
               return;
            }
         }
         Thread.yield();
      }
   }

   @Override
   public boolean tryLock() {
      return locked.compareAndSet(false,  true);
   }

   @Override
   public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      long deadline = System.nanoTime() + unit.toNanos(time);
      while (true) {
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         long nanosRemaining = deadline - System.nanoTime();
         if (nanosRemaining < 100) {
            do {
               if (locked.compareAndSet(false, true)) {
                  return true;
               }
               nanosRemaining = deadline - System.nanoTime();
            } while (nanosRemaining > 0);
            
            return false;

         } else {
            int attempts;
            if (nanosRemaining < 100_000)  {
               attempts = 10;
            } else if (nanosRemaining < 1_000_000) {
               attempts = 100;
            } else if (nanosRemaining < 10_000_000) {
               attempts = 1000;
            } else {
               attempts = 10_000;
            }
            for (int i = 0; i < attempts; i++) {
               if (locked.compareAndSet(false, true)) {
                  return true;
               }
            }
         }
         Thread.yield();
      }
   }

   @Override
   public void unlock() {
      locked.set(false);
   }

   @Override
   public Condition newCondition() {
      return new ConditionObject();
   }
   
   /**
    * A simple condition queue associated with a {@link SpinLock}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class ConditionObject implements Condition {
      private final ConcurrentLinkedQueue<Thread> waiters = new ConcurrentLinkedQueue<>();
      
      ConditionObject() {
      }
      
      /**
       * Ensures that the spin lock associated with this condition is locked.
       * 
       * @throws IllegalMonitorStateException if the lock is not locked
       */
      private void checkLock() {
         if (!locked.get()) {
            throw new IllegalMonitorStateException();
         }
      }
      
      /**
       * Unlocks the spin lock associated with this condition, requiring that it was locked.
       * 
       * @throws IllegalMonitorStateException if the lock cannot be unlocked because it was not
       *       locked to begin with
       */
      private void release() {
         if (!locked.compareAndSet(true, false)) {
            throw new IllegalMonitorStateException();
         }
      }
      
      @Override
      public void await() throws InterruptedException {
         checkLock();
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         Thread th = Thread.currentThread();
         release();
         waiters.add(th);
         try {
            LockSupport.park(this);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
         } finally {
            waiters.remove(th);
            lock();
         }
      }

      @Override
      public void awaitUninterruptibly() {
         checkLock();
         Thread th = Thread.currentThread();
         release();
         waiters.add(th);
         try {
            LockSupport.park(this);
         } finally {
            waiters.remove(th);
            lock();
         }
      }

      @Override
      public long awaitNanos(long nanosTimeout) throws InterruptedException {
         checkLock();
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         long start = System.nanoTime();
         Thread th = Thread.currentThread();
         release();
         waiters.add(th);
         try {
            LockSupport.parkNanos(this, nanosTimeout);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            return nanosTimeout - (System.nanoTime() - start);
         } finally {
            waiters.remove(th);
            lock();
         }
      }

      @Override
      public boolean await(long time, TimeUnit unit) throws InterruptedException {
         return awaitNanos(unit.toNanos(time)) > 0;
      }

      @Override
      public boolean awaitUntil(Date deadline) throws InterruptedException {
         Date now = new Date();
         return awaitNanos(TimeUnit.MILLISECONDS.toNanos(deadline.getTime() - now.getTime())) > 0;
      }

      @Override
      public void signal() {
         checkLock();
         Thread th = waiters.poll();
         if (th != null) {
            LockSupport.unpark(th);
         }
      }

      @Override
      public void signalAll() {
         checkLock();
         while (true) {
            Thread th = waiters.poll();
            if (th == null) {
               return;
            }
            LockSupport.unpark(th);
         }
      }
   }
}
