// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

/**
 * This utility class provides concurrent set implementations that wrap
 * normal (non-concurrent) sets to provide thread-safe access.
 * 
 * The wrappers reduce lock contention during concurrent access by using
 * multiple underlying sets -- each one a "shard" that contains a subset of the
 * set's elements. Shards are accessed via read-write locks instead of via
 * synchronizing on the set itself to improve performance by allowing multiple
 * readers simultaneous access.
 * <p>Iteration in the face of concurrent writes is provided by iterating over a
 * snapshot of the set. This is done using "copy-on-iteration" semantics.
 * <p>To reduce the memory pressure that could be caused by creating a snapshot
 * of the set for every iterator, three strategies are employed:
 *   <ol>
 *   <li>The snapshots are taken "lazily". So if no write operation ever occurs
 *   during an iteration, no copy is needed and the iterator effectively reads the
 *   actual set contents instead of a copy.</li>
 *   <li>Iterators will effectively share snapshots if iteration began when the
 *   set was in the same state. They can even share partial snapshots if at least
 *   one shard was in the same state when they were created.</li>
 *   <li>The granularity of a snapshot is a single shard. So if mutative
 *   operations only impact one shard, only one shard is copied on the heap for
 *   the benefit iteration.</li>
 *   </ol>
 * </li>
 * </ol>
 * <p>All wrapped sets provide strong consistency and all mutative methods are
 * guaranteed to be atomic (unlike many other implementations of concurrent
 * collections).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ConcurrentSets {
   
   public static final int DEFAULT_CONCURRENCY = 10;
   
   public interface ConcurrentSetBuilder<E, T extends Set<E>> {
      ConcurrentSetBuilder<E, T> concurrency(int concurrency);
      ConcurrentSetBuilder<E, T> fair(boolean fair);
      T create();
   }
   
   private static abstract class ConcurrentSetBuilderImpl<E, T extends Set<E>>
         implements ConcurrentSetBuilder<E, T> {
      int concurrency;
      boolean fair;
      
      ConcurrentSetBuilderImpl() {
         concurrency = DEFAULT_CONCURRENCY;
         fair = false;
      }
      
      @Override
      public ConcurrentSetBuilder<E, T> concurrency(int concurrency) {
         this.concurrency = concurrency;
         return this;
      }
      
      @Override
      public ConcurrentSetBuilder<E, T> fair(boolean fair) {
         this.fair = fair;
         return this;
      }
      
   }
   
   
   public static <E, S extends Set<E> & Cloneable> ConcurrentSetBuilder<E, Set<E>>
         wrapSet(final S set) {
      
      return new ConcurrentSetBuilderImpl<E, Set<E>>() {
         @Override
         public Set<E> create() {
            return new ConcurrentSet<E>(set, concurrency, fair);
         }
      };
   }

   public static <E, S extends SortedSet<E> & Cloneable> ConcurrentSetBuilder<E, SortedSet<E>>
         wrapSortedSet(final S set) {

      return new ConcurrentSetBuilderImpl<E, SortedSet<E>>() {
         @Override
         public SortedSet<E> create() {
            return new ConcurrentSortedSet<E>(set, concurrency, fair);
         }
      };
   }

   public static <E, S extends NavigableSet<E> & Cloneable> ConcurrentSetBuilder<E, NavigableSet<E>>
         wrapNavigableSet(final S set) {
      
      return new ConcurrentSetBuilderImpl<E, NavigableSet<E>>() {
         @Override
         public NavigableSet<E> create() {
            return new ConcurrentNavigableSet<E>(set, concurrency, fair);
         }
      };
   }
}
