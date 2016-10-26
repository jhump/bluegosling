package com.bluegosling.collections.maps;

import com.bluegosling.collections.AbstractTestNavigableMap;
import com.bluegosling.collections.maps.SortedArrayMap;
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
