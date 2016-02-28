package com.bluegosling.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class ActorThreadPoolTest {
   ActorThreadPool<?> tp;
   boolean alreadyTerminated = false;
   
   @After public void shutDown() throws Exception {
      assertEquals(alreadyTerminated, tp.isTerminated());
      assertNotNull(tp.shutdownNow());
      assertTrue(tp.awaitTermination(5, TimeUnit.SECONDS));
      assertTrue(tp.isTerminated());
   }
   
   @Test public void simple() throws Exception {
      ActorThreadPool<Integer> ex = new ActorThreadPool<>(3, 5, 100, TimeUnit.MILLISECONDS);
      tp = ex;
      AtomicInteger active = new AtomicInteger();
      AtomicInteger max = new AtomicInteger();
      List<Future<?>> all = new ArrayList<>(100);
      for (int i = 0; i < 10; i++) {
         AtomicBoolean running = new AtomicBoolean();
         for (int j = 0; j < 10; j++) {
            all.add(ex.submit(i, () -> {
               assertTrue(running.compareAndSet(false, true));
               int a = active.incrementAndGet();
               max.accumulateAndGet(a, Math::max);
               try {
                  Thread.sleep(20);
               } catch (InterruptedException e) {
                  throw new RuntimeException(e);
               } finally {
                  active.decrementAndGet();
                  running.set(false);
               }
            }));
         }
      }
      assertEquals(5, ex.getActiveCount());
      assertEquals(100, ex.getTaskCount());
      for (Future<?> f : all) {
         f.get();
      }
      // all tasks done, but the executor threads decrement active count concurrently so give them
      // a chance to do so
      Thread.sleep(50);
      assertEquals(0, ex.getActiveCount());
      assertTrue(ex.getCurrentPoolSize() >= 3 && ex.getCurrentPoolSize() <= 5);
      assertEquals(5, ex.getLargestPoolSize());
      assertEquals(5, max.get());
      Thread.sleep(300); // give the non-core threads enough time to time out and terminate
      assertEquals(3, ex.getCurrentPoolSize());
      assertEquals(5, ex.getLargestPoolSize());
      assertEquals(100, ex.getTaskCount());
      assertEquals(100, ex.getCompletedTaskCount());
   }
}
