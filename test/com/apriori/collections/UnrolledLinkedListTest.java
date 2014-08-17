package com.apriori.collections;

import java.util.List;

import junit.framework.TestSuite;


public class UnrolledLinkedListTest extends AbstractTestList {

   public static TestSuite suite() {
      return makeSuite(UnrolledLinkedListTest.class);
   }

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
