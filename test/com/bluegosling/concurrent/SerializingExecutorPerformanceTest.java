package com.bluegosling.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@Ignore
public class SerializingExecutorPerformanceTest {
   
   private static final ToLongFunction<ActorThreadPool<?>> ACTOR_THREAD_POOL_SHUTDOWN = e -> {
      e.shutdown();
      try {
         assertTrue(e.awaitTermination(1, TimeUnit.SECONDS));
      } catch (InterruptedException ex) {
         throw new AssertionError(ex);
      }
      return e.getBatchCount();
   };
   
   private static final Consumer<ActorThreadPool<?>> ACTOR_THREAD_POOL_STATS = e -> {
      System.out.println("steal count = " + e.getStealCount());
   };
   
   private static final ToLongFunction<PipeliningExecutor<?>> PIPELINING_EXECUTOR_SHUTDOWN = e -> {
      ThreadPoolExecutor tpe = (ThreadPoolExecutor) e.executor;
      try {
         e.awaitQuiescence();
         tpe.shutdown();
         assertTrue(tpe.awaitTermination(1, TimeUnit.SECONDS));
      } catch (InterruptedException ex) {
         throw new AssertionError(ex);
      }
      return tpe.getTaskCount();
   };
   
   private final Consumer<PipeliningExecutor<?>> PIPELINING_EXECUTOR_STATS = e -> {
   };
   
   @Test public void actorThreadPool_noBatching() throws Exception {
      // warm-up
      doTest(null, ActorThreadPool.newBuilder().setPoolSize(2).setMaxBatchSize(1).build(),
            ACTOR_THREAD_POOL_SHUTDOWN, ACTOR_THREAD_POOL_STATS, 4, 1, 5_000, true);
      
      // real deal
      doTest("ActorThreadPool - no batching",
            ActorThreadPool.newBuilder().setPoolSize(8).setMaxBatchSize(1).build(),
            ACTOR_THREAD_POOL_SHUTDOWN, ACTOR_THREAD_POOL_STATS, 50, 3, 20_000, false);
   }

   @Test public void actorThreadPool_defaultBatch() throws Exception {
      // warm-up
      doTest(null, ActorThreadPool.newBuilder().setPoolSize(2).build(), ACTOR_THREAD_POOL_SHUTDOWN,
            ACTOR_THREAD_POOL_STATS, 4, 1, 5_000, true);
      
      // real deal
      doTest("ActorThreadPool - default batch size",
            ActorThreadPool.newBuilder().setPoolSize(8).build(), ACTOR_THREAD_POOL_SHUTDOWN,
            ACTOR_THREAD_POOL_STATS, 50, 3, 20_000, false);
   }
   
   @Test public void pipeliningExecutor_noBatching() throws Exception {
      // warm-up
      ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
      doTest(null, new PipeliningExecutor<>(tpe, 1), PIPELINING_EXECUTOR_SHUTDOWN,
            PIPELINING_EXECUTOR_STATS, 4, 1, 5_000, true);

      // real deal
      tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
      doTest("PipeliningExecutor - no batching", new PipeliningExecutor<>(tpe, 1),
            PIPELINING_EXECUTOR_SHUTDOWN, PIPELINING_EXECUTOR_STATS, 50, 3, 20_000, false);
   }
   
   @Test public void pipeliningExecutor_defaultBatch() throws Exception {
      // warm-up
      ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
      doTest(null, new PipeliningExecutor<>(tpe), PIPELINING_EXECUTOR_SHUTDOWN,
            PIPELINING_EXECUTOR_STATS, 4, 1, 5_000, true);

      // real deal
      tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
      doTest("PipeliningExecutor - default batch size", new PipeliningExecutor<>(tpe),
            PIPELINING_EXECUTOR_SHUTDOWN, PIPELINING_EXECUTOR_STATS, 50, 3, 20_000, false);
   }
   
   <E extends SerializingExecutor<Integer>> void doTest(String testName, E executor,
         ToLongFunction<? super E> shutdown, Consumer<? super E> printStats, int numActors,
         int numProducerThreads, long durationMillis, boolean warmup) throws Exception {
      CountDownLatch ready = new CountDownLatch(numProducerThreads);
      CountDownLatch go = new CountDownLatch(1);
      AtomicBoolean done = new AtomicBoolean();
      LongAdder produced = new LongAdder();
      Thread producers[] = new Thread[numProducerThreads];
      ConcurrentMap<Integer, AtomicLong> expectedCounts = new ConcurrentHashMap<>();
      ConcurrentMap<Integer, LongSummaryStatistics> actualCounts = new ConcurrentHashMap<>();
      ConcurrentMap<Integer, AtomicBoolean> actorStates = new ConcurrentHashMap<>();
      for (int i = 0; i < numProducerThreads; i++) {
         producers[i] = new Thread(() -> {
            ready.countDown();
            try {
               go.await();
            } catch (InterruptedException e) {
               throw new AssertionError(e);
            }
            while (!done.get()) {
               int actor = ThreadLocalRandom.current().nextInt(numActors); 
               ListenableFuture<?> future = executor.submit(actor,
                     newTask(actor, expectedCounts, actualCounts, actorStates));
               produced.increment();
               for (int j = 0; j < 100; j++) {
                  executor.execute(actor,
                        newTask(actor, expectedCounts, actualCounts, actorStates));
                  produced.increment();
               }
               try {
                  // block for one of the tasks so we don't overload the actor queues
                  future.get();
               } catch (Exception e) {
                  throw new AssertionError(e);
               }
            }
         });
         producers[i].start();
      }
      
      ready.await();
      long startNanos = System.nanoTime();
      go.countDown();
      Thread.sleep(durationMillis);
      done.set(true);
      long endNanos = System.nanoTime();
      for (Thread th : producers) {
         th.join();
      }
      long numTasks = shutdown.applyAsLong(executor);
      long finalNanos = System.nanoTime();
      
      Map<Integer, Long> expected = new HashMap<>();
      Map<Integer, Long> actual = new HashMap<>();
      expectedCounts.forEach((k, v) -> expected.put(k, v.get()));
      actualCounts.forEach((k, v) -> actual.put(k, v.getCount()));
      
      assertEquals(expected, actual);
      long actualTaskCount = actual.values().stream().mapToLong(i -> i).sum();
      assertEquals(produced.longValue(), actualTaskCount);
      
      if (warmup) {
         return; // don't bother printing stats for warm-up run
      }

      System.out.println(testName);
      System.out.println("----------------------------------");

      System.out.println(actual);
      System.out.println("producing time = " + (endNanos - startNanos) + "ns");
      System.out.println("total time = " + (finalNanos - startNanos) + "ns");
      System.out.println("total task count = " + actualTaskCount);
      System.out.println("executor task count = " + numTasks);
      
      System.out.println("latency stats = " + actualCounts.values().stream()
            .collect(Collectors.reducing((s1, s2) -> {
               s1.combine(s2);
               return s1;
            })).get());
      
      printStats.accept(executor);
      
      System.out.println();
   }
   
   Runnable newTask(int actor, ConcurrentMap<Integer, AtomicLong> expected,
         ConcurrentMap<Integer, LongSummaryStatistics> actual,
         ConcurrentMap<Integer, AtomicBoolean> states) {
      expected.computeIfAbsent(actor, a -> new AtomicLong()).incrementAndGet();
      long submittedNanos = System.nanoTime();
      return () -> {
         long startedNanos = System.nanoTime();
         AtomicBoolean b = states.computeIfAbsent(actor, a -> new AtomicBoolean());
         boolean started = b.compareAndSet(false, true);
         if (!started) {
            System.err.println(Thread.currentThread().getName() + ": Actor " + actor
                  + " is already running!");
         }
         actual.computeIfAbsent(actor, a -> new LongSummaryStatistics())
               .accept(startedNanos - submittedNanos);
         if (!b.compareAndSet(true, false) && started) {
            // successfully set to true, but did not successfully set to false
            System.err.println(Thread.currentThread().getName() + ": Actor " + actor
                  + " has already stopped?");
         }
      };
   }
}
