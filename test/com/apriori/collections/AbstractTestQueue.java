package com.apriori.collections;

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

//TODO: more tests for queue methods
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
   public abstract Queue<?> makeCollection();
   
   @Override
   public Queue<?> makeFullCollection() {
      return (Queue<?>) super.makeFullCollection();
   }

   @Override
   public Queue<?> makeConfirmedCollection() {
      return new LinkedList<Object>();
   }

   @Override
   public Queue<?> makeConfirmedFullCollection() {
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
}
