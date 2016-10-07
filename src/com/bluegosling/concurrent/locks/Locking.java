package com.bluegosling.concurrent.locks;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * An interface that encapsulates typical locking patterns. This interface, and the related enclosed
 * interfaces, provide mechanisms to execute a block of code with a lock held. It is implemented in
 * a way that will never "leak" a lock (e.g. fail to unlock).
 * 
 * <p>For read-write and stamped locks, operations are also provided for demoting a held write lock
 * to a read lock. Stamped locks also include operations for promoting a read lock to a write lock
 * and for performing optimistic reads.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests!
public interface Locking {
   /**
    * Runs the given action while holding a lock.
    * 
    * @param action the action to run
    */
   void runWithLock(Runnable action);
   
   /**
    * Invokes the given action while holding a lock.
    * 
    * @param action the action to run
    * @return the value returned by the given action
    * @throws Exception if the given action throws an action
    */
   <T> T callWithLock(Callable<T> action) throws Exception;
   
   /**
    * An interface that encapsulates typical patterns for read-write locks.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface ReadWriteLocking {
      /**
       * Runs the given action while holding a read (e.g. shared) lock.
       * 
       * @param action the action to run
       */
      void runWithReadLock(Runnable action);
      
      /**
       * Invokes the given action while holding a read (e.g. shared) lock.
       * 
       * @param action the action to run
       * @return the value returned by the given action
       * @throws Exception if the given action throws an action
       */
      <T> T callWithReadLock(Callable<T> action) throws Exception;
      
      /**
       * Runs the given action while holding a write (e.g. exclusive) lock.
       * 
       * @param action the action to run
       * @throws IllegalMonitorStateException if the thread already holds a read lock (since a read
       *       lock cannot be upgraded to a write lock this way)
       */
      void runWithWriteLock(Runnable action);

      /**
       * Invokes the given action while holding a write (e.g. exclusive) lock.
       * 
       * @param action the action to run
       * @return the value returned by the given action
       * @throws Exception if the given action throws an action
       * @throws IllegalMonitorStateException if the thread already holds a read lock (since a read
       *       lock cannot be upgraded to a write lock this way)
       */
      <T> T callWithWriteLock(Callable<T> action) throws Exception;
      
      /**
       * Demotes the currently held write lock to a read lock.
       * 
       * @throws IllegalMonitorStateException if the current thread does not hold a write lock
       */
      void demoteToRead();
      
      /**
       * Returns a locking interface that corresponds to the read (e.g. shared) lock.
       * 
       * @return a locking interface that corresponds to the read lock
       */
      default Locking forReadLock() {
         return new Locking() {
            @Override
            public void runWithLock(Runnable action) {
               runWithReadLock(action);
            }

            @Override
            public <T> T callWithLock(Callable<T> action) throws Exception {
               return callWithReadLock(action);
            }
         };
      }

      /**
       * Returns a locking interface that corresponds to the write (e.g. exclusive) lock.
       * 
       * @return a locking interface that corresponds to the write lock
       */
      default Locking forWriteLock() {
         return new Locking() {
            @Override
            public void runWithLock(Runnable action) {
               runWithWriteLock(action);
            }

            @Override
            public <T> T callWithLock(Callable<T> action) throws Exception {
               return callWithWriteLock(action);
            }
         };
      }
   }

   /**
    * An interface that encapsulates typical patterns for stamped locks. Unlike typical usage of a
    * stamp lock, this interface does allow re-entrance. In particular:
    * <ul>
    * <li>An action running within {@link #runWithReadLock(Runnable)} can recursively invoke
    * {@link #runWithReadLock(Runnable)}.</li>
    * <li>An action running within {@link #runWithWriteLock(Runnable)} can recursively invoke
    * {@link #runWithWriteLock(Runnable)}, <em>or</em> it can invoke
    * {@link #runWithReadLock(Runnable)}. However, the converse (holding a read lock and then
    * invoking an action that needs a write lock) is not allowed.</li>
    * </ul>
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface StampedLocking extends ReadWriteLocking {
      /**
       * Runs the given action without acquiring a lock, relying on optimistic concurrency. If the
       * read fails due to interference (due to a thread holding or acquiring the write lock), falls
       * back to pessimistic concurrency by acquiring a read lock. Because of this fallback case,
       * it is possible that the given action is executed a second time, so it <em>must be
       * idempotent</em>.
       * 
       * @param action the action to run
       */
      void runWithOptimisticReadLock(Runnable action);
      
      /**
       * Invokes the given action without acquiring a lock, relying on optimistic concurrency. If
       * the read fails due to interference (due to a thread holding or acquiring the write lock),
       * falls back to pessimistic concurrency by acquiring a read lock. Because of this fallback
       * case, it is possible that the given action is executed a second time, so it <em>must be
       * idempotent</em>.
       * 
       * @param action the action to run
       * @return the value returned by the given action
       * @throws Exception if the given action throws an action
       */
      <T> T callWithOptimisticReadLock(Callable<T> action) throws Exception;
      
      /**
       * Promotes a held read lock into a write lock. If the read lock cannot be promoted (like due
       * to other threads holding read locks), the read lock is released and then a write lock
       * acquired. If promotion fails, the write lock is still held when this method returns, but
       * other threads may have acquired the write lock and made changes during the period when this
       * thread did not hold a lock (e.g. between releasing read lock and acquiring write lock).
       * 
       * @return true if the promotion was successful or false if the read lock had to first be
       *       relinquished and then the write lock acquired
       * @throws IllegalMonitorStateException if the current thread does not hold a read lock
       */
      boolean promoteToWrite();
   }
   
   /**
    * Returns a locking interface for the given lock. Note that this is not the appropriate way to
    * create a locking interface for the read or write portion of a {@link ReadWriteLock}:<pre>
    * // BAD. NOT the recommended way:
    * Locking readLock = Locking.forLock(rwLock.readLock());
    * 
    * // GOOD. This is the recommended alternative:
    * Locking readLock = Locking.forReadWriteLock(rwLock).forReadLock();
    * </pre>
    * @param lock the lock that is held when actions are invoked through the interface
    * @return a locking interface for the given lock
    */
   static Locking forLock(Lock lock) {
      return new Locking() {
         @Override
         public void runWithLock(Runnable action) {
            lock.lock();
            try {
               action.run();
            } finally {
               lock.unlock();
            }
         }

         @Override
         public <T> T callWithLock(Callable<T> action) throws Exception {
            lock.lock();
            try {
               return action.call();
            } finally {
               lock.unlock();
            }
         }
      };
   }
   
   /**
    * Returns a locking interface for the given read-write lock.
    * 
    * @param lock a read-write lock
    * @return a locking interface for the given lock
    */
   static ReadWriteLocking forReadWriteLock(ReadWriteLock lock) {
      return new ReadWriteLocking() {
         final ThreadLocal<Boolean> currentWriteLock = new ThreadLocal<>();
         
         @Override
         public void runWithReadLock(Runnable action) {
            Boolean prevWrite = currentWriteLock.get();
            currentWriteLock.set(false);
            lock.readLock().lock();
            try {
               action.run();
            } finally {
               lock.readLock().unlock();
               if (prevWrite == null) {
                  currentWriteLock.remove();
               } else {
                  currentWriteLock.set(prevWrite);
               }
            }
         }

         @Override
         public <T> T callWithReadLock(Callable<T> action) throws Exception {
            Boolean prevWrite = currentWriteLock.get();
            currentWriteLock.set(false);
            lock.readLock().lock();
            try {
               return action.call();
            } finally {
               lock.readLock().unlock();
               if (prevWrite == null) {
                  currentWriteLock.remove();
               } else {
                  currentWriteLock.set(prevWrite);
               }
            }
         }

         @Override
         public void runWithWriteLock(Runnable action) {
            Boolean prevWrite = currentWriteLock.get();
            if (prevWrite != null && !prevWrite) {
               throw new IllegalMonitorStateException(
                     "holding read lock, cannot acquire write lock");
            }
            currentWriteLock.set(true);
            lock.writeLock().lock();
            try {
               action.run();
            } finally {
               // could have demoted to read-lock, so check which one to release
               Boolean nowWrite = currentWriteLock.get();
               if (nowWrite) {
                  lock.writeLock().unlock();
               } else {
                  lock.readLock().unlock();
               }
               if (prevWrite == null) {
                  currentWriteLock.remove();
               } else {
                  currentWriteLock.set(prevWrite);
               }
            }
         }

         @Override
         public <T> T callWithWriteLock(Callable<T> action) throws Exception {
            Boolean prevWrite = currentWriteLock.get();
            if (prevWrite != null && !prevWrite) {
               throw new IllegalMonitorStateException(
                     "holding read lock, cannot acquire write lock");
            }
            currentWriteLock.set(true);
            lock.writeLock().lock();
            try {
               return action.call();
            } finally {
               // could have demoted to read-lock, so check which one to release
               Boolean nowWrite = currentWriteLock.get();
               if (nowWrite) {
                  lock.writeLock().unlock();
               } else {
                  lock.readLock().unlock();
               }
               if (prevWrite == null) {
                  currentWriteLock.remove();
               } else {
                  currentWriteLock.set(prevWrite);
               }
            }
         }
         
         @Override
         public void demoteToRead() {
            Boolean prevWrite = currentWriteLock.get();
            if (prevWrite == null || !prevWrite) {
               throw new IllegalMonitorStateException("not holding write lock, nothing to demote");
            }
            lock.readLock().lock();
            lock.writeLock().unlock();
            currentWriteLock.set(false);
         }
      };
   }
   
   /**
    * Returns a locking interface for the given stamped lock.
    * 
    * @param lock a stamped lock
    * @return a locking interface for the given lock
    */
   static StampedLocking forStampedLock(StampedLock lock) {
      return new StampedLocking() {
         final ThreadLocal<Long> currentStamp = new ThreadLocal<>();
         
         @Override
         public void runWithReadLock(Runnable action) {
            Long prevStamp = currentStamp.get();
            long stamp;
            if (prevStamp == null) {
               stamp = lock.readLock();
               currentStamp.set(stamp);
            } else {
               stamp = 0;
            }
            try {
               action.run();
            } finally {
               if (stamp != 0) {
                  // have to query stamp in case it was promoted
                  lock.unlock(currentStamp.get());
                  currentStamp.remove();
               }
            }
         }

         @Override
         public <T> T callWithReadLock(Callable<T> action) throws Exception {
            Long prevStamp = currentStamp.get();
            long stamp;
            if (prevStamp == null) {
               stamp = lock.readLock();
               currentStamp.set(stamp);
            } else {
               stamp = 0;
            }
            try {
               return action.call();
            } finally {
               if (stamp != 0) {
                  // have to query stamp in case it was promoted
                  lock.unlock(currentStamp.get());
                  currentStamp.remove();
               }
            }
         }

         @Override
         public void runWithWriteLock(Runnable action) {
            Long prevStamp = currentStamp.get();
            long stamp;
            if (prevStamp == null) {
               stamp = lock.writeLock();
               currentStamp.set(stamp);
            } else if (!lock.isWriteLocked()) {
               throw new IllegalMonitorStateException(
                     "holding read lock, cannot acquire write lock");
            } else {
               stamp = 0;
            }
            try {
               action.run();
            } finally {
               if (stamp != 0) {
                  // have to query stamp in case it was demoted
                  lock.unlock(currentStamp.get());
                  currentStamp.remove();
               }
            }
         }

         @Override
         public <T> T callWithWriteLock(Callable<T> action) throws Exception {
            Long prevStamp = currentStamp.get();
            long stamp;
            if (prevStamp == null) {
               stamp = lock.writeLock();
               currentStamp.set(stamp);
            } else if (!lock.isWriteLocked()) {
               throw new IllegalMonitorStateException(
                     "holding read lock, cannot acquire write lock");
            } else {
               stamp = 0;
            }
            try {
               return action.call();
            } finally {
               if (stamp != 0) {
                  // have to query stamp in case it was demoted
                  lock.unlock(currentStamp.get());
                  currentStamp.remove();
               }
            }
         }

         @Override
         public void demoteToRead() {
            Long prevStamp = currentStamp.get();
            if (prevStamp == null || !lock.isWriteLocked()) {
               throw new IllegalMonitorStateException("not holding write lock, nothing to demote");
            }
            long newStamp = lock.tryConvertToReadLock(prevStamp);
            if (newStamp == 0) {
               throw new IllegalMonitorStateException("failed to convert to read lock");
            }
            currentStamp.set(newStamp);
         }

         @Override
         public void runWithOptimisticReadLock(Runnable action) {
            long stamp = lock.tryOptimisticRead();
            if (stamp == 0) {
               runWithReadLock(action);
               return;
            }
            action.run();
            if (lock.validate(stamp)) {
               return;
            }
            runWithReadLock(action);
         }

         @Override
         public <T> T callWithOptimisticReadLock(Callable<T> action) throws Exception {
            long stamp = lock.tryOptimisticRead();
            if (stamp == 0) {
               return callWithReadLock(action);
            }
            T ret = action.call();
            if (lock.validate(stamp)) {
               return ret;
            }
            return callWithReadLock(action);
         }

         @Override
         public boolean promoteToWrite() {
            Long prevStamp = currentStamp.get();
            if (prevStamp == null || lock.isWriteLocked()) {
               throw new IllegalMonitorStateException("not holding read lock, nothing to promote");
            }
            long newStamp = lock.tryConvertToWriteLock(prevStamp);
            boolean promoted = newStamp != 0;
            if (!promoted) {
               lock.unlock(prevStamp);
               newStamp = lock.writeLock();
               return false;
            }
            currentStamp.set(newStamp);
            return promoted;
         }
      };
   }
}
