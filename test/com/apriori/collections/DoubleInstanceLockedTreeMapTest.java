package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.Map;

// TODO: ConcurrentMap test cases
@RunWith(BulkTestRunner.class)
public class DoubleInstanceLockedTreeMapTest extends AbstractTestNavigableMap {

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
