package com.apriori.collections;

import java.util.Map;

import junit.framework.TestSuite;

// TODO make this test work (fix WeightBalancedTreeMap where necessary)
public class WeightBalancedTreeMapTest extends AbstractTestRandomAccessNavigableMap {

   public static TestSuite suite() {
      return makeSuite(WeightBalancedTreeMapTest.class);
   }

   public WeightBalancedTreeMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<?, ?> makeEmptyMap() {
      return new WeightBalancedTreeMap<Object, Object>();
   }
   
   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
}
