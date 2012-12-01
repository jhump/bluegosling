package com.apriori.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thought experiment into sorting large lists using multiple CPUs/cores. This breaks up the
 * incoming list into chunks which are sorted in parallel. The chunks are then merged in parallel
 * as well: a single thread merged two chunks and multiple threads are used to merge all resulting
 * pairs of chunks.
 * 
 * <p>This is just a thought experiment. The extra overhead of using queues to stream partial
 * results from one thread to another (and synchronization that necessitates) is so significant that
 * the algorithm always performs much worse than a single-threaded sort. The code lives, despite its
 * poor performance and thus lack of practical value, as a fun exercise.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class SlowParallelSort {

   /**
    * Sorts the specified list using the specified number of threads. A new sorted list is returned
    * and the original specified remains unmodified. If more threads are specified than there are
    * elements in the list, fewer threads will be used.
    * 
    * <p>This returns a new list instead of modifying the list out of paranoia: since the merging is
    * occuring concurrently, an exception in any worker thread means the list could be corrupted or
    * destroyed.
    * 
    * @param <T> the type of element in the list
    * @param items the list of items to sort
    * @param requestedNumThreads the number of threads to use
    * @return a new sorted list
    * @throws NullPointerException if any of the reference arguments are null or if the specified
    *       list contains any null elements
    * @throws IllegalArgumentException if the specified number of threads is zero or negative
    * @throws RuntimeException if any of the threads are interrupted
    */
   public static <T extends Comparable<T>> List<T> sort(List<? extends T> items,
         int requestedNumThreads) {
      return sort(items, CollectionUtils.<T> naturalOrdering(), requestedNumThreads);
   }
   
   /**
    * Sorts the specified list using the specified number of threads. A new sorted list is returned
    * and the original specified remains unmodified. If more threads are specified than there are
    * elements in the list, fewer threads will be used.
    * 
    * <p>This returns a new list instead of modifying the list out of paranoia: since the merging is
    * occuring concurrently, an exception in any worker thread means the list could be corrupted or
    * destroyed.
    * 
    * @param <T> the type of element in the list
    * @param items the list of items to sort
    * @param comparator the comparator to use for ordering items relative to one another
    * @param requestedNumThreads the number of threads to use
    * @return a new sorted list
    * @throws NullPointerException if any of the reference arguments are null or if the specified
    *       list contains any null elements
    * @throws IllegalArgumentException if the specified number of threads is zero or negative
    * @throws RuntimeException if any of the threads are interrupted
    */
   public static <T> List<T> sort(List<? extends T> items, Comparator<? super T> comparator,
         int requestedNumThreads) {
      // validate inputs
      if (items == null || comparator == null) {
         throw new NullPointerException();
      }
      if (items.isEmpty()) {
         return new ArrayList<T>(); // mutable copy
      }
      if (requestedNumThreads < 1) {
         throw new IllegalArgumentException("number of threads must be >= 1");
      } else if (requestedNumThreads == 1) {
         // one thread? just sort it right here
         List<T> ret = new ArrayList<T>(items);
         Collections.sort(ret, comparator);
         return ret;
      }
      int remaining = items.size();
      final int numThreads = requestedNumThreads > remaining ? remaining : requestedNumThreads;
      @SuppressWarnings("unchecked")
      T chunks[][] = (T[][]) new Object[numThreads][];
      // break input into array chunks (check for nulls while doing so)
      Iterator<? extends T> iter = items.iterator();
      for (int i = 0; i < numThreads; i++) {
         int sz = remaining / (numThreads - i);
         @SuppressWarnings("unchecked")
         T chunk[] = (T[]) new Object[sz];
         for (int j = 0; j < sz; j++) {
            T item = iter.next();
            if (item == null) {
               throw new NullPointerException("items contained a null value");
            }
            chunk[j] = item;
         }
         chunks[i] = chunk;
         remaining -= sz;
      }
      
      // create destination list (doing it "in place" would require clearing the list and then
      // streaming the results back into it or overwriting list values while streaming results,
      // which risks destruction of the list if the sorting process gets interrupted)
      List<T> ret = new ArrayList<T>(items.size());
      
      // now create the tasks
      CountDownLatch sortLatches[] = new CountDownLatch[numThreads];
      Runnable sorters[] = new Runnable[numThreads];
      for (int i = 0; i < numThreads; i++) {
         T chunk[] = chunks[i];
         if (chunk.length > 1) {
            sortLatches[i] = new CountDownLatch(1); // tells merger when sort is complete
            sorters[i] = new ChunkSorter<T>(chunk, comparator, sortLatches[i]);
         } else {
            // nothing to do!
            sortLatches[i] = new CountDownLatch(0);
         }
      }
      Runnable mergers[] = new Runnable[numThreads];
      // producers to supply inputs to merger tasks, stored in a queue so we can doll them out to
      // merger tasks appropriately
      ArrayDeque<Producer<T>> producers = new ArrayDeque<Producer<T>>(numThreads);
      for (int i = 0; i < numThreads; i++) {
         producers.add(new ProducerFromChunk<T>(chunks[i], sortLatches[i]));
      }
      // there will be exactly numThreads - 1 mergers; final thread will be a passthrough to
      // handle insertion into final list
      for (int i = 0; !producers.isEmpty(); i++) {
         Producer<T> source1 = producers.remove();
         Producer<T> source2 = producers.remove();
         if (producers.isEmpty()) {
            // no more things to merge -- just pass through to final list
            mergers[i] =
                  new Merger<T>(source1, source2, new ConsumerToCollection<T>(ret), comparator);
         } else {
            ArrayBlockingQueue<Object> pipe = new ArrayBlockingQueue<Object>(100);
            mergers[i] = new Merger<T>(source1, source2, new ConsumerToQueue<T>(pipe), comparator);
            // add the output of this merge
            producers.add(new ProducerFromQueue<T>(pipe));
         }
      }

      // finally, run them all
      Future<?> results[] = new Future<?>[numThreads];
      ExecutorService svc = Executors.newFixedThreadPool(numThreads);
      try {
         final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
         final CountDownLatch done = new CountDownLatch(numThreads);
         for (int i = 0; i < numThreads; i++) {
            final Runnable sorter = sorters[i];
            final Runnable merger = mergers[i];
            results[i] = svc.submit(new Runnable() {
               @Override public void run() {
                  try {
                     if (sorter != null) {
                        sorter.run();
                     }
                     if (merger != null) {
                        merger.run();
                     }
                     done.countDown();
                  } catch (Throwable t) {
                     failure.compareAndSet(null, t);
                     for (int j = 0; j < numThreads; j++) {
                        // count *all* the way down on exception
                        done.countDown();
                     }
                  }
               }
            });
         }
         // if any single task fails, we'll abort the whole thing
         try {
            done.await();
         }
         catch (InterruptedException e) {
            failure.set(e);
         }
         Throwable t = failure.get();
         if (t != null) {
            // on failure, make sure all tasks are cancelled
            for (int i = 0; i < numThreads; i++) {
               results[i].cancel(true);
            }
            // and then propagate
            if (t instanceof RuntimeException) {
               throw (RuntimeException) t;
            } else if (t instanceof Error) {
               throw (Error) t;
            } else {
               throw new RuntimeException(t);
            }
         }
         // if we got here and there are no errors, we're all set!
         return ret;
      } finally {
         svc.shutdownNow();
      }
   }
   
   /**
    * A placeholder that represents {@code null}. This is sent through a {@link BlockingQueue} to
    * indicate the end of a stream of data (since {@code null} is not allowed).
    */
   static final Object NULL_SENTINEL = new Object();
   
   /**
    * A runnable task that sorts a chunk. The chunk is an array and represents a contiguous block of
    * elements from the list that is being sorted. After sorting is completed, this will count down
    * a latch as a signal to downstream merger task that the chunk is ready to be merged.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element in the chunk
    */
   private static class ChunkSorter<T> implements Runnable {
      private final T[] chunk;
      private final Comparator<? super T> comparator;
      private final CountDownLatch latch;
      
      ChunkSorter(T[] chunk, Comparator<? super T> comparator, CountDownLatch latch) {
         this.chunk = chunk;
         this.comparator = comparator;
         this.latch = latch;
      }
      
      @Override
      public void run() {
         Arrays.sort(chunk, comparator);
         latch.countDown();
      }
   }
   
   /**
    * A runnable task that merges two sorted streams of values. The streams are input to the merger
    * via a {@link Producer}. The resulting sorted stream is output via a {@link Consumer}. The end
    * of a stream occurs when the producer provides a null element.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element that is merged and sorted
    */
   private static class Merger<T> implements Runnable {
      private final Producer<T> source1;
      private final Producer<T> source2;
      private final Consumer<T> target;
      private final Comparator<? super T> comparator;
      
      Merger(Producer<T> source1, Producer<T> source2, Consumer<T> target,
            Comparator<? super T> comparator) {
         this.source1 = source1;
         this.source2 = source2;
         this.target = target;
         this.comparator = comparator;
      }
      
      @Override
      public void run() {
         try {
            T item1 = source1.produce();
            T item2 = source2.produce();
            while (item1 != null || item2 != null) {
               if (item1 == null) {
                  target.consume(item2);
                  item2 = source2.produce();
               } else if (item2 == null) {
                  target.consume(item1);
                  item1 = source1.produce();
               } else if (comparator.compare(item1, item2) > 0) {
                  target.consume(item2);
                  item2 = source2.produce();
               } else {
                  target.consume(item1);
                  item1 = source1.produce();
               }
            }
            // signal the end
            target.consume(null);
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
      }
   }
   
   /**
    * An interface that represents a consumer of data, aka "target" or "sink".
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element consumed
    */
   private interface Consumer<T> {
      void consume(T t) throws InterruptedException;
   }

   /**
    * An interface that represents a producer of data, aka "source".
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element produced
    */
   private interface Producer<T> {
      T produce() throws InterruptedException;
   }
   
   /**
    * A consumer that puts consumed elements into a collection.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element consumed
    */
   private static class ConsumerToCollection<T> implements Consumer<T> {
      private final Collection<T> target;

      ConsumerToCollection(Collection<T> target) {
         this.target = target;
      }
      
      @Override
      public void consume(T t) {
         // ignore any null sentinels
         if (t != null) {
            target.add(t);
         }
      }
   }
   
   /**
    * A consumer that adds consumed elements into a {@link BlockingQueue}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element consumed
    */
   private static class ConsumerToQueue<T> implements Consumer<T> {
      private final BlockingQueue<Object> target;

      ConsumerToQueue(BlockingQueue<Object> target) {
         this.target = target;
      }
      
      @Override
      public void consume(T t) throws InterruptedException {
         if (t == null) {
            target.put(NULL_SENTINEL);
         } else {
            target.put(t);
         }
      }
   }
   
   /**
    * A producer that is backed by a {@link BlockingQueue}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element produced
    */
   private static class ProducerFromQueue<T> implements Producer<T> {
      private final BlockingQueue<Object> source;
      
      ProducerFromQueue(BlockingQueue<Object> source) {
         this.source = source;
      }
      
      @SuppressWarnings("unchecked")
      @Override
      public T produce() throws InterruptedException {
         Object o = source.take();
         if (o == NULL_SENTINEL) {
            return null;
         }
         return (T) o;
      }
   }
   
   /**
    * A producer that is backed by a sorted array of elements. The array, or "chunk", may start off
    * unsorted, so the first element cannot be produced until the chunk is sorted. A latch is used
    * to signal that the chunk is ready. When all elements of the array have been provided, the
    * producer provides a null element to indicate the end of the stream.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element produced
    */
   private static class ProducerFromChunk<T> implements Producer<T> {
      private final T[] chunk;
      private final int len;
      private final CountDownLatch latch;
      private int idx;
      
      ProducerFromChunk(T[] chunk, CountDownLatch latch) {
         this.chunk = chunk;
         this.len = chunk.length;
         this.latch = latch;
         this.idx = -1;
      }

      @Override
      public T produce() throws InterruptedException {
         if (idx == -1) {
            latch.await();
            idx++;
         }
         if (idx < len) {
            return chunk[idx++];
         }
         return null;
      }
   }
}
