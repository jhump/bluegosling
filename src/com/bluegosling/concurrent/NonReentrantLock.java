package com.bluegosling.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A non-reentrant lock. It can be created in a fair mode, which ensures FIFO acquisition order, or
 * in an unfair mode (default), which has generally higher throughput. The unfair lock makes a best
 * effort to satisfy acquisitions in FIFO order, but barging can occur (and often will if the lock
 * is highly contended).
 * 
 * <p>Like the name says, this lock is non-reentrant and will deadlock if re-entrance is
 * accidentally attempted.
 * 
 * <p>Since it is not reentrant (i.e. no bookkeeping that allows it to track re-entrance),
 * this lock does not have an exclusive owner thread. This enables patterns of use where it is
 * locked by one thread but then unlocked by another, which facilitates interesting locking
 * protocols that support thread-skips when asynchronous programming styles are used.
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

 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class NonReentrantLock implements Lock {
   
   private final Sync sync;
   
   /**
    * Constructs a new, unfair lock.
    */
   public NonReentrantLock() {
      this(false);
   }
   
   /**
    * Constructs a new lock.
    * 
    * @param fair if true, the new lock is a fair lock; otherwise, it will be unfair
    */
   public NonReentrantLock(boolean fair) {
      sync = fair ? new FairSync() : new Sync();
   }

   /**
    * Returns true if this is a fair lock.
    *
    * @return true if this is a fair lock
    */
   public boolean isFair() {
      return sync instanceof FairSync;
   }
   
   @Override
   public void lock() {
      sync.acquire(1);
   }

   @Override
   public void lockInterruptibly() throws InterruptedException {
      sync.acquireInterruptibly(1);
   }

   @Override
   public boolean tryLock() {
      return sync.tryAcquire(1);
   }

   @Override
   public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return sync.tryAcquireNanos(1, unit.toNanos(time));
   }

   @Override
   public void unlock() {
      if (!sync.release(1)) {
         throw new IllegalMonitorStateException();
      }
   }

   @Override
   public Condition newCondition() {
      return sync.newCondition();
   }
   
   /**
    * The synchronizer used by unfair locks.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Sync extends AbstractQueuedSynchronizer {
      private static final long serialVersionUID = -6822562836982881979L;

      Sync() {
      }
      
      @Override
      protected boolean tryAcquire(int i) {
         assert i == 1;
         return compareAndSetState(0, i);
      }
      
      @Override
      protected boolean tryRelease(int i) {
         if (i == 0) {
            // this happens if awaiting a condition but lock not held
            return false;
         }
         assert i == 1;
         return compareAndSetState(i, 0);
      }
      
      @Override
      protected boolean isHeldExclusively() {
         return getState() != 0;
      }
      
      Condition newCondition() {
         return new ConditionObject();
      }
   }

   /**
    * The synchronizer used by fair locks.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class FairSync extends Sync {
      private static final long serialVersionUID = 2018198650583469557L;

      FairSync() {
      }

      @Override
      protected boolean tryAcquire(int i) {
         return !hasQueuedPredecessors() && super.tryAcquire(i);
      }
   }
}
