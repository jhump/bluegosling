package com.apriori.collections;

import org.apache.commons.collections.map.AbstractTestMap;

import java.util.Map;

import junit.framework.TestSuite;

public class LinearHashingMapTest extends AbstractTestMap {

   public static TestSuite suite() {
      return makeSuite(LinearHashingMapTest.class);
   }
   
   public LinearHashingMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<?, ?> makeEmptyMap() {
      return new LinearHashingMap<Object, Object>();
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
}
