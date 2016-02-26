package com.apriori.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import static java.util.stream.Collectors.toList;

import com.apriori.vars.Variable;
import com.apriori.vars.VariableBoolean;
import com.apriori.vars.VariableInt;
import com.apriori.vars.VariableLong;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
   
   @Test(timeout = 5000)
   public void exclusiveWriters() throws Exception {
      // Checks not only that writers are exclusive with respect to one another but also attempts
      // to check that writers are exclusive with respect to readers, too. E.g. readers must be
      // drained from a side before the writer touches that side.
      
      CountDownLatch ready = new CountDownLatch(100);
      CountDownLatch go = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(100);
      Variable<Throwable> failed = new Variable<>();
      AtomicInteger active = new AtomicInteger();
      AtomicInteger count = new AtomicInteger();
      // spin up 100 threads trying to write
      for (int i = 0; i < 100; i++) { 
         executor.execute(() -> {
            ready.countDown();
            try {
               go.await();
               lock.writeWith(l -> {
                  assertEquals(1, active.incrementAndGet());
                  int c = count.getAndIncrement();
                  int w = c >>> 1;
                  int n = c & 1;
                  try {
                     // keeps alternating, first op being the right one then second op
                     if (((w & 1) == 1 && n == 0)
                           || ((w & 1) == 0 && n == 1)) {
                        assertSame(list, l);
                     } else {
                        assertNotSame(list, l);
                     }
                     try {
                        Thread.sleep(10);
                     } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                     }
                     l.add(l.size() + 1);
                  } finally {
                     active.decrementAndGet();
                  }
               });
            } catch (Throwable t) {
               t.printStackTrace();
               failed.set(t);
            } finally {
               done.countDown();
            }
         });
      }
      // spin up reader threads to verify they don't interfere incorrectly with writers
      List<Future<Long>> readers = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
         VariableInt prevSz = new VariableInt(5);
         readers.add(executor.submit(() -> {
            VariableLong readCount = new VariableLong();
            while (done.getCount() > 0) {
               lock.readWith(l -> {
                  // reader is not blocked by writers, so we'll catch some version in the middle
                  int sz = l.size();
                  assertTrue(sz >= 5 && sz <= 105);
                  // verify progress of writers is monotonic
                  assertTrue("" + sz + " < " + prevSz.get(), sz >= prevSz.get());
                  prevSz.set(sz);
                  readCount.incrementAndGet();
               });
            }
            return readCount.get();
         }));
      }
      
      ready.await(); // if this works, all writers are queued up
      
      go.countDown();
      long start = System.currentTimeMillis();
      // wait for all tasks to complete
      done.await();
      long end = System.currentTimeMillis();

      long totalReads = 0;
      for (Future<Long> f : readers) {
         totalReads += f.get();
      }
      // make sure writers succeeded
      assertNull(failed.get());
      // ensure writes took expected duration of time (since they were serialized)
      long duration = end - start;
      assertTrue(duration >= 2000);
      assertEquals(200, count.get()); // each writer is used 2x (to apply write to both sides)
      List<Integer> l = lock.read(Function.identity());
      assertSame(list, l); // swapped even number of times, so back to the same
      List<Integer> expected = IntStream.rangeClosed(1, 105).boxed().collect(toList());
      assertEquals(expected, l);
      assertTrue(totalReads > 400); // certain to be way higher
   }
   
   @Test public void swapsOnWrite() {
      List<Integer> l1 = lock.write(Function.identity());
      assertSame(list, l1);
      List<Integer> l2 = lock.write(Function.identity());
      assertNotSame(list, l2);
      for (int i = 0; i < 1000; i++) {
         assertSame(l1, lock.write(Function.identity()));
         assertSame(l2, lock.write(Function.identity()));
      }
   }
   
   @Test public void snapshot() {
      List<Integer> l1 = lock.snapshot();
      // readers share snapshot until writer actually comes along
      assertSame(l1, lock.snapshot());
      assertSame(l1, lock.snapshot());
      assertSame(l1, lock.read(Function.identity()));

      List<Integer> l2 = lock.write(l -> { l.add(l.size() + 1); return l; });
      assertNotSame(list, l2);
      List<Integer> l3 = lock.write(l -> { l.add(l.size() + 1); return l; });
      assertNotSame(list, l3);
      
      // snapshot not touched by above writes
      assertEquals(Arrays.asList(1, 2, 3, 4, 5), l1);
      // but locked instances were, of course
      assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), lock.read(Function.identity()));
   }
}
