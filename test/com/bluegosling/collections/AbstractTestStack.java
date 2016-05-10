package com.bluegosling.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections.collection.AbstractTestCollection;

public abstract class AbstractTestStack extends AbstractTestCollection {
   protected AbstractTestStack(String testName) {
      super(testName);
   }

   @SuppressWarnings("unchecked")
   public Stack<Object> getCollection() {
      return (Stack<Object>) collection;
   }
   
   @SuppressWarnings("unchecked")
   public Stack<Object> getConfirmed() {
      return (Stack<Object>) confirmed;
   }

   @Override
   public abstract Stack<Object> makeCollection();

   @Override
   @SuppressWarnings("unchecked")
   public Stack<Object> makeFullCollection() {
      return (Stack<Object>) super.makeFullCollection();
   }

   @Override
   public Stack<Object> makeConfirmedCollection() {
      return Stack.fromList(new ArrayList<>());
   }

   @Override
   public Stack<Object> makeConfirmedFullCollection() {
      return Stack.fromList(new ArrayList<>(Arrays.asList(getFullElements())));
   }
   
   @Override
   public void verify() {
       super.verify();
       // confirm iteration order
       Iterator<?> iterator1 = getCollection().iterator();
       Iterator<?> iterator2 = getConfirmed().iterator();
       while (iterator2.hasNext()) {
           assertTrue(iterator1.hasNext());
           final Object o1 = iterator1.next();
           final Object o2 = iterator2.next();
           assertEquals(o1, o2);
       }
       assertFalse(iterator1.hasNext());
   }
   
   public void testPop() {
      // remove from empty stack
      resetEmpty();
      Stack<Object> stack = getCollection();
      
      try {
         stack.pop();
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
      
      // remove from stack with single element
      Object o = getFullElements()[0];
      stack.add(o);
      assertEquals(o, stack.pop());
      assertTrue(stack.isEmpty());
      
      // repeated removes from a full stack
      resetFull();
      stack = getCollection();
      Stack<Object> benchmark = getConfirmed();
      
      // seed w/ lots o' data
      stack.addAll(Arrays.asList(getFullElements()));
      stack.addAll(Arrays.asList(getFullElements()));
      stack.addAll(Arrays.asList(getOtherNonNullStringElements()));
      stack.addAll(Arrays.asList(getOtherNonNullStringElements()));

      benchmark.addAll(Arrays.asList(getFullElements()));
      benchmark.addAll(Arrays.asList(getFullElements()));
      benchmark.addAll(Arrays.asList(getOtherNonNullStringElements()));
      benchmark.addAll(Arrays.asList(getOtherNonNullStringElements()));
      
      verify();
      
      while (!stack.isEmpty()) {
         Object o1 = stack.pop();
         Object o2 = benchmark.pop();
         assertEquals(o2, o1);
         verify();
      }

      try {
         stack.pop();
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
   }
   
   public void testPush() {
      resetEmpty();
      
      Stack<Object> stack = getCollection();
      
      // offer and make sure remove returns that same item
      Object o = getFullElements()[0];
      stack.push(o);
      assertFalse(stack.isEmpty());
      assertEquals(o, stack.pop());
      
      // offer several
      Stack<Object> benchmark = getConfirmed();
      for (Object obj : getFullElements()) {
         stack.push(obj);
         benchmark.push(obj);
         verify();
      }
      
      while (!stack.isEmpty()) {
         Object o1 = stack.poll();
         Object o2 = benchmark.poll();
         assertSame(o1, o2);
         verify();
      }
   }

   public void testPoll() {
      // remove from empty stack
      resetEmpty();
      Stack<Object> stack = getCollection();
      assertNull(stack.poll());
      
      // remove from stack with single element
      Object o = getFullElements()[0];
      stack.add(o);
      assertEquals(o, stack.poll());
      assertTrue(stack.isEmpty());
      
      // repeated removes from a full stack
      resetFull();
      stack = getCollection();
      Stack<Object> benchmark = getConfirmed();
      
      // seed w/ lots o' data
      stack.addAll(Arrays.asList(getFullElements()));
      stack.addAll(Arrays.asList(getFullElements()));
      stack.addAll(Arrays.asList(getOtherNonNullStringElements()));
      stack.addAll(Arrays.asList(getOtherNonNullStringElements()));

      benchmark.addAll(Arrays.asList(getFullElements()));
      benchmark.addAll(Arrays.asList(getFullElements()));
      benchmark.addAll(Arrays.asList(getOtherNonNullStringElements()));
      benchmark.addAll(Arrays.asList(getOtherNonNullStringElements()));
      
      verify();
      
      while (!stack.isEmpty()) {
         Object o1 = stack.poll();
         Object o2 = benchmark.poll();
         assertEquals(o2, o1);
         verify();
      }

      assertNull(stack.poll());
   }
   
   public void testPeek() {
      // (very similar to testPoll, but we verify that peek doesn't remove and subsequent poll
      // returns the same element as returned by peek)
      
      // remove from empty stack
      resetEmpty();
      Stack<Object> stack = getCollection();
      assertNull(stack.peek());

      // remove from stack with single element
      Object o = getFullElements()[0];
      stack.add(o);
      assertEquals(o, stack.peek());
      
      // repeated removes from a full stack
      resetFull();
      stack = getCollection();
      Stack<Object> benchmark = getConfirmed();
      
      verify();
      
      while (!stack.isEmpty()) {
         o = benchmark.poll();
         assertEquals(o, stack.peek());
         assertEquals(o, stack.poll());
         verify();
      }

      assertNull(stack.peek());
   }
   
   public void testElement() {
      // (basically same test as testPeek, except throws exception when empty instead
      // of returning null)
      
      // remove from empty stack
      resetEmpty();
      Stack<Object> stack = getCollection();
      try {
         stack.element();
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }

      // remove from stack with single element
      Object o = getFullElements()[0];
      stack.add(o);
      assertEquals(o, stack.element());
      
      // repeated removes from a full stack
      resetFull();
      stack = getCollection();
      Stack<Object> benchmark = getConfirmed();
      
      verify();
      
      while (!stack.isEmpty()) {
         o = benchmark.poll();
         assertEquals(o, stack.element());
         assertEquals(o, stack.poll());
         verify();
      }

      try {
         stack.element();
         fail("Expecting exception but none thrown");
      } catch (NoSuchElementException expected) {
      }
   }
}
