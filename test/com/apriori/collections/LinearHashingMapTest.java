package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.apache.commons.collections.map.AbstractTestMap;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(BulkTestRunner.class)
public class LinearHashingMapTest extends AbstractTestMap {
   
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
