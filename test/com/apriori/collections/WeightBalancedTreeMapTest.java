package com.apriori.collections;

import org.apache.commons.collections.map.AbstractTestSortedMap;

import java.util.Map;

// TODO make this test work (fix WeightBalancedTreeMap where necessary)
public class WeightBalancedTreeMapTest extends AbstractTestSortedMap {

   public WeightBalancedTreeMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<?, ?> makeEmptyMap() {
      return new WeightBalancedTreeMap<Object, Object>();
   }

}
