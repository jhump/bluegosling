package com.bluegosling.collections;

import com.bluegosling.concurrent.executors.FluentExecutorService;
import com.bluegosling.concurrent.futures.fluent.FluentFuture;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * A utility for sorting large lists using multiple CPUs/cores. This is a bit more sophisticated
 * than {@link ParallelSort} in that it performs merge operations concurrently, not just the sorting
 * of chunks.
 * 
 * <p>The merge step is parallelized by merging two adjacent chunks in a single thread. So if there
 * are eight chunks, up to four threads can be utilized to merge them. In this example, once the
 * eight chunks are merged into four, up to two threads can be utilized to merge those. The final
 * step can only be done in one thread and it merges the last two chunks into the final sorted list.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see ParallelSort
 * @see SlowParallelSort
 */
public class ParallelSort2 {
   
   /**
    * Sorts the specified list using the specified executor for running concurrent sub-tasks.
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
      sort(list, Comparator.naturalOrder(), requestedNumThreads);
   }
   
   /**
    * Sorts the specified list using the specified executor for running concurrent sub-tasks.
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
   public static <T> void sort(final List<T> list, final Comparator<? super T> comparator,
         final int requestedNumThreads) {
      // validate inputs
      if (list == null || comparator == null) {
         throw new NullPointerException();
      }
      if (list.size() <= 1) {
         return;
      }
      if (requestedNumThreads < 1) {
         throw new IllegalArgumentException("number of threads must be >= 1");
      }
      
      int remaining = list.size();
      final int numThreads = requestedNumThreads > remaining/2 ? remaining/2 : requestedNumThreads;
      if (numThreads == 1) {
         // one thread? just sort it right here
         Collections.sort(list, comparator);
         return;
      }
      final int threshold;
      if (remaining % numThreads != 0) {
         threshold = (remaining / numThreads) + 1;
      } else {
         threshold = remaining / numThreads;
      }

      final FluentExecutorService executor =
            FluentExecutorService.makeFluent(Executors.newFixedThreadPool(numThreads - 1));
      try {
         @SuppressWarnings("unchecked") // we only deal with Ts in the array
         final T array[] = (T[]) list.toArray();
         final int hi = array.length;
         final int mid = hi >>> 1;
         FluentFuture<?> loHalf = FluentFuture.dereference(executor.submit(
               new Callable<FluentFuture<Void>>() {
                  @SuppressWarnings("synthetic-access") // invokes private member of enclosing class
                  @Override public FluentFuture<Void> call() {
                     return recursiveSort(array, 0, mid, threshold, comparator, executor);
                  }
               }));
         FluentFuture<?> hiHalf = FluentFuture.dereference(executor.submit(
               new Callable<FluentFuture<Void>>() {
                  @SuppressWarnings("synthetic-access") // invokes private member of enclosing class
                  @Override public FluentFuture<Void> call() {
                     return recursiveSort(array, mid, hi, threshold, comparator, executor);
                  }
               }));
         FluentFuture<?> result = loHalf.combineWith(hiHalf,
               (o1, o2) -> {
                  // once two halves are sorted, merge them into list
                  ListIterator<T> iter = list.listIterator();
                  for (int i = 0, j = mid; i < mid || j < hi;) {
                     if (i == mid) {
                        iter.next();
                        iter.set(array[j++]);
                     } else if (j == hi) {
                        iter.next();
                        iter.set(array[i++]);
                     } else {
                        T t1 = array[i];
                        T t2 = array[j];
                        if (comparator.compare(t1, t2) < 0) {
                           iter.next();
                           iter.set(t1);
                           i++;
                        } else {
                           iter.next();
                           iter.set(t2);
                           j++;
                        }
                     }
                  }
                  return null;
               });

         // wait for result to be ready
         result.get();
         
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt(); // restore interrupt status
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
      } finally {
         executor.shutdownNow();
      }
   }

   private static <T> FluentFuture<Void> recursiveSort(final T array[], final int lo,
         final int hi, final int threshold, final Comparator<? super T> comparator,
         final FluentExecutorService executor) {
      if (hi - lo <= threshold) {
         Arrays.sort(array, lo, hi, comparator);
         return FluentFuture.completedFuture(null);
      }
      final int mid = lo + ((hi - lo) >>> 1);
      // submit task to sort lower half in parallel
      FluentFuture<Void> loHalf = FluentFuture.dereference(executor.submit(
            new Callable<FluentFuture<Void>>() {
               @SuppressWarnings("synthetic-access") // invokes private member of enclosing class
               @Override public FluentFuture<Void> call() {
                  return recursiveSort(array, lo, mid, threshold, comparator, executor);
               }
            }));
      // sort high half in this thread
      FluentFuture<Void> hiHalf =
            recursiveSort(array, mid, hi, threshold, comparator, executor);
      // once two halves are sorted, merge them into list
      return loHalf.combineWith(hiHalf, 
            (v1, v2) -> {
               final T tmp[] = Arrays.copyOfRange(array, lo, mid);
               final int l = tmp.length;
               for (int i = 0, j = mid, k = lo; i < l || j < hi;) {
                  if (i == l) {
                     array[k++] = array[j++];
                  } else if (j == hi) {
                     array[k++] = tmp[i++];
                  } else {
                     T t1 = tmp[i];
                     T t2 = array[j];
                     if (comparator.compare(t1, t2) < 0) {
                        array[k++] = t1;
                        i++;
                     } else {
                        array[k++] = t2;
                        j++;
                     }
                  }
               }            
               return null;
            });
   }
}
