package com.apriori.collections;

import java.util.Queue;

public abstract class AbstractTestMergeablePriorityOrderedQueue
      extends AbstractTestPriorityOrderedQueue {

   protected AbstractTestMergeablePriorityOrderedQueue(String testName) {
      super(testName);
   }

   @Override
   @SuppressWarnings("unchecked") // it's a wildcard; why does compiler think it's unsafe?
   public MergeablePriorityOrderedQueue<Object, ?> getCollection() {
      return (MergeablePriorityOrderedQueue<Object, ?>) super.getCollection();
   }
   
   @Override
   public abstract MergeablePriorityOrderedQueue<Object, ?> makeCollection();
   
   @Override
   @SuppressWarnings("unchecked") // it's a wildcard; why does compiler think it's unsafe?
   public MergeablePriorityOrderedQueue<Object, ?> makeFullCollection() {
      return (MergeablePriorityOrderedQueue<Object, ?>) super.makeFullCollection();
   }
   
   @SuppressWarnings("unchecked") // requires implementation under test to be mergeable with itself
   private MergeablePriorityOrderedQueue<Object, MergeablePriorityOrderedQueue<?, ?>> castQueue() {
      return (MergeablePriorityOrderedQueue<Object, MergeablePriorityOrderedQueue<?, ?>>) getCollection();
   }
   
   public void testMerge() {
      // merge empty
      resetEmpty();
      MergeablePriorityOrderedQueue<Object, MergeablePriorityOrderedQueue<?, ?>> queue = castQueue();
      MergeablePriorityOrderedQueue<Object, ?> other = makeCollection();
      queue.mergeFrom(other);
      assertTrue(queue.isEmpty()); // still empty
      assertTrue(other.isEmpty());
      
      // not empty
      resetFull();
      queue = castQueue();
      other = makeCollection();
      other.addAll(queue);
      // benchmark doesn't have "merge" operation, so use addAll instead
      Queue<Object> benchmark = getConfirmed();
      benchmark.addAll(other);
      
      // method under test:
      queue.mergeFrom(other);
      assertEquals(getFullElements().length * 2, queue.size());
      assertTrue(other.isEmpty()); // elements were *moved* out of other
      
      verify();
   }
}
