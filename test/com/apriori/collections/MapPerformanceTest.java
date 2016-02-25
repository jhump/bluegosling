package com.apriori.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests the performance of various map implementations. The test is single-threaded and most of
 * the implementations under test are not thread-safe.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@Ignore
public class MapPerformanceTest {
   
   // These weights define the ratio of operations.
   private static final int WEIGHT_GET = 100;
   private static final int WEIGHT_PUT = 300;
   private static final int WEIGHT_REMOVE = 250;
   private static final int WEIGHT_ITERATE = 5;
   
   // Parameters around JIT warm-up
   private static final int NUM_WARMUP_CYCLES = 2_000_000;
   private static final long WARMUP_KEYSPACE_SIZE = 100_000;
   
   // Parameters for actual test run
   private static final int NUM_SECONDS_TO_TEST = 5;
   private static final int TEST_CYCLE_INCREMENT = 300;
   private static final long FULL_TEST_KEYSPACE_SIZE = 10_000_000;
   
   private Results[] createTests() {
      // all of the Map implementations under test
      return new Results[] {
            new Results(new LinearHashingMap<Long, Object>()),
            new Results(new LinkedHashMap<Long, Object>()),
            new Results(new TreeMap<Long, Object>()),
            new Results(new HamtMap<Long, Object>()),
            new Results(new HashMap<Long, Object>())
      };
   }
   
   private static class Results {
      Map<Long, Object> map;
      Random random;
      int numPuts;
      long maxPutNanos;
      long totalPutNanos;
      int numGets;
      long maxGetNanos;
      long totalGetNanos;
      int numRemoves;
      long maxRemoveNanos;
      long totalRemoveNanos;
      int numIterations;
      long totalMappingsIterated;
      long maxIterationNanos;
      long totalIterationNanos;

      Results(Map<Long, Object> map) {
         this.map = map;
         this.random = new Random(0);
      }
   }
   
   private int cumulativeWeightGet;
   private int cumulativeWeightPut;
   private int cumulativeWeightRemove;
   private int cumulativeWeightIterate;
   private Results results[];
   
   @Before public void setUp() {
      results = createTests();
      cumulativeWeightGet = WEIGHT_GET;
      cumulativeWeightPut = cumulativeWeightGet + WEIGHT_PUT;
      cumulativeWeightRemove = cumulativeWeightPut + WEIGHT_REMOVE;
      cumulativeWeightIterate = cumulativeWeightRemove + WEIGHT_ITERATE;
   }
   
   @Test public void testPerformance() throws Exception {
      warmUp();

      // reset all stats -- we don't about results from the warm-up period
      results = createTests();
      
      // crank each map through the ringer
      System.out.print("Testing\n");
      int total = NUM_SECONDS_TO_TEST * results.length;
      int c = -1;
      for (int i = 0; i < NUM_SECONDS_TO_TEST; i++) {
         for (Results r : results) {
            if (++c % 5 == 0 && c > 0) {
               System.out.printf(" %3.1f%%", (c * 100.0) / total);
            }
            // we interleave the tests for all maps, running each for 1 second at a time
            long startNanos = System.nanoTime();
            long endNanos = startNanos + TimeUnit.SECONDS.toNanos(1);
            do {
               run(TEST_CYCLE_INCREMENT, FULL_TEST_KEYSPACE_SIZE, r);
            } while (System.nanoTime() <= endNanos);
            // if operation weights mean we have lots of churn (puts and removes) then let's try
            // not to let GC influence timings by forcing GC outside of the timed test loop
            forceGc();
         }
      }
      System.out.println("\n");
      
      printResults();
   }
   
   private void warmUp() {
      System.out.print("Warming up...\n ");
      for (Results r : results) {
         System.out.print(" " + r.map.getClass().getSimpleName() + "...");
         // 2 million runs should be enough to get all code paths warmed up and
         // JITed with sufficient optimizations for subsequent test
         run(NUM_WARMUP_CYCLES, WARMUP_KEYSPACE_SIZE, r);
      }
      System.out.println("\n");

      for (Results r : results) {
         System.out.println("Initial size of map: " + r.map.size());
      }
      System.out.println();
   }
   
   private void run(int numberOfCycles, long keyspaceSize, Results r) {
      Map<Long, Object> map = r.map;
      Random rand = r.random;
      for (int i = 0; i < numberOfCycles; i++) {
         int c = rand.nextInt(cumulativeWeightIterate);
         if (c < cumulativeWeightGet) {
            long key = (rand.nextLong() & Long.MAX_VALUE) % keyspaceSize;
            long beforeNanos = System.nanoTime();
            map.get(key);
            long afterNanos = System.nanoTime();
            r.numGets++;
            long durationNanos = afterNanos - beforeNanos;
            r.maxGetNanos = Math.max(r.maxGetNanos, durationNanos);
            r.totalGetNanos += durationNanos;
         } else if (c < cumulativeWeightPut) {
            long key = (rand.nextLong() & Long.MAX_VALUE) % keyspaceSize;
            long beforeNanos = System.nanoTime();
            map.put(key, new Object());
            long afterNanos = System.nanoTime();
            r.numPuts++;
            long durationNanos = afterNanos - beforeNanos;
            r.maxPutNanos = Math.max(r.maxPutNanos, durationNanos);
            r.totalPutNanos += durationNanos;
         } else if (c < cumulativeWeightRemove) {
            long key = (rand.nextLong() & Long.MAX_VALUE) % keyspaceSize;
            long beforeNanos = System.nanoTime();
            map.remove(key);
            long afterNanos = System.nanoTime();
            r.numRemoves++;
            long durationNanos = afterNanos - beforeNanos;
            r.maxRemoveNanos = Math.max(r.maxRemoveNanos, durationNanos);
            r.totalRemoveNanos += durationNanos;
         } else {
            int n = 0;
            long beforeNanos = System.nanoTime();
            for (Iterator<?> iter = map.entrySet().iterator(); iter.hasNext();) {
               iter.next();
               n++;
            }
            long afterNanos = System.nanoTime();
            r.numIterations++;
            r.totalMappingsIterated += n;
            long durationNanos = afterNanos - beforeNanos;
            r.maxIterationNanos = Math.max(r.maxIterationNanos, durationNanos);
            r.totalIterationNanos += durationNanos;
         }
      }
   }
   
   private void printResults() {
      for (Results r : results) {
         System.out.println(r.map.getClass().getSimpleName());
         System.out.println("--------");
         System.out.printf("Final size of map: %,d%n", r.map.size());
         if (WEIGHT_GET != 0) {
            System.out.printf("Gets:       %,10d, Max: %,15dns, Avg: %,10.0fns%n",
                  r.numGets, r.maxGetNanos, r.totalGetNanos * 1.0 / r.numGets);
         }
         if (WEIGHT_PUT != 0) {
            System.out.printf("Puts:       %,10d, Max: %,15dns, Avg: %,10.0fns%n",
                  r.numPuts, r.maxPutNanos, r.totalPutNanos * 1.0 / r.numPuts);
         }
         if (WEIGHT_REMOVE != 0) {
            System.out.printf("Removes:    %,10d, Max: %,15dns, Avg: %,10.0fns%n",
                  r.numRemoves, r.maxRemoveNanos, r.totalRemoveNanos * 1.0 / r.numRemoves);
         }
         if (WEIGHT_ITERATE != 0) {
            double avgIterNanos = r.totalIterationNanos * 1.0 / r.numIterations;
            System.out.printf("Iterations: %,10d, Max: %,15dns, Avg: %,10.0fns%n",
                  r.numIterations, r.maxIterationNanos, avgIterNanos);
            double avgIterElements = r.totalMappingsIterated * 1.0 / r.numIterations; 
            System.out.printf("   Avg len: %,10.0f, Per Item, Max: %,5.0fns, Avg: %,10.0fns%n",
                  avgIterElements, r.maxIterationNanos * 1.0 / avgIterElements, avgIterNanos / avgIterElements);
         }
         System.out.println();
      }
   }

   private void forceGc() throws InterruptedException {
      CountDownLatch gcHappened = new CountDownLatch(100);
      for (int i = 0; i < 100; i++) {
         new Object() {
            @Override protected void finalize() throws Throwable {
               super.finalize();
               gcHappened.countDown();
            }
         };
      }
      while (true) {
         System.gc();
         if (gcHappened.await(100, TimeUnit.MILLISECONDS)) {
            return;
         }
      }
   }
}
