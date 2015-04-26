package com.apriori.collections;

import java.util.concurrent.BlockingDeque;

import junit.framework.TestSuite;


public class LockFreeLinkedBlockingDequeTest extends AbstractTestBlockingDeque {

   public static TestSuite suite() {
      return makeSuite(LockFreeLinkedBlockingDequeTest.class);
   }
   
   public LockFreeLinkedBlockingDequeTest(String testName) {
      super(testName);
   }

   @Override
   public BlockingDeque<Object> makeCollection() {
      return new LockFreeLinkedBlockingDeque<>();
   }

   @Override
   public BlockingDeque<Object> makeCollection(int maxCapacity) {
      return new LockFreeLinkedBlockingDeque<>(maxCapacity);
   }
}
