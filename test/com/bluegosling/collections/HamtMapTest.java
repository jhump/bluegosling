package com.bluegosling.collections;

import com.bluegosling.testing.BulkTestRunner;

import org.apache.commons.collections.map.AbstractTestMap;
import org.junit.runner.RunWith;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@RunWith(BulkTestRunner.class)
public class HamtMapTest extends AbstractTestMap {
   
   public HamtMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<Object, Object> makeEmptyMap() {
      return new HamtMap<Object, Object>();
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
      // HamtMap uses a linked list to manage multiple mappings for the same hash code.
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
   public void testEntryViewHandlesConcurrentModifications() {
      resetEmpty();
      Object o1 = new HashCollision(0);
      Object o2 = new HashCollision(0);
      map.put(o1, 1);
      map.put(o2, 2);
      Iterator<Entry<Object, Object>> iter = map.entrySet().iterator();
      // TrieNode is also head of linked list. Head will be item #1. Its next points to item #2
      Entry<Object, Object> e1 = iter.next();
      assertEquals(1, e1.getValue());
      Entry<Object, Object> e2 = iter.next();
      assertEquals(2, e2.getValue());
      // To simplify implementation, TrieNodes won't remove themselves from their list (that
      // would require more complex interface so that parent node knows when to replace their
      // ref with a new node that is the new head-of-list). So instead, removing item #1 will
      // actually cause it to swap places with item #2, so we can do simple removal of list node
      // without trying to update the TrieNode from its parent.
      map.remove(o1);
      // The underlying list nodes are now swapped. But the Entry implementation is smart enough
      // to detect that. Removed node continues to report its last known values
      assertSame(o1, e1.getKey());
      assertEquals(1, e1.getValue());
      // Node that still exists also sees that it swapped and finds correct underlying list node.
      assertSame(o2, e2.getKey());
      assertEquals(2, e2.getValue());
      assertEquals(2, map.put(o2, 42));
      assertEquals(42, e2.getValue()); // entry shows new value
      assertEquals(42, e2.setValue(99));
      assertEquals(99, e2.getValue());
      assertEquals(99, map.get(o2));
      // cannot set a value on a node that has been removed
      try {
         e1.setValue(0);
         fail();
      } catch (ConcurrentModificationException expected) {
      }
      // but, behold, entry is valid again if key re-appears in map
      map.put(o1, 0);
      assertEquals(0, e1.getValue());
      assertEquals(0, e1.setValue(100));
      assertEquals(100, map.get(o1));
      // latest updated value is memoized
      map.remove(o2);
      assertEquals(99, e2.getValue());
      try {
         e2.setValue(0);
         fail();
      } catch (ConcurrentModificationException expected) {
      }
   }
   
   @SuppressWarnings("unchecked")
   public void testExpandAndCollapseInnerLeafNodes() {
      resetEmpty();
      // this will create shallow map -- just one inner-leaf node
      Object o1 = new HashCollision(0);
      map.put(o1, o1);
      confirmed.put(o1, o1);
      assertEquals(1, ((HamtMap<?, ?>) map).depth());
      // this hash only varies in the most-significant bit, which means the inner-leaf node must
      // be expanded all the way down to the bottom to accommodate
      Object o2 = new HashCollision(Integer.MIN_VALUE);
      map.put(o2, o2);
      confirmed.put(o2, o2);
      verify();
      // all six intermediate levels + leaf level
      assertEquals(7, ((HamtMap<?, ?>) map).depth());
      // this should then collapse the branch all the way back to one inner-leaf node
      map.remove(o1);
      confirmed.remove(o1);
      verify();
      assertEquals(1, ((HamtMap<?, ?>) map).depth());
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


   
   private static class Stats {
      int numPuts;
      int numGets;
      int numRemoves;
      int numIterations;
      int totalMappingsIterated;

      Stats() {
      }
   }

   public void testPerformance() {
      Random r1 = new Random(0);
      Random r2 = new Random(0);
      HamtMap<Long, Object> map1 = new HamtMap<Long, Object>();
      HashMap<Long, Object> map2 = new HashMap<Long, Object>();
      // try to warm-up the code
      run(500 * 1000, map1, r1, new Stats());
      run(500 * 1000, map2, r2, new Stats());

      System.out.println("Initial size of map: " + map1.size());
      showDiffs(map1, map2);
      assertEquals(map1, map2);
      assertEquals(map2, map1);

      // run for 10 seconds, measuring how many ops per second we achieve
      long startNanos = System.nanoTime();
      long endNanos = startNanos + TimeUnit.SECONDS.toNanos(10);
      Stats stats1 = new Stats();
      do {
         run(1000, map1, r1, stats1);
      } while (System.nanoTime() <= endNanos);
      System.out.println("Final size of map: " + map1.size());
      System.out.println("Number of puts: " + stats1.numPuts);
      System.out.println("Number of gets: " + stats1.numGets);
      System.out.println("Number of removes: " + stats1.numRemoves);
      System.out.println("Number of iterations: " + stats1.numIterations);
      System.out.println("   Avg iteration len: " + (stats1.totalMappingsIterated * 1.0 / stats1.numIterations));

      // and again for HashMap
      startNanos = System.nanoTime();
      endNanos = startNanos + TimeUnit.SECONDS.toNanos(10);
      Stats stats2 = new Stats();
      do {
         run(10 * 1000, map2, r2, stats2);
      } while (System.nanoTime() <= endNanos);

      System.out.println("Final size of map: " + map2.size());
      System.out.println("Number of puts: " + stats2.numPuts);
      System.out.println("Number of gets: " + stats2.numGets);
      System.out.println("Number of removes: " + stats2.numRemoves);
      System.out.println("Number of iterations: " + stats2.numIterations);
      System.out.println("   Avg iteration len: " + (stats2.totalMappingsIterated * 1.0 / stats2.numIterations));
   }
   
   private void run(int numberOfOps, Map<Long, Object> mapUnderTest, Random r, Stats stats) {
      for (int i = 0; i < numberOfOps; i++) {
         int c = r.nextInt(100);
         if (c < 90) {
            long key = r.nextInt(100 * 1000);
            mapUnderTest.get(key);
            stats.numGets++;
         } else if (c < 95) {
            long key = r.nextInt(100 * 1000);
            mapUnderTest.put(key, null);
            stats.numPuts++;
         } else if (c < 99) {
            long key = r.nextInt(100 * 1000);
            mapUnderTest.remove(key);
            stats.numRemoves++;
         } else {
            int n = 0;
            for (Iterator<Entry<Long, Object>> iter = mapUnderTest.entrySet().iterator();
                  iter.hasNext();) {
               iter.next();
               n++;
            }
            stats.numIterations++;
            stats.totalMappingsIterated += n;
         }
      }
   }
   
   private void showDiffs(Map<Long, Object> map1, Map<Long, Object> map2) {
      TreeMap<Long, Object> tree1 = new TreeMap<>(map1);
      TreeMap<Long, Object> tree2 = new TreeMap<>(map2);
      Iterator<Entry<Long, Object>> entries1 = tree1.entrySet().iterator();
      Iterator<Entry<Long, Object>> entries2 = tree2.entrySet().iterator();
      Entry<Long, Object> e1 = null;
      Entry<Long, Object> e2 = null;
      while ((e1 != null || entries1.hasNext()) && (e2 != null || entries2.hasNext())) {
         if (e1 == null) {
            e1 = entries1.next();
            assertEquals(e1.getValue(), map1.get(e1.getKey()));
            assertTrue(map1.containsKey(e1.getKey()));
         }
         if (e2 == null) {
            e2 = entries2.next();
            assertEquals(e2.getValue(), map2.get(e2.getKey()));
            assertTrue(map2.containsKey(e2.getKey()));
         }
         if (e1.getKey() > e2.getKey()) {
            System.out.println("Map #1 missing key " + e2.getKey());
            e2 = null;
         } else if (e1.getKey() < e2.getKey()) {
            System.out.println("Map #2 missing key " + e1.getKey());
            e1 = null;
         } else {
            // equal, go to next key
            assertEquals(e1.getValue(), e2.getValue());
            e1 = e2 = null;
         }
      }
      while (entries1.hasNext()) {
         System.out.println("Map #2 missing key " + entries1.next().getKey());
      }
      while (entries2.hasNext()) {
         System.out.println("Map #1 missing key " + entries2.next().getKey());
      }
   }
}
