package com.apriori.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.apriori.concurrent.HierarchicalLock.ExclusiveLock;
import com.apriori.concurrent.HierarchicalLock.SharedLock;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** 
 * Test cases for {@link HierarchicalLock}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class HierarchicalLockTest {

   private static interface CheckedRunnable {
      void run() throws Exception;
   }
   
   HierarchicalLock lock;
   
   @Before public void setUp() {
      lock = createLock(false);
   }

   /**
    * Constructs the lock under test.
    */
   protected HierarchicalLock createLock(boolean fair) {
      return HierarchicalLock.create(fair);
   }
   
   /**
    * Tests that various methods behave as expected when there is no lock held.
    */
   @Test public void lockNotHeld() {
      assertNull(lock.getExclusiveHolder());
      assertNull(lock.getFirstQueuedThread());
      assertTrue(lock.getQueuedThreads().isEmpty());
      assertTrue(lock.getSharedHolders().isEmpty());
   }
   
   private void lockHeldInSharedMode(Thread... holders) {
      lockHeldInSharedMode(new HashSet<Thread>(Arrays.asList(holders)));
   }
   
   private void lockHeldInSharedMode(Set<Thread> holders) {
      assertNull(lock.getExclusiveHolder());
      assertNull(lock.getFirstQueuedThread());
      assertTrue(lock.getQueuedThreads().isEmpty());
      assertEquals(holders, new HashSet<Thread>(lock.getSharedHolders()));
   }
   
   private Set<Thread> lockHeldInSharedModeWithQueuedThreads(Thread... holders) {
      return lockHeldInSharedModeWithQueuedThreads(new HashSet<Thread>(Arrays.asList(holders)));
   }
   
   private Set<Thread> lockHeldInSharedModeWithQueuedThreads(Set<Thread> holders) {
      assertNull(lock.getExclusiveHolder());
      assertNotNull(lock.getFirstQueuedThread());
      Set<Thread> queued = new HashSet<Thread>(lock.getQueuedThreads());
      assertFalse(lock.getQueuedThreads().isEmpty());
      assertEquals(holders, new HashSet<Thread>(lock.getSharedHolders()));
      return queued;
   }
   
   private void expectException(Class<? extends Exception> exceptionType,
         CheckedRunnable runnable) {
      try {
         runnable.run();
         fail("Expecting but did not catch " + exceptionType.getSimpleName());
      } catch (Exception e) {
         assertTrue(exceptionType.isInstance(e));
      }
   }
   
   private void sharedLockNotValid(final SharedLock share) {
      expectException(IllegalStateException.class, new CheckedRunnable() {
         @Override public void run() {
            share.unlock();
         }
      });
      expectException(IllegalStateException.class, new CheckedRunnable() {
         @Override public void run() {
            share.promoteToExclusive();
         }
      });
      expectException(IllegalStateException.class, new CheckedRunnable() {
         @Override public void run() throws Exception {
            share.promoteToExclusiveInterruptibly();
         }
      });
      expectException(IllegalStateException.class, new CheckedRunnable() {
         @Override public void run() throws Exception {
            share.tryPromoteToExclusive();
         }
      });
      expectException(IllegalStateException.class, new CheckedRunnable() {
         @Override public void run() throws Exception {
            share.tryPromoteToExclusive(1, TimeUnit.NANOSECONDS);
         }
      });
      expectException(IllegalStateException.class, new CheckedRunnable() {
         @Override public void run() throws Exception {
            share.promoteToParent();
         }
      });
   }
   
   private void lockHeldInExclusiveMode(Thread holder) {
      assertSame(holder, lock.getExclusiveHolder());
      assertNull(lock.getFirstQueuedThread());
      assertTrue(lock.getQueuedThreads().isEmpty());
      assertTrue(lock.getSharedHolders().isEmpty());
   }
   
   private Set<Thread> lockHeldInExclusiveModeWithQueuedThreads(Thread holder) {
      assertSame(holder, lock.getExclusiveHolder());
      assertNotNull(lock.getFirstQueuedThread());
      Set<Thread> queued = new HashSet<Thread>(lock.getQueuedThreads());
      assertFalse(lock.getQueuedThreads().isEmpty());
      assertTrue(lock.getSharedHolders().isEmpty());
      return queued;
   }
   
   private void exclusiveLockNotValid(final ExclusiveLock exclusive) {
      expectException(IllegalStateException.class, new CheckedRunnable() {
         @Override public void run() {
            exclusive.unlock();
         }
      });
      expectException(IllegalStateException.class, new CheckedRunnable() {
         @Override public void run() {
            exclusive.demoteToShared();
         }
      });
   }

   private void cannotExclusiveLock(boolean expectDeadlock) throws Exception {
      assertNull(lock.tryExclusiveLock());
      if (expectDeadlock) {
         try {
            lock.exclusiveLock();
            fail("Expecting but did not catch a DeadlockException");
         } catch (DeadlockException expected) {
         }
         try {
            lock.exclusiveLockInterruptibly();
            fail("Expecting but did not catch a DeadlockException");
         } catch (DeadlockException expected) {
         }
         try {
            lock.tryExclusiveLock(1,  TimeUnit.NANOSECONDS);
            fail("Expecting but did not catch a DeadlockException");
         } catch (DeadlockException expected) {
         }
      }
   }
   
   private void cannotSharedLock(boolean expectDeadlock) throws Exception {
      assertNull(lock.trySharedLock());
      if (expectDeadlock) {
         try {
            lock.sharedLock();
            fail("Expecting but did not catch a DeadlockException");
         } catch (DeadlockException expected) {
         }
         try {
            lock.sharedLockInterruptibly();
            fail("Expecting but did not catch a DeadlockException");
         } catch (DeadlockException expected) {
         }
         try {
            lock.trySharedLock(1,  TimeUnit.NANOSECONDS);
            fail("Expecting but did not catch a DeadlockException");
         } catch (DeadlockException expected) {
         }
      }
   }
   
   /**
    * Tests the simplest case of uncontested acquisition and release of a shared lock.
    */
   @Test public void sharedLock_oneThread() throws Exception {
      SharedLock share = lock.sharedLock();
      lockHeldInSharedMode(Thread.currentThread());
      cannotExclusiveLock(true);
      lockHeldInSharedMode(Thread.currentThread());
      share.unlock();
      sharedLockNotValid(share);
      lockNotHeld();
   }

   /**
    * Tests acquiring a shared lock multiple times (re-entrant) from the same thread.
    */
   @Test public void sharedLock_oneThread_reentrant() throws Exception {
      SharedLock share1 = lock.sharedLock();
      SharedLock share2 = lock.sharedLock();
      SharedLock share3 = lock.sharedLock();
      SharedLock share4 = lock.sharedLock();
      lockHeldInSharedMode(Thread.currentThread());
      cannotExclusiveLock(true);
      lockHeldInSharedMode(Thread.currentThread());
      share1.unlock();
      sharedLockNotValid(share1);
      share2.unlock();
      sharedLockNotValid(share2);
      lockHeldInSharedMode(Thread.currentThread());
      share3.unlock();
      sharedLockNotValid(share3);
      share4.unlock();
      sharedLockNotValid(share4);
      lockNotHeld();
   }

   /**
    * Tests acquiring and releasing shared locks from multiple threads. One of the threads will
    * acquire and release an exclusive lock, just to ensure that exclusive locks behave properly
    * when other threads already hold it in exclusive mode.
    */
   @Test public void sharedLock_multipleThreads() throws Exception {
      final CountDownLatch allLocked = new CountDownLatch(5);
      final CountDownLatch doUnlock1 = new CountDownLatch(1);
      final CountDownLatch unlocked1 = new CountDownLatch(5);
      final CountDownLatch doUnlock2 = new CountDownLatch(1);
      final CountDownLatch unlocked2 = new CountDownLatch(3);
      final CountDownLatch doUnlock3 = new CountDownLatch(1);
      final CountDownLatch exclusiveLocked = new CountDownLatch(1);
      final CountDownLatch done = new CountDownLatch(1);
      final Set<Thread> holdingThreads = Collections.synchronizedSet(new HashSet<Thread>());
      final AtomicReference<Thread> exclusiveThread = new AtomicReference<Thread>();
      
      ExecutorService executor = Executors.newFixedThreadPool(6);
      for (int i = 0; i < 5; i++) {
         final boolean group1 = i < 3;
         executor.submit(new Callable<Void>() {
            @Override public Void call() throws Exception{
               // each thread grabs two locks and notifies
               SharedLock share1 = lock.sharedLock();
               SharedLock share2 = lock.sharedLock();
               holdingThreads.add(Thread.currentThread());
               allLocked.countDown();
               // when its time to unlock, all unlock a single lock
               doUnlock1.await();
               unlocked1.countDown();
               share1.unlock();
               // when its time to unlock, again group1 unlocks first, groups second
               (group1 ? doUnlock2 : doUnlock3).await();
               share2.unlock();
               holdingThreads.remove(Thread.currentThread());
               unlocked2.countDown();
               return null;
            }
         });
      }
      // one thread waiting on exclusive lock
      executor.submit(new Callable<Void>() {
         @Override public Void call() throws Exception {
            // wait until shared locks acquired
            allLocked.await();
            // this should block until locks above are released
            ExclusiveLock exclusive = lock.exclusiveLock();
            exclusiveThread.set(Thread.currentThread());
            exclusiveLocked.countDown();
            done.await();
            exclusive.unlock();
            return null;
         }
      });
      
      SharedLock share1 = lock.sharedLock();
      SharedLock share2 = lock.sharedLock();
      holdingThreads.add(Thread.currentThread());
      assertTrue(allLocked.await(1, TimeUnit.SECONDS));
      Thread.sleep(100); // give exclusive lock thread a chance to get queued
      
      // 5 threads in pool plus current one
      assertEquals(6, holdingThreads.size());
      Set<Thread> queuedThreads = lockHeldInSharedModeWithQueuedThreads(holdingThreads);
      assertEquals(1, queuedThreads.size());
      Thread expectedExclusive = queuedThreads.iterator().next();
      assertNull(exclusiveThread.get());
      cannotExclusiveLock(true);
      
      share1.unlock();
      sharedLockNotValid(share1);
      share2.unlock();
      sharedLockNotValid(share2);
      holdingThreads.remove(Thread.currentThread());
      doUnlock1.countDown();
      unlocked1.await(1, TimeUnit.SECONDS);

      // 5 threads in pool still hold locks
      assertEquals(5, holdingThreads.size());
      queuedThreads = lockHeldInSharedModeWithQueuedThreads(holdingThreads);
      assertEquals(1, queuedThreads.size());
      assertSame(expectedExclusive, queuedThreads.iterator().next());
      assertNull(exclusiveThread.get());
      cannotExclusiveLock(false);

      doUnlock2.countDown();
      unlocked2.await(1, TimeUnit.SECONDS);

      // 2 left still holding locks
      assertEquals(2, holdingThreads.size());
      queuedThreads = lockHeldInSharedModeWithQueuedThreads(holdingThreads);
      assertEquals(1, queuedThreads.size());
      assertSame(expectedExclusive, queuedThreads.iterator().next());
      assertNull(exclusiveThread.get());
      cannotExclusiveLock(false);

      doUnlock3.countDown();
      exclusiveLocked.await(1, TimeUnit.SECONDS);
      assertNotNull(exclusiveThread.get());
      lockHeldInExclusiveMode(exclusiveThread.get());
      assertSame(expectedExclusive, exclusiveThread.get());
      cannotExclusiveLock(false);
      cannotSharedLock(false);
      
      done.countDown();
      // last threads in pool should complete after done latch opened
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.SECONDS);
      
      lockNotHeld();
   }

   /**
    * Tests the simplest case of uncontested acquisition and release of an exclusive lock.
    */
   @Test public void exclusiveLock_oneThread() throws Exception {
      ExclusiveLock exclusive = lock.exclusiveLock();
      lockHeldInExclusiveMode(Thread.currentThread());
      cannotSharedLock(true);
      lockHeldInExclusiveMode(Thread.currentThread());
      exclusive.unlock();
      exclusiveLockNotValid(exclusive);
      lockNotHeld();
   }

   /**
    * Tests acquiring an exclusive lock multiple times (re-entrant) from the same thread.
    */
   @Test public void exclusiveLock_oneThread_reentrant() throws Exception {
      ExclusiveLock exclusive1 = lock.exclusiveLock();
      ExclusiveLock exclusive2 = lock.exclusiveLock();
      ExclusiveLock exclusive3 = lock.exclusiveLock();
      ExclusiveLock exclusive4 = lock.exclusiveLock();
      lockHeldInExclusiveMode(Thread.currentThread());
      cannotSharedLock(true);
      lockHeldInExclusiveMode(Thread.currentThread());
      exclusive1.unlock();
      exclusiveLockNotValid(exclusive1);
      exclusive2.unlock();
      exclusiveLockNotValid(exclusive2);
      lockHeldInExclusiveMode(Thread.currentThread());
      exclusive3.unlock();
      exclusiveLockNotValid(exclusive3);
      exclusive4.unlock();
      exclusiveLockNotValid(exclusive4);
      lockNotHeld();
   }

   /**
    * Tests acquiring and releasing exclusive locks from multiple threads. One of the threads will
    * acquire and release a shared lock, just to ensure that shared locks behave properly when a
    * thread already holds it in exclusive mode.
    */
   @Test public void exclusiveLock_multipleThreads() throws Exception {
      final Object sentinel = new Object();
      final BlockingQueue<Object> locked = new LinkedBlockingQueue<Object>();
      final BlockingQueue<Object> proceed = new LinkedBlockingQueue<Object>();
      final CountDownLatch start = new CountDownLatch(1);
      final Set<Thread> sharedThreads = Collections.synchronizedSet(new HashSet<Thread>());
      final Set<Thread> exclusiveThreads = Collections.synchronizedSet(new HashSet<Thread>());
      
      ExecutorService executor = Executors.newFixedThreadPool(3);
      for (int i = 0; i < 3; i++) {
         final boolean shared = i == 1;
         executor.submit(new Callable<Void>() {
            @Override public Void call() throws Exception{
               start.await();
               if (shared) {
                  SharedLock share = lock.sharedLock();
                  sharedThreads.add(Thread.currentThread());
                  locked.put(sentinel);
                  proceed.take();
                  sharedThreads.remove(Thread.currentThread());
                  share.unlock();
               } else {
                  ExclusiveLock exclusive = lock.exclusiveLock();
                  exclusiveThreads.add(Thread.currentThread());
                  locked.put(sentinel);
                  proceed.take();
                  exclusiveThreads.remove(Thread.currentThread());
                  exclusive.unlock();
               }
               return null;
            }
         });
      }

      ExclusiveLock exclusive = lock.exclusiveLock();
      exclusiveThreads.add(Thread.currentThread());
      start.countDown();
      Thread.sleep(50); // let all threads try to acquire

      assertEquals(0, sharedThreads.size());
      assertEquals(1, exclusiveThreads.size());
      Set<Thread> expectedQueuedThreads =
            lockHeldInExclusiveModeWithQueuedThreads(exclusiveThreads.iterator().next());
      exclusive.unlock();
      exclusiveLockNotValid(exclusive);
      exclusiveThreads.remove(Thread.currentThread());
      
      Set<Thread> threads = new HashSet<Thread>();
      threads.add(Thread.currentThread());
      int sharedCount = 0;
      for (int i = 0; i < 3; i++) {
         Thread t;
         assertNotNull(locked.poll(1, TimeUnit.SECONDS));
         if (sharedThreads.isEmpty()) {
            assertEquals(0, sharedThreads.size());
            assertEquals(1, exclusiveThreads.size());
            t = exclusiveThreads.iterator().next();
            if (i == 2) {
               // no more waiting threads
               lockHeldInExclusiveMode(t);
            } else {
               Set<Thread> queuedThreads = lockHeldInExclusiveModeWithQueuedThreads(t);
               assertEquals(2 - i, queuedThreads.size());
               assertTrue(expectedQueuedThreads.containsAll(queuedThreads));
            }
         } else {
            sharedCount++;
            assertEquals(1, sharedThreads.size());
            assertEquals(0, exclusiveThreads.size());
            t = sharedThreads.iterator().next();
            if (i == 2) {
               // no more waiting threads
               lockHeldInSharedMode(t);
            } else {
               Set<Thread> queuedThreads = lockHeldInSharedModeWithQueuedThreads(t);
               assertEquals(2 - i, queuedThreads.size());
               assertTrue(expectedQueuedThreads.containsAll(queuedThreads));
            }
         }
         assertTrue(threads.add(t));
         proceed.put(sentinel);
      }
      
      // last thread in pool should complete after last sentinel added to proceed queue
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.SECONDS);
      
      assertEquals(1, sharedCount);
      
      lockNotHeld();
   }
   
   /**
    * Tests promotion of a shared lock to an exclusive one.
    */
   @Test public void promoteSharedToExclusive_oneThread() throws Exception {
      SharedLock share1 = lock.sharedLock();
      SharedLock share2 = lock.sharedLock();
      lockHeldInSharedMode(Thread.currentThread());
      cannotExclusiveLock(true);
      
      try {
         share1.promoteToExclusive();
         fail("Expecting but did not catch an IllegalStateException");
      } catch (IllegalStateException e) {
      }
      try {
         share2.promoteToExclusive();
         fail("Expecting but did not catch an IllegalStateException");
      } catch (IllegalStateException e) {
      }
      lockHeldInSharedMode(Thread.currentThread());
      cannotExclusiveLock(true);
      
      share2.unlock();
      sharedLockNotValid(share2);
      
      ExclusiveLock exclusive = share1.promoteToExclusive();
      sharedLockNotValid(share1);
      lockHeldInExclusiveMode(Thread.currentThread());
      cannotSharedLock(true);

      exclusive.unlock();
      exclusiveLockNotValid(exclusive);
      lockNotHeld();
   }

   /**
    * Tests demotion of an exclusive lock to a shared one.
    */
   @Test public void demoteExclusiveToShared_oneThread() throws Exception {
      ExclusiveLock exclusive1 = lock.exclusiveLock();
      ExclusiveLock exclusive2 = lock.exclusiveLock();
      lockHeldInExclusiveMode(Thread.currentThread());
      cannotSharedLock(true);
      
      try {
         exclusive1.demoteToShared();
         fail("Expecting but did not catch an IllegalStateException");
      } catch (IllegalStateException e) {
      }
      try {
         exclusive2.demoteToShared();
         fail("Expecting but did not catch an IllegalStateException");
      } catch (IllegalStateException e) {
      }
      lockHeldInExclusiveMode(Thread.currentThread());
      cannotSharedLock(true);
      
      exclusive2.unlock();
      exclusiveLockNotValid(exclusive2);
      
      SharedLock share = exclusive1.demoteToShared();
      exclusiveLockNotValid(exclusive1);
      lockHeldInSharedMode(Thread.currentThread());
      cannotExclusiveLock(true);

      share.unlock();
      sharedLockNotValid(share);
      lockNotHeld();
   }
}
