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

   /**
    * The default level of concurrency when creating concurrent sets. This
    * will be the number of shards used in a set and should be the expected
    * peak number of modifying threads.
    */
   public static final int DEFAULT_CONCURRENCY = 10;
   
   /**
    * Creates concurrent set objects using the builder pattern.
    * 
    * <p>Example usage:
    * <pre>
    * <em>// return a concurrent set that is based on a
    * // {@link java.util.HashSet} and uses default parameters of 10
    * // concurrent writer threads and unfair locks.</em>
    * return ConcurrentSets.withSet(new HashSet()).create();
    * 
    * <em>// return a concurrent sorted set that is based on a
    * // {@link java.util.TreeSet}, expects 5 concurrent writer threads,
    * // and uses fair locks.</em>
    * return ConcurrentSets.withSet(new TreeSet())
    *                      .concurrency(5)
    *                      .fair(true)
    *                      .create();
    * </pre>
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @param <E> the type of element in the returned set
    * @param <T> the type of set (e.g. {@code Set}, {@code SortedSet},
    *       or {@code NavigableSet})
    */
   public interface Builder<E, T extends Set<E>> {
      /**
       * Sets the concurrency level for the set that will be created.
       * If this method is never called, the created set will use
       * {@link ConcurrentSets#DEFAULT_CONCURRENCY}.
       *
       * @param concurrency the level of concurrency
       * @return {@code this}
       */
      Builder<E, T> concurrency(int concurrency);
      
      /**
       * Sets whether or not access to the set uses fair read-write locks.
       * If this method is never called, the created set will <em>not</em>
       * use fair locks.
       *
       * @param fair whether or not to use fair locks
       * @return {@code this}
       * 
       * @see java.util.concurrent.locks.ReentrantReadWriteLock#isFair()
       */
      Builder<E, T> fair(boolean fair);
      
      /**
       * Creates a set.
       *
       * @return the concurrent set
       */
      T create();
   }
   
   /**
    * Abstract implementor of the builder interface. Concrete subclasses must
    * implement the {@link #create()} method.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @param <E> the type of element in the returned set
    * @param <T> the type of set
    */
   private static abstract class BuilderImpl<E, T extends Set<E>>
         implements Builder<E, T> {
      
      int concurrency;
      boolean fair;
      
      BuilderImpl() {
         concurrency = DEFAULT_CONCURRENCY;
         fair = false;
      }
      
      @Override
      public Builder<E, T> concurrency(int concurrency) {
         this.concurrency = concurrency;
         return this;
      }
      
      @Override
      public Builder<E, T> fair(boolean fair) {
         this.fair = fair;
         return this;
      }
   }
   
   /**
    * Returns a builder for creating a concurrent set implementation that is
    * backed by the specified {@link Set}.
    * 
    * <p>The specified set provides the implementation of the underlying set
    * and its initial contents. The actual instance specified will be
    * {@linkplain Object#clone() cloned} to create the new set's shards. So the
    * resulting concurrent set will actually be independent of the specified
    * set; mutations to one will not be reflected in the other.
    *
    * @param <E> the type of element in the set
    * @param <S> the type of the wrapped set
    * @param set the set to use as a basis for a concurrent set
    * @return a concurrent set
    */
   public static <E, S extends Set<E> & Cloneable> Builder<E, Set<E>>
         withSet(final S set) {
      
      return new BuilderImpl<E, Set<E>>() {
         @Override
         public Set<E> create() {
            return new ConcurrentSet<E>(set, concurrency, fair);
         }
      };
   }

   /**
    * Returns a builder for creating a concurrent set implementation that is
    * backed by the specified {@link SortedSet}.
    * 
    * <p>The specified set provides the implementation of the underlying set
    * and its initial contents. The actual instance specified will be
    * {@linkplain Object#clone() cloned} to create the new set's shards. So the
    * resulting concurrent set will actually be independent of the specified
    * set; mutations to one will not be reflected in the other.
    *
    * @param <E> the type of element in the set
    * @param <S> the type of the wrapped set
    * @param set the set to use as a basis for a concurrent set
    * @return a concurrent sorted set
    */
   public static <E, S extends SortedSet<E> & Cloneable> Builder<E, SortedSet<E>>
         withSortedSet(final S set) {

      return new BuilderImpl<E, SortedSet<E>>() {
         @Override
         public SortedSet<E> create() {
            return new ConcurrentSortedSet<E>(set, concurrency, fair);
         }
      };
   }

   /**
    * Returns a builder for creating a concurrent set implementation that is
    * backed by the specified {@link NavigableSet}.
    * 
    * <p>The specified set provides the implementation of the underlying set
    * and its initial contents. The actual instance specified will be
    * {@linkplain Object#clone() cloned} to create the new set's shards. So the
    * resulting concurrent set will actually be independent of the specified
    * set; mutations to one will not be reflected in the other.
    *
    * @param <E> the type of element in the set
    * @param <S> the type of the wrapped set
    * @param set the set to use as a basis for a concurrent set
    * @return a concurrent navigable set
    */
   public static <E, S extends NavigableSet<E> & Cloneable> Builder<E, NavigableSet<E>>
         withNavigableSet(final S set) {
      
      return new BuilderImpl<E, NavigableSet<E>>() {
         @Override
         public NavigableSet<E> create() {
            return new ConcurrentNavigableSet<E>(set, concurrency, fair);
         }
      };
   }
}
