package com.apriori.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import static java.util.stream.Collectors.toList;

import com.apriori.util.VariableBoolean;
import com.apriori.util.VariableInt;
import com.apriori.util.VariableLong;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.IntStream;


public class DoubleInstanceLockTest {

   private ArrayList<Integer> list;
   private DoubleInstanceLock<ArrayList<Integer>> lock;
   private ExecutorService executor;
   
   @Before public void setUp() {
      list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
      lock = DoubleInstanceLock.newLock(list);
      executor = Executors.newCachedThreadPool();
   }
   
   @Test(timeout = 4000)
   public void concurrentReaders() throws Exception {
      CountDownLatch ready = new CountDownLatch(100);
      CountDownLatch go = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(100);
      VariableBoolean failed = new VariableBoolean();
      for (int i = 0; i < 100; i++) { 
         executor.execute(() -> {
            lock.readWith(l -> {
               try {
                  ready.countDown();
                  go.await();
                  Thread.sleep(1000);
                  assertSame(list, l);
                  assertEquals(Arrays.asList(1, 2, 3, 4, 5), l);
               } catch (Throwable t) {
                  failed.set(true);
               } finally {
                  done.countDown();
               }
            });
         });
      }
      ready.await(); // if this works, all readers are concurrently reading
      VariableInt count = new VariableInt();
      go.countDown();
      long start = System.currentTimeMillis();
      lock.writeWith(l -> {
         if (count.getAndIncrement() == 1) {
            assertSame(list, l);
         }
         assertEquals(Arrays.asList(1, 2, 3, 4, 5), l);
         l.add(6);
      });
      long end = System.currentTimeMillis();
      // all readers succeeded
      assertEquals(0, done.getCount());
      assertFalse(failed.get());
      // writer was blocked an expected duration of time
      long duration = end - start;
      assertTrue(duration > 500); // this one's a bit tight, could be flaky w/ strange scheduling
      assertTrue(duration < 2000);
      assertEquals(2, count.get()); // writer called 2x, once for each instance
      List<Integer> l = lock.read(Function.identity());
      assertNotSame(list, l); // swapped after write, no longer the same
      assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), l);
   }
   
   @Test public void exclusiveWriters() throws Exception {
      CountDownLatch ready = new CountDownLatch(100);
      CountDownLatch go = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(100);
      VariableBoolean failed = new VariableBoolean();
      AtomicInteger active = new AtomicInteger();
      AtomicInteger count = new AtomicInteger();
      for (int i = 0; i < 100; i++) { 
         executor.execute(() -> {
            lock.writeWith(l -> {
               assertEquals(1, active.incrementAndGet());
               int c = count.getAndIncrement();
               int w = c >>> 1;
               int n = c & 1;
               try {
                  ready.countDown();
                  go.await();
                  if ((w & 1) == 0 && n == 0) {
                     // first write op for every other writer is the original list
                     assertSame(list, l);
                  } else {
                     assertNotSame(list, l);
                  }
                  Thread.sleep(20);
                  l.add(l.size() + 1);
               } catch (Throwable t) {
                  failed.set(true);
               } finally {
                  done.countDown();
                  active.decrementAndGet();
               }
            });
         });
      }
      ready.await(); // if this works, all writers are queued up
      go.countDown();
      long start = System.currentTimeMillis();
      boolean finished = false;
      VariableInt prevSz = new VariableInt(5);
      VariableLong readCount = new VariableLong();
      while (!finished) {
         finished = lock.read(l -> {
            // reader is not blocked by writers, so we'll catch some version in the middle
            int sz = l.size();
            assertTrue(sz >= 5 && sz <= 105);
            assertTrue(sz >= prevSz.get()); // monotonic progress of writers
            prevSz.set(sz);
            readCount.incrementAndGet();
            return sz == 105;
         });
         Thread.yield();
      }
      // all writers succeeded
      done.await();
      long end = System.currentTimeMillis();
      assertFalse(failed.get());
      // all writers together took expected duration of time
      long duration = end - start;
      assertTrue(duration > 1500); // this one's a bit tight, could be flaky w/ strange scheduling
      assertTrue(duration < 3000);
      assertEquals(200, count.get()); // each writer calls 2x
      List<Integer> l = lock.read(Function.identity());
      assertSame(list, l); // swapped even number of times, so back to the same
      List<Integer> expected = IntStream.rangeClosed(1, 105).boxed().collect(toList());
      assertEquals(expected, l);
   }
   
   @Test public void swapsOnWrite() {
      // TODO
   }
   
   @Test public void snapshot() {
      // TODO
   }
}
