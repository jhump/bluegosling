package com.bluegosling.concurrent.locks;

import static com.bluegosling.testing.MoreAsserts.assertThrows;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bluegosling.vars.VariableBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
   
   /** A simple task that can throw a checked exception. */
   private interface CheckedRunnable {
      void run() throws Exception;
   }

   /** A task that accepts a condition and can throw a checked exception. */
   private interface CheckedConditionRunnable {
      void run(Condition c) throws Exception;
   }
   
   /** A task that accepts a condition and a time limit and can throw a checked exception. */
   private interface CheckedTimedConditionRunnable {
      boolean run(Condition c, long timeLimit, TimeUnit unit) throws Exception;
   }

   /**
    * A task that waits for a given boolean to become true by awaiting the given condition. The
    * task can throw a checked exception. The task returns true if the wait succeeded and the
    * given boolean became true.
    */
   private interface CheckedConditionWaiter {
      boolean run(Condition c, AtomicBoolean b) throws Exception;
   }

   protected ExecutorService executor;
   protected Lock lock;
   
   protected abstract Lock makeLock();
   
   protected abstract boolean isReentrant();

   @Before public void setUp() {
      executor = Executors.newCachedThreadPool();
      lock = makeLock();
   }
   
   @After public void tearDown() throws Exception {
      executor.shutdown();
      assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
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
   
   @Test(timeout = 2000)
   public void timedAcquire() throws Exception {
      CountDownLatch ready = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      // another thread holds lock
      Future<?> f = executor.submit(() -> {
         assertTrue(lock.tryLock());
         ready.countDown();
         release.await();
         // make sure other thread is waiting to acquire
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
      CheckedConditionWaiter waiter = makeWaiter(ready, await);
      Future<Boolean> c1await = executor.submit(() -> waiter.run(c1, b1));
      Future<Boolean> c2await = executor.submit(() -> waiter.run(c2, b2));
      
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
      assertTrue(c1await.get()); // make sure it returns successfully
      
      // make sure signaling first waiter didn't mistakenly wake up the second
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
      assertTrue(c2await.get()); // make sure it returns successfully
   }
   
   /**
    * Makes a waiter task that uses the given block to await a condition and wait for a flag to
    * become true.
    * 
    * @param ready an optional latch that, if not {@code null}, the task will count down when it
    *       starts (useful if caller is coordinating multiple waiter tasks)
    * @param await the block that is executed to await a condition
    */
   private CheckedConditionWaiter makeWaiter(CountDownLatch ready,
         CheckedConditionRunnable await) {
      // makes a task that waits until the given boolean to become true by awaiting the given
      // condition object
      return (c, b) -> {
         if (ready != null) {
            ready.countDown();
         }
         VariableBoolean ret = new VariableBoolean();
         decorate(cond -> {
            assertFalse(b.get());
            await.run(cond);
            ret.set(b.get()); // see if it became healthy
         }).run(c);
         return ret.get();
      };
   }
   
   /**
    * Decorates a simple task so that it can correctly await a condition. This involves making sure
    * the thread won't spuriously wake up and making sure the lock is held on entry and released on
    * exit.
    */
   private CheckedConditionRunnable decorate(CheckedConditionRunnable r) {
      return c -> {
         // make sure that this thread has no permit (from a prior or racing call to unpark) so it
         // won't spuriously wake-up below
         LockSupport.parkNanos(100);
         lock.lock();
         try {
            r.run(c);
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
         makeWaiter(ready, task).run(c, b);
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
   public void condition_awaitAlreadyInterrupted() {
      verifyAwaitAlreadyInterrupted(c -> c.await());
      verifyAwaitAlreadyInterrupted(c -> c.await(10, TimeUnit.SECONDS));
      verifyAwaitAlreadyInterrupted(c -> c.awaitNanos(TimeUnit.SECONDS.toNanos(10)));
      verifyAwaitAlreadyInterrupted(c -> c.awaitUntil(new Date(System.currentTimeMillis()
            + TimeUnit.SECONDS.toMillis(10))));
   }
   
   private void verifyAwaitAlreadyInterrupted(CheckedConditionRunnable await) {
      Thread.currentThread().interrupt();
      assertThrows(InterruptedException.class, () -> decorate(await).run(lock.newCondition()));
   }
   
   @Test(timeout = 3000)
   public void condition_timedAwait() throws Exception {
      verifyTimedAwait((c, l, u) -> c.await(l, u));
      verifyTimedAwait((c, l, u) -> c.awaitNanos(u.toNanos(l)) > 0);
      verifyTimedAwait((c, l, u) ->
            c.awaitUntil(new Date(System.currentTimeMillis() + u.toMillis(l))));
   }
   
   private void verifyTimedAwait(CheckedTimedConditionRunnable await) throws Exception {
      CountDownLatch ready = new CountDownLatch(1);
      CountDownLatch release = new CountDownLatch(1);
      Condition c = lock.newCondition();
      AtomicBoolean b = new AtomicBoolean();
      // another thread holds lock
      Future<?> f = executor.submit(() -> {
         ready.countDown();
         release.await();
         // make sure other thread is awaiting condition
         Thread.sleep(200);
         assertTrue(lock.tryLock());
         b.set(true);
         c.signal();
         lock.unlock();
         return null;
      });
      
      ready.await();
      assertFalse(makeWaiter(null, cond -> await.run(cond, 0, TimeUnit.NANOSECONDS)).run(c, b));
      assertFalse(makeWaiter(null, cond -> await.run(cond, 100, TimeUnit.MILLISECONDS)).run(c, b));
      release.countDown();
      assertTrue(makeWaiter(null, cond -> await.run(cond, 10, TimeUnit.SECONDS)).run(c, b));
      
      f.get(); // just to make sure the other thread encountered no exceptions...
   }

   @Test(timeout = 6000)
   public void condition_signalOnlyOne() throws Exception {
      verifySignal(false, c -> c.await());
      verifySignal(false, c -> c.awaitUninterruptibly());
      verifySignal(false, c -> c.await(10, TimeUnit.SECONDS));
      verifySignal(false, c -> c.awaitNanos(TimeUnit.SECONDS.toNanos(10)));
      verifySignal(false, c -> c.awaitUntil(new Date(System.currentTimeMillis()
            + TimeUnit.SECONDS.toMillis(10))));
   }

   @Test(timeout = 5000)
   public void condition_signalAll() throws Exception {
      verifySignal(true, c -> c.await());
      verifySignal(true, c -> c.awaitUninterruptibly());
      verifySignal(true, c -> c.await(10, TimeUnit.SECONDS));
      verifySignal(true, c -> c.awaitNanos(TimeUnit.SECONDS.toNanos(10)));
      verifySignal(true, c -> c.awaitUntil(new Date(System.currentTimeMillis()
            + TimeUnit.SECONDS.toMillis(10))));
   }

   private void verifySignal(boolean signalAll, CheckedConditionRunnable await) throws Exception {
      CountDownLatch ready = new CountDownLatch(3);
      AtomicBoolean b = new AtomicBoolean();
      Condition c = lock.newCondition();
      CheckedConditionWaiter waiter = makeWaiter(ready, await);
      List<Future<Boolean>> waitingTasks = new ArrayList<>(3);
      waitingTasks.add(executor.submit(() -> waiter.run(c, b)));
      Thread.sleep(100); // let first task get in line, head of queue
      waitingTasks.add(executor.submit(() -> waiter.run(c, b)));
      Thread.sleep(100); // let second task enqueue itself
      waitingTasks.add(executor.submit(() -> waiter.run(c, b)));
      
      ready.await();
      Thread.sleep(100);
      b.set(true); // set value so that waiting threads will complete
      
      if (signalAll) {
         // all in one shot
         verifySignalAll(waitingTasks, c);
      } else {
         while (!waitingTasks.isEmpty()) {
            // one at a time
            verifySignalOne(waitingTasks, c);
         }
      }
   }
   
   private void verifySignalAll(List<Future<Boolean>> waitingTasks, Condition c) throws Exception {
      lock.lock();
      try {
         c.signalAll();
      } finally {         
         lock.unlock();
      }

      // give signaled threads a chance to react
      Thread.sleep(100);
      
      for (Future<Boolean> f : waitingTasks) {
         // all have been signaled
         assertTrue(f.isDone());
      }
         
      for (Future<Boolean> f : waitingTasks) {
         // and all completed successfully
         assertTrue(f.get());
      }
   }

   private void verifySignalOne(List<Future<Boolean>> waitingTasks, Condition c) throws Exception {
      lock.lock();
      try {
         c.signal();
      } finally {         
         lock.unlock();
      }

      // give signaled threads a chance to react
      Thread.sleep(100);
      
      Future<Boolean> f = waitingTasks.remove(0);
      // first one has been signaled
      assertTrue(f.isDone());
      // and completed successfully
      assertTrue(f.get());
      
      // others have not been signaled
      for (Future<Boolean> other : waitingTasks) {
         assertFalse(other.isDone());
      }
   }
}

