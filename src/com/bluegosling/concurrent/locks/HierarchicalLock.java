package com.bluegosling.concurrent.locks;

import static com.bluegosling.concurrent.ManagedBlockers.managedBlockFor;
import static com.bluegosling.concurrent.ManagedBlockers.managedBlockUninterruptiblyFor;

import com.bluegosling.concurrent.DeadlockException;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ManagedBlocker;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * A hierarchical lock that supports shared and exclusive locking modes and supports lock
 * promotion and demotion. Locks can be promoted from child to parent or from shared to exclusive,
 * and they can be demoted from parent to child or from exclusive to shared.
 * 
 * <p>To prevent deadlock, promotions and demotions perform deadlock detection before blocking when
 * the target lock is unavailable.
 * 
 * <p>Hierarchical locks are re-entrant. So if a thread holds a lock, an attempt to re-acquire the
 * same lock will succeed. Lock releases must be balanced with acquisitions or else the lock may
 * never be properly released. An exclusive or shared lock that has been acquired by one thread
 * cannot be manipulated by another. An attempt to acquire a shared lock followed by an exclusive
 * lock (or vice versa) without using the lock promotion/demotion methods will fail with a deadlock
 * exception. Furthermore, lock promotion/demotion only works when the held lock has been acquired
 * only once. If the lock has been re-entered, it cannot be promoted or demoted until additional
 * entrances are released.
 * 
 * <p>When a child lock is acquired, whether it be a shared lock or an exclusive lock, a shared lock
 * on the parent is implicitly acquired. So, an attempt to acquire a shared lock on one component
 * and then an exclusive lock on its parent will fail with a deadlock exception. Instead, the first
 * lock must be promoted to a lock on the parent, and then that resulting lock must be promoted to
 * exclusive. This also means that demoting a shared lock to a child doesn't actually release the
 * parent lock as it is still held implicitly by having a lock on the child. But the parent lock
 * object cannot be used to release it; it will be released implicitly when the lock on the child is
 * released.
 * 
 * <p>This lock places a limit on the number of simultaneously held locks. Up to 32,767
 * (2<sup>15</sup> - 1) exclusive locks can be held simultaneously. Since only one thread can hold
 * the lock exclusively, this allows for that thread to acquire/re-enter the lock 32k times before
 * an error occurs. And up to {@link Integer#MAX_VALUE} (2<sup>31</sup> - 1) shared locks can be
 * held simultaneously. This limit is the total of acquisitions, be it in the form of one thread
 * acquiring/re-entering the lock 2 billion times, or 2 billion threads each acquiring it once.
 * (These limits are intentionally very high such that no practical use of the lock will ever bump
 * into them.)
 * 
 * <p>This lock is safe to use in a {@link ForkJoinPool}. If blocking methods are invoked from such
 * a pool then a {@link ManagedBlocker} is used.
 * 
 * <p><strong>NOTE:</strong> Deadlock detection in this class only works when other waiting threads
 * are also waiting for a {@link HierarchicalLock}. It is still possible for deadlock to occur if
 * threads in the graph are waiting on other kinds of synchronizers. Code that acquires a
 * {@link HierarchicalLock} should generally not do so while holding any other kind of synchronizer
 * so that deadlock detection works properly.
 *
 * @see SharedLock
 * @see ExclusiveLock
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public class HierarchicalLock implements Serializable {
   private static final long serialVersionUID = 997443573217210719L;

   /**
    * A map of waiting threads to the exclusive lock on which they wait. This is maintained to
    * enable deadlock detection.
    */
   static ConcurrentHashMap<Thread, HierarchicalLock> blockedForExclusive = 
         new ConcurrentHashMap<>();

   /**
    * A map of waiting threads to the shared lock on which they wait. This is maintained to enable
    * deadlock detection.
    */
   static ConcurrentHashMap<Thread, HierarchicalLock> blockedForShared = new ConcurrentHashMap<>();
   
   /**
    * The result of acquiring a {@link HierarchicalLock}. After acquisition, this object can be used
    * to release, demote, or promote the held lock. This API just contains methods in common to both
    * types of acquired locks. Both concrete acquired locks provide a wider API, with methods that
    * are specific to them. 
    *
    * @see ExclusiveLock
    * @see SharedLock
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static interface AcquiredLock {
      /**
       * Releases the held lock.
       * 
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *       released it
       */
      void unlock();
      
      /**
       * Demotes this lock to a lock of the same type (exclusive or shared) on one of its children.
       * On success, the held lock is released and replaced with a new {@link AcquiredLock} that
       * represents the lock on the child. This method will block until the child lock becomes
       * available, if necessary.
       *
       * @param child the child of this lock to which this lock is demoted
       * @return the newly acquired lock on the given child
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; if the given lock is not actually a child of the {@link HierarchicalLock}
       *    which produced this {@link AcquiredLock}; or if the lock could not be demoted because
       *    this thread holds the lock more than once (from re-entrance)
       * @throws DeadlockException if waiting for the child lock would result in a deadlock (e.g.
       *    this thread holds a lock for which another thread is waiting, and that thread, or some
       *    other thread that directly or indirectly is waiting on it, is holding the child lock)
       */
      AcquiredLock demoteToChild(HierarchicalLock child);
      
      /**
       * Promotes this lock to a lock of the same type (exclusive or shared) on its parent. On
       * success, the held lock is released and replaced with a new {@link AcquiredLock} that
       * represents the lock on the parent. This method will block until the parent lock becomes
       * available, if necessary.
       *
       * @return the newly acquired lock on the parent
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; if the lock that produced this {@link AcquiredLock} is a root lock and does
       *    not have a parent; or if this operation involved promoting the implicit parent lock, and
       *    that could not be done because this thread already holds the parent lock more than once
       *    (from re-entrance)
       * @throws DeadlockException if waiting for the parent lock would result in a deadlock (e.g.
       *    this thread holds a lock for which another thread is waiting, and that thread, or some
       *    other thread that directly or indirectly is waiting on it, is holding the parent lock)
       */
      AcquiredLock promoteToParent();
   }

   /**
    * Constructs a new lock that is unfair.
    *
    * @return a new unfair lock
    * 
    * @see #create(boolean)
    */
   public static HierarchicalLock create() {
      return new HierarchicalLock(false);
   }

   /**
    * Constructs a new lock with the specified fairness. A fair lock means that acquisitions will
    * be fairly ordered with regards to the order that threads request the lock. However, fair locks
    * have lower throughput.
    * 
    * <p>An unfair lock, on the other hand, has higher throughput but allows barging and possible
    * starvation of threads that are waiting to acquire the lock in exclusive mode.
    * 
    * <p>The fairness setting applies to the whole hierarchy. So creating a fair lock means that all
    * child locks created, in fact all descendants, will also be fair.
    * 
    * <p>Note that promoting a lock from shared to exclusive <em>can</em> barge in front of queued
    * threads that are also waiting for the lock in exclusive mode, even if the lock was created
    * as a fair lock.
    *
    * @return a new lock
    */
   public static HierarchicalLock create(boolean fair) {
      return new HierarchicalLock(fair);
   }

   /**
    * The lock's synchronizer.
    */
   final Sync sync;
   
   /**
    * Constructs a new lock
    * 
    * @see #create()
    * @see #create(boolean)
    */
   HierarchicalLock(boolean fair) {
      sync = new Sync(fair);
   }
   
   /**
    * Marks the current thread as blocking for this lock in exclusive mode.
    */
   void beginBlockingForExclusive() {
      HierarchicalLock l = blockedForExclusive.put(Thread.currentThread(), this);
      assert l == null;
   }
   
   /**
    * Marks the current thread as no longer blocking for this lock in exclusive mode.
    */
   void endBlockingForExclusive() {
      HierarchicalLock l = blockedForExclusive.remove(Thread.currentThread());
      assert l != null;
   }

   /**
    * Marks the current thread as blocking for this lock in shared mode.
    */
   void beginBlockingForShared() {
      HierarchicalLock l = blockedForShared.put(Thread.currentThread(), this);
      assert l == null;
   }
   
   /**
    * Marks the current thread as no longer blocking for this lock in shared mode.
    */
   void endBlockingForShared() {
      HierarchicalLock l = blockedForShared.remove(Thread.currentThread());
      assert l != null;
   }

   /**
    * Creates a new child of this lock.
    *
    * @return a new child of this lock
    */
   @SuppressWarnings("synthetic-access") // let it peek at private field of sync
   public HierarchicalLock newChild() {
      return new ChildLock(sync.fair);
   }
   
   /**
    * Gets the lock's parent. If this lock was created using {@link #create()} then it has no
    * parent. If it was created using {@link #newChild()} then it does have a parent.
    *
    * @return the lock's parent or {@code null} if it has no parent
    */
   public HierarchicalLock getParent() {
      // overridden by ChildLock to return non-null
      return null;
   }
   
   /**
    * Gets the thread that holds this lock in exclusive mode. Unless it is the current thread that
    * holds the lock, this method is best effort as it is possible that the other thread actually
    * releases the lock before this method can return. Similarly, it is possible that this method
    * returns {@code null} but that a thread acquired the lock before this method returns.
    *
    * @return the thread that holds this lock in exclusive mode or {@code null} if no thread holds
    *       the lock in exclusive mode
    *       
    * @see #getSharedHolders()
    */
   public Thread getExclusiveHolder() {
      return sync.getExclusiveHolder();
   }

   /**
    * Gets the set of threads that hold this lock in shared mode. This method is best effort as it
    * is possible for the set of lock-holders to change before this method returns its answer.
    *
    * @return the threads that hold this lock in shared mode; an empty list if held by none
    * 
    * @see #getExclusiveHolder()
    */
   public Collection<Thread> getSharedHolders() {
      return sync.getSharedHolders();
   }

   /**
    * Gets the first queued thread. This is the thread that has been waiting the longest to acquire
    * the lock.
    *
    * @return the first queued thread
    * 
    * @see AbstractQueuedSynchronizer#getFirstQueuedThread()
    */
   public Thread getFirstQueuedThread() {
      return sync.getFirstQueuedThread();
   }
   
   /**
    * Gets the collection of all queued threads.
    *
    * @return the collection of all queued threads
    * 
    * @see AbstractQueuedSynchronizer#getQueuedThreads()
    */
   public Collection<Thread> getQueuedThreads() {
      return sync.getQueuedThreads();
   }

   /**
    * A factory method to create a new {@link SharedLock} for this object.
    *
    * @param parentLock the implicit lock on the parent, may be {@code null}
    * @return a new {@link SharedLock}
    */
   SharedLock newSharedLock(SharedLock parentLock) {
      // overridden in ChildLock to support non-null parent lock
      assert parentLock == null;
      return new SharedLock();
   }
   
   /**
    * Tries to acquire this lock in shared mode, but does not block if it is not available.
    *
    * @return a {@link SharedLock} on success or {@code null} if it is unavailable
    */
   public SharedLock trySharedLock() {
      return doTrySharedLock(null);
   }
   
   SharedLock doTrySharedLock(SharedLock parentLock) {
      if (sync.tryAcquireShared(2) >= 0) {
         return newSharedLock(parentLock);
      } else {
         return null;
      }
   }

   /**
    * Tries to acquire this lock in shared mode and waits up to the specified amount of time if the
    * lock is unavailable.
    *
    * @param timeLimit the maximum amount of time to wait for the lock to become available
    * @param unit the time limit's unit
    * @return a {@link SharedLock} on success or {@code null} if it is unavailable
    * @throws InterruptedException if this thread is interrupted while waiting for the lock
    * @throws DeadlockException if waiting for this lock would result in a deadlock (e.g. this
    *    thread holds a lock for which another thread is waiting, and that thread, or some other
    *    thread that directly or indirectly is waiting on it, is holding this lock)
    */
   public SharedLock trySharedLock(long timeLimit, TimeUnit unit) throws InterruptedException {
      if (ForkJoinTask.inForkJoinPool()) {
         return managedBlockFor(() -> doTrySharedLock(timeLimit, unit));
      } else {
         return doTrySharedLock(timeLimit, unit);
      }
   }

   SharedLock doTrySharedLock(long timeLimit, TimeUnit unit) throws InterruptedException {
      return doTrySharedLock(timeLimit, unit, null);
   }
   
   SharedLock doTrySharedLock(long timeLimit, TimeUnit unit, SharedLock parentLock)
         throws InterruptedException {
      if (sync.tryAcquireShared(1) >= 0) {
         return newSharedLock(parentLock);
      } else {
         beginBlockingForShared();
         try {
            if (sync.tryAcquireSharedNanos(1, unit.toNanos(timeLimit))) {
               return newSharedLock(parentLock);
            } else {
               return null;
            }
         } finally {
            endBlockingForShared();
         }
      }
   }
   
   /**
    * Acquires the lock in shared mode, waiting if necessary for it to be available. The wait will
    * not be interrupted
    *
    * @return a {@link SharedLock}
    * @throws DeadlockException if waiting for this lock would result in a deadlock (e.g. this
    *    thread holds a lock for which another thread is waiting, and that thread, or some other
    *    thread that directly or indirectly is waiting on it, is holding this lock)
    *    
    * @see #sharedLockInterruptibly()
    */
   public SharedLock sharedLock() {
      if (ForkJoinTask.inForkJoinPool()) {
         return managedBlockUninterruptiblyFor(this::doSharedLock);
      } else {
         return doSharedLock();
      }
   }
   
   SharedLock doSharedLock() {
      return doSharedLock(null);
   }
   
   SharedLock doSharedLock(SharedLock parentLock) {
      if (sync.tryAcquireShared(1) >= 0) {
         return newSharedLock(parentLock);
      } else {
         beginBlockingForShared();
         try {
            sync.acquireShared(1);
            return newSharedLock(parentLock);
         } finally {
            endBlockingForShared();
         }
      }
   }
   
   /**
    * Acquires the lock in shared mode, waiting interruptibly if necessary for it to be available.
    *
    * @return a {@link SharedLock}
    * @throws InterruptedException if this thread is interrupted while waiting for the lock
    * @throws DeadlockException if waiting for this lock would result in a deadlock (e.g. this
    *    thread holds a lock for which another thread is waiting, and that thread, or some other
    *    thread that directly or indirectly is waiting on it, is holding this lock)
    */
   public SharedLock sharedLockInterruptibly() throws InterruptedException {
      if (ForkJoinTask.inForkJoinPool()) {
         return managedBlockFor(this::doSharedLockInterruptibly);
      } else {
         return doSharedLockInterruptibly();
      }
   }
   
   SharedLock doSharedLockInterruptibly() throws InterruptedException {
      return doSharedLockInterruptibly(null);
   }
   
   SharedLock doSharedLockInterruptibly(SharedLock parentLock) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      if (sync.tryAcquireShared(1) >= 0) {
         return newSharedLock(parentLock);
      } else {
         beginBlockingForShared();
         try {
            sync.acquireSharedInterruptibly(1);
            return newSharedLock(parentLock);
         } finally {
            endBlockingForShared();
         }
      }
   }
   
   /**
    * Invokes the specified callable while holding this lock in shared mode. This first {@linkplain
    * #sharedLock() acquires the lock}, then executes the callable, and then finally releases the
    * lock.
    *
    * @param callable the task to execute under lock
    * @return the value returned from the specified task
    * @throws DeadlockException if waiting to acquire this lock would result in a deadlock (e.g.
    *    this thread holds a lock for which another thread is waiting, and that thread, or some
    *    other thread that directly or indirectly is waiting on it, is holding this lock)
    * @throws Exception if the task throws an exception during its execution
    */
   public <T> T callWithSharedLock(Callable<T> callable) throws Exception {
      SharedLock lock = sharedLock();
      try {
         return callable.call();
      } finally {
         lock.unlock();
      }
   }
   
   /**
    * Invokes the specified runnable while holding this lock in shared mode. This first {@linkplain
    * #sharedLock() acquires the lock}, then executes the runnable, and then finally releases the
    * lock.
    *
    * @param runnable the task to execute under lock
    * @throws DeadlockException if waiting to acquire this lock would result in a deadlock (e.g.
    *    this thread holds a lock for which another thread is waiting, and that thread, or some
    *    other thread that directly or indirectly is waiting on it, is holding this lock)
    */
   public void runWithSharedLock(Runnable runnable) {
      SharedLock lock = sharedLock();
      try {
         runnable.run();
      } finally {
         lock.unlock();
      }
   }

   /**
    * A factory method to create a new {@link ExclusiveLock} for this object.
    *
    * @param parentLock the implicit lock on the parent, may be {@code null}
    * @return a new {@link ExclusiveLock}
    */
   ExclusiveLock newExclusiveLock(SharedLock parentLock) {
      // overridden in ChildLock to support non-null parent lock
      assert parentLock == null;
      return new ExclusiveLock();
   }
   
   /**
    * Tries to acquire this lock in exclusive mode, but does not block if it is not available.
    *
    * @return an {@link ExclusiveLock} on success or {@code null} if it is unavailable
    */
   public ExclusiveLock tryExclusiveLock() {
      return doTryExclusiveLock(null);
   }
   
   ExclusiveLock doTryExclusiveLock(SharedLock parentLock) {
      if (sync.tryAcquire(2)) {
         return newExclusiveLock(parentLock);
      } else {
         return null;
      }
   }
   
   /**
    * Tries to acquire this lock in exclusive mode and waits up to the specified amount of time if
    * the lock is unavailable.
    *
    * @param timeLimit the maximum amount of time to wait for the lock to become available
    * @param unit the time limit's unit
    * @return an {@link ExclusiveLock} on success or {@code null} if it is unavailable
    * @throws InterruptedException if this thread is interrupted while waiting for the lock
    * @throws DeadlockException if waiting for this lock would result in a deadlock (e.g. this
    *    thread holds a lock for which another thread is waiting, and that thread, or some other
    *    thread that directly or indirectly is waiting on it, is holding this lock)
    */
   public ExclusiveLock tryExclusiveLock(long timeLimit, TimeUnit unit)
         throws InterruptedException {
      if (ForkJoinTask.inForkJoinPool()) {
         return managedBlockFor(() -> doTryExclusiveLock(timeLimit, unit));
      } else {
         return doTryExclusiveLock(timeLimit, unit);
      }
   }

   ExclusiveLock doTryExclusiveLock(long timeLimit, TimeUnit unit)
         throws InterruptedException {
      return doTryExclusiveLock(timeLimit, unit, null);
   }

   ExclusiveLock doTryExclusiveLock(long timeLimit, TimeUnit unit, SharedLock parentLock)
         throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      if (sync.tryAcquire(1)) {
         return newExclusiveLock(parentLock);
      } else {
         beginBlockingForExclusive();
         try {
            if (sync.tryAcquireNanos(1, unit.toNanos(timeLimit))) {
               return newExclusiveLock(parentLock);
            } else {
               return null;
            }
         } finally {
            endBlockingForExclusive();
         }
      }
   }
   
   /**
    * Acquires the lock in exclusive mode, waiting if necessary for it to be available. The wait
    * will not be interrupted
    *
    * @return an {@link ExclusiveLock}
    * @throws DeadlockException if waiting for this lock would result in a deadlock (e.g. this
    *    thread holds a lock for which another thread is waiting, and that thread, or some other
    *    thread that directly or indirectly is waiting on it, is holding this lock)
    *    
    * @see #exclusiveLockInterruptibly()
    */
   public ExclusiveLock exclusiveLock() {
      if (ForkJoinTask.inForkJoinPool()) {
         return managedBlockUninterruptiblyFor(this::doExclusiveLock);
      } else {
         return doExclusiveLock();
      }
   }
   
   ExclusiveLock doExclusiveLock() {
      return doExclusiveLock(null);
   }
   
   ExclusiveLock doExclusiveLock(SharedLock parentLock) {
      if (sync.tryAcquire(1)) {
         return newExclusiveLock(parentLock);
      } else {
         beginBlockingForExclusive();
         try {
            sync.acquire(1);
            return newExclusiveLock(parentLock);
         } finally {
            endBlockingForExclusive();
         }
      }
   }
   
   /**
    * Acquires the lock in exclusive mode, waiting interruptibly if necessary for it to be
    * available.
    *
    * @return an {@link ExclusiveLock}
    * @throws InterruptedException if this thread is interrupted while waiting for the lock
    * @throws DeadlockException if waiting for this lock would result in a deadlock (e.g. this
    *    thread holds a lock for which another thread is waiting, and that thread, or some other
    *    thread that directly or indirectly is waiting on it, is holding this lock)
    */
   public ExclusiveLock exclusiveLockInterruptibly() throws InterruptedException {
      if (ForkJoinTask.inForkJoinPool()) {
         return managedBlockFor(this::doExclusiveLockInterruptibly);
      } else {
         return doExclusiveLockInterruptibly();
      }
   }
   
   ExclusiveLock doExclusiveLockInterruptibly() throws InterruptedException {
      return doExclusiveLockInterruptibly(null);
   }
   
   ExclusiveLock doExclusiveLockInterruptibly(SharedLock parentLock) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      if (sync.tryAcquire(1)) {
         return newExclusiveLock(parentLock);
      } else {
         beginBlockingForExclusive();
         try {
            sync.acquireInterruptibly(1);
            return newExclusiveLock(parentLock);
         } finally {
            endBlockingForExclusive();
         }
      }
   }
   
   /**
    * Invokes the specified callable while holding this lock in exclusive mode. This first
    * {@linkplain #exclusiveLock() acquires the lock}, then executes the callable, and then finally
    * releases the lock.
    *
    * @param callable the task to execute under lock
    * @return the value returned from the specified task
    * @throws DeadlockException if waiting to acquire this lock would result in a deadlock (e.g.
    *    this thread holds a lock for which another thread is waiting, and that thread, or some
    *    other thread that directly or indirectly is waiting on it, is holding this lock)
    * @throws Exception if the task throws an exception during its execution
    */
   public <T> T callWithExclusiveLock(Callable<T> callable) throws Exception {
      ExclusiveLock lock = exclusiveLock();
      try {
         return callable.call();
      } finally {
         lock.unlock();
      }
   }
   
   /**
    * Invokes the specified runnable while holding this lock in exclusive mode. This first
    * {@linkplain #exclusiveLock() acquires the lock}, then executes the runnable, and then finally
    * releases the lock.
    *
    * @param runnable the task to execute under lock
    * @throws DeadlockException if waiting to acquire this lock would result in a deadlock (e.g.
    *    this thread holds a lock for which another thread is waiting, and that thread, or some
    *    other thread that directly or indirectly is waiting on it, is holding this lock)
    */
   public void runWithExclusiveLock(Runnable runnable) {
      ExclusiveLock lock = exclusiveLock();
      try {
         runnable.run();
      } finally {
         lock.unlock();
      }
   }
   
   /**
    * Confirms that the specified lock is a child of this lock.
    *
    * @param child the child to check
    * @throws IllegalArgumentException if the specified object is not a child of this lock
    */
   void checkRelationship(HierarchicalLock child) {
      if (child.getParent() != this) {
         throw new IllegalArgumentException("specified object is not a child of this lock");
      }
   }
   
   /**
    * A node in a linked list of threads that hold a {@link HierarchicalLock}. This class extends
    * {@link AbstractQueuedSynchronizer} so it can support locking operations so list manipulation
    * can be done in a thread-safe way. Its locking and unlocking mechanisms are not reentrant. 
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @SuppressWarnings("serial") // cannot be serialized despite that it extends AQS which can
   private static class HolderNode extends AbstractQueuedSynchronizer {
      final boolean isExclusive;
      final Thread thread;
      volatile int holdCount;
      volatile HolderNode previous;
      volatile HolderNode next;
      
      HolderNode(boolean isExclusive) {
         this.isExclusive = isExclusive;
         thread = Thread.currentThread();
         holdCount = 1;
      }
      
      @Override protected boolean tryAcquire(int i) {
         return compareAndSetState(0, 1);
      }
      
      @Override protected boolean tryRelease(int i) {
         return compareAndSetState(1, 0);
      }
      
      void lock() {
         acquire(1);
      }
      
      void unlock() {
         if (!release(1)) {
            throw new IllegalStateException("node is not locked");
         }
      }
   }
   
   /**
    * The synchronizer for a {@link HierarchicalLock}. To support deadlock detection, this
    * synchronizer maintains a list of threads that hold the lock.
    *
    * @see HolderNode
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Sync extends AbstractQueuedLongSynchronizer {
      private static final long serialVersionUID = -4893343769900801555L;
      
      private static final long SHARED_MASK = 0x000000007fffffffL;
      private static final long SHARED_UNIT = 0x0000000000000001L;
      private static final long MAX_SHARED_COUNT = 0x000000007fffffffL;
      
      private static final long EXCLUSIVE_MASK = 0x00003fff80000000L;
      private static final int EXCLUSIVE_SHIFT = 31;
      private static final long EXCLUSIVE_UNIT = 0x0000000080000000L;
      private static final long MAX_EXCLUSIVE_COUNT = 0x0000000000007fffL;
      
      private static final long STAMP_MASK = 0xffffc00000000000L;
      private static final long STAMP_UNIT = 0x0000400000000000L;
      
      private static final AtomicReferenceFieldUpdater<Sync, HolderNode> holdersUpdater =
            AtomicReferenceFieldUpdater.newUpdater(Sync.class, HolderNode.class, "holders");
      
      private static long exclusiveCount(long state) {
         return (state & EXCLUSIVE_MASK) >>> EXCLUSIVE_SHIFT;
      }
      
      private static long sharedCount(long state) {
         return state & SHARED_MASK;
      }
      
      private static long stamp(long state) {
         return state & STAMP_MASK;
      }

      private final boolean fair;
      private transient volatile HolderNode holders;
      private final transient ThreadLocal<HolderNode> currentHolder = new ThreadLocal<>();
      
      Sync(boolean fair) {
         this.fair = fair;
      }
    
      private void checkForDeadlock(long i, boolean isWaitingForExclusive) {
         if (i > 1 || i < -1) {
            // calling code was just trying for the lock, not blocking on it, so there's
            // no deadlock
            return;
         }
         
         checkForDeadlock(isWaitingForExclusive, new HashSet<>());
      }

      private void checkForDeadlock(boolean isWaitingForExclusive, Set<Thread> blocked) {
         Thread exclusive = getExclusiveHolder();
         if (!isWaitingForExclusive && exclusive == null) {
            // waiting on shared lock and exclusive lock no longer held, so deadlock not possible
            return;
         }
         Collection<Thread> threads = exclusive != null
               ? Collections.singleton(exclusive) : getSharedHolders();
         for (Thread thread : threads) {
            if (blocked.contains(thread)) {
               throw new DeadlockException();
            }
            HierarchicalLock lock = blockedForExclusive.get(thread);
            if (lock != null) {
               isWaitingForExclusive = true;
            } else {
               lock = blockedForShared.get(thread);
               isWaitingForExclusive = false;
            }
            if (lock != null) {
               blocked.add(thread);
               lock.sync.checkForDeadlock(isWaitingForExclusive, blocked);
               blocked.remove(thread);
            }
         }
      }

      @Override protected boolean tryAcquire(long i) {
         HolderNode node = currentHolder.get();
         // i > 0 => normal acquisition; i < 0 => promotion from shared
         if (i > 0) {
            if (node != null && node.isExclusive) {
               // reentrant acquisition
               long state = getState();
               long exclusiveCount = exclusiveCount(state);
               assert holders == node && node.next == null && node.previous == null;
               assert sharedCount(state) == 0;
               assert node.holdCount > 0 && exclusiveCount == node.holdCount;
               
               if (exclusiveCount == MAX_EXCLUSIVE_COUNT) {
                  throw new IllegalStateException("already locked maximum number of times");
               }
               node.holdCount++;
               setState(state + EXCLUSIVE_UNIT + STAMP_UNIT);
               
               return true;
            }
            // non-reentrant
            if (fair && hasQueuedPredecessors()) {
               return false;
            }
            while (true) {
               long state = getState();
               if (sharedCount(state) > 0 || exclusiveCount(state) > 0) {
                  checkForDeadlock(i, true);
                  return false;
               }
               
               assert holders == null && node == null;
               
               long newState = stamp(state) + EXCLUSIVE_UNIT + STAMP_UNIT;
               if (compareAndSetState(state, newState)) {
                  break;
               }
            }
            
         } else {
            // lock promotion
            if (node == null || node.holdCount == 0 || node.isExclusive) {
               throw new IllegalStateException("no shared lock to promote");
            }
            if (node.holdCount > 1) {
               throw new IllegalStateException("cannot promote re-entrant shared lock");
            }
            while (true) {
               long state = getState();
               long sharedCount = sharedCount(state);
               
               assert sharedCount >= 1;
               
               if (sharedCount != 1) {
                  checkForDeadlock(i, true);
                  return false;
               }
               
               assert holders == node;
               assert node.next == null;
               
               long newState = stamp(state) + EXCLUSIVE_UNIT + STAMP_UNIT;
               if (compareAndSetState(state, newState)) {
                  break;
               }
            }
         }
         
         // got the exclusive lock!
         node = new HolderNode(true);
         holders = node;
         currentHolder.set(node);
         return true;
      }
      
      @Override protected boolean tryRelease(long i) {
         HolderNode node = currentHolder.get();
         if (node == null || !node.isExclusive) {
            return false;
         }
         long state = getState();
         assert holders == node && node.next == null && node.previous == null;
         assert sharedCount(state) == 0;
         assert exclusiveCount(state) == node.holdCount;
         if (--node.holdCount == 0) {
            holders = null;
            currentHolder.set(null);
         }
         setState(state - EXCLUSIVE_UNIT + STAMP_UNIT);
         return true;
      }
      
      @Override protected long tryAcquireShared(long i) {
         HolderNode node = currentHolder.get();
         // i > 0 => normal acquisition; i < 0 => demotion from exclusive
         if (i > 0) {
            if (node != null && !node.isExclusive) {
               // reentrant acquisition
               while (true) {
                  long state = getState();
                  long sharedCount = sharedCount(state);
                  assert exclusiveCount(state) == 0;
                  assert node.holdCount > 0 && sharedCount >= node.holdCount;
                  
                  if (sharedCount == MAX_SHARED_COUNT) {
                     throw new IllegalStateException("already locked maximum number of times");
                  }
                  long newState = state + SHARED_UNIT + STAMP_UNIT;
                  if (compareAndSetState(state, newState)) {
                     node.holdCount++;
                     return 1;
                  }
               }
            }
            // non-reentrant
            if (fair && hasQueuedPredecessors()) {
               return -1;
            }
            while (true) {
               long state = getState();
               if (exclusiveCount(state) > 0) {
                  checkForDeadlock(i, false);
                  return -1;
               }
               if (sharedCount(state) == MAX_SHARED_COUNT) {
                  throw new IllegalStateException("already locked maximum number of times");
               }
               
               assert node == null;
               
               long newState = state + SHARED_UNIT + STAMP_UNIT;
               if (compareAndSetState(state, newState)) {
                  break;
               }
            }
            
         } else {
            // lock demotion
            if (node == null || node.holdCount == 0 || !node.isExclusive) {
               throw new IllegalStateException("no exclusive lock to demote");
            }
            if (node.holdCount > 1) {
               throw new IllegalStateException("cannot demote re-entrant exclusive lock");
            }

            long state = getState();

            assert holders == node && node.next == null && node.previous == null;
            assert exclusiveCount(state) == node.holdCount;
               
            long newState = stamp(state) + SHARED_UNIT + STAMP_UNIT;
            holders = null;
            setState(newState);
         }
         
         // got a new shared lock!
         assert holders == null || !holders.isExclusive;
         
         node = new HolderNode(false);
         addHolder(node);
         currentHolder.set(node);
         return 1;
      }
      
      @Override protected boolean tryReleaseShared(long i) {
         HolderNode node = currentHolder.get();
         if (node == null || node.isExclusive) {
            return false;
         }
         if (--node.holdCount == 0) {
            removeHolder(node);
            currentHolder.set(null);
         }
         while (true) {
            long state = getState();
            assert exclusiveCount(state) == 0;
            assert sharedCount(state) > 0;
            if (compareAndSetState(state, state - SHARED_UNIT + STAMP_UNIT)) {
               break;
            }
         }
         return true;
      }

      Thread getExclusiveHolder() {
         HolderNode node;
         while (true) {
            long state = getState();
            if (exclusiveCount(state) == 0) {
               return null;
            }
            node = holders;
            if (node == null) {
               // racing with an acquisition or release -- can just return null
               return null;
            }
            if (getState() == state) {
               break;
            }
         }
         assert node.previous == null && node.next == null;
         return node.thread;
      }

      Collection<Thread> getSharedHolders() {
         return new AbstractCollection<Thread>() {
            
            @SuppressWarnings("synthetic-access") // accesses private API of Sync
            private HolderNode head() {
               // get head of list of shared holders
               HolderNode head;
               while (true) {
                  long state = getState();
                  if (sharedCount(state) == 0) {
                     return null;
                  }
                  if ((head = holders) == null) {
                     // racing with an acquisition or release; let list be empty
                     return null;
                  }
                  // before returning holders, double-check state to make sure we weren't racing
                  // with a thread that was putting an exclusive holder in it
                  if (getState() == state) {
                     return head;
                  }
               }
            }
            
            @Override
            public Iterator<Thread> iterator() {
               HolderNode head = head();
               
               // now we can return a simple iterator that just traverses the linked list
               return new Iterator<Thread>() {
                  HolderNode current = head;
                  
                  @Override
                  public boolean hasNext() {
                     return current != null;
                  }

                  @Override
                  public Thread next() {
                     if (current == null) {
                        throw new NoSuchElementException();
                     }
                     // don't need a lock to read these atomically since current.thread is final
                     Thread thread = current.thread;
                     current = current.next;
                     return thread;
                  }
               };
            }

            @Override
            public int size() {
               int sz = 0;
               for (HolderNode node = head(); node != null; sz++, node = node.next);
               return sz;
            }
         };
      }

      void addHolder(HolderNode node) {
         while (true) {
            HolderNode head = holders;
            if (head == null) {
               if (holdersUpdater.compareAndSet(this, null,  node)) {
                  return;
               }
            } else {
               head.lock();
               try {
                  // have to double-check in case head was changed while we were waiting
                  // to acquire its lock
                  if (holders == head) {
                     node.next = head;
                     head.previous = node;
                     boolean set = holdersUpdater.compareAndSet(this, head, node);
                     assert set;
                     return;
                  }
               } finally {
                  head.unlock();
               }
            }
         }
      }
      
      void removeHolder(HolderNode node) {
         HolderNode predecessor;
         while (true) {
            node.lock();
            predecessor = node.previous;
            if (predecessor == null) {
               break;
            }
            // if there's a predecessor, we have to establish locks in the other order
            // (always left-to-right) to prevent deadlock
            node.unlock();
            predecessor.lock();
            node.lock();
            if (predecessor == node.previous) {
               break;
            }
            // predecessor changed by concurrent list operation! try again
            node.unlock();
            predecessor.unlock();
         }
         
         try {
            if (node.next != null) {
               node.next.lock();
               node.next.previous = predecessor;
            }
            assert node.holdCount == 0;
            if (predecessor != null) {
               predecessor.next = node.next;
            } else {
               // no predecessor means this is the head of the list
               boolean set = holdersUpdater.compareAndSet(this, node, node.next);
               assert set;
            }
         } finally {
            if (node.next != null) {
               node.next.unlock();
            }
            node.unlock();
            if (predecessor != null) {
               predecessor.unlock();
            }
         }
      }
   }
   
   /**
    * The result of acquiring a shared lock on a {@link HierarchicalLock}. This object is used to
    * release the lock or to change it (via promotion or demotion).
    * 
    * <p>Unlike other types of promotion and demotion, promoting a shared lock to its parent or
    * demoting to one of its children will succeed, even if more than one such shared lock is held
    * (from re-entrance). 
    *
    * @see HierarchicalLock#sharedLock()
    * @see HierarchicalLock#sharedLockInterruptibly()
    * @see HierarchicalLock#trySharedLock()
    * @see HierarchicalLock#trySharedLock(long, TimeUnit)
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public class SharedLock implements AcquiredLock {
      private Thread owner;
      
      SharedLock() {
         this.owner = Thread.currentThread();
      }
      
      private void checkLock() {
         if (owner != Thread.currentThread()) {
            throw new IllegalStateException("current thread does not hold this lock");
         }
      }
      
      @Override public void unlock() {
         checkLock();
         boolean released = sync.releaseShared(1);
         assert released;
         owner = null;
      }
      
      /**
       * Promotes this shared lock into an exclusive lock. This basically releases the held lock and
       * replaces it with an exclusive lock on the same {@link HierarchicalLock}. This method will
       * block until the exclusive lock becomes available.
       *
       * @return the newly acquired exclusive lock
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the shared lock has been acquired by this thread more than once (in a
       *    re-entrant manner)
       * @throws DeadlockException if waiting for the exclusive lock would result in a deadlock
       *    (e.g. this thread holds a lock for which another thread is waiting, and that thread, or
       *    some other thread that directly or indirectly is waiting on it, is holding the exclusive
       *    lock)
       */
      public ExclusiveLock promoteToExclusive() {
         if (ForkJoinTask.inForkJoinPool()) {
            return managedBlockUninterruptiblyFor(this::doPromoteToExclusive);
         } else {
            return doPromoteToExclusive();
         }
      }
      
      ExclusiveLock doPromoteToExclusive() {
         return doPromoteToExclusive(null);
      }
      
      ExclusiveLock doPromoteToExclusive(SharedLock parentLock) {
         checkLock();
         if (sync.tryAcquire(-1)) {
            owner = null;
            return newExclusiveLock(parentLock);
         } else {
            beginBlockingForExclusive();
            try {
               sync.acquire(-1);
               owner = null;
               return newExclusiveLock(parentLock);
            } finally {
               endBlockingForExclusive();
            }
         }
      }
      
      /**
       * Promotes this shared lock into an exclusive lock, waiting interruptibly until the exclusive
       * lock becomes available. This basically releases the held lock and replaces it with an
       * exclusive lock on the same {@link HierarchicalLock}.
       *
       * @return the newly acquired exclusive lock
       * @throws InterruptedException if this thread is interrupted while waiting for the exclusive
       *    lock to become available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the shared lock has been acquired by this thread more than once (in a
       *    re-entrant manner)
       * @throws DeadlockException if waiting for the exclusive lock would result in a deadlock
       *    (e.g. this thread holds a lock for which another thread is waiting, and that thread, or
       *    some other thread that directly or indirectly is waiting on it, is holding the exclusive
       *    lock)
       */
      public ExclusiveLock promoteToExclusiveInterruptibly() throws InterruptedException {
         if (ForkJoinTask.inForkJoinPool()) {
            return managedBlockFor(this::doPromoteToExclusiveInterruptibly);
         } else {
            return doPromoteToExclusiveInterruptibly();
         }
      }
      
      ExclusiveLock doPromoteToExclusiveInterruptibly() throws InterruptedException {
         return doPromoteToExclusiveInterruptibly(null);
      }
      
      ExclusiveLock doPromoteToExclusiveInterruptibly(SharedLock parentLock)
            throws InterruptedException {
         checkLock();
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         if (sync.tryAcquire(-1)) {
            owner = null;
            return newExclusiveLock(parentLock);
         } else {
            beginBlockingForExclusive();
            try {
               sync.acquireInterruptibly(-1);
               owner = null;
               return newExclusiveLock(parentLock);
            } finally {
               endBlockingForExclusive();
            }
         }
      }

      /**
       * Promotes this shared lock into an exclusive lock, returning immediately if the exclusive
       * lock is unavailable. This basically releases the held lock and replaces it with an
       * exclusive lock on the same {@link HierarchicalLock}.
       *
       * @return the newly acquired exclusive lock or {@code null} if it is not available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the shared lock has been acquired by this thread more than once (in a
       *    re-entrant manner)
       * @throws DeadlockException if waiting for the exclusive lock would result in a deadlock
       *    (e.g. this thread holds a lock for which another thread is waiting, and that thread, or
       *    some other thread that directly or indirectly is waiting on it, is holding the exclusive
       *    lock)
       */
      public ExclusiveLock tryPromoteToExclusive() {
         return doTryPromoteToExclusive(null);
      }

      ExclusiveLock doTryPromoteToExclusive(SharedLock parentLock) {
         checkLock();
         if (sync.tryAcquire(-1)) {
            owner = null;
            return newExclusiveLock(parentLock);
         } else {
            return null;
         }
      }
      
      /**
       * Promotes this shared lock into an exclusive lock, waiting up to the given amount of time
       * before giving up. This basically releases the held lock and replaces it with an exclusive
       * lock on the same {@link HierarchicalLock}.
       *
       * @param timeLimit the limit of time to wait for the lock
       * @param unit the unit of the time limit
       * @return the newly acquired exclusive lock or {@code null} if the time limit lapsed before
       *    the lock became available
       * @throws InterruptedException if this thread is interrupted while waiting for the exclusive
       *    lock to become available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the shared lock has been acquired by this thread more than once (in a
       *    re-entrant manner)
       * @throws DeadlockException if waiting for the exclusive lock would result in a deadlock
       *    (e.g. this thread holds a lock for which another thread is waiting, and that thread, or
       *    some other thread that directly or indirectly is waiting on it, is holding the exclusive
       *    lock)
       */
      public ExclusiveLock tryPromoteToExclusive(long timeLimit, TimeUnit unit)
            throws InterruptedException {
         if (ForkJoinTask.inForkJoinPool()) {
            return managedBlockFor(() -> doTryPromoteToExclusive(timeLimit, unit));
         } else {
            return doTryPromoteToExclusive(timeLimit, unit);
         }
      }
      
      ExclusiveLock doTryPromoteToExclusive(long timeLimit, TimeUnit unit)
            throws InterruptedException {
         return doTryPromoteToExclusive(timeLimit, unit, null);
      }

      ExclusiveLock doTryPromoteToExclusive(long timeLimit, TimeUnit unit, SharedLock parentLock)
            throws InterruptedException {
         checkLock();
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         if (sync.tryAcquire(-1)) {
            owner = null;
            return newExclusiveLock(parentLock);
         } else {
            beginBlockingForExclusive();
            try {
               if (sync.tryAcquireNanos(-1, unit.toNanos(timeLimit))) {
                  owner = null;
                  return newExclusiveLock(parentLock);
               } else {
                  return null;
               }
            } finally {
               endBlockingForExclusive();
            }
         }
      }
      
      /**
       * Demotes this lock to a shared lock on one of its children. On success, this held lock
       * is released and replaced with a new {@link SharedLock} that represents the lock on the
       * child. This method will block until the child lock is available.
       *
       * @param child {@inheritDoc}
       * @return {@inheritDoc}
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the given lock is not actually a child of the
       *    {@link HierarchicalLock} which produced this {@link SharedLock}
       * @throws DeadlockException {@inheritDoc}
       */
      @Override public SharedLock demoteToChild(HierarchicalLock child) {
         if (ForkJoinTask.inForkJoinPool()) {
            return managedBlockUninterruptiblyFor(() -> doDemoteToChild(child));
         } else {
            return doDemoteToChild(child);
         }
      }
      
      SharedLock doDemoteToChild(HierarchicalLock child) {
         checkLock();
         checkRelationship(child);
         SharedLock childLock = child.sharedLock();
         unlock();
         return childLock;
      }
      
      /**
       * Demotes this lock to a shared lock on one of its children, waiting interruptibly until it
       * is available. On success, this held lock is released and replaced with a new
       * {@link SharedLock} that represents the lock on the child.
       *
       * @throws DeadlockException {@inheritDoc}
       * @param child the child of this lock to which this lock is demoted
       * @return the newly acquired lock on the given child
       * @throws InterruptedException if this thread is interrupted while waiting for the child lock
       *    to become available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the given lock is not actually a child of the
       *    {@link HierarchicalLock} which produced this {@link SharedLock}
       * @throws DeadlockException if waiting for the child lock would result in a deadlock (e.g.
       *    this thread holds a lock for which another thread is waiting, and that thread, or some
       *    other thread that directly or indirectly is waiting on it, is holding the child lock)
       */
      public SharedLock demoteToChildInterruptibly(HierarchicalLock child)
            throws InterruptedException {
         if (ForkJoinTask.inForkJoinPool()) {
            return managedBlockFor(() -> doDemoteToChildInterruptibly(child));
         } else {
            return doDemoteToChildInterruptibly(child);
         }
      }
      
      SharedLock doDemoteToChildInterruptibly(HierarchicalLock child) throws InterruptedException {
         checkLock();
         checkRelationship(child);
         SharedLock childLock = child.sharedLockInterruptibly();
         unlock();
         return childLock;
      }
      
      /**
       * Tries to demote this lock to a shared lock on one of its children, returning immediately if
       * the child lock is unavailable. On success, this held lock is released and replaced with a
       * new {@link SharedLock} that represents the lock on the child.
       *
       * @throws DeadlockException {@inheritDoc}
       * @param child the child of this lock to which this lock is demoted
       * @return the newly acquired lock on the given child or {@code null} if it was not available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the given lock is not actually a child of the
       *    {@link HierarchicalLock} which produced this {@link SharedLock}
       * @throws DeadlockException if waiting for the child lock would result in a deadlock (e.g.
       *    this thread holds a lock for which another thread is waiting, and that thread, or some
       *    other thread that directly or indirectly is waiting on it, is holding the child lock)
       */
      public SharedLock tryDemoteToChild(HierarchicalLock child) {
         checkLock();
         checkRelationship(child);
         SharedLock childLock = child.trySharedLock();
         if (childLock != null) {
            unlock();
         }
         return childLock;
      }
      
      /**
       * Tries to demote this lock to a shared lock on one of its children, waiting for the
       * specified amount of time before giving up. On success, this held lock is released and
       * replaced with a new {@link SharedLock} that represents the lock on the child.
       *
       * @param child the child of this lock to which this lock is demoted
       * @param timeLimit the limit of time to wait for the lock
       * @param unit the unit of the time limit
       * @return the newly acquired lock on the given child or {@code null} if the time limit lapsed
       *    before the lock became available
       * @throws InterruptedException if this thread is interrupted while waiting for the child lock
       *    to become available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the given lock is not actually a child of the
       *    {@link HierarchicalLock} which produced this {@link SharedLock}
       * @throws DeadlockException if waiting for the child lock would result in a deadlock (e.g.
       *    this thread holds a lock for which another thread is waiting, and that thread, or some
       *    other thread that directly or indirectly is waiting on it, is holding the child lock)
       */
      public SharedLock tryDemoteToChild(HierarchicalLock child, long timeLimit, TimeUnit unit)
            throws InterruptedException {
         if (ForkJoinTask.inForkJoinPool()) {
            return managedBlockFor(() -> doTryDemoteToChild(child, timeLimit, unit));
         } else {
            return doTryDemoteToChild(child, timeLimit, unit);
         }
      }
      
      SharedLock doTryDemoteToChild(HierarchicalLock child, long timeLimit, TimeUnit unit)
            throws InterruptedException {
         checkLock();
         checkRelationship(child);
         SharedLock childLock = child.trySharedLock(timeLimit, unit);
         if (childLock != null) {
            unlock();
         }
         return childLock;
      }
      
      /**
       * Promotes this lock to a shared lock on its parent. On success, the held lock is released
       * and replaced with a new {@link SharedLock} that represents the lock on the parent. This
       * method does not need to block since all acquired locks also hold implicit shared locks on
       * parent/ancestor locks. 
       *
       * @return {@inheritDoc}
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the lock that produced this {@link SharedLock} is a root lock and
       *    does not have a parent
       * @throws DeadlockException {@inheritDoc}
       */
      @Override public SharedLock promoteToParent() {
         throw new IllegalStateException("this lock has no parent");
      }
   }
   
   /**
    * The result of acquiring an exclusive lock on a {@link HierarchicalLock}. This object is used
    * to release the lock or to change it (via promotion or demotion).
    *
    * @see HierarchicalLock#exclusiveLock()
    * @see HierarchicalLock#exclusiveLockInterruptibly()
    * @see HierarchicalLock#tryExclusiveLock()
    * @see HierarchicalLock#tryExclusiveLock(long, TimeUnit)
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public class ExclusiveLock implements AcquiredLock {
      private Thread owner;
      
      ExclusiveLock() {
         this.owner = Thread.currentThread();
      }
      
      void checkLock() {
         if (owner != Thread.currentThread()) {
            throw new IllegalStateException("current thread does not hold this lock");
         }
      }
      
      @Override public void unlock() {
         checkLock();
         boolean released = sync.release(1);
         assert released;
         owner = null;
      }
      
      /**
       * Demotes this exclusive lock into a shared lock. This basically releases the held lock and
       * replaces it with a shared lock on the same {@link HierarchicalLock}. Due to already holding
       * the exclusive lock, demotion will not need to block.
       *
       * @return the newly acquired shared lock
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; or if the exclusive lock has been acquired more than once (in a re-entrant
       *    manner)
       */
      public SharedLock demoteToShared() {
         return doDemoteToShared(null);
      }
      
      SharedLock doDemoteToShared(SharedLock parentLock) {
         checkLock();
         long acquired = sync.tryAcquireShared(-1);
         assert acquired > 0;
         owner = null;
         return newSharedLock(parentLock);
      }
      
      /**
       * Demotes this lock to an exclusive lock on one of its children. On success, this held lock
       * is released and replaced with a new {@link ExclusiveLock} that represents the lock on the
       * child. Due to exclusively holding this lock (preventing other child locks from being held),
       * demotion will not block.
       *
       * @param child {@inheritDoc}
       * @return {@inheritDoc}
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; if the given lock is not actually a child of the {@link HierarchicalLock}
       *    which produced this {@link ExclusiveLock}; or if this thread holds more than one such
       *    exclusive lock (from re-entrance)
       * @throws DeadlockException {@inheritDoc}
       */
      @Override public ExclusiveLock demoteToChild(HierarchicalLock child) {
         checkLock();
         checkRelationship(child);
         ChildLock childLock = (ChildLock) child;
         boolean acquired = childLock.sync.tryAcquire(1);
         assert acquired;
         boolean release = true;
         try {
            SharedLock parentLock = demoteToShared();
            release = false;
            return childLock.newExclusiveLock(parentLock);
         } finally {
            if (release) {
               boolean released = childLock.sync.tryRelease(1);
               assert released;
            }
         }
      }
      
      /**
       * Promotes this lock to an exclusive lock on its parent. On success, the held lock is
       * released and replaced with a new {@link ExclusiveLock} that represents the lock on the
       * parent. This method will block until the parent lock becomes available.
       *
       * @return {@inheritDoc}
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; if the lock that produced this {@link ExclusiveLock} is a root lock and
       *    does not have a parent; or if the implicit parent lock cannot be promoted to exclusive
       *    because this thread already holds the parent lock more than once (from re-entrance)
       * @throws DeadlockException {@inheritDoc}
       */
      @Override public ExclusiveLock promoteToParent() {
         throw new IllegalStateException("this lock has no parent");
      }
      
      /**
       * Promotes this lock to an exclusive lock on its parent, waiting interruptibly until it
       * becomes available. On success, the held lock is released and replaced with a new
       * {@link ExclusiveLock} that represents the lock on the parent.
       *
       * @return the newly acquired lock on the parent
       * @throws InterruptedException if this thread is interrupted while waiting for the parent
       *    lock to become available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; if the lock that produced this {@link ExclusiveLock} is a root lock and
       *    does not have a parent; or if the implicit parent lock cannot be promoted to exclusive
       *    because this thread already holds the parent lock more than once (from re-entrance)
       * @throws DeadlockException if waiting for the parent lock would result in a deadlock (e.g.
       *    this thread holds a lock for which another thread is waiting, and that thread, or some
       *    other thread that directly or indirectly is waiting on it, is holding the parent lock)
       */
      public ExclusiveLock promoteToParentInterruptibly() throws InterruptedException {
         throw new IllegalStateException("this lock has no parent");
      }
      
      /**
       * Tries to promote this lock to an exclusive lock on its parent, returning immediately if
       * the parent lock is unavailable. On success, the held lock is released and replaced with a
       * new {@link ExclusiveLock} that represents the lock on the parent.
       *
       * @return the newly acquired lock on the parent or {@code null} if the parent lock is not
       *    available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; if the lock that produced this {@link ExclusiveLock} is a root lock and
       *    does not have a parent; or if the implicit parent lock cannot be promoted to exclusive
       *    because this thread already holds the parent lock more than once (from re-entrance)
       * @throws DeadlockException if waiting for the parent lock would result in a deadlock (e.g.
       *    this thread holds a lock for which another thread is waiting, and that thread, or some
       *    other thread that directly or indirectly is waiting on it, is holding the parent lock)
       */
      public ExclusiveLock tryPromoteToParent() {
         throw new IllegalStateException("this lock has no parent");
      }
      
      /**
       * Tries to promote this lock to an exclusive lock on its parent, waiting up to the specified
       * amount of time before giving up. On success, the held lock is released and replaced with a
       * new {@link ExclusiveLock} that represents the lock on the parent.
       *
       * @param timeLimit the limit of time to wait for the lock
       * @param unit the unit of the time limit
       * @return the newly acquired lock on the parent or {@code null} if the time limit lapsed
       *    and the parent lock never became available
       * @throws InterruptedException if this thread is interrupted while waiting for the parent
       *    lock to become available
       * @throws IllegalStateException if this thread does not hold this lock or has already
       *    released it; if the lock that produced this {@link ExclusiveLock} is a root lock and
       *    does not have a parent; or if the implicit parent lock cannot be promoted to exclusive
       *    because this thread already holds the parent lock more than once (from re-entrance)
       * @throws DeadlockException if waiting for the parent lock would result in a deadlock (e.g.
       *    this thread holds a lock for which another thread is waiting, and that thread, or some
       *    other thread that directly or indirectly is waiting on it, is holding the parent lock)
       */
      public ExclusiveLock tryPromoteToParent(long timeLimit, TimeUnit unit)
            throws InterruptedException {
         throw new IllegalStateException("this lock has no parent");
      }
   }

   /**
    * A {@link HierarchicalLock} that is a child component of another such lock. Acquiring any type
    * of lock on this object (be it shared or exclusive) requires first acquiring a shared lock on
    * the parent object.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class ChildLock extends HierarchicalLock {
      private static final long serialVersionUID = -1641628892267912504L;

      ChildLock(boolean fair) {
         super(fair);
      }
      
      @Override public HierarchicalLock getParent() {
         return HierarchicalLock.this;
      }
      
      @Override SharedLock newSharedLock(SharedLock parentLock) {
         assert parentLock != null;
         return new ChildSharedLock(parentLock);
      }
      
      @Override ExclusiveLock newExclusiveLock(SharedLock parentLock) {
         assert parentLock != null;
         return new ChildExclusiveLock(parentLock);
      }
      
      @Override public SharedLock trySharedLock() {
         SharedLock parentLock = getParent().trySharedLock();
         if (parentLock == null) {
            return null;
         }
         boolean release = true;
         try {
            SharedLock ret = doTrySharedLock(parentLock);
            if (ret != null) {
               release = false;
            }
            return ret;
         } finally {
            if (release) {
               parentLock.unlock();
            }
         }
      }
      
      @Override SharedLock doTrySharedLock(long timeLimit, TimeUnit unit)
            throws InterruptedException {
         long startNanos = System.nanoTime();
         SharedLock parentLock = getParent().trySharedLock(timeLimit, unit);
         if (parentLock == null) {
            return null;
         }
         boolean release = true;
         try {
            long nanosLeft = startNanos + unit.toNanos(timeLimit) - System.nanoTime();
            SharedLock ret = doTrySharedLock(nanosLeft, TimeUnit.NANOSECONDS, parentLock);
            if (ret != null) {
               release = false;
            }
            return ret;
         } finally {
            if (release) {
               parentLock.unlock();
            }
         }
      }

      @Override SharedLock doSharedLock() {
         SharedLock parentLock = getParent().sharedLock();
         boolean release = true;
         try {
            SharedLock ret = doSharedLock(parentLock);
            release = false;
            return ret;
         } finally {
            if (release) {
               parentLock.unlock();
            }
         }
      }

      @Override SharedLock doSharedLockInterruptibly() throws InterruptedException {
         SharedLock parentLock = getParent().sharedLockInterruptibly();
         boolean release = true;
         try {
            SharedLock ret = doSharedLockInterruptibly(parentLock);
            release = false;
            return ret;
         } finally {
            if (release) {
               parentLock.unlock();
            }
         }
      }

      @Override public ExclusiveLock tryExclusiveLock() {
         SharedLock parentLock = getParent().trySharedLock();
         if (parentLock == null) {
            return null;
         }
         boolean release = true;
         try {
            ExclusiveLock ret = doTryExclusiveLock(parentLock);
            if (ret != null) {
               release = false;
            }
            return ret;
         } finally {
            if (release) {
               parentLock.unlock();
            }
         }
      }
      
      @Override ExclusiveLock doTryExclusiveLock(long timeLimit, TimeUnit unit)
            throws InterruptedException {
         long startNanos = System.nanoTime();
         SharedLock parentLock = getParent().trySharedLock(timeLimit, unit);
         if (parentLock == null) {
            return null;
         }
         boolean release = true;
         try {
            long nanosLeft = startNanos + unit.toNanos(timeLimit) - System.nanoTime();
            ExclusiveLock ret = doTryExclusiveLock(nanosLeft, TimeUnit.NANOSECONDS, parentLock);
            if (ret != null) {
               release = false;
            }
            return ret;
         } finally {
            if (release) {
               parentLock.unlock();
            }
         }
      }

      @Override ExclusiveLock doExclusiveLock() {
         SharedLock parentLock = getParent().sharedLock();
         boolean release = true;
         try {
            ExclusiveLock ret = doExclusiveLock(parentLock);
            release = false;
            return ret;
         } finally {
            if (release) {
               parentLock.unlock();
            }
         }
      }

      @Override ExclusiveLock doExclusiveLockInterruptibly() throws InterruptedException {
         SharedLock parentLock = getParent().sharedLockInterruptibly();
         boolean release = true;
         try {
            ExclusiveLock ret = doExclusiveLockInterruptibly(parentLock);
            release = false;
            return ret;
         } finally {
            if (release) {
               parentLock.unlock();
            }
         }
      }
      
      /**
       * The result of acquiring a shared lock on a {@link ChildLock}.
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class ChildSharedLock extends SharedLock {
         private final SharedLock parentLock;
         
         ChildSharedLock(SharedLock parentLock) {
            this.parentLock = parentLock;
         }
         
         @Override public void unlock() {
            super.unlock();
            parentLock.unlock();
         }
         
         @Override ExclusiveLock doPromoteToExclusive() {
            return doPromoteToExclusive(parentLock);
         }
         
         @Override ExclusiveLock doPromoteToExclusiveInterruptibly()
               throws InterruptedException {
            return doPromoteToExclusiveInterruptibly(parentLock);
         }
         
         @Override public ExclusiveLock tryPromoteToExclusive() {
            return doTryPromoteToExclusive(parentLock);
         }
         
         @Override ExclusiveLock doTryPromoteToExclusive(long timeLimit, TimeUnit unit)
               throws InterruptedException {
            return doTryPromoteToExclusive(timeLimit, unit, parentLock);
         }
         
         @Override public SharedLock promoteToParent() {
            super.unlock();
            return parentLock;
         }
      }
      
      /**
       * The result of acquiring an exclusive lock on a {@link ChildLock}.
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class ChildExclusiveLock extends ExclusiveLock {
         private final SharedLock parentLock;
         
         ChildExclusiveLock(SharedLock parentLock) {
            this.parentLock = parentLock;
         }
         
         @Override public void unlock() {
            super.unlock();
            parentLock.unlock();
         }
         
         @Override public SharedLock demoteToShared() {
            return doDemoteToShared(parentLock);
         }
         
         @Override public ExclusiveLock promoteToParent() {
            if (ForkJoinTask.inForkJoinPool()) {
               return managedBlockUninterruptiblyFor(this::doPromoteToParent);
            } else {
               return doPromoteToParent();
            }
         }
         
         ExclusiveLock doPromoteToParent() {
            checkLock();
            ExclusiveLock newLock = parentLock.promoteToExclusive();
            super.unlock();
            return newLock;
         }
         
         @Override public ExclusiveLock promoteToParentInterruptibly() throws InterruptedException {
            if (ForkJoinTask.inForkJoinPool()) {
               return managedBlockFor(this::doPromoteToParentInterruptibly);
            } else {
               return doPromoteToParentInterruptibly();
            }
         }
         
         ExclusiveLock doPromoteToParentInterruptibly() throws InterruptedException {
            checkLock();
            ExclusiveLock newLock = parentLock.promoteToExclusiveInterruptibly();
            super.unlock();
            return newLock;
         }
         
         @Override public ExclusiveLock tryPromoteToParent() {
            checkLock();
            ExclusiveLock newLock = parentLock.tryPromoteToExclusive();
            if (newLock != null) {
               super.unlock();
            }
            return newLock;
         }
         
         @Override public ExclusiveLock tryPromoteToParent(long timeLimit, TimeUnit unit)
               throws InterruptedException {
            if (ForkJoinTask.inForkJoinPool()) {
               return managedBlockFor(() -> doTryPromoteToParent(timeLimit, unit));
            } else {
               return doTryPromoteToParent(timeLimit, unit);
            }
         }
         
         ExclusiveLock doTryPromoteToParent(long timeLimit, TimeUnit unit)
               throws InterruptedException {
            checkLock();
            ExclusiveLock newLock = parentLock.tryPromoteToExclusive(timeLimit, unit);
            if (newLock != null) {
               super.unlock();
            }
            return newLock;
         }
      }
   }
}
