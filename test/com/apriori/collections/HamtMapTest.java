package com.apriori.collections;

import org.apache.commons.collections.map.AbstractTestMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import junit.framework.TestSuite;

public class HamtMapTest extends AbstractTestMap {

   public static TestSuite suite() {
      return makeSuite(HamtMapTest.class);
   }
   
   public HamtMapTest(String testName) {
      super(testName);
   }

   @Override
   public Map<?, ?> makeEmptyMap() {
      return new HamtMap<Object, Object>();
   }

   @Override
   protected boolean skipSerializedCanonicalTests() {
      return true;
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
      run(100 * 1000, map1, r1, new Stats());
      run(100 * 1000, map2, r2, new Stats());

      System.out.println("Initial size of map: " + map1.size());
      System.out.println("Initial size of map: " + map2.size());

      // run for 10 seconds, measuring how many ops per second we achieve
      long startNanos = System.nanoTime();
      long endNanos = startNanos + TimeUnit.SECONDS.toNanos(10);
      Stats stats1 = new Stats();
      do {
         run(10 * 1000, map1, r1, stats1);
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
   
   private void run(int numberOfOps, Map<Long, Object> map, Random r, Stats stats) {
      for (int i = 0; i < numberOfOps; i++) {
         int c = r.nextInt(100);
         if (c < 90) {
            long key = r.nextInt(100 * 1000);
            map.get(key);
            stats.numGets++;
         } else if (c < 95) {
            long key = r.nextInt(100 * 1000);
            map.put(key, null);
            stats.numPuts++;
         } else if (c < 99) {
            long key = r.nextInt(100 * 1000);
            map.remove(key);
            stats.numRemoves++;
         } else {
            int n = 0;
            for (Map.Entry<Long, Object> entry : map.entrySet()) {
               n++;
            }
            stats.numIterations++;
            stats.totalMappingsIterated += n;
         }
      }
   }
}
