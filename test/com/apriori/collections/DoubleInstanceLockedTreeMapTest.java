package com.apriori.collections;

import java.util.Map;

import junit.framework.TestSuite;

// TODO: ConcurrentMap test cases
public class DoubleInstanceLockedTreeMapTest extends AbstractTestNavigableMap {

   public static TestSuite suite() {
      return makeSuite(DoubleInstanceLockedTreeMapTest.class);
   }

   public DoubleInstanceLockedTreeMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<?, ?> makeEmptyMap() {
      return new DoubleInstanceLockedTreeMap<>();
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      //this.isTestSerialization()
      return true;
   }
}
