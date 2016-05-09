package com.bluegosling.collections;

import org.apache.commons.collections.collection.AbstractTestCollection;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;

//TODO: javadoc
public abstract class AbstractTestQueue extends AbstractTestCollection {

   protected AbstractTestQueue(String testName) {
      super(testName);
   }

   @SuppressWarnings("unchecked")
   public Queue<Object> getCollection() {
      return (Queue<Object>) collection;
   }
   
   @SuppressWarnings("unchecked")
   public Queue<Object> getConfirmed() {
      return (Queue<Object>) confirmed;
   }
   
   @Override
   public abstract Queue<Object> makeCollection();

   /**
    * Creates a queue with limited capacity. If the queue under test does not support constraining
    * the capacity, this method should return {@code null}.
    *
    * @param maxCapacity the maximum capacity the queue can hold
    * @return a new queue or {@code null} if the queue under test doesn't support capacity limits
    */
   public abstract Queue<Object> makeCollection(int maxCapacity);

   @Override
   @SuppressWarnings("unchecked")
   public Queue<Object> makeFullCollection() {
      return (Queue<Object>) super.makeFullCollection();
   }

   @Override
   public Queue<Object> makeConfirmedCollection() {
      return new LinkedList<Object>();
   }

   @Override
   public Queue<Object> makeConfirmedFullCollection() {
      return new LinkedList<Object>(Arrays.asList(getFullElements()));
   }
   
   public boolean isCheckingIterationOrder() {
      return true;
   }
   
   @Override
   public void verify() {
       super.verify();
       if (isCheckingIterationOrder()) {
          Iterator<?> iterator1 = getCollection().iterator();
          Iterator<?> iterator2 = getConfirmed().iterator();
          while (iterator2.hasNext()) {
              assertTrue(iterator1.hasNext());
              final Object o1 = iterator1.next();
              final Object o2 = iterator2.next();
              assertEquals(o1, o2);
          }
          assertFalse(iterator1.hasNext());
       } else {
          Map<Object, Integer> items1 = getItems(getCollection());
          Map<Object, Integer> items2 = getItems(getConfirmed());
          assertEquals(items1, items2);
       }
   }

   private Map<Object, Integer> getItems(Collection<?> coll) {
      Map<Object, Integer> items = new HashMap<Object, Integer>();
      for (Object o : coll) {
         Integer count = items.get(o);
         if (count == null) {
            items.put(o, 1);
         } else {
            items.put(o, count + 1);
         }
      }
      return items;
   }

   public void testEmptyQueueSerialization() throws IOException, ClassNotFoundException {
       final Queue<?> queue = makeCollection();
       if (!(queue instanceof Serializable && isTestSerialization())) {
           return;
       }

       final byte[] objekt = writeExternalFormToBytes((Serializable) queue);
       final Queue<?> queue2 = (Queue<?>) readExternalFormFromBytes(objekt);

       assertEquals("Both queues are empty", 0, queue.size());
       assertEquals("Both queues are empty", 0, queue2.size());
   }

   public void testFullQueueSerialization() throws IOException, ClassNotFoundException {
       final Queue<?> queue = makeFullCollection();
       final int size = getFullElements().length;
       if (!(queue instanceof Serializable && isTestSerialization())) {
           return;
       }

       final byte[] objekt = writeExternalFormToBytes((Serializable) queue);
       final Queue<?> queue2 = (Queue<?>) readExternalFormFromBytes(objekt);

       assertEquals("Both queues are same size", size, queue.size());
       assertEquals("Both queues are same size", size, queue2.size());
   }
   
   public void testQueueRemove() {
      // remove from empty queue
      resetEmpty();
      Queue<Object> queue = getCollection();
      
      try {
         queue.remove();
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
      
      // remove from queue with single element
      Object o = getFullElements()[0];
      queue.add(o);
      assertEquals(o, queue.remove());
      assertTrue(queue.isEmpty());
      
      // repeated removes from a full queue
      resetFull();
      queue = getCollection();
      Queue<Object> benchmark = getConfirmed();
      
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
         Object o1 = queue.remove();
         Object o2 = benchmark.remove();
         assertEquals(o2, o1);
         verify();
      }

      try {
         queue.remove();
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
   }
   
   public void testOffer() {
      resetEmpty();
      
      Queue<Object> queue = getCollection();
      
      // offer and make sure remove returns that same item
      Object o = getFullElements()[0];
      assertTrue(queue.offer(o));
      assertFalse(queue.isEmpty());
      assertEquals(o, queue.remove());
      
      // offer several
      Queue<Object> benchmark = getConfirmed();
      for (Object obj : getFullElements()) {
         assertTrue(queue.offer(obj));
         benchmark.offer(obj);
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
         assertTrue(queue.offer(o));
         // capacity of 1 means we can't add second item
         assertFalse(queue.offer(o));
         // until after we remove the first one
         assertSame(o, queue.remove());
         assertTrue(queue.offer(o));
         assertFalse(queue.offer(o));
      }
      queue = makeCollection(getFullElements().length);
      // test returns null if capacity constraint not supported
      if (queue != null) {
         queue.addAll(Arrays.asList(getFullElements()));
         assertEquals(getFullElements().length, queue.size());
         // queue is now full
         assertFalse(queue.offer(o));
         // until after we remove one
         assertEquals(o, queue.remove());
         assertTrue(queue.offer(o));
         assertFalse(queue.offer(o));
      }
   }

   public void testPoll() {
      // (basically same test as testQueueRemove, except returns null instead of throws exception
      // when empty)
      
      // remove from empty queue
      resetEmpty();
      Queue<Object> queue = getCollection();
      assertNull(queue.poll());
      
      // remove from queue with single element
      Object o = getFullElements()[0];
      queue.add(o);
      assertEquals(o, queue.poll());
      assertTrue(queue.isEmpty());
      
      // repeated removes from a full queue
      resetFull();
      queue = getCollection();
      Queue<Object> benchmark = getConfirmed();
      
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
         Object o1 = queue.poll();
         Object o2 = benchmark.poll();
         assertEquals(o2, o1);
         verify();
      }

      assertNull(queue.poll());
   }
   
   public void testPeek() {
      // (very similar to testPoll, but we verify that peek doesn't remove and subsequent poll
      // returns the same element as returned by peek)
      
      // remove from empty queue
      resetEmpty();
      Queue<Object> queue = getCollection();
      assertNull(queue.peek());

      // remove from queue with single element
      Object o = getFullElements()[0];
      queue.add(o);
      assertEquals(o, queue.peek());
      
      // repeated removes from a full queue
      resetFull();
      queue = getCollection();
      Queue<Object> benchmark = getConfirmed();
      
      verify();
      
      while (!queue.isEmpty()) {
         o = queue.peek();
         assertEquals(o, queue.remove());
         benchmark.remove();
         verify();
      }

      assertNull(queue.peek());
   }
   
   public void testElement() {
      // (basically same test as testPeek, except throws exception when empty instead
      // of returning null)
      
      // remove from empty queue
      resetEmpty();
      Queue<Object> queue = getCollection();
      try {
         queue.element();
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }

      // remove from queue with single element
      Object o = getFullElements()[0];
      queue.add(o);
      assertEquals(o, queue.element());
      
      // repeated removes from a full queue
      resetFull();
      queue = getCollection();
      Queue<Object> benchmark = getConfirmed();
      
      verify();
      
      while (!queue.isEmpty()) {
         o = queue.element();
         assertEquals(o, queue.remove());
         benchmark.remove();
         verify();
      }

      try {
         queue.element();
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
   }
}
