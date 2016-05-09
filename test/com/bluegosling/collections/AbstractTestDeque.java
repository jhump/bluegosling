package com.bluegosling.collections;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;

//TODO: javadoc
public abstract class AbstractTestDeque extends AbstractTestQueue {

   protected AbstractTestDeque(String testName) {
      super(testName);
   }

   @Override
   public Deque<Object> getCollection() {
      return (Deque<Object>) super.getCollection();
   }

   @Override
   public Deque<Object> getConfirmed() {
      return (Deque<Object>) super.getConfirmed();
   }
   
   @Override
   public abstract Deque<Object> makeCollection();

   @Override
   public abstract Deque<Object> makeCollection(int maxCapacity);

   @Override
   public Deque<Object> makeFullCollection() {
      return (Deque<Object>) super.makeFullCollection();
   }

   @Override
   public Deque<Object> makeConfirmedCollection() {
      return new LinkedList<Object>();
   }

   @Override
   public Deque<Object> makeConfirmedFullCollection() {
      return new LinkedList<Object>(Arrays.asList(getFullElements()));
   }
   
   public void testRemoveFirst() {
      doTestRemove(Deque::removeFirst);
   }

   public void testRemoveLast() {
      doTestRemove(Deque::removeLast);
   }
   
   public void testPop() {
      doTestRemove(Deque::pop);
   }
   
   private void doTestRemove(Function<Deque<Object>, Object> remove) {
      // remove from empty queue
      resetEmpty();
      Deque<Object> queue = getCollection();
      
      try {
         remove.apply(queue);
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
      
      // remove from queue with single element
      Object o = getFullElements()[0];
      queue.add(o);
      assertEquals(o, remove.apply(queue));
      assertTrue(queue.isEmpty());
      
      // repeated removes from a full queue
      resetFull();
      queue = getCollection();
      Deque<Object> benchmark = getConfirmed();
      
      // seed w/ lots o' data
      queue.addAll(Arrays.asList(getFullElements()));
      queue.addAll(Arrays.asList(getFullElements()));
      queue.addAll(Arrays.asList(getOtherNonNullStringElements()));
      queue.addAll(Arrays.asList(getOtherNonNullStringElements()));

      benchmark.addAll(Arrays.asList(getFullElements()));
      benchmark.addAll(Arrays.asList(getFullElements()));
      benchmark.addAll(Arrays.asList(getOtherNonNullStringElements()));
      benchmark.addAll(Arrays.asList(getOtherNonNullStringElements()));
      
      verify();
      
      while (!queue.isEmpty()) {
         Object o1 = remove.apply(queue);
         Object o2 = remove.apply(benchmark);
         assertEquals(o2, o1);
         verify();
      }

      try {
         remove.apply(queue);
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
   }
   
   public void testOfferFirst() {
      doTestOffer(Deque::offerFirst);
   }

   public void testOfferLast() {
      doTestOffer(Deque::offerLast);
   }
   
   private void doTestOffer(BiFunction<Deque<Object>, Object, Boolean> offer) {
      resetEmpty();
      
      Deque<Object> queue = getCollection();
      
      // offer and make sure remove returns that same item
      Object o = getFullElements()[0];
      assertTrue(offer.apply(queue, o));
      assertFalse(queue.isEmpty());
      assertEquals(o, queue.remove());
      
      // offer several
      Deque<Object> benchmark = getConfirmed();
      for (Object obj : getFullElements()) {
         assertTrue(offer.apply(queue, obj));
         offer.apply(benchmark, obj);
         verify();
      }
      
      while (!queue.isEmpty()) {
         Object o1 = queue.remove();
         Object o2 = benchmark.remove();
         assertSame(o1, o2);
         verify();
      }
      
      // capacity constrained
      queue = makeCollection(1);
      // test returns null if capacity constraint not supported
      if (queue != null) {
         assertTrue(offer.apply(queue, o));
         // capacity of 1 means we can't add second item
         assertFalse(offer.apply(queue, o));
         // until after we remove the first one
         assertSame(o, queue.remove());
         assertTrue(offer.apply(queue, o));
         assertFalse(offer.apply(queue, o));
      }
      queue = makeCollection(getFullElements().length);
      // test returns null if capacity constraint not supported
      if (queue != null) {
         queue.addAll(Arrays.asList(getFullElements()));
         assertEquals(getFullElements().length, queue.size());
         // queue is now full
         assertFalse(offer.apply(queue, o));
         // until after we remove one
         assertEquals(o, queue.remove());
         assertTrue(offer.apply(queue, o));
         assertFalse(offer.apply(queue, o));
      }
   }

   public void testPollFirst() {
      doTestPoll(Deque::pollFirst);
   }

   public void testPollLast() {
      doTestPoll(Deque::pollLast);
   }
   
   private void doTestPoll(Function<Deque<Object>, Object> poll) {
      // (basically same test as testQueueRemove, except returns null instead of throws exception
      // when empty)
      
      // remove from empty queue
      resetEmpty();
      Deque<Object> queue = getCollection();
      assertNull(poll.apply(queue));
      
      // remove from queue with single element
      Object o = getFullElements()[0];
      queue.add(o);
      assertEquals(o, poll.apply(queue));
      assertTrue(queue.isEmpty());
      
      // repeated removes from a full queue
      resetFull();
      queue = getCollection();
      Deque<Object> benchmark = getConfirmed();
      
      // seed w/ lots o' data
      queue.addAll(Arrays.asList(getFullElements()));
      queue.addAll(Arrays.asList(getFullElements()));
      queue.addAll(Arrays.asList(getOtherNonNullStringElements()));
      queue.addAll(Arrays.asList(getOtherNonNullStringElements()));

      benchmark.addAll(Arrays.asList(getFullElements()));
      benchmark.addAll(Arrays.asList(getFullElements()));
      benchmark.addAll(Arrays.asList(getOtherNonNullStringElements()));
      benchmark.addAll(Arrays.asList(getOtherNonNullStringElements()));
      
      verify();
      
      while (!queue.isEmpty()) {
         Object o1 = poll.apply(queue);
         Object o2 = poll.apply(benchmark);
         assertEquals(o2, o1);
         verify();
      }

      assertNull(poll.apply(queue));
   }
   
   public void testPeekFirst() {
      doTestPeek(Deque::peekFirst, Deque::removeFirst);
   }

   public void testPeekLast() {
      doTestPeek(Deque::peekLast, Deque::removeLast);
   }

   private void doTestPeek(Function<Deque<Object>, Object> peek,
         Function<Deque<Object>, Object> remove) {
      // (very similar to testPoll, but we verify that peek doesn't remove and subsequent poll
      // returns the same element as returned by peek)
      
      // remove from empty queue
      resetEmpty();
      Deque<Object> queue = getCollection();
      assertNull(peek.apply(queue));

      // remove from queue with single element
      Object o = getFullElements()[0];
      queue.add(o);
      assertEquals(o, peek.apply(queue));
      
      // repeated removes from a full queue
      resetFull();
      queue = getCollection();
      Deque<Object> benchmark = getConfirmed();
      
      verify();
      
      while (!queue.isEmpty()) {
         o = remove.apply(benchmark);
         assertEquals(o, peek.apply(queue));
         assertEquals(o, remove.apply(queue));
         verify();
      }

      assertNull(peek.apply(queue));
   }
   
   public void testGetFirst() {
      doTestGet(Deque::getFirst, Deque::removeFirst);
   }
   
   public void testGetLast() {
      doTestGet(Deque::getLast, Deque::removeLast);
   }
   
   private void doTestGet(Function<Deque<Object>, Object> get,
         Function<Deque<Object>, Object> remove) {
      // (basically same test as testPeek, except throws exception when empty instead
      // of returning null)
      
      // remove from empty queue
      resetEmpty();
      Deque<Object> queue = getCollection();
      try {
         get.apply(queue);
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }

      // remove from queue with single element
      Object o = getFullElements()[0];
      queue.add(o);
      assertEquals(o, get.apply(queue));
      
      // repeated removes from a full queue
      resetFull();
      queue = getCollection();
      Deque<Object> benchmark = getConfirmed();
      
      verify();
      
      while (!queue.isEmpty()) {
         o = remove.apply(benchmark);
         assertEquals(o, get.apply(queue));
         assertEquals(o, remove.apply(queue));
         verify();
      }

      try {
         get.apply(queue);
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
   }
}
