package com.bluegosling.concurrent;

import static com.bluegosling.testing.MoreAsserts.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpAndDownLatchTest {
   @Test
   public void countUpAndDown() {
      UpAndDownLatch l = new UpAndDownLatch();
      assertEquals(0, l.getCount());
      assertTrue(l.isDone());

      assertTrue(l.countUp());
      assertEquals(1, l.getCount());
      assertFalse(l.isDone());
      
      assertFalse(l.countUp());
      assertEquals(2, l.getCount());
      assertFalse(l.isDone());

      assertFalse(l.countDown());
      assertEquals(1, l.getCount());
      assertFalse(l.isDone());
      
      assertTrue(l.countDown());
      assertEquals(0, l.getCount());
      assertTrue(l.isDone());

      assertFalse(l.countDown(-1));
      assertEquals(1, l.getCount());
      assertFalse(l.isDone());

      assertFalse(l.countUp(-1));
      assertEquals(0, l.getCount());
      assertTrue(l.isDone());
      
      assertFalse(l.countUp(0));
      assertEquals(0, l.getCount());
      assertTrue(l.isDone());
      assertFalse(l.countDown(0));
      assertEquals(0, l.getCount());
      assertTrue(l.isDone());
   }
   
   @Test
   public void cannotBeNegative() {
      assertThrows(IllegalArgumentException.class, () -> new UpAndDownLatch(-1));
      
      UpAndDownLatch l = new UpAndDownLatch();
      assertThrows(IllegalStateException.class, () -> l.countUp(-1));
      assertThrows(IllegalStateException.class, () -> l.countDown(1));
      
      l.countUp(100);
      assertThrows(IllegalStateException.class, () -> l.countUp(-101));
      assertThrows(IllegalStateException.class, () -> l.countDown(101));
      l.countDown(100);
      assertThrows(IllegalStateException.class, () -> l.countDown());
   }

   @Test
   public void cannotOverflowIntMaxValue() {
      UpAndDownLatch l = new UpAndDownLatch(Integer.MAX_VALUE);
      assertThrows(IllegalStateException.class, () -> l.countUp(1));
      assertThrows(IllegalStateException.class, () -> l.countDown(-11));

      l.countDown(100);
      assertThrows(IllegalStateException.class, () -> l.countUp(101));
      assertThrows(IllegalStateException.class, () -> l.countDown(-101));
      l.countUp(100);
      assertThrows(IllegalStateException.class, () -> l.countUp());
   }
   
   @Test(timeout = 300)
   public void awaitReturnsImmediatelyWhenCountIsZero() throws InterruptedException {
      UpAndDownLatch l = new UpAndDownLatch();
      l.await();
      assertTrue(l.await(0, TimeUnit.SECONDS));
      assertTrue(l.await(1, TimeUnit.SECONDS));
   }
   
   @Test(timeout = 2000)
   public void awaitDoesNotReturnWhenCountIsNonZero() throws InterruptedException {
      UpAndDownLatch l = new UpAndDownLatch(1);
      AtomicBoolean interrupted = new AtomicBoolean();
      Thread th = new Thread(() -> {
         try {
            l.await();
         } catch (InterruptedException e) {
            interrupted.set(true);
         }
      });
      th.start();
      Thread.sleep(200);
      th.interrupt();
      th.join();
      assertTrue(interrupted.get());
      
      assertFalse(l.await(0, TimeUnit.MILLISECONDS));
      assertFalse(l.await(100, TimeUnit.MILLISECONDS));
   }
   
   @Test(timeout = 3000)
   public void awaitReturnsWhenCountReachesZero() throws InterruptedException {
      UpAndDownLatch l = new UpAndDownLatch(1);
      AtomicBoolean finished = new AtomicBoolean();

      Thread th = new Thread(() -> {
         try {
            l.await();
            finished.set(true);
         } catch (InterruptedException e) {
         }
      });
      th.start();
      Thread.sleep(200);
      l.countDown();
      th.join(200);
      assertTrue(finished.get());
      assertFalse(th.isAlive());
      
      l.countUp();
      finished.set(false);
      th = new Thread(() -> {
         try {
            assertFalse(l.await(0, TimeUnit.SECONDS));
            assertTrue(l.await(2, TimeUnit.SECONDS));
            finished.set(true);
         } catch (InterruptedException e) {
         }
      });
      th.start();
      Thread.sleep(200);
      l.countDown();
      th.join(200);
      assertFalse(th.isAlive());
      assertTrue(finished.get());
   }
}