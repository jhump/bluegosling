package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.concurrent.BlockingDeque;

@RunWith(BulkTestRunner.class)
public class LockFreeLinkedBlockingDequeTest extends AbstractTestBlockingDeque {
   
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
