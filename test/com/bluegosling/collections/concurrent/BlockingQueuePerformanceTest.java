package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.concurrent.LockFreeArrayBlockingQueue;
import com.bluegosling.collections.concurrent.LockFreeLinkedBlockingDeque;
import com.bluegosling.collections.concurrent.LockFreeLinkedBlockingQueue;
import com.bluegosling.collections.concurrent.LockFreeSkipListBlockingQueue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Ignore
public class BlockingQueuePerformanceTest {
   AtomicLong l = new AtomicLong();
   
   // These weights define the ratio of operations.
   private static final int WEIGHT_PEEK = 50;
   private static final int WEIGHT_OFFER = 550;
   private static final int WEIGHT_POLL = 500;
   private static final int WEIGHT_ITERATE = 5;
   
   // Parameters around JIT warm-up
   private static final int NUM_WARMUP_CYCLES = 2_000_000;
   private static final long WARMUP_KEYSPACE_SIZE = 100_000;
   
   // Parameters for actual test run
   private static final int NUM_SECONDS_TO_TEST = 5;
   private static final int TEST_CYCLE_INCREMENT = 300;
   private static final long FULL_TEST_KEYSPACE_SIZE = 10_000_000;
   
   // Number of parallel threads, for multi-threaded tests
   private static final int NUM_THREADS = 8;
   
   // Max capacity of queues for tests of bounded-length queues.
   private static final int MAX_CAPACITY = 100_000;
   
   private Results[] createTestsUnbounded() {
      // all of the BlockingQueue implementations under test that support unbounded queue
      return new Results[] {
            new Results(new LinkedBlockingQueue<>()),
            new Results(new LinkedBlockingDeque<>()),
            new Results(new LockFreeLinkedBlockingQueue<>()),
            new Results(new LockFreeLinkedBlockingDeque<>())
      };
   }

   private Results[] createTestsBounded(int capacity) {
      // all of the BlockingQueue implementations under test that support bounded queue
      return new Results[] {
            new Results(new LinkedBlockingQueue<>(capacity)),
            new Results(new LinkedBlockingDeque<>(capacity)),
            new Results(new LockFreeLinkedBlockingQueue<>(capacity)),
            new Results(new LockFreeLinkedBlockingDeque<>(capacity)),
            new Results(new ArrayBlockingQueue<>(capacity)),
            new Results(new LockFreeArrayBlockingQueue<>(capacity)),
            new Results(new ArrayBlockingDeque<>(capacity))
      };
   }

   private Results[] createTestsOrdered() {
      // all of the BlockingQueue implementations under test that are ordered
      return new Results[] {
            new Results(new PriorityBlockingQueue<>()),
            new Results(new LockFreeSkipListBlockingQueue<>())
      };
   }

   private static class Results {
      BlockingQueue<Long> queue;
      Random random;
      int numOffers;
      long maxOfferNanos;
      long totalOfferNanos;
      int numPeeks;
      long maxPeekNanos;
      long totalPeekNanos;
      int numPolls;
      long maxPollNanos;
      long totalPollNanos;
      int numIterations;
      long totalMappingsIterated;
      long maxIterationNanos;
      long totalIterationNanos;

      Results(BlockingQueue<Long> queue) {
         this.queue = queue;
         this.random = new Random(0);
      }

      void add(Results other) {
         numOffers += other.numOffers;
         maxOfferNanos = Math.max(maxOfferNanos, other.maxOfferNanos);
         totalOfferNanos += other.totalOfferNanos;
         numPeeks += other.numPeeks;
         maxPeekNanos = Math.max(maxPeekNanos, other.maxPeekNanos);
         totalPeekNanos += other.totalPeekNanos;
         numPolls += other.numPolls;
         maxPollNanos = Math.max(maxPollNanos, other.maxPollNanos);
         totalPollNanos += other.totalPollNanos;
         numIterations += other.numIterations;
         totalMappingsIterated += other.totalMappingsIterated;
         maxIterationNanos = Math.max(maxIterationNanos, other.maxIterationNanos);
         totalIterationNanos += other.totalIterationNanos;
      }
   }
   
   private int cumulativeWeightPeek;
   private int cumulativeWeightOffer;
   private int cumulativeWeightPoll;
   private int cumulativeWeightIterate;
   private Results results[];
   
   @Before public void setup() {
      cumulativeWeightPeek = WEIGHT_PEEK;
      cumulativeWeightOffer = cumulativeWeightPeek + WEIGHT_OFFER;
      cumulativeWeightPoll = cumulativeWeightOffer + WEIGHT_POLL;
      cumulativeWeightIterate = cumulativeWeightPoll + WEIGHT_ITERATE;
   }
   
   @Test public void testPerformance_singleThread_bounded() throws Exception {
      doTestPerformance_singleThread(() -> createTestsBounded(MAX_CAPACITY), false);
   }
   
   @Test public void testPerformance_singleThread_unbounded() throws Exception {
      doTestPerformance_singleThread(() -> createTestsUnbounded(), false);
   }
   
   @Test public void testPerformance_singleThread_ordered() throws Exception {
      doTestPerformance_singleThread(() -> createTestsOrdered(), true);
   }
   
   @Test public void testPerformance_multiThread_bounded() throws Exception {
      doTestPerformance_multiThread(() -> createTestsBounded(MAX_CAPACITY), false);
   }

   @Test public void testPerformance_multiThread_unbounded() throws Exception {
      doTestPerformance_multiThread(() -> createTestsUnbounded(), false);
   }

   @Test public void testPerformance_multiThread_ordered() throws Exception {
      doTestPerformance_multiThread(() -> createTestsOrdered(), true);
   }

   private void doTestPerformance_singleThread(Supplier<Results[]> creator, boolean ordered)
         throws Exception {
      results = creator.get();
      warmUp(ordered);

      // reset all stats -- we don't about results from the warm-up period
      results = creator.get();
      
      // crank each queue through the ringer
      System.out.print("Testing\n");
      int total = NUM_SECONDS_TO_TEST * results.length;
      int c = -1;
      for (int i = 0; i < NUM_SECONDS_TO_TEST; i++) {
         for (Results r : results) {
            if (++c % 5 == 0 && c > 0) {
               System.out.printf(" %3.1f%%", (c * 100.0) / total);
            }
            // we interleave the tests for all queue, running each for 1 second at a time
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
   
   private void doTestPerformance_multiThread(Supplier<Results[]> creator, boolean ordered)
         throws Exception {
      results = creator.get();
      warmUp(ordered);
      
      int publisherCounts[] = new int[] { 1, NUM_THREADS, NUM_THREADS / 2, 1 };
      int consumerCounts[] = new int[] { 1, 1, NUM_THREADS / 2, NUM_THREADS };
      
      for (int i = 0; i < publisherCounts.length; i++) {
         int numPublishers = publisherCounts[i];
         int numConsumers = consumerCounts[i];
         int numThreads = numPublishers + numConsumers;
         
         Results threadedResults[][] = new Results[numThreads][];
         results = creator.get();
         for (int t = 0; t < numThreads; t++) {
            threadedResults[t] = creator.get();
            for (int j = 0; j < results.length; j++) {
               // make sure all threads are using the same queue
               threadedResults[t][j].queue = results[j].queue;
            }
         }

         // crank each queue through the ringer
         System.out.printf("Testing with %d publishers and %d consumers\n",
               numPublishers, numConsumers);
         int total = NUM_SECONDS_TO_TEST * results.length;
         int c = 0;

         for (int j = 0; j < results.length; j++) {
            CountDownLatch ready = new CountDownLatch(numThreads);
            CountDownLatch go = new CountDownLatch(1);
            AtomicBoolean stop = new AtomicBoolean();
            Thread threads[] = new Thread[numThreads];
            for (int t = 0; t < numThreads; t++) {
               Results currentResults = threadedResults[t][j];
               Runnable r;
               if (t < numPublishers) {
                  // spin up producer thread
                  r = () -> runProducer(TEST_CYCLE_INCREMENT, currentResults);
               } else {
                  // spin up consumer thread
                  r = () -> runConsumer(TEST_CYCLE_INCREMENT, currentResults);
               }
               threads[t] = new Thread(() -> {
                  ready.countDown();
                  try {
                     go.await();
                     while (!stop.get()) {
                        r.run();
                     }
                  } catch (InterruptedException e) {
                     throw new AssertionError(e);
                  }
               });
               threads[t].start();
            }
            
            ready.await();
            go.countDown();
            Thread.sleep(TimeUnit.SECONDS.toMillis(NUM_SECONDS_TO_TEST));
            c += NUM_SECONDS_TO_TEST;
            System.out.printf(" %3.1f%%", (c * 100.0) / total);
            stop.set(true);
            for (Thread th : threads) {
               // it may be waiting to put or take, so interrupt
               th.interrupt();
               th.join();
            }

            // combine results from all threads
            for (int t = 0; t < numThreads; t++) {
               results[j].add(threadedResults[t][j]);
            }
         }
         System.out.println("\n");
         
         printResults();
      }
   }
   
   private void warmUp(boolean ordered) {
      System.out.print("Warming up...\n ");
      for (Results r : results) {
         System.out.print(" " + r.queue.getClass().getSimpleName() + "...");
         // 2 million runs should be enough to get all code paths warmed up and
         // JITed with sufficient optimizations for subsequent test
         run(NUM_WARMUP_CYCLES, WARMUP_KEYSPACE_SIZE, r);
      }
      System.out.println("\n");

      List<List<Long>> asLists = new ArrayList<>(results.length);
      for (int i = 0; i < results.length; i++) {
         Results r = results[i];
         System.out.println("Initial size of queue: " + r.queue.size());
         asLists.add(new ArrayList<>(r.queue));
      }

      int sorted = -1;
      for (int i = 0; i < results.length; i++) {
         for (int j = i + 1; j < results.length; j++) {
            BlockingQueue<Long> q1 = results[i].queue;
            BlockingQueue<Long> q2 = results[j].queue;
            List<Long> l1 = asLists.get(i);
            List<Long> l2 = asLists.get(j);
            if (ordered) {
               // lists will be in iteration order; we expect first element to be the one that
               // would be poll'ed so they should match
               Long head1 = l1.get(0);
               Long head2 = l2.get(0);
               if (!head1.equals(head2)) {
                  System.err.println("Warm-up resulted in unequal results for "
                        + q1.getClass().getSimpleName() + " and "
                        + q2.getClass().getSimpleName());
               }
               // rest of queue has no promises about order; so we'll check for equality by
               // sorting to make sure they have same contents, regardless of order
               if (sorted < i) {
                  l1.sort(Long::compare);
               }
               if (sorted < j) {
                  l2.sort(Long::compare);
               }
               sorted = j;
               if (!l1.equals(l2)) {
                  System.err.println("Warm-up resulted in unequal results for "
                        + q1.getClass().getSimpleName() + " and "
                        + q2.getClass().getSimpleName());
               }
            } else {
               // if not ordered, then iteration order should represent FIFO order, and be the same
               // across all implementations
               if (!l1.equals(l2)) {
                  System.err.println("Warm-up resulted in unequal results for "
                        + q1.getClass().getSimpleName() + " and "
                        + q2.getClass().getSimpleName());
               }
            }
         }
      }
      
      System.out.println();
   }
   
   private void run(int numberOfCycles, long keyspaceSize, Results r) {
      BlockingQueue<Long> queue = r.queue;
      Random rand = r.random;
      for (int i = 0; i < numberOfCycles; i++) {
         int c = rand.nextInt(cumulativeWeightIterate);
         if (c < cumulativeWeightPeek) {
            long beforeNanos = System.nanoTime();
            queue.peek();
            long afterNanos = System.nanoTime();
            r.numPeeks++;
            long durationNanos = afterNanos - beforeNanos;
            r.maxPeekNanos = Math.max(r.maxPeekNanos, durationNanos);
            r.totalPeekNanos += durationNanos;
         } else if (c < cumulativeWeightOffer) {
            long key = (rand.nextLong() & Long.MAX_VALUE) % keyspaceSize;
            long beforeNanos = System.nanoTime();
            queue.offer(key);
            long afterNanos = System.nanoTime();
            r.numOffers++;
            long durationNanos = afterNanos - beforeNanos;
            r.maxOfferNanos = Math.max(r.maxOfferNanos, durationNanos);
            r.totalOfferNanos += durationNanos;
         } else if (c < cumulativeWeightPoll) {
            long beforeNanos = System.nanoTime();
            queue.poll();
            long afterNanos = System.nanoTime();
            r.numPolls++;
            long durationNanos = afterNanos - beforeNanos;
            r.maxPollNanos = Math.max(r.maxPollNanos, durationNanos);
            r.totalPollNanos += durationNanos;
         } else {
            int n = 0;
            long beforeNanos = System.nanoTime();
            for (Iterator<?> iter = queue.iterator(); iter.hasNext();) {
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
   
   private void runProducer(int numberOfCycles, Results r) {
      for (int i = 0; i < numberOfCycles; i++) {
         long key = l.getAndIncrement();
         long beforeNanos = System.nanoTime();
         try {
            r.queue.put(key);
         } catch (InterruptedException e) {
            return; // bail
         } finally {
            long afterNanos = System.nanoTime();
            r.numOffers++;
            long durationNanos = afterNanos - beforeNanos;
            r.maxOfferNanos = Math.max(r.maxOfferNanos, durationNanos);
            r.totalOfferNanos += durationNanos;
         }
      }
   }

   private void runConsumer(int numberOfCycles, Results r) {
      for (int i = 0; i < numberOfCycles; i++) {
         long beforeNanos = System.nanoTime();
         try {
            r.queue.take();
         } catch (InterruptedException e) {
            return; // bail
         } finally {
            long afterNanos = System.nanoTime();
            r.numPolls++;
            long durationNanos = afterNanos - beforeNanos;
            r.maxPollNanos = Math.max(r.maxPollNanos, durationNanos);
            r.totalPollNanos += durationNanos;
         }
      }      
   }

   private void printResults() {
      for (Results r : results) {
         System.out.println(r.queue.getClass().getSimpleName());
         System.out.println("--------");
         System.out.printf("Final size of queue: %,d%n", r.queue.size());
         if (r.numPeeks != 0) {
            System.out.printf("Peeks:      %,10d, Max: %,15dns, Avg: %,10.0fns%n",
                  r.numPeeks, r.maxPeekNanos, r.totalPeekNanos * 1.0 / r.numPeeks);
         }
         if (r.numOffers != 0) {
            System.out.printf("Offers:     %,10d, Max: %,15dns, Avg: %,10.0fns%n",
                  r.numOffers, r.maxOfferNanos, r.totalOfferNanos * 1.0 / r.numOffers);
         }
         if (r.numPolls != 0) {
            System.out.printf("Polls:      %,10d, Max: %,15dns, Avg: %,10.0fns%n",
                  r.numPolls, r.maxPollNanos, r.totalPollNanos * 1.0 / r.numPolls);
         }
         if (r.numIterations != 0) {
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
