package com.bluegosling.collections;

import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.concurrent.BlockingQueue;

@RunWith(BulkTestRunner.class)
public class LockFreeLinkedBlockingQueueTest extends AbstractTestBlockingQueue {
   
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
