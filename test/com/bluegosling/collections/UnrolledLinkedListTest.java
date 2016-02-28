package com.bluegosling.collections;

import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.List;

@RunWith(BulkTestRunner.class)
public class UnrolledLinkedListTest extends AbstractTestList {

   public UnrolledLinkedListTest(String testName) {
      super(testName);
   }

   @Override
   public List<Object> makeEmptyList() {
      return new UnrolledLinkedList<Object>(5);
   }

   @Override
   public boolean isFailFastSupported() {
      return true;
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
}
