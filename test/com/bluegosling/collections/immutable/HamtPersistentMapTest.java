package com.bluegosling.collections.immutable;

import com.bluegosling.testing.BulkTestRunner;

import org.apache.commons.collections.map.AbstractTestMap;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

//TODO: test cases for PersistentMap methods
@RunWith(BulkTestRunner.class)
public class HamtPersistentMapTest extends AbstractTestMap {
   
   public HamtPersistentMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<Object, Object> makeEmptyMap() {
      return HamtPersistentMap.create();
   }

   @Override
   public Map<Object, Object> makeFullMap() {
      HashMap<Object, Object> m = new HashMap<>();
      addSampleMappings(m);
      return HamtPersistentMap.create(m);
   }

   @Override
   public boolean isPutAddSupported() {
      return false;
   }

   @Override
   public boolean isPutChangeSupported() {
      return false;
   }

   @Override
   public boolean isRemoveSupported() {
      return false;
   }

   @Override
   public boolean isSetValueSupported() {
      return false;
   }
   
   @Override
   public void testMakeMap() {
      // Superclass implementation tries to ensure that repeated calls to makeEmptyMap() return
      // distinct objects. But, since we're testing an immutable data structure, creating an empty
      // map always returns the same instance -- a constant empty map. So we skip this test.
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
   }
   
   @Override
   public void verifyMap() {
      super.verifyMap();
      // map equality should go both ways
      assertTrue(confirmed.equals(map));
   }
}
