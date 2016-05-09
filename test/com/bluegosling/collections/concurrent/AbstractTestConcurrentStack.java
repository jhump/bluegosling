package com.bluegosling.collections.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.bluegosling.collections.AbstractTestStack;
import com.bluegosling.collections.ArrayUtils;
import com.bluegosling.collections.Stack;

public abstract class AbstractTestConcurrentStack extends AbstractTestStack {

   protected AbstractTestConcurrentStack(String testName) {
      super(testName);
   }

   public ConcurrentStack<Object> getCollection() {
      return (ConcurrentStack<Object>) super.getCollection();
   }
   
   @Override
   public abstract ConcurrentStack<Object> makeCollection();

   @Override
   public ConcurrentStack<Object> makeFullCollection() {
      return (ConcurrentStack<Object>) super.makeFullCollection();
   }
   
   public void testDrainTo() {
      Object[] elements = getFullElements();
      ArrayUtils.reverse(elements);

      ConcurrentStack<Object> stack = (ConcurrentStack<Object>) makeFullCollection();
      
      Collection<Object> coll = new ArrayList<>();
      
      stack.drainTo(coll);
      
      assertTrue(stack.isEmpty());
      assertFalse(stack.iterator().hasNext());

      assertFalse(coll.isEmpty());
      assertTrue(coll.iterator().hasNext());

      assertTrue(Arrays.equals(coll.toArray(), elements));
   }

   public void testRemoveAll() {
      Object[] elements = getFullElements();
      ArrayUtils.reverse(elements);
      
      ConcurrentStack<Object> stack = (ConcurrentStack<Object>) makeFullCollection();
      
      ConcurrentStack<Object> copy = stack.removeAll();
      
      assertTrue(stack.isEmpty());
      assertFalse(stack.iterator().hasNext());
      
      assertFalse(copy.isEmpty());
      assertTrue(copy.iterator().hasNext());

      assertTrue(Arrays.equals(copy.toArray(), elements));
   }
}
