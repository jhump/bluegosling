package com.apriori.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A utility for sorting large lists using multiple CPUs/cores. This is a simplistic parallel sort
 * that breaks the incoming list into chunks, sorts all of the chunks in parallel, and then finally
 * merges the chunks. Only the sorting of chunks is parallelized.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see SlowParallelSort
 */
public class ParallelSort {

   /**
    * Sorts the specified list using the specified number of threads.
    * 
    * @param <T> the type of element in the list
    * @param list the list to sort
    * @param requestedNumThreads the number of threads to use
    * @throws NullPointerException if any of the reference arguments are null or if the specified
    *       list contains any null elements
    * @throws IllegalArgumentException if the specified number of threads is zero or negative
    * @throws RuntimeException if this thread is interrupted while waiting on parallel operations
    *       to complete
    */
   public static <T extends Comparable<T>> void sort(List<T> list, int requestedNumThreads) {
      sort(list, CollectionUtils.<T> naturalOrdering(), requestedNumThreads);
   }
   
   /**
    * Sorts the specified list using the specified number of threads.
    * 
    * @param <T> the type of element in the list
    * @param list the list to sort
    * @param comparator the comparator to use for ordering items relative to one another
    * @param requestedNumThreads the number of threads to use
    * @throws NullPointerException if any of the reference arguments are null or if the specified
    *       list contains any null elements
    * @throws IllegalArgumentException if the specified number of threads is zero or negative
    * @throws RuntimeException if this thread is interrupted while waiting on parallel operations
    *       to complete
    */
   public static <T> void sort(List<T> list, final Comparator<? super T> comparator,
         int requestedNumThreads) {
      // validate inputs
      if (list == null || comparator == null) {
         throw new NullPointerException();
      }
      if (list.isEmpty()) {
         return;
      }
      if (requestedNumThreads < 1) {
         throw new IllegalArgumentException("number of threads must be >= 1");
      } else if (requestedNumThreads == 1) {
         // one thread? just sort it right here
         Collections.sort(list, comparator);
         return;
      }
      
      int remaining = list.size();
      final int numThreads = requestedNumThreads > remaining ? remaining : requestedNumThreads;
      Future<?> results[] = new Future<?>[numThreads - 1];
      ExecutorService svc = Executors.newFixedThreadPool(numThreads - 1);
      try {
         @SuppressWarnings("unchecked")
         T chunks[][] = (T[][]) new Object[numThreads][];
         // break input into array chunks (check for nulls while doing so)
         Iterator<? extends T> iter = list.iterator();
         for (int i = 0; i < numThreads; i++) {
            int sz = remaining / (numThreads - i);
            @SuppressWarnings("unchecked")
            final T chunk[] = (T[]) new Object[sz];
            for (int j = 0; j < sz; j++) {
               T item = iter.next();
               if (item == null) {
                  throw new NullPointerException("items contained a null value");
               }
               chunk[j] = item;
            }
            chunks[i] = chunk;
            // kick off task to sort it
            if (i == numThreads - 1) {
               // last chunk? sort it in current thread
               Arrays.sort(chunk, comparator);
            } else {
               results[i] = svc.submit(new Runnable() {
                  @Override public void run() {
                     Arrays.sort(chunk, comparator);
                  }
               });
            }
            remaining -= sz;
         }
         // wait for chunks to all be sorted
         try {
            for (int i = 0, len = results.length; i < len; i++) {
               results[i].get();
            }
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof Error) {
               throw (Error) t;
            } else if (t instanceof RuntimeException) {
               throw (RuntimeException) t;
            } else {
               throw new RuntimeException(t);
            }
         }
         // and now merge them
         if (numThreads > 100 /* PriorityQueue doesn't make sense for values smaller than this */) {
            logMerge(chunks, comparator, list.listIterator());
         } else {
            linearMerge(chunks, comparator, list.listIterator());
         }
      } finally {
         svc.shutdownNow();
      }
   }
   
   private static <T> void logMerge(final T chunks[][], final Comparator<? super T> comparator,
         ListIterator<T> iter) {
      final int headIndex[] = new int[chunks.length];
      PriorityQueue<Integer> chunkQueue = new PriorityQueue<Integer>(chunks.length,
            new Comparator<Integer>() {
               @Override public int compare(Integer i1, Integer i2) {
                  return comparator.compare(chunks[i1][headIndex[i1]], chunks[i2][headIndex[i2]]);
               }
            });
      for (int i = 0; i < chunks.length; i++) {
         chunkQueue.add(i);
      }
      while (!chunkQueue.isEmpty()) {
         int nextChunk = chunkQueue.remove();
         int curHead = headIndex[nextChunk];
         iter.next();
         iter.set(chunks[nextChunk][curHead]);
         if (++curHead < chunks[nextChunk].length) {
            headIndex[nextChunk] = curHead;
            chunkQueue.add(nextChunk);
         }
      }
   }

   private static <T> void linearMerge(T chunks[][], Comparator<? super T> comparator,
         ListIterator<T> iter) {
      final int headIndex[] = new int[chunks.length];
      int len = chunks.length;
      while (true) {
         T min = null;
         int minIndex = -1;
         for (int i = 0; i < len; i++) {
            int curHead = headIndex[i];
            if (curHead >= 0) {
               T curValue = chunks[i][curHead];
               if (min == null || comparator.compare(curValue, min) < 0) {
                  min = curValue;
                  minIndex = i;
               }
            }
         }
         if (min == null) {
            return; // done
         } else {
            iter.next();
            iter.set(min);
            int newHead = headIndex[minIndex] + 1;
            headIndex[minIndex] = newHead >= chunks[minIndex].length ? -1 : newHead;
         }
      }
   }
}
