package com.bluegosling.collections.queues;

import com.bluegosling.collections.queues.FibonacciHeapOrderedQueue;
import com.bluegosling.collections.queues.MeldableOrderedQueue;
import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

/**
 * Test cases for {@link FibonacciHeapOrderedQueue}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
@RunWith(BulkTestRunner.class)
public class FibonacciHeapOrderedQueueTest extends AbstractTestMeldableOrderedQueue {
   
   public FibonacciHeapOrderedQueueTest(String testName) {
      super(testName);
   }

   @Override
   public MeldableOrderedQueue<Object, ?> makeCollection() {
      return new FibonacciHeapOrderedQueue<Object>(getComparator());
   }
   
   @Override
   public MeldableOrderedQueue<Object, ?> makeCollection(int capacity) {
      return null; // capacity constraint not supported
   }
   
   @Override
   public boolean isFailFastSupported() {
      return true;
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
   
   @Override public void testCollectionIteratorRemove() {
      super.testCollectionIteratorRemove();
      
      // TODO: more thorough checks to make sure tree remains in good shape after all kinds of
      // mutations from the iterator
   }
}
