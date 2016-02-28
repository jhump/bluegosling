package com.bluegosling.collections;

import java.util.Queue;

public abstract class AbstractTestMeldableOrderedQueue
      extends AbstractTestOrderedQueue {

   protected AbstractTestMeldableOrderedQueue(String testName) {
      super(testName);
   }

   @Override
   @SuppressWarnings("unchecked") // it's a wildcard; why does compiler think it's unsafe?
   public MeldableOrderedQueue<Object, ?> getCollection() {
      return (MeldableOrderedQueue<Object, ?>) super.getCollection();
   }
   
   @Override
   public abstract MeldableOrderedQueue<Object, ?> makeCollection();
   
   @Override
   public abstract MeldableOrderedQueue<Object, ?> makeCollection(int capacity);
   
   @Override
   @SuppressWarnings("unchecked") // it's a wildcard; why does compiler think it's unsafe?
   public MeldableOrderedQueue<Object, ?> makeFullCollection() {
      return (MeldableOrderedQueue<Object, ?>) super.makeFullCollection();
   }
   
   @SuppressWarnings("unchecked") // requires implementation under test to be mergeable with itself
   private MeldableOrderedQueue<Object, MeldableOrderedQueue<?, ?>> castQueue() {
      return (MeldableOrderedQueue<Object, MeldableOrderedQueue<?, ?>>) getCollection();
   }
   
   public void testMerge() {
      // merge empty
      resetEmpty();
      MeldableOrderedQueue<Object, MeldableOrderedQueue<?, ?>> queue = castQueue();
      MeldableOrderedQueue<Object, ?> other = makeCollection();
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
