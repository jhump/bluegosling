package com.apriori.collections;

import junit.framework.TestSuite;


public class SkewHeapOrderedQueueTest extends AbstractTestMeldableOrderedQueue {
   public static TestSuite suite() {
      return makeSuite(SkewHeapOrderedQueueTest.class);
   }
   
   public SkewHeapOrderedQueueTest(String testName) {
      super(testName);
   }

   @Override
   public MeldableOrderedQueue<Object, ?> makeCollection() {
      return new SkewHeapOrderedQueue<Object>(getComparator());
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
}
