package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.AbstractTestDeque;

import java.util.Deque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

//TODO: tests for BlockingDeque methods
public abstract class AbstractTestBlockingDeque extends AbstractTestDeque {

   protected AbstractTestBlockingDeque(String testName) {
      super(testName);
   }

   @Override
   public final boolean isNullSupported() {
      return false;
   }

   @Override
   public BlockingDeque<Object> getCollection() {
      return (BlockingDeque<Object>) super.getCollection();
   }
   
   @Override
   public abstract BlockingDeque<Object> makeCollection();

   @Override
   public abstract BlockingDeque<Object> makeCollection(int maxCapacity);

   @Override
   public BlockingDeque<Object> makeFullCollection() {
      return (BlockingDeque<Object>) super.makeFullCollection();
   }
   
   @FunctionalInterface
   interface TimedOfferFunction {
      boolean apply(BlockingDeque<Object> deque, Object o, long timeLimit, TimeUnit unit)
            throws InterruptedException;
   }

   @FunctionalInterface
   interface PutFunction {
      void apply(BlockingDeque<Object> deque, Object o) throws InterruptedException;
   }

   @FunctionalInterface
   interface TimedPollFunction {
      Object apply(BlockingDeque<Object> deque, long timeLimit, TimeUnit unit)
            throws InterruptedException;
   }

   @FunctionalInterface
   interface TakeFunction {
      Object apply(BlockingDeque<Object> deque) throws InterruptedException;
   }

   public void testOfferTimed() {
      doTestOfferTimed(BlockingDeque::offer, Deque::add);
   }
   
   public void testOfferFirstTimed() {
      doTestOfferTimed(BlockingDeque::offerFirst, Deque::addFirst);
   }
   
   public void testOfferLastTimed() {
      doTestOfferTimed(BlockingDeque::offerLast, Deque::addLast);
   }
   
   private void doTestOfferTimed(TimedOfferFunction offer, BiConsumer<Deque<Object>, Object> add) {
      // TODO
   }
   
   public void testPut() {
      doTestPut(BlockingDeque::put, Deque::add);
   }
   
   public void testPutFirst() {
      doTestPut(BlockingDeque::putFirst, Deque::addFirst);
   }
   
   public void testPutLast() {
      doTestPut(BlockingDeque::putLast, Deque::addLast);
   }
   
   private void doTestPut(PutFunction put, BiConsumer<Deque<Object>, Object> add) {
      // TODO
   }
   
   public void testPollTimed() {
      doTestPollTimed(BlockingDeque::poll, Deque::poll);
   }
   
   public void testPollFirstTimed() {
      doTestPollTimed(BlockingDeque::pollFirst, Deque::pollFirst);
   }
   
   public void testPollLastTimed() {
      doTestPollTimed(BlockingDeque::pollLast, Deque::pollLast);
   }
   
   private void doTestPollTimed(TimedPollFunction timedPoll, Function<Deque<Object>, Object> poll) {
      // TODO
   }
   
   public void testTake() {
      doTestTake(BlockingDeque::take, Deque::poll);
   }
   
   public void testTakeFirst() {
      doTestTake(BlockingDeque::takeFirst, Deque::pollFirst);
   }
   
   public void testTakeLast() {
      doTestTake(BlockingDeque::takeLast, Deque::pollLast);
   }
   
   private void doTestTake(TakeFunction take, Function<Deque<Object>, Object> poll) {
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
   
   // TODO: moar iterator tests? bulk test?
   public void testDescendingIterator() {
      // TODO
   }

}
