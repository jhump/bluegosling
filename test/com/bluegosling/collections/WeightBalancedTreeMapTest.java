package com.bluegosling.collections;

import com.bluegosling.testing.BulkTestRunner;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import java.util.Map;

// TODO make this test work (fix WeightBalancedTreeMap where necessary) and un-ignore
@Ignore
@RunWith(BulkTestRunner.class)
public class WeightBalancedTreeMapTest extends AbstractTestRandomAccessNavigableMap {

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
