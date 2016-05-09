package com.bluegosling.collections;

import java.util.LinkedList;

import org.junit.runner.RunWith;

import com.bluegosling.testing.BulkTestRunner;

@RunWith(BulkTestRunner.class)
public class StackFromDequeTest extends AbstractTestStack {

   public StackFromDequeTest(String testName) {
      super(testName);
   }

   @Override
   public Stack<Object> makeCollection() {
      return Stack.fromDeque(new LinkedList<>());
   }
}
