package com.apriori.collections;

import java.util.Map;

import junit.framework.TestSuite;


public class SortedArrayMapTest extends AbstractTestNavigableMap {

   public static TestSuite suite() {
      return makeSuite(SortedArrayMapTest.class);
   }

   public SortedArrayMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<Object, Object> makeEmptyMap() {
      return new SortedArrayMap<Object, Object>();
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
}
