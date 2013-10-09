package com.apriori.concurrent;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
 * lock (or vice versa) without using the lock promotion methods (or demotion methods) will fail
 * with a deadlock exception. 
 *
 * @see SharedLock
 * @see ExclusiveLock
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public class HierarchicalLock {

   static ConcurrentHashMap<Thread, HierarchicalLock> blockedForExclusive =
         new ConcurrentHashMap<Thread, HierarchicalLock>(16, 0.75F, 100);
   static ConcurrentHashMap<Thread, HierarchicalLock> blockedForShared =
         new ConcurrentHashMap<Thread, HierarchicalLock>(16, 0.75F, 100);
   
   public static HierarchicalLock create() {
      return new HierarchicalLock();
   }
   
   final Sync sync = new Sync();
   private final Set<HierarchicalLock> children =
         Collections.newSetFromMap(new ConcurrentHashMap<HierarchicalLock, Boolean>());
   
   
   HierarchicalLock() {
   }
   
   void beginBlockingForExclusive() {
      blockedForExclusive.put(Thread.currentThread(), this);
   }
   
   boolean endBlockingForExclusive() {
      return blockedForExclusive.remove(Thread.currentThread()) != null;
   }

   void beginBlockingForShared() {
      blockedForShared.put(Thread.currentThread(), this);
   }
   
   boolean endBlockingForShared() {
      return blockedForShared.remove(Thread.currentThread()) != null;
   }

   public HierarchicalLock newChild() {
      HierarchicalLock child = new ChildLock();
      children.add(child);
      return child;
   }
   
   public Set<HierarchicalLock> getChildren() {
      return Collections.unmodifiableSet(new HashSet<HierarchicalLock>(children));
   }
   
   public HierarchicalLock getParent() {
      return null;
   }
   
   public Thread getExclusiveHolder() {
      return sync.getExclusiveHolder();
   }

   public Collection<Thread> getSharedHolders() {
      return sync.getSharedHolders();
   }

   public Thread getFirstQueuedThread() {
      return sync.getFirstQueuedThread();
   }
   
   public Collection<Thread> getQueuedThreads() {
      return sync.getQueuedThreads();
   }

   SharedLock newSharedLock(SharedLock parentLock) {
      assert parentLock == null;
      return new SharedLock();
   }
   
   public SharedLock trySharedLock() {
      return doTrySharedLock(null);
   }
   
   SharedLock doTrySharedLock(SharedLock parentLock) {
      if (sync.tryAcquireShared(1) >= 0) {
         return newSharedLock(parentLock);
      } else {
         return null;
      }
   }

   public SharedLock trySharedLock(long timeLimit, TimeUnit unit) throws InterruptedException {
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
   
   public SharedLock sharedLock() {
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
   
   public SharedLock sharedLockInterruptibly() throws InterruptedException {
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
   
   public <T> T callWithSharedLock(Callable<T> callable) throws Exception {
      SharedLock lock = sharedLock();
      try {
         return callable.call();
      } finally {
         lock.unlock();
      }
   }
   
   public void runWithSharedLock(Runnable runnable) {
      SharedLock lock = sharedLock();
      try {
         runnable.run();
      } finally {
         lock.unlock();
      }
   }

   ExclusiveLock newExclusiveLock(SharedLock parentLock) {
      assert parentLock == null;
      return new ExclusiveLock();
   }
   
   public ExclusiveLock tryExclusiveLock() {
      return doTryExclusiveLock(null);
   }
   
   ExclusiveLock doTryExclusiveLock(SharedLock parentLock) {
      if (sync.tryAcquire(1)) {
         return newExclusiveLock(parentLock);
      } else {
         return null;
      }
   }
   
   public ExclusiveLock tryExclusiveLock(long timeLimit, TimeUnit unit)
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
   
   public ExclusiveLock exclusiveLock() {
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
   
   public ExclusiveLock exclusiveLockInterruptibly() throws InterruptedException {
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
   
   public <T> T callWithExclusiveLock(Callable<T> callable) throws Exception {
      ExclusiveLock lock = exclusiveLock();
      try {
         return callable.call();
      } finally {
         lock.unlock();
      }
   }
   
   public void runWithExclusiveLock(Runnable runnable) {
      ExclusiveLock lock = exclusiveLock();
      try {
         runnable.run();
      } finally {
         lock.unlock();
      }
   }
   
   void checkRelationship(HierarchicalLock child) {
      if (child.getParent() != this) {
         throw new IllegalArgumentException("specified object is not a child of this lock");
      }
   }
   
   /**
    * A node in a linked list of threads that hold a {@link HierarchicalLock}. This class extends
    * {@link AbstractQueuedSynchronizer} so it can support locking operations so list manipulation
    * can be done in a thread-safe way. Its locking and unlocking mechanisms are not re-entrant. 
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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
      
      private void writeObject(@SuppressWarnings("unused") ObjectOutputStream out)
            throws IOException {
         throw new NotSerializableException();
      }
      
      private void readObject(@SuppressWarnings("unused") ObjectInputStream in)
            throws IOException {
         throw new NotSerializableException();
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
      
      void toCollection(Collection<Thread> collection) {
         HolderNode current = this;
         current.lock();
         try {
            while (current != null) {
               collection.add(current.thread);
               HolderNode nextNode = current.next;
               if (nextNode != null) {
                  nextNode.lock();
               }
               current.unlock();
               current = nextNode;
            }
         } finally {
            if (current != null) {
               current.unlock();
            }
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
      
      private static final long EXCLUSIVE_MASK = 0x00007fff80000000L;
      private static final int EXCLUSIVE_SHIFT = 31;
      private static final long EXCLUSIVE_UNIT = 0x0000000080000000L;
      private static final long MAX_EXCLUSIVE_COUNT = 0x000000000000ffffL;
      
      private static final long STAMP_MASK = 0xffff800000000000L;
      private static final long STAMP_UNIT = 0x0000800000000000L;
      
      private static long exclusiveCount(long state) {
         return (state & EXCLUSIVE_MASK) >>> EXCLUSIVE_SHIFT;
      }
      
      private static long sharedCount(long state) {
         return state & SHARED_MASK;
      }
      
      private static long stamp(long state) {
         return state & STAMP_MASK;
      }

      Sync() {
      }
    
      private final transient AtomicReference<HolderNode> holders =
            new AtomicReference<HolderNode>();
      private final transient ThreadLocal<HolderNode> currentHolder = new ThreadLocal<HolderNode>();
      
      private void checkForDeadlock(long i, boolean isWaitingForExclusive) {
         if (i > 1 || i < -1) {
            // calling code was just trying for the lock, not blocking on it, so there's
            // no deadlock
            return;
         }
         
         checkForDeadlock(isWaitingForExclusive, new HashSet<Thread>());
      }

      private void checkForDeadlock(boolean isWaitingForExclusive, Set<Thread> blocked) {
         Collection<Thread> threads = Collections.singleton(getExclusiveHolder());
         if (isWaitingForExclusive && threads.isEmpty()) {
            threads = getSharedHolders();
         }
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
               assert holders.get() == node && node.next == null && node.previous == null;
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
            while (true) {
               long state = getState();
               if (sharedCount(state) > 0 || exclusiveCount(state) > 0) {
                  checkForDeadlock(i, true);
                  return false;
               }
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
               
               long newState = stamp(state) + EXCLUSIVE_UNIT + STAMP_UNIT;
               if (compareAndSetState(state, newState)) {
                  break;
               }
            }
         }
         
         // got the exclusive lock!
         assert holders.get() == null && node == null;
         
         node = new HolderNode(true);
         holders.set(node);
         currentHolder.set(node);
         return true;
      }
      
      @Override protected boolean tryRelease(long i) {
         HolderNode node = currentHolder.get();
         if (node == null || !node.isExclusive) {
            return false;
         }
         long state = getState();
         assert holders.get() == node && node.next == null && node.previous == null;
         assert sharedCount(state) == 0;
         assert exclusiveCount(state) == node.holdCount;
         if (node.holdCount == 1) {
            holders.set(null);
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
            while (true) {
               long state = getState();
               if (exclusiveCount(state) > 0) {
                  checkForDeadlock(i, false);
                  return -1;
               }
               if (sharedCount(state) == MAX_SHARED_COUNT) {
                  throw new IllegalStateException("already locked maximum number of times");
               }
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

            assert holders.get() == node && node.next == null && node.previous == null;
            assert exclusiveCount(state) == node.holdCount;
               
            long newState = stamp(state) + SHARED_UNIT + STAMP_UNIT;
            holders.set(null);
            setState(newState);
         }
         
         // got a new shared lock!
         assert node == null;
         
         node = new HolderNode(true);
         addHolder(node);
         currentHolder.set(node);
         return 1;
      }
      
      @Override protected boolean tryReleaseShared(long i) {
         HolderNode node = currentHolder.get();
         if (node == null || node.isExclusive) {
            return false;
         }
         if (node.holdCount == 1) {
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
            node = holders.get();
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
         HolderNode node;
         long count;
         while (true) {
            long state = getState();
            count = sharedCount(state);
            if (count == 0) {
               return Collections.emptySet();
            }
            node = holders.get();
            if (node == null) {
               // racing with an acquisition or release -- can just return empty
               return Collections.emptySet();
            }
            if (getState() == state) {
               break;
            }
         }
         int holderCount = ((int) count) & Integer.MAX_VALUE;
         Collection<Thread> collection = new HashSet<Thread>(holderCount);
         node.toCollection(collection);
         return collection;
      }

      void addHolder(HolderNode node) {
         while (true) {
            HolderNode head = holders.get();
            if (head == null) {
               if (holders.compareAndSet(null,  node)) {
                  return;
               }
            } else {
               head.lock();
               try {
                  // have to double-check in case head was changed while we were waiting
                  // to acquire its lock
                  if (holders.get() == head) {
                     node.next = head;
                     head.previous = node;
                     boolean set = holders.compareAndSet(head, node);
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
               boolean set = holders.compareAndSet(node, node.next);
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
    * The result of acquiring a shared lock on a {@link HierarchicalLock}.
    *
    * @see HierarchicalLock#sharedLock()
    * @see HierarchicalLock#sharedLockInteruptibly()
    * @see HierarchicalLock#trySharedLock()
    * @see HierarchicalLock#trySharedLock(long, TimeUnit)
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public class SharedLock {
      private Thread owner;
      
      SharedLock() {
         this.owner = Thread.currentThread();
      }
      
      private void checkLock() {
         if (owner != Thread.currentThread()) {
            throw new IllegalStateException("current thread does not hold this lock");
         }
      }
      
      public void unlock() {
         checkLock();
         boolean released = sync.releaseShared(1);
         assert released;
         owner = null;
      }
      
      public ExclusiveLock promoteToExclusive() {
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
      
      public ExclusiveLock promoteToExclusiveInterruptibly() throws InterruptedException {
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
      
      public ExclusiveLock tryPromoteToExclusive(long timeLimit, TimeUnit unit)
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
      
      public SharedLock demoteToChild(HierarchicalLock child) {
         checkLock();
         checkRelationship(child);
         SharedLock childLock = child.sharedLock();
         unlock();
         return childLock;
      }
      
      public SharedLock demoteToChildInterruptibly(HierarchicalLock child)
            throws InterruptedException {
         checkLock();
         checkRelationship(child);
         SharedLock childLock = child.sharedLockInterruptibly();
         unlock();
         return childLock;
      }
      
      public SharedLock tryDemoteToChild(HierarchicalLock child) {
         checkLock();
         checkRelationship(child);
         SharedLock childLock = child.trySharedLock();
         if (childLock != null) {
            unlock();
         }
         return childLock;
      }
      
      public SharedLock tryDemoteToChild(HierarchicalLock child, long timeLimit, TimeUnit unit)
            throws InterruptedException {
         checkLock();
         checkRelationship(child);
         SharedLock childLock = child.trySharedLock(timeLimit, unit);
         if (childLock != null) {
            unlock();
         }
         return childLock;
      }
      
      public SharedLock promoteToParent() {
         throw new IllegalStateException("this lock has no parent");
      }
   }
   
   /**
    * The result of acquiring an exclusive lock on a {@link HierarchicalLock}.
    *
    * @see HierarchicalLock#exclusiveLock()
    * @see HierarchicalLock#exclusiveLockInteruptibly()
    * @see HierarchicalLock#tryExclusiveLock()
    * @see HierarchicalLock#tryExclusiveLock(long, TimeUnit)
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public class ExclusiveLock {
      private Thread owner;
      
      ExclusiveLock() {
         this.owner = Thread.currentThread();
      }
      
      private void checkLock() {
         if (owner != Thread.currentThread()) {
            throw new IllegalStateException("current thread does not hold this lock");
         }
      }
      
      public void unlock() {
         checkLock();
         boolean released = sync.release(1);
         assert released;
         owner = null;
      }
      
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
      
      public ExclusiveLock demoteToChild(HierarchicalLock child) {
         checkLock();
         checkRelationship(child);
         ChildLock childLock = (ChildLock) child;
         boolean acquired = childLock.sync.tryAcquire(1);
         assert acquired;
         SharedLock parentLock = demoteToShared();
         return childLock.newExclusiveLock(parentLock);
      }
      
      public ExclusiveLock promoteToParent() {
         throw new IllegalStateException("this lock has no parent");
      }
      
      public ExclusiveLock promoteToParentInterruptibly() throws InterruptedException {
         throw new IllegalStateException("this lock has no parent");
      }
      
      public ExclusiveLock tryPromoteToParent() {
         throw new IllegalStateException("this lock has no parent");
      }
      
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

      ChildLock() {
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
      
      @Override public SharedLock trySharedLock(long timeLimit, TimeUnit unit)
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

      @Override public SharedLock sharedLock() {
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

      @Override public SharedLock sharedLockInterruptibly() throws InterruptedException {
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
      
      @Override public ExclusiveLock tryExclusiveLock(long timeLimit, TimeUnit unit)
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

      @Override public ExclusiveLock exclusiveLock() {
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

      @Override public ExclusiveLock exclusiveLockInterruptibly() throws InterruptedException {
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
         
         // TODO: Override all locks to handle parentLock and implement parent-related methods
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

         // TODO: Override all locks to handle parentLock and implement parent-related methods
      }
   }
}
