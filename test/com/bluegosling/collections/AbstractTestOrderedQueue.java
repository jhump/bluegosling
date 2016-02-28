package com.bluegosling.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;

public abstract class AbstractTestOrderedQueue extends AbstractTestQueue {

   protected AbstractTestOrderedQueue(String testName) {
      super(testName);
   }
   
   @Override
   public boolean isNullSupported() {
      return false;
   }
   
   @Override
   public Object[] getFullElements() {
      return new Object[] { "a", "z", "Abcdefg", "mno", "PQRS", "Wxyz", "def", "s", "T", "u", "V" };
   }
   
   // needs to implement Serializable for serialization tests
   @SuppressWarnings("serial")
   static class TestComparator implements Comparator<Object>, Serializable {
      @Override public int compare(Object o1, Object o2) {
         String s1 = o1.toString();
         String s2 = o2.toString();
         // case-insensitive descending order
         return -s1.compareToIgnoreCase(s2);
      }
   }
   
   static final Comparator<Object> TEST_COMPARATOR = new TestComparator();
   
   protected Comparator<Object> getComparator() {
      return TEST_COMPARATOR;
   }

   @Override
   public OrderedQueue<Object> getCollection() {
      return (OrderedQueue<Object>) super.getCollection();
   }
   
   @Override
   public abstract OrderedQueue<Object> makeCollection();
   
   @Override
   public abstract OrderedQueue<Object> makeCollection(int capacity);
   
   @Override
   public OrderedQueue<Object> makeFullCollection() {
      return (OrderedQueue<Object>) super.makeFullCollection();
   }

   @Override
   public Queue<Object> makeConfirmedCollection() {
      return new PriorityQueue<Object>(10, getComparator());
   }

   @Override
   public Queue<Object> makeConfirmedFullCollection() {
      Queue<Object> queue = new PriorityQueue<Object>(10, getComparator());
      queue.addAll(Arrays.asList(getFullElements()));
      return queue;
   }

   @Override
   public boolean isCheckingIterationOrder() {
      return false;
   }
   
   public void testOrderRemove() {
      doTestOrder(q -> q.remove());
   }
   
   public void testOrderPoll() {
      doTestOrder(Queue::poll);
   }
   
   public void testOrderPeek() {
      doTestOrder(q -> {
         Object o = q.peek();
         assertSame(o, q.poll());
         return o;
      });
   }
   
   private void doTestOrder(Function<Queue<Object>, Object> poll) {
      // explicit test that we get items out in expected order
      resetEmpty();
      Queue<Object> queue = getCollection();
      
      // seed w/ lots o' data
      queue.addAll(Arrays.asList(getFullElements()));
      queue.addAll(Arrays.asList(getFullElements()));
      queue.addAll(Arrays.asList(getOtherNonNullStringElements()));
      queue.addAll(Arrays.asList(getOtherNonNullStringElements()));

      List<Object> sorted = new ArrayList<Object>(queue.size());
      sorted.addAll(Arrays.asList(getFullElements()));
      sorted.addAll(Arrays.asList(getFullElements()));
      sorted.addAll(Arrays.asList(getOtherNonNullStringElements()));
      sorted.addAll(Arrays.asList(getOtherNonNullStringElements()));
      Collections.sort(sorted, getComparator());

      List<Object> removed = new ArrayList<Object>(queue.size());
      while (!queue.isEmpty()) {
         removed.add(poll.apply(queue));
      }
      
      assertEquals(sorted, removed);
   }
}
