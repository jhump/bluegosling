package com.bluegosling.collections.sorting;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LongSummaryStatistics;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Stopwatch;


public class SmoothSortTest {
   private static final long NANOS_PER_MILLI = TimeUnit.MILLISECONDS.toNanos(1);
   private static final long NANOS_PER_MICRO = TimeUnit.MICROSECONDS.toNanos(1);
   
   private static final Random RANDOM = new Random(1);
   
   @Test public void sort_list() {
      for (int i = 0; i < 100; i++) {
         int len = RANDOM.nextInt(10000) + 1;
         ArrayList<Integer> list1 = new ArrayList<Integer>(len);
         ArrayList<Integer> list2 = new ArrayList<Integer>(len);
         for (int j = 0; j < len; j++) {
            Integer v = RANDOM.nextInt();
            list1.add(v);
            list2.add(v);
         }
         // benchmark
         Collections.sort(list1);
         // ours
         SmoothSort.sort(list2);
         
         assertEquals(list1, list2);
      }
   }

   @Test public void sort_listAndComparator() {
      Comparator<Object> lexical = new Comparator<Object>() {
         @Override public int compare(Object o1, Object o2) {
            return String.valueOf(o1).compareTo(String.valueOf(o2));
         }
      };
      for (int i = 0; i < 100; i++) {
         int len = RANDOM.nextInt(1000) + 1;
         ArrayList<Integer> list1 = new ArrayList<Integer>(len);
         ArrayList<Integer> list2 = new ArrayList<Integer>(len);
         for (int j = 0; j < len; j++) {
            Integer v = RANDOM.nextInt();
            list1.add(v);
            list2.add(v);
         }
         // benchmark
         Collections.sort(list1, lexical);
         // ours
         SmoothSort.sort(list2, lexical);
         
         assertEquals(list1, list2);
      }
   }
   
   @Ignore
   @Test public void sort_list_performance() {
      // warm up
      int maxLen = 10;
      int count = 1000;
      while (count > 0) {
         for (int i = 0; i < count; i++) {
            int len = RANDOM.nextInt(maxLen) + 1;
            ArrayList<Integer> list1 = new ArrayList<Integer>(len);
            ArrayList<Integer> list2 = new ArrayList<Integer>(len);
            for (int j = 0; j < len; j++) {
               Integer v = RANDOM.nextInt();
               list1.add(v);
               list2.add(v);
            }
            // benchmark
            Collections.sort(list1, Comparator.naturalOrder());
            // ours
            SmoothSort.sort(list2);
            
            assertEquals(list1, list2);
         }
         count /= 10;
         maxLen *= 10;
      }
      // now measure
      Stopwatch sw = Stopwatch.createUnstarted();
      LongSummaryStatistics stats1 = new LongSummaryStatistics();
      LongSummaryStatistics stats2 = new LongSummaryStatistics();
      maxLen = 100;
      count = 10000;
      while (count > 0) {
         for (int i = 0; i < count; i++) {
            int len = RANDOM.nextInt(maxLen) + 1;
            ArrayList<Integer> list1 = new ArrayList<Integer>(len);
            ArrayList<Integer> list2 = new ArrayList<Integer>(len);
            for (int j = 0; j < len; j++) {
               Integer v = RANDOM.nextInt();
               list1.add(v);
               list2.add(v);
            }
            // benchmark
            sw.reset().start();
            Collections.sort(list1, Comparator.naturalOrder());
            sw.stop();
            stats1.accept(sw.elapsed(TimeUnit.NANOSECONDS));
            // ours
            sw.reset().start();
            SmoothSort.sort(list2);
            sw.stop();
            stats2.accept(sw.elapsed(TimeUnit.NANOSECONDS));
            
            assertEquals(list1, list2);
         }
         
         System.out.println("Benchmark:");
         printResults(stats1);
         System.out.println("Smoothsort:");
         printResults(stats2);

         stats1 = new LongSummaryStatistics();
         stats2 = new LongSummaryStatistics();
         
         count /= 10;
         maxLen *= 10;
      }
   }
   
   private void printResults(LongSummaryStatistics stats) {
      System.out.println(String.format(" Total: %dms (%d laps), Avg: %dus, Max: %dus",
            stats.getSum() / NANOS_PER_MILLI,
            stats.getCount(),
            Math.round(stats.getAverage() / NANOS_PER_MICRO),
            stats.getMax() / NANOS_PER_MICRO));
   }
}
