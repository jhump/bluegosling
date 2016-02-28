package com.bluegosling.collections;

import static org.junit.Assert.assertEquals;

import com.bluegosling.util.Stopwatch;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO: javadoc
public class ParallelSortTest {

   private List<Integer> getDescendingList(int numItems) {
      ArrayList<Integer> list = new ArrayList<Integer>(numItems);
      for (int i = 0; i < numItems; i++) {
         list.add(numItems - i);
      }
      return list;
   }
   
   private List<Integer> sortIntegers(int numIntegers) {
      List<Integer> list = getDescendingList(numIntegers);
      Collections.sort(list);
      return list;
   }
   
   private List<Integer> parallelSortIntegers(int numIntegers, int numThreads) {
      List<Integer> list = getDescendingList(numIntegers);
      ParallelSort.sort(list, numThreads);
      return list;
   }
   
   private void warmUp() {
      System.out.println("Making sure VM is warm...");
      // sort for 5 seconds
      long start = System.currentTimeMillis();
      int count = 0;
      while (true) {
         List<Integer> l1 = sortIntegers(5000);
         List<Integer> l2 = parallelSortIntegers(5000, 1);
         assertEquals(l1, l2);
         l1 = sortIntegers(5000);
         l2 = parallelSortIntegers(5000, 2);
         assertEquals(l1, l2);
         l1 = sortIntegers(5000);
         l2 = parallelSortIntegers(5000, 5);
         assertEquals(l1, l2);
         l1 = sortIntegers(5000);
         l2 = parallelSortIntegers(5000, 10);
         assertEquals(l1, l2);
         count++;
         if (System.currentTimeMillis() > start + 5000) {
            break; // done warming up
         }
      }
      System.out.println("... Done (completed warm-up loop " + count + " times)");
   }
   
   @Test public void smallSet() {
      assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), parallelSortIntegers(10, 3));
   }
   
   @Test public void oneItemPerThread() {
      assertEquals(Arrays.asList(1, 2, 3), parallelSortIntegers(3, 3));
   }
   
   @Test public void fewerItemsThanThreads() {
      assertEquals(Arrays.asList(1, 2, 3, 4, 5), parallelSortIntegers(5, 3));
   }
   
   @Test public void singleThread() {
      assertEquals(Arrays.asList(1, 2, 3, 4, 5), parallelSortIntegers(5, 1));
   }
   
   @Test public void sortMillionIntegersTimed() {
      warmUp();
      
      int numItems = 1000*1000;
      
      Stopwatch stopwatch = new Stopwatch();
      for (int i = 0; i < 5; i++) {
         List<Integer> list = getDescendingList(numItems);
         stopwatch.start();
         Collections.sort(list);
         stopwatch.stop();
         stopwatch.lap();
      }
      
      System.out.println("Total time with one thread (avg of 5 runs, in millis): "
            + stopwatch.lapAverage(TimeUnit.MILLISECONDS));

      stopwatch.reset();
      for (int i = 0; i < 5; i++) {
         List<Integer> list = getDescendingList(numItems);
         stopwatch.start();
         ParallelSort.sort(list, 4);
         stopwatch.stop();
         stopwatch.lap();
      }
      
      System.out.println("Total time with four threads (avg of 5 runs, in millis): "
            + stopwatch.lapAverage(TimeUnit.MILLISECONDS));
   }
}
