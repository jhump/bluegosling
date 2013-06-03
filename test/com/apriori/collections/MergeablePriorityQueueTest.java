package com.apriori.collections;

import java.util.Queue;

// TODO: javadoc
public class MergeablePriorityQueueTest extends AbstractTestPriorityQueue {

   public MergeablePriorityQueueTest(String testName) {
      super(testName);
   }

   @Override
   public Queue<?> makeCollection() {
      return new MergeablePriorityQueue<Object>(getComparator());
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
      Queue<Object> benchmark = getConfirmed();
      benchmark.addAll(other);
      
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
