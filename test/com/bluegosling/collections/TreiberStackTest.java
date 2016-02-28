package com.bluegosling.collections;

import com.bluegosling.testing.BulkTestRunner;

import org.apache.commons.collections.collection.AbstractTestCollection;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@RunWith(BulkTestRunner.class)
public class TreiberStackTest extends AbstractTestCollection {
   
   public TreiberStackTest(String testName) {
      super(testName);
   }

   @Override
   public Collection<?> makeConfirmedCollection() {
      return new ArrayList<Object>();
   }

   @Override
   public Collection<?> makeConfirmedFullCollection() {
      return new ArrayList<Object>(Arrays.asList(getFullElements()));
   }

   @Override
   public Collection<?> makeCollection() {
      return new TreiberStack<>();
   }

   @Override
   public Collection<?> makeFullCollection() {
      return new TreiberStack<>(Arrays.asList(getFullElements()));
   }
   
   // TODO: concurrency test
   // TODO: tests for Stack methods
}
