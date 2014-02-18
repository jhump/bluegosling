// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import org.apache.commons.collections.BulkTest;
import org.apache.commons.collections.set.AbstractTestSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestSuite;

/**
 * Tests the implementation of concurrent {@link Set}s returned from
 * {@link ShardedConcurrentSets#withSet}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ShardedConcurrentSetTest extends AbstractTestSet {

   /**
    * Creates a new test suite that includes all test cases (including Apache {@code BulkTest}s,
    * which recursively include cases for sub-sets, etc.).
    *
    * @return a test suite that includes all test cases
    */
   public static TestSuite suite() {
      return makeSuite(ShardedConcurrentSetTest.class);
   }
   
   /**
    * Constructs a new test.
    *
    * @param name the name of the test case
    */
   public ShardedConcurrentSetTest(String name) {
      super(name);
   }

   @Override
   public Set<?> makeEmptySet() {
      return ShardedConcurrentSets.withSet(new HashSet<Object>()).create();
   }
   
   @Override
   public boolean isNullSupported() {
      return true;
   }

   @Override
   public boolean isFailFastSupported() {
      return false;
   }

   @Override
   public boolean isEqualsCheckable() {
      return true;
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
   
   /**
    * Returns a bulk test with cases that verify concurrent access
    * behavior.
    *
    * @return a bulk test
    */
   public BulkTest bulkTestConcurrentAccess() {
      return new BulkTestConcurrentAccess(this);
   }
   
   /**
    * Tests for concurrent access to a set. This checks that exceptions
    * aren't thrown when the set is concurrently modified during an
    * on-going iteration. It also includes a brief "stress test" that
    * runs ten threads that continuously read and modify the set to
    * possibly catch dead lock issues, improper synchronization, and
    * other issues that can break the invariants of the set.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   public static class BulkTestConcurrentAccess extends BulkTest {
      
      private AbstractTestSet setTest;
      
      BulkTestConcurrentAccess(AbstractTestSet setTest) {
         super("");
         this.setTest = setTest;
      }
   
      private static void checkIterator(Iterator<?> testIter, Iterator<?> confirmedIter) {
         // since Sets do not have defined order of iteration, we can't just
         // proceed through iteration in sequence and expect iterator under test to
         // return the same values as confirmed iterator.
         HashSet<Object> testContents = new HashSet<Object>();
         HashSet<Object> confirmedContents = new HashSet<Object>();
         while (testIter.hasNext()) {
            testContents.add(testIter.next());
         }
         while (confirmedIter.hasNext()) {
            confirmedContents.add(confirmedIter.next());
         }
         assertEquals(confirmedContents, testContents);
      }
   
      /**
       * Tests that iteration is strongly consistent and succeeds even in the face of
       * concurrent clearing of the set.
       */
      public void testConcurrentModificationClear() {
         setTest.resetFull();
         Set<?> testSet = setTest.getSet();
   
         // iterator should have a snapshot, so this won't effect it
         Iterator<?> testIter = testSet.iterator();
         testSet.clear();
         assertTrue(testIter.hasNext());
         // check contents
         checkIterator(testIter, setTest.getConfirmedSet().iterator());
         
         // a new iterator will be empty
         testIter = testSet.iterator();
         assertFalse(testIter.hasNext());
      }
   
      /**
       * Tests that iteration is strongly consistent and succeeds even in the face of
       * concurrent updates to the set via adding and removing items.
       */
      public void testConcurrentModificationAddRemove() {
         setTest.resetEmpty();
         @SuppressWarnings("unchecked")
         Set<Integer> testSet = setTest.getSet();
         @SuppressWarnings("unchecked")
         Set<Integer> confirmedSet = setTest.getConfirmedSet();
         testSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
         confirmedSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
         
         // iterator should have a snapshot, so this won't effect it
         Iterator<?> testIter = testSet.iterator();
         testSet.addAll(Arrays.asList(11, 12, 13, 14, 15, 16));
         testSet.removeAll(Arrays.asList(2, 4, 6, 8, 10));
         assertTrue(testIter.hasNext());
         // check contents
         checkIterator(testIter, confirmedSet.iterator());
         
         // a new iterator will reflect the changes
         confirmedSet.addAll(Arrays.asList(11, 12, 13, 14, 15, 16));
         confirmedSet.removeAll(Arrays.asList(2, 4, 6, 8, 10));
         checkIterator(testSet.iterator(), confirmedSet.iterator());
      }
   
      /**
       * Tests that removing items from the set via an iterator succeeds even in the face
       * of concurrent updates.
       */
      public void testConcurrentModificationIteratorRemove() {
         setTest.resetFull();
         Set<?> testSet = setTest.getSet();
         Set<?> confirmedSet = setTest.getConfirmedSet();
         Iterator<?> testIter = testSet.iterator();
         
         Object o = testIter.next();
         testSet.remove(o); // remove directly from underlying set
         // iterator should proceed silently if element already removed
         testIter.remove();
         confirmedSet.remove(o);
         
         o = testIter.next();
         // just remove from iterator -- should be immediately reflected in set
         assertTrue(testSet.contains(o));
         testIter.remove();
         assertFalse(testSet.contains(o));
         confirmedSet.remove(o);
         
         // check contents
         checkIterator(testSet.iterator(), confirmedSet.iterator());
      }
      
      /**
       * Tests that concurrent access, including mutations and iterations, all
       * succeed and do not cause runtime exceptions or break set invariants. This
       * is accomplished by running ten threads in parallel for ten seconds, all
       * performing random add, remove, and iteration operations on the set.
       *
       * @throws InterruptedException if this thread is interrupted while waiting for
       *       ten worker threads to complete
       */
      public void testMultiThreadedAccess() throws InterruptedException {
         // So we start with an empty set.
         // Three threads are adding random numbers between 1 and 100.
         // Two threads are removing random numbers between 1 and 100.
         // Five threads are iterating over the set.
         // There should be no runtime exceptions. At the end, the set should
         // contain anywhere from 0 to 100 values, all integers between 1 and 100.
         
         setTest.resetEmpty();
         @SuppressWarnings("unchecked")
         final Set<Integer> set = setTest.getSet();
         // wish we had ThreadLocalRandom from Java 7...
         final ThreadLocal<Random> rand = new ThreadLocal<Random>() {
            @Override
            protected Random initialValue() {
               return new Random();
            }
         };
         // collect stats on the operations performed
         final AtomicInteger addCount = new AtomicInteger();
         final AtomicInteger addAttemptCount = new AtomicInteger();
         final AtomicInteger removeCount = new AtomicInteger();
         final AtomicInteger removeAttemptCount = new AtomicInteger();
         final AtomicInteger iteratorCount = new AtomicInteger();
         final AtomicInteger iteratorFetchCount = new AtomicInteger();
         final AtomicInteger iteratorRemoveCount = new AtomicInteger();
         
         MultipleAccessTestHelper helper = new MultipleAccessTestHelper();
         
         helper.addAccessors(3, new Runnable() {
            @Override
            public void run() {
               addAttemptCount.incrementAndGet();
               if (set.add(rand.get().nextInt(100) + 1)) {
                  addCount.incrementAndGet();
               }
            }
         });
         
         helper.addAccessors(2, new Runnable() {
            @Override
            public void run() {
               removeAttemptCount.incrementAndGet();
               if (set.remove(rand.get().nextInt(100) + 1)) {
                  removeCount.incrementAndGet();
               }
            }
         });
   
         helper.addAccessors(5, new Runnable() {
            @Override
            public void run() {
               iteratorCount.incrementAndGet();
               for (Iterator<?> iter = set.iterator(); iter.hasNext(); ) {
                  iter.next();
                  iteratorFetchCount.incrementAndGet();
                  if (rand.get().nextInt(100) == 0) {
                     // remove 1% of values
                     iter.remove();
                     iteratorRemoveCount.incrementAndGet();
                  }
               }
            }
         });
         
         // let threads run for 10 seconds
         Collection<Throwable> errs = helper.run(10, TimeUnit.SECONDS);
         
         // print the failures to assist w/ troubleshooting
         int idx = 1;
         for (Throwable t : errs) {
            System.err.println("Failure #" + (idx++) + ":");
            t.printStackTrace();
            System.err.println();
         }
         
         // now verify results
         assertEquals("Concurrent access thread(s) failed", 0, errs.size());
         
         // assert expectations for final disposition of the set
         assertTrue(set.size() <= 100);
         for (Integer i : set) {
            assertTrue(i >= 1 && i <= 100);
         }
         
         // make sure there are no duplicates (i.e. want to make sure that concurrent
         // access didn't result in breaking of set invariant)
         assertEquals(new HashSet<Integer>(set), set);
         
         // print stats on what the threads performed
         System.out.println("Items remaining in set: " + set.size());
         System.out.println("Items added: " + addCount.intValue());
         System.out.println("  Attempts to add: " + addAttemptCount.intValue());
         System.out.println("Items removed: " + removeCount.intValue());
         System.out.println("  Attempts to remove: " + removeAttemptCount.intValue());
         System.out.println("Iterations performed: " + iteratorCount.intValue());
         System.out.println("  Iterator fetches: " + iteratorFetchCount.intValue());
         System.out.println("  Iterator removals: " + iteratorRemoveCount.intValue());
      }
   }
}
