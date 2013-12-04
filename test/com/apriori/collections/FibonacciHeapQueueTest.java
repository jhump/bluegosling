package com.apriori.collections;

import junit.framework.TestSuite;

/**
 * Test cases for {@link FibonacciHeapQueue}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FibonacciHeapQueueTest extends AbstractTestMergeablePriorityOrderedQueue {

   public static TestSuite suite() {
      return makeSuite(FibonacciHeapQueueTest.class);
   }
   
   public FibonacciHeapQueueTest(String testName) {
      super(testName);
   }

   @Override
   public MergeablePriorityOrderedQueue<Object, ?> makeCollection() {
      return new FibonacciHeapQueue<Object>(getComparator());
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
