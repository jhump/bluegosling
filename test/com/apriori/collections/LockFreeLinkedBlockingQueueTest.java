package com.apriori.collections;

import com.apriori.collections.LockFreeLinkedBlockingQueue;

import java.util.concurrent.BlockingQueue;

import junit.framework.TestSuite;


public class LockFreeLinkedBlockingQueueTest extends AbstractTestBlockingQueue {

   public static TestSuite suite() {
      return makeSuite(LockFreeLinkedBlockingQueueTest.class);
   }
   
   public LockFreeLinkedBlockingQueueTest(String testName) {
      super(testName);
   }

   @Override
   public BlockingQueue<Object> makeCollection() {
      return new LockFreeLinkedBlockingQueue<>();
   }

   @Override
   public BlockingQueue<Object> makeCollection(int maxCapacity) {
      return new LockFreeLinkedBlockingQueue<>(maxCapacity);
   }
}
