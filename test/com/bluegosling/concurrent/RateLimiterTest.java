package com.bluegosling.concurrent;

import org.junit.Test;

import com.bluegosling.time.Stopwatch;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

// TODO: real tests!
public class RateLimiterTest {

   @Test public void test() throws Exception {
      for (int i = 0; i < 20; i++) {
         test1();
      }
   }

   @Test public void test1() throws Exception {
      int numThreads = 1;
      int qps = 9_900_000;
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
            for (int j = 0; j < qps; j++) {
               l.acquire();
            }
            done.countDown();
         }).start();
      }
      ready.await();
      RateLimiter l = new RateLimiter(qps, 1, 0);
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
            for (int j = 0; j < 100; j++) {
               int p = 1; //r.nextInt(10) + 1;
               l.acquire(p);
               System.out.println("" + th + " got " + p + " permits @ " + sw);
            }
            done.countDown();
         }).start();
      }
      ready.await();
      RateLimiter l = new RateLimiter(100, 1, 0); 
      rl.set(l);
      sw.start();
      go.countDown();
      Thread.sleep(1500);
      l.setRate(20);
      done.await();
   }
}
