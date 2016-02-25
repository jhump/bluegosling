package com.apriori.collections;

import com.apriori.testing.BulkTestRunner;

import org.apache.commons.collections.map.AbstractTestMap;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.Map;

// TODO: use a test for ImmutableMaps/PersistentMaps? as is, we wrap the persistent map into a
// mutable implementation and then run that through the existing Apache map test
@RunWith(BulkTestRunner.class)
public class HamtPersistentMapTest extends AbstractTestMap {
   
   public HamtPersistentMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<Object, Object> makeEmptyMap() {
      // sneaky way of exposing a persistent map via standard/mutable map interface
      return new PersistentMapBackedConcurrentMap<>(HamtPersistentMap.create());
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
   
   @SuppressWarnings("rawtypes")
   public void testHashCollision() {
      // HamtPersistentMap uses a linked list to manage multiple mappings for the same hash code.
      // So this test case checks that it works.
      resetEmpty();
      putCollidingEntries();
      // remove items w/ colliding hash codes via Map.remove(...)
      for (Iterator keyIter = confirmed.keySet().iterator(); keyIter.hasNext(); ) {
         Object key = keyIter.next();
         keyIter.remove();
         map.remove(key);
         verify();
      }
      assertTrue(map.isEmpty());

      putCollidingEntries();
      // and try remove items w/ colliding hash codes via Iterator.remove()
      for (Iterator keyIter = map.keySet().iterator(); keyIter.hasNext(); ) {
         Object key = keyIter.next();
         keyIter.remove();
         confirmed.remove(key);
         verify();
      }
      assertTrue(map.isEmpty());
   }
   
   @SuppressWarnings("unchecked")
   private void putCollidingEntries() {
      for (int i = 0; i < 10; i++) {
         Object o = new HashCollision(123);
         map.put(o, o);
         confirmed.put(o, o);
         verify();
      }
      for (int i = 0; i < 10; i++) {
         Object o = new HashCollision(54321);
         map.put(o, o);
         confirmed.put(o, o);
         verify();
      }
      for (int i = 0; i < 10; i++) {
         Object o = new HashCollision(-1029384756);
         map.put(o, o);
         confirmed.put(o, o);
         verify();
      }
   }
   
   private static class HashCollision {
      private final int hash;
      HashCollision(int hash) {
         this.hash = hash;
      }
      @Override public int hashCode() {
         return hash;
      }
   }
}
