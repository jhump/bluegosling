package com.apriori.concurrent;

import static com.apriori.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;


public abstract class AbstractLockTest {
   
   private interface CheckedRunnable {
      void run() throws Exception;
   }

   private interface CheckedConditionRunnable {
      void run(Condition c) throws Exception;
   }
   
   private interface CheckedBiConsumer<T, U> {
      void accept(T t, U u) throws Exception;
   }

   protected ExecutorService executor;
   protected Lock lock;
   
   protected abstract Lock makeLock();
   
   protected abstract boolean isReentrant();

   @Before public void setUp() {
      executor = Executors.newCachedThreadPool();
      lock = makeLock();
   }
   
   @After public void tearDown() {
      executor.shutdown();
   }
   
   private final AtomicInteger reentranceCount = new AtomicInteger();

   @Test(timeout = 2000)
   public void lockAndUnlock() throws Exception {
      assertTrue(lock.tryLock());
      verifyLocked();
      lock.unlock();

      assertTrue(lock.tryLock(0, TimeUnit.NANOSECONDS));
      verifyLocked();
      lock.unlock();

      lock.lock();
      verifyLocked();
      lock.unlock();

      lock.lockInterruptibly();
      verifyLocked();
      lock.unlock();
   }
   
   private void verifyLocked() throws Exception {
      if (isReentrant()) {
         // check re-entrance by recursively running the lock test
         // cases while already holding the lock
         if (reentranceCount.get() == 3) {
            // avoid infinite recursion
            return;
         }
         reentranceCount.incrementAndGet();
         try {
            lockAndUnlock();
            if (reentranceCount.get() > 1) {
               // only fall-through for other tests below the first time
               return;
            }
         } finally {
            reentranceCount.decrementAndGet();
         }
      } else {
         // if it's not reentrant, then we can't acquire, even from the same thread
         assertFalse(lock.tryLock());
         assertFalse(lock.tryLock(0, TimeUnit.NANOSECONDS));
         assertFalse(lock.tryLock(100, TimeUnit.MILLISECONDS));
      }
      
      // reentrant or not, should not be able to acquire from another thread
      executor.submit(() -> {
         assertFalse(lock.tryLock());
         assertFalse(lock.tryLock(100, TimeUnit.MILLISECONDS));
         return null;
      }).get();
   }
   
   @Test(timeout = 2000)
   public void interruptible() throws Exception {
      // with lock unavailable
      assertTrue(lock.tryLock());
      // other threads trying to acquire will block interruptibly
      verifyInterruptibleAcquire(() -> lock.lockInterruptibly());
      verifyInterruptibleAcquire(() -> lock.tryLock(5, TimeUnit.SECONDS));
      lock.unlock();
   }

   private void verifyInterruptibleAcquire(CheckedRunnable task) throws Exception {
      AtomicReference<Thread> runner = new AtomicReference<>();
      CountDownLatch ready = new CountDownLatch(1);
      Future<?> f = executor.submit(() -> {
         runner.set(Thread.currentThread());
         ready.countDown();
         task.run();
         return null;
      });
      
      // make sure thread is waiting for lock
      ready.await();
      Thread.sleep(200);
      // and then interrupt it
      runner.get().interrupt();

      ExecutionException ex = assertThrows(ExecutionException.class, () -> f.get());
      assertSame(InterruptedException.class, ex.getCause().getClass());
   }
   
   @Test(timeout = 1000)
   public void alreadyInterrupted() throws Exception {
      // lock is available but thread is interrupted
      Thread.currentThread().interrupt();
      assertThrows(InterruptedException.class, () -> lock.lockInterruptibly());
      Thread.currentThread().interrupt();
      assertThrows(InterruptedException.class, () -> lock.tryLock(0, TimeUnit.SECONDS));
   }
   
   @Test(timeout = 3000)
   public void timedAcquire() throws Exception {
      CountDownLatch ready = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      // another thread holds lock
      Future<?> f = executor.submit(() -> {
         assertTrue(lock.tryLock());
         ready.countDown();
         release.await();
         // make sure other thread is in timed wait to acquire
         Thread.sleep(200);
         lock.unlock();
         return null;
      });
      
      ready.await();
      assertFalse(lock.tryLock(0, TimeUnit.NANOSECONDS));
      assertFalse(lock.tryLock(100, TimeUnit.MILLISECONDS));
      release.countDown();
      assertTrue(lock.tryLock(10, TimeUnit.SECONDS));
      
      f.get(); // just to make sure the other thread encountered no exceptions...
   }
   
   @Test(timeout = 1000)
   public void condition_mustBeLockedToUse() throws Exception {
      Condition c = lock.newCondition();
      assertThrows(IllegalMonitorStateException.class, () -> c.await());
      assertThrows(IllegalMonitorStateException.class, () -> c.awaitUninterruptibly());
      assertThrows(IllegalMonitorStateException.class, () -> c.await(100, TimeUnit.NANOSECONDS));
      assertThrows(IllegalMonitorStateException.class, () -> c.awaitNanos(100));
      assertThrows(IllegalMonitorStateException.class, () -> c.awaitUntil(new Date()));
      assertThrows(IllegalMonitorStateException.class, () -> c.signal());
      assertThrows(IllegalMonitorStateException.class, () -> c.signalAll());
   }
   
   @Test(timeout = 5000)
   public void condition_awaitAndSignal() throws Exception {
      verifyConditions(c -> c.await());
      verifyConditions(c -> c.awaitUninterruptibly());
      verifyConditions(c -> c.await(10, TimeUnit.SECONDS));
      verifyConditions(c -> c.awaitNanos(TimeUnit.SECONDS.toNanos(10)));
      verifyConditions(c -> c.awaitUntil(new Date(System.currentTimeMillis()
            + TimeUnit.SECONDS.toMillis(10))));
   }
   
   private void verifyConditions(CheckedConditionRunnable await) throws Exception {
      verifyConditions(await, c -> c.signal());
      verifyConditions(await, c -> c.signalAll());
   }
   
   private void verifyConditions(CheckedConditionRunnable await, CheckedConditionRunnable signal)
         throws Exception {
      CountDownLatch ready = new CountDownLatch(2);
      AtomicBoolean b1 = new AtomicBoolean();
      AtomicBoolean b2 = new AtomicBoolean();
      Condition c1 = lock.newCondition();
      Condition c2 = lock.newCondition();
      CheckedBiConsumer<Condition, AtomicBoolean> task = makeAwaitTask(ready, await);
      Future<Void> c1await = executor.submit(() -> { task.accept(c1, b1); return null; });
      Future<Void> c2await = executor.submit(() -> { task.accept(c2, b2); return null; });
      
      ready.await();
      Thread.sleep(100);
      // signal the first task
      lock.lock();
      try {
         b1.set(true);
         signal.run(c1);
      } finally {         
         lock.unlock();
      }
      c1await.get(); // make sure it returns successfully
      
      // make sure signalling first didn't wake up second
      Thread.sleep(100);
      assertFalse(c2await.isDone());
      
      // then signal the second task
      lock.lock();
      try {
         b2.set(true);
         signal.run(c2);
      } finally {         
         lock.unlock();
      }
      c2await.get(); // make sure it returns successfully
   }
   
   CheckedBiConsumer<Condition, AtomicBoolean> makeAwaitTask(CountDownLatch ready,
         CheckedConditionRunnable await) {
      return (c, b) -> {
         ready.countDown();
         // make sure that this thread has no permit so it won't spuriously wake-up below
         LockSupport.parkNanos(100);
         lock.lock();
         try {
            assertFalse(b.get());
            await.run(c);
            assertTrue(b.get());
         } finally {
            lock.unlock();
         }
      };
   }

   @Test(timeout = 3000)
   public void condition_awaitInterruptibly() throws Exception {
      verifyInterruptibleAwait(c -> c.await());
      verifyInterruptibleAwait(c -> c.await(10, TimeUnit.SECONDS));
      verifyInterruptibleAwait(c -> c.awaitNanos(TimeUnit.SECONDS.toNanos(10)));
      verifyInterruptibleAwait(c -> c.awaitUntil(new Date(System.currentTimeMillis()
            + TimeUnit.SECONDS.toMillis(10))));
   }

   private void verifyInterruptibleAwait(CheckedConditionRunnable task) throws Exception {
      AtomicReference<Thread> runner = new AtomicReference<>();
      AtomicBoolean b = new AtomicBoolean();
      Condition c = lock.newCondition(); 
      CountDownLatch ready = new CountDownLatch(1);
      Future<?> f = executor.submit(() -> {
         runner.set(Thread.currentThread());
         makeAwaitTask(ready, task).accept(c, b);
         return null;
      });      
      // make sure thread is waiting for lock
      ready.await();
      Thread.sleep(200);
      // and then interrupt it
      runner.get().interrupt();

      ExecutionException ex = assertThrows(ExecutionException.class, () -> f.get());
      assertSame(InterruptedException.class, ex.getCause().getClass());
   }
   
   @Test public void condition_awaitAlreadyInterrupted() {
      // TODO
   }

   @Test public void condition_timedAwait() {
      // TODO
   }

   @Test public void condition_signalOnlyOne() {
      // TODO
   }

   @Test public void condition_signalAll() {
      // TODO
   }
}
