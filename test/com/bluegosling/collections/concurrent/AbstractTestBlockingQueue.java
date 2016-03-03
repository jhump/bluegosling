package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.AbstractTestQueue;

import java.util.concurrent.BlockingQueue;

public abstract class AbstractTestBlockingQueue extends AbstractTestQueue {

   protected AbstractTestBlockingQueue(String testName) {
      super(testName);
   }
   
   @Override
   public final boolean isNullSupported() {
      return false;
   }
   
   @Override
   public BlockingQueue<Object> getCollection() {
      return (BlockingQueue<Object>) super.getCollection();
   }
   
   @Override
   public abstract BlockingQueue<Object> makeCollection();

   @Override
   public abstract BlockingQueue<Object> makeCollection(int maxCapacity);

   @Override
   public BlockingQueue<Object> makeFullCollection() {
      return (BlockingQueue<Object>) super.makeFullCollection();
   }
   
   public void testOfferTimed() {
      // TODO
   }
   
   public void testPut() {
      // TODO
   }
   
   public void testPollTimed() {
      // TODO
   }
   
   public void testTake() {
      // TODO
   }
   
   public void testDrainTo() {
      // TODO
   }
   
   public void testDrainToWithLimit() {
      // TODO
   }
   
   public void testRemainingCapacity() {
      // TODO
   }

}
