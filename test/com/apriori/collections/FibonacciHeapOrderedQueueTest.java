package com.apriori.collections;

import junit.framework.TestSuite;

/**
 * Test cases for {@link FibonacciHeapOrderedQueue}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class FibonacciHeapOrderedQueueTest extends AbstractTestMeldableOrderedQueue {

   public static TestSuite suite() {
      return makeSuite(FibonacciHeapOrderedQueueTest.class);
   }
   
   public FibonacciHeapOrderedQueueTest(String testName) {
      super(testName);
   }

   @Override
   public MeldableOrderedQueue<Object, ?> makeCollection() {
      return new FibonacciHeapOrderedQueue<Object>(getComparator());
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
