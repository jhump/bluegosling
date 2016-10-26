package com.bluegosling.collections.sorting;

import static org.junit.Assert.assertEquals;

import com.bluegosling.collections.sorting.SmoothSort;
import com.bluegosling.time.Stopwatch;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class SmoothSortTest {
   
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
      Stopwatch sw1 = new Stopwatch();
      Stopwatch sw2 = new Stopwatch();
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
            sw1.start();
            Collections.sort(list1, Comparator.naturalOrder());
            sw1.stop();
            sw1.lap();
            // ours
            sw2.start();
            SmoothSort.sort(list2);
            sw2.stop();
            sw2.lap();
            
            assertEquals(list1, list2);
         }
         
         System.out.println("Benchmark:");
         printResults(sw1);
         System.out.println("Smoothsort:");
         printResults(sw2);

         sw1.reset();
         sw2.reset();
         
         count /= 10;
         maxLen *= 10;
      }
   }
   
   private void printResults(Stopwatch sw) {
      long laps[] = sw.lapResults(TimeUnit.MICROSECONDS);
      long sum = 0, max = Long.MIN_VALUE;
      for (long lap : laps) {
         sum += lap;
         if (lap > max) {
            max = lap;
         }
      }
      System.out.println(String.format(" Total: %dms (%d laps), Avg: %dus, Max: %dus",
            TimeUnit.MICROSECONDS.toMillis(sum), laps.length, Math.round(1.0*sum / laps.length), max));
   }
}
