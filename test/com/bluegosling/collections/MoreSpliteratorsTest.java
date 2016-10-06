package com.bluegosling.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.bluegosling.function.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Uninterruptibles;

@RunWith(Parameterized.class)
public class MoreSpliteratorsTest {
   private static final int FORK_SEQUENTIAL_NUM_ELEMENTS = 10_000;
   private static final int FORK_SEQUENTIAL_NUM_FORKS = 160;
   private static final int FORK_PARALLEL_NUM_ELEMENTS = 100_000;
   private static final int FORK_PARALLEL_NUM_FORKS = 16;
   private static final float FORK_CHANCE_TO_SPLIT = 0.8f;

   /**
    * We test spliterators with various characteristics by using different kinds of sources.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private enum SourceType {
      /**
       * Array lists have ordered, sized, and sub-sized spliterators. 
       */
      LIST(ArrayList::new, ArrayList<Integer>::new),
      
      /**
       * Tree sets have ordered, sorted, distinct, and sized spliterators.
       */
      TREE_SET(TreeSet::new, TreeSet<Integer>::new),
      
      /**
       * Hash sets have distinct and sized spliterators.
       */
      HASH_SET(HashSet::new, HashSet<Integer>::new),
      
      /**
       * Hash sets have ordered, distinct, and sized spliterators.
       */
      LINKED_HASH_SET(LinkedHashSet::new, LinkedHashSet<Integer>::new),
      
      /**
       * Concurrent linked queues have concurrent, ordered, and non-null spliterators.
       */
      CONCURRENT_QUEUE(ConcurrentLinkedQueue::new, ConcurrentLinkedQueue<Integer>::new,
            Iterables::elementsEqual),
      
      /**
       * Concurrent hash map key sets have concurrent, distinct, non-null, and sized spliterators.
       */
      CONCURRENT_SET(ConcurrentHashMap::newKeySet);
      
      private final Supplier<? extends Collection<Integer>> factory;
      private final Function<? super Collection<Integer>, ? extends Collection<Integer>> copy;
      private final BiPredicate<Collection<Integer>, Collection<Integer>> equals;

      <C extends Collection<Integer>> SourceType(Supplier<C> factory) {
         this.factory = factory;
         this.copy = coll -> {
            Collection<Integer> copy = factory.get();
            copy.addAll(coll);
            return copy;
         };
         this.equals = Collection<Integer>::equals;
      }

      <C extends Collection<Integer>> SourceType(Supplier<C> factory,
            Function<? super Collection<Integer>, ? extends C> copy) {
         this.factory = factory;
         this.copy = copy;
         this.equals = Collection<Integer>::equals;
      }

      <C extends Collection<Integer>> SourceType(Supplier<C> factory,
            Function<? super Collection<Integer>, ? extends C> copy,
            BiPredicate<Collection<Integer>, Collection<Integer>> equals) {
         this.factory = factory;
         this.copy = copy;
         this.equals = equals;
      }

      Collection<Integer> newCollection() {
         return factory.get();
      }
      
      Collection<Integer> copyCollection(Collection<Integer> c) {
         return copy.apply(c);
      }
      
      boolean areEqual(Collection<Integer> coll, Collection<Integer> other) {
         return equals.test(coll, other);
      }
   }
   
   /**
    * Some of the tests use this list of shuffled integers between 0 and 256.
    */
   private static final List<Integer> INTS;
   static {
      List<Integer> ints = IntStream.range(0, 256)
            .mapToObj(i -> i)
            .collect(Collectors.toList());
      Collections.shuffle(ints);
      INTS = Collections.unmodifiableList(ints);
   }
   
   /**
    * Returns the set of spliterator sources as test parameters. We run tests for each kind of
    * source.
    * 
    * @return the set of spliterator sources
    */
   @Parameters
   public static List<Object[]> parameters() {
      return Arrays.stream(SourceType.values())
            .map(s -> new SourceType[] { s })
            .collect(Collectors.toList());
   }

   private final SourceType sourceType;
   private Random random;
   private ExecutorService executor;

   public MoreSpliteratorsTest(SourceType sourceType) {
      this.sourceType = sourceType;
   }

   @Before public void setup() {
      random = new Random(1);
      executor = Executors.newCachedThreadPool();
   }
   
   @After public void tearDown() {
      executor.shutdownNow();
   }
   
   @Test public void map() {
      Collection<Integer> source = sourceType.copyCollection(INTS);

      for (int depth = 0; depth < 4; depth++) {
         Spliterator<Integer> mapped = MoreSpliterators.map(source.spliterator(), i -> i * i);
         Spliterator<Integer> example = testMap(source.spliterator(), (Integer i) -> i * i);
         checkSpliterator(mapped, example, depth, false);
         
         mapped = MoreSpliterators.map(source.spliterator(), i -> i);
         example = source.spliterator();
         checkSpliterator(mapped, example, depth, true);
      }
   }
   
   private <T> Spliterator<T> testMap(Spliterator<Integer> split, Function<Integer, T> fn) {
      return StreamSupport.stream(split, false)
            .map(fn)
            .spliterator();
   }
   
   @Test public void filter() {
      Collection<Integer> source = sourceType.copyCollection(INTS);

      for (int depth = 0; depth < 4; depth++) {
         Spliterator<Integer> mapped = MoreSpliterators.filter(source.spliterator(),
               i -> i % 2 == 0);
         Spliterator<Integer> example = testFilter(source.spliterator(), i -> i % 2 == 0);
         checkSpliterator(mapped, example, depth, false);
         
         mapped = MoreSpliterators.filter(source.spliterator(), Predicates.alwaysAccept());
         example = source.spliterator();
         checkSpliterator(mapped, example, depth, true);
      }
   }

   private Spliterator<Integer> testFilter(Spliterator<Integer> split, Predicate<Integer> filter) {
      return StreamSupport.stream(split, false)
            .filter(filter)
            .spliterator();
   }
   
   private <T> void checkSpliterator(Spliterator<T> test, Spliterator<T> bench, int depth,
         boolean strict) {
      Supplier<Collection<T>> outputFactory =
            test.hasCharacteristics(Spliterator.ORDERED)
                  && bench.hasCharacteristics(Spliterator.ORDERED)
            ? () -> new ArrayList<>()
            : () -> new HashSet<>();
      if (strict) {
         // verifies that the splits are the same, as well as actual elements and encounter order
         if (depth == 0) {
            Collection<T> testOut = outputFactory.get();
            while (test.tryAdvance(testOut::add));
            Collection<T> benchOut = outputFactory.get();
            while (bench.tryAdvance(benchOut::add));
            assertEquals(benchOut, testOut);
         } else {
            Spliterator<T> testSplit = test.trySplit();
            Spliterator<T> benchSplit = bench.trySplit();
            assertEquals(benchSplit == null, testSplit == null);
            if (testSplit != null) {
               checkSpliterator(testSplit, benchSplit, depth - 1, true);
            }
            checkSpliterator(test, bench, depth - 1, true);
         }
      } else {
         // just verifies that emitted elements and their encounter order are the same
         Collection<T> testOut = outputFactory.get();
         consumeSpliterator(test, testOut, depth);
         Collection<T> benchOut = outputFactory.get();
         consumeSpliterator(bench, benchOut, depth);
         assertEquals(benchOut, testOut);
      }
   }

   private <T> void consumeSpliterator(Spliterator<T> split, Collection<T> output, int depth) {
      if (depth == 0) {
         while (split.tryAdvance(output::add));
         return;
      }
      Spliterator<T> child = split.trySplit();
      if (child != null) {
         consumeSpliterator(child, output, depth - 1);
      }
      consumeSpliterator(split, output, depth - 1);
   }

   @Test public void fork_sequential() throws Exception {
      doForkTest(false);
   }

   @Test public void fork_parallel() throws Exception {
      doForkTest(true);
   }

   private void doForkTest(boolean parallel) throws Exception {
      int numElements = parallel ? FORK_PARALLEL_NUM_ELEMENTS : FORK_SEQUENTIAL_NUM_ELEMENTS;
      int numForks = parallel ? FORK_PARALLEL_NUM_FORKS : FORK_SEQUENTIAL_NUM_FORKS;
      
      Collection<Integer> values = random.ints(numElements)
            .sorted()
            .mapToObj(Integer::valueOf)
            .collect(Collectors.toCollection(sourceType::newCollection));
      Spliterator<Integer> source = values.spliterator();
      
      Supplier<Spliterator<Integer>> forks = MoreSpliterators.fork(source, numForks);
      List<Collection<Integer>> outputs = new ArrayList<>(numForks);
      
      // these are for diagnostics, for troubleshooting cases where we've broken the test
      int[] actualSplits = new int[numForks];
      int[] attemptedSplits = new int[numForks];

      CountDownLatch ready = new CountDownLatch(numForks);
      CountDownLatch start = new CountDownLatch(parallel ? 1 : 0);
      Runnable[] forkTasks = new Runnable[numForks];
      
      for (int i = 0; i < numForks; i++) {
         Collection<Integer> out = sourceType.newCollection();
         outputs.add(out);
         final int finalIndex = i;
         
         forkTasks[i] = () -> {
            ready.countDown();
            Uninterruptibles.awaitUninterruptibly(start);

            Spliterator<Integer> fork = forks.get();
            
            Spliterator<Integer> other;
            if (finalIndex == 0 || random.nextFloat() < FORK_CHANCE_TO_SPLIT) {
               other = fork.trySplit();
               attemptedSplits[finalIndex]++;
            } else {
               other = null;
            }
            if (other != null) {
               actualSplits[finalIndex]++;
               process(other, out, actualSplits, attemptedSplits, finalIndex);
            }
            process(fork, out, actualSplits, attemptedSplits, finalIndex);
         };
      }
      
      if (parallel) {
         List<Future<?>> futures = new ArrayList<>(numForks);
         for (Runnable forkTask : forkTasks) {
            futures.add(executor.submit(forkTask));
         }
         ready.await();
         start.countDown();
         for (Future<?> f : futures) {
            f.get();
         }
         
      } else {
         for (Runnable forkTask : forkTasks) {
            forkTask.run();
         }
      }
      
      for (int i = 0; i < numForks; i++) {
//         System.out.printf("Output #%d: %d/%d splits, %d items\n",
//               i, actualSplits[i], attemptedSplits[i], outputs.get(i).size());
         if (numElements < 100) {
            assertTrue("Output stream #" + i + " incorrect: expected "
                  + values + " but got " + outputs.get(i),
                  sourceType.areEqual(values, outputs.get(i)));
         } else {
            assertTrue("Output stream #" + i + " incorrect.",
                  sourceType.areEqual(values, outputs.get(i)));
         }
      }
   }
   
   private <C extends Collection<Integer>> void process(Spliterator<Integer> split, C out,
         int[] actualSplits, int[] attemptedSplits, int splitsIndex) {
      Spliterator<Integer> other;
      if (random.nextFloat() < FORK_CHANCE_TO_SPLIT) {
         other = split.trySplit();
         attemptedSplits[splitsIndex]++;
      } else {
         other = null;
      }
      if (other != null) {
         actualSplits[splitsIndex]++;
         process(other, out, actualSplits, attemptedSplits, splitsIndex);
         process(split, out, actualSplits, attemptedSplits, splitsIndex);
      } else {
         while (split.tryAdvance(out::add));
      }
   }
}
