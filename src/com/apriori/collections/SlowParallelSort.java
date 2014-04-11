package com.apriori.collections;

import com.apriori.concurrent.ListenableExecutorService;
import com.apriori.concurrent.ListenableExecutors;
import com.apriori.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A thought experiment into sorting large lists using multiple CPUs/cores. This breaks up the
 * incoming list into chunks which are sorted in parallel. The chunks are then merged in parallel
 * as well: a single thread merges two chunks, and multiple threads are used to merge all resulting
 * pairs of chunks.
 * 
 * <p>This is just a thought experiment. The extra overhead of using queues to stream partial
 * results from one thread to another is so significant that the algorithm always performs much
 * worse than a single-threaded sort. The code lives, despite its poor performance and thus lack of
 * practical value, because it was a fun exercise.
 * 
 * <p>The first thing done in the algorithm is to divide the collection into {@code n} chunks, where
 * {@code n} is the number of concurrent threads that will be used to sort the collection. Each
 * chunk is then sorted in its own thread -- all concurrently.
 * 
 * <p>Once a chunk is sorted that chunk is enqueued as a ready source for merging. The thread then
 * goes into merge mode, where it will dequeue two sources and then enqueue itself. It performs a
 * simple merge of the two streams and provides the resulting stream as a source for another merger.
 * The final step, once all streams have been merged into one, is to store the elements into a new
 * list.
 * 
 * <p>For {@code n} chunks, there will be exactly {@code n - 1} merger tasks required to assemble
 * all chunks back into a single stream. This diagram shows an example with five chunks:
 * <pre>{@code
 *                   /- Chunk #1 -\
 *                  /              >- Merger #1 -\
 *                 /--- Chunk #2 -/               \
 *                /                                >- Merger #3 -\
 * Unsorted List <----- Chunk #3 -\               /               >- Merger #4 -> Sorted List
 *                \                >- Merger #2 -/               /
 *                 \--- Chunk #4 -/                             /
 *                  \                                          /
 *                   \- Chunk #5 -----------------------------/
 * }</pre>
 * A given thread serves the role of both a chunk sorter and a merger. So in the above example, once
 * the last chunk is sorted, one of the threads will immediately terminate since it has no merging
 * work to do.
 * 
 * @see ParallelSort
 * @see ParallelSort2
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
    * occurring concurrently, an exception in any worker thread means the list could be corrupted or
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
      return sort(items, CollectionUtils.<T>naturalOrdering(), requestedNumThreads);
   }
   
   /**
    * Sorts the specified list using the specified number of threads. A new sorted list is returned
    * and the original specified remains unmodified. If more threads are specified than there are
    * elements in the list, fewer threads will be used.
    * 
    * <p>This returns a new list instead of modifying the list out of paranoia: since the merging is
    * occurring concurrently, an exception in any worker thread means the list could be corrupted or
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
      if (items.size() < 2) {
         // empty or one element? already sorted
         return new ArrayList<T>(items);
      }
      if (requestedNumThreads < 1) {
         throw new IllegalArgumentException("number of threads must be >= 1");
      }
      int remaining = items.size();
      // No need to spin up threads that have no sorting to do. We limit to *half* the number of
      // items so that each thread will have at least 2 items to "sort".
      int numThreads = requestedNumThreads > remaining / 2 ? remaining / 2 : requestedNumThreads;
      if (numThreads == 1) {
         // one thread? just sort it right here
         List<T> ret = new ArrayList<T>(items);
         Collections.sort(ret, comparator);
         return ret;
      }
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
      BlockingQueue<Supplier<T>> sources = new ArrayBlockingQueue<Supplier<T>>(numThreads);
      AtomicInteger chunksSorted = new AtomicInteger();
      AtomicInteger mergersStarted = new AtomicInteger();
      ListenableFuture<?> results[] = new ListenableFuture<?>[numThreads];
      ListenableExecutorService svc =
            ListenableExecutors.makeListenable(Executors.newFixedThreadPool(numThreads));
      
      try {
         for (int i = 0; i < numThreads; i++) {
            results[i] = svc.submit(new SorterMerger<T>(numThreads, chunks[i], comparator, sources,
                  chunksSorted, mergersStarted, ret));
         }
         // wait for them all to finish
         ListenableFuture.join(results).get();
      
      } catch (Exception e) {
         // on failure, make sure all tasks are cancelled
         for (int i = 0; i < numThreads; i++) {
            results[i].cancel(true);
         }
         // and then propagate
         if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
         } else {
            throw new RuntimeException(e);
         }
      } finally {
         svc.shutdownNow();
      }

      return ret;
   }
   
   /**
    * A placeholder that represents {@code null}. This is sent through a {@link BlockingQueue} to
    * indicate the end of a stream of data (since {@code null} is not allowed).
    */
   static final Object NULL_SENTINEL = new Object();
   
   /**
    * Performs a subset of sorting and merging work in a parallel sort. Each thread in such a
    * parallel sort runs an instance of this class.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of elements being sorted
    */
   private static class SorterMerger<T> implements Runnable {
      private final int numThreads;
      private final T[] chunk;
      private final Comparator<? super T> comparator;
      private final BlockingQueue<Supplier<T>> sources;
      private final AtomicInteger chunksSorted;
      private final AtomicInteger mergersStarted;
      private final List<T> result;
      
      SorterMerger(int numThreads, T[] chunk, Comparator<? super T> comparator,
            BlockingQueue<Supplier<T>> sources, AtomicInteger chunksSorted,
            AtomicInteger mergersStarted, List<T> result) {
         this.numThreads = numThreads;
         this.chunk = chunk;
         this.comparator = comparator;
         this.sources = sources;
         this.chunksSorted = chunksSorted;
         this.mergersStarted = mergersStarted;
         this.result = result;
      }
      
      @Override public void run() {
         try {
            // sort the chunk
            Arrays.sort(chunk, comparator);
            sources.add(new ChunkSource<T>(chunk));
            if (chunksSorted.incrementAndGet() == numThreads) {
               // we are the last thread to finish sorting - nothing left to do
               return;
            }
            // setup for merging
            Supplier<T> source1 = sources.take();
            Supplier<T> source2 = sources.take();
            Consumer<T> sink;
            if (mergersStarted.incrementAndGet() == numThreads - 1) {
               // we are the last merger, so we send results directly to final list
               sink = new CollectionSink<T>(result);
            } else {
               BlockingQueue<Object> stream = new ArrayBlockingQueue<Object>(512);
               sink = new QueueSink<T>(stream);
               sources.put(new QueueSource<T>(stream));
            }
            // now merge
            T item1 = source1.get();
            T item2 = source2.get();
            while (item1 != null || item2 != null) {
               if (item1 == null) {
                  sink.accept(item2);
                  item2 = source2.get();
               } else if (item2 == null) {
                  sink.accept(item1);
                  item1 = source1.get();
               } else if (comparator.compare(item1, item2) > 0) {
                  sink.accept(item2);
                  item2 = source2.get();
               } else {
                  sink.accept(item1);
                  item1 = source1.get();
               }
            }
            // signal the end
            sink.accept(null);
            
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag
            throw new RuntimeException(e);
         }
      }
   }
   
   /**
    * A sink that puts consumed elements into a collection.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element consumed
    */
   private static class CollectionSink<T> implements Consumer<T> {
      private final Collection<T> target;

      CollectionSink(Collection<T> target) {
         this.target = target;
      }
      
      @Override
      public void accept(T t) {
         // ignore any null sentinels
         if (t != null) {
            target.add(t);
         }
      }
   }
   
   /**
    * A sink that adds consumed elements into a {@link BlockingQueue}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element consumed
    */
   private static class QueueSink<T> implements Consumer<T> {
      private final BlockingQueue<Object> target;

      QueueSink(BlockingQueue<Object> target) {
         this.target = target;
      }
      
      @Override
      public void accept(T t) {
         try {
            if (t == null) {
               target.put(NULL_SENTINEL);
            } else {
               target.put(t);
            }
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag
            throw new RuntimeException(e);
         }
      }
   }
   
   /**
    * A source that is backed by a {@link BlockingQueue}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element produced
    */
   private static class QueueSource<T> implements Supplier<T> {
      private final BlockingQueue<Object> source;
      
      QueueSource(BlockingQueue<Object> source) {
         this.source = source;
      }
      
      @SuppressWarnings("unchecked")
      @Override
      public T get() {
         try {
            Object o = source.take();
            if (o == NULL_SENTINEL) {
               return null;
            }
            return (T) o;
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag
            throw new RuntimeException(e);
         }
      }
   }
   
   /**
    * A source that is backed by a sorted array of elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element produced
    */
   private static class ChunkSource<T> implements Supplier<T> {
      private final T[] chunk;
      private final int len;
      private int idx;
      
      ChunkSource(T[] chunk) {
         this.chunk = chunk;
         this.len = chunk.length;
         this.idx = 0;
      }

      @Override
      public T get() {
         if (idx < len) {
            return chunk[idx++];
         }
         return null;
      }
   }
}
