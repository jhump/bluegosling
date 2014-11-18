package com.apriori.concurrent;

import com.apriori.util.Stopwatch;

import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

// TODO: real tests!
public class RateLimiterTest {

   @Test public void test() throws Exception {
      test1();
      test1();
      test1();
      test1();
      test1();
      test1();
   }

   @Test public void test1() throws Exception {
      int numThreads = 5;
      Stopwatch sw = new Stopwatch();
      AtomicReference<RateLimiter> rl = new AtomicReference<>();
      CountDownLatch ready = new CountDownLatch(numThreads);
      CountDownLatch go = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(numThreads);
      for (int i = 0; i < numThreads; i++) {
         new Thread(() -> {
            ready.countDown();
            try {
               go.await();
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            }
            RateLimiter l = rl.get();
            for (int j = 0; j < 1; j++) {
               l.acquire(100);
            }
            done.countDown();
         }).start();
      }
      ready.await();
      RateLimiter l = new RateLimiter(100, 1, 0, 1.0);
      rl.set(l);
      sw.start();
      go.countDown();
      done.await();
      System.out.println(sw);
   }

   @Test public void test2() throws Exception {
      Stopwatch sw = new Stopwatch();
      AtomicReference<RateLimiter> rl = new AtomicReference<>();
      Random r = new Random();
      CountDownLatch ready = new CountDownLatch(3);
      CountDownLatch go = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(3);
      for (int i = 0; i < 3; i++) {
         new Thread(() -> {
            ready.countDown();
            try {
               go.await();
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            }
            RateLimiter l = rl.get();
            Thread th = Thread.currentThread();
            for (int j = 0; j < 10; j++) {
               int p = r.nextInt(10) + 1;
               l.acquire(p);
               System.out.println("" + th + " got " + p + " permits @ " + sw);
            }
            done.countDown();
         }).start();
      }
      ready.await();
      RateLimiter l = new RateLimiter(10, 1, 0); 
      rl.set(l);
      sw.start();
      go.countDown();
//      Thread.sleep(1500);
//      l.setRate(20);
      done.await();
   }
}
