package com.apriori.collections;

import java.util.Queue;

import junit.framework.TestSuite;

/**
 * Test cases for {@link MergeablePriorityQueue}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MergeablePriorityQueueTest extends AbstractTestPriorityQueue {

   public static TestSuite suite() {
      return makeSuite(MergeablePriorityQueueTest.class);
   }
   
   public MergeablePriorityQueueTest(String testName) {
      super(testName);
   }

   @Override
   public Queue<?> makeCollection() {
      return new MergeablePriorityQueue<Object>(getComparator());
   }
   
   @Override
   public boolean isFailFastSupported() {
      return true;
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
   
   public void testMerge() {
      // merge empty
      resetEmpty();
      MergeablePriorityQueue<Object> queue = (MergeablePriorityQueue<Object>) getCollection();
      MergeablePriorityQueue<Object> other = new MergeablePriorityQueue<Object>();
      queue.mergeFrom(other);
      assertTrue(queue.isEmpty()); // still empty
      assertTrue(other.isEmpty());
      
      // not empty
      resetFull();
      queue = (MergeablePriorityQueue<Object>) getCollection();
      other = queue.clone();
      // benchmark doesn't have "merge" operation, so use addAll instead
      Queue<Object> benchmark = getConfirmed();
      benchmark.addAll(other);
      
      // method under test:
      queue.mergeFrom(other);
      assertEquals(getFullElements().length * 2, queue.size());
      assertTrue(other.isEmpty()); // elements were *moved* out of other
      
      verify();
   }
   
   @Override public void testCollectionIteratorRemove() {
      super.testCollectionIteratorRemove();
      
      // TODO: more thorough checks to make sure tree remains in good shape after all kinds of
      // mutations from the iterator
   }
}
