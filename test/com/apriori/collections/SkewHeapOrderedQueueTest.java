package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

@RunWith(BulkTestRunner.class)
public class SkewHeapOrderedQueueTest extends AbstractTestMeldableOrderedQueue {
   
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
