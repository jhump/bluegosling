package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.concurrent.TreiberStack;
import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(BulkTestRunner.class)
public class TreiberStackTest extends AbstractTestConcurrentStack {
   
   public TreiberStackTest(String testName) {
      super(testName);
   }

   @Override
   public ConcurrentStack<Object> makeCollection() {
      return new TreiberStack<>();
   }

   @Override
   public ConcurrentStack<Object> makeFullCollection() {
      return new TreiberStack<>(Arrays.asList(getFullElements()));
   }
   
   // TODO: concurrency test
}
