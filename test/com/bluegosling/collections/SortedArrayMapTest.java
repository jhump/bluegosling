package com.bluegosling.collections;

import com.bluegosling.testing.BulkTestRunner;

import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(BulkTestRunner.class)
public class SortedArrayMapTest extends AbstractTestNavigableMap {

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
