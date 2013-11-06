package com.apriori.collections;

import org.apache.commons.collections.map.AbstractTestMap;

import java.util.Map;

import junit.framework.TestSuite;


public class HamtMapTest extends AbstractTestMap {

   public static TestSuite suite() {
      return makeSuite(HamtMapTest.class);
   }
   
   public HamtMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<?, ?> makeEmptyMap() {
      return new HamtMap<Object, Object>();
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
}
