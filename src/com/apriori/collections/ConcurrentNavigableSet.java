// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;

/**
 * TODO document me
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class ConcurrentNavigableSet<E> extends ConcurrentSortedSet<E>
      implements NavigableSet<E> {

   /**
    * Iterates over the concurrent set in descending order.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class DescendingIteratorImpl extends SortedIteratorImpl {
      DescendingIteratorImpl(Set<E> stableShards[]) {
         super(stableShards);
      }
      
      @Override
      Iterator<E> getIterator(Set<E> shard) {
         return ((NavigableSet<E>) shard).descendingIterator();
      }
      
      @Override
      boolean isLessThan(E e1, E e2) {
         // return the opposite of usual comparison here since
         // we're iterating backwards
         return comp.compare(e1, e2) > 0;
      }
   }
   
   /**
    * Constructs a new set, based on the provided set implementation.
    * 
    * @param <S> the type of the underlying set implementation
    * @param set the underlying set implementation
    * @param concurrency the number of expected concurrent writers
    * @param fair whether or not fair read-write locks are used
    * 
    * @see ConcurrentSet#ConcurrentSet(Set, int, boolean)
    */
   public <S extends NavigableSet<E> & Cloneable> ConcurrentNavigableSet(S set,
         int concurrency, boolean fair) {
      super(set, concurrency, fair);
   }

   /** {@inheritDoc} */
   @Override
   public E ceiling(E e) {
      acquireReadLocks();
      try {
         E ret = null;
         for (Set<E> shard : shards) {
            E other = ((NavigableSet<E>) shard).ceiling(e);
            if (ret == null || comp.compare(other, ret) < 0) {
               ret = other;
            }
         }
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public Iterator<E> descendingIterator() {
      return new DescendingIteratorImpl(shards);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> descendingSet() {
      return new DescendingSet<E>(this);
   }

   /** {@inheritDoc} */
   @Override
   public E floor(E e) {
      acquireReadLocks();
      try {
         E ret = null;
         for (Set<E> shard : shards) {
            E other = ((NavigableSet<E>) shard).floor(e);
            if (ret == null || comp.compare(other, ret) > 0) {
               ret = other;
            }
         }
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      // TODO implement me
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public E higher(E e) {
      acquireReadLocks();
      try {
         E ret = null;
         for (Set<E> shard : shards) {
            E other = ((NavigableSet<E>) shard).higher(e);
            if (ret == null || comp.compare(other, ret) < 0) {
               ret = other;
            }
         }
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public E lower(E e) {
      acquireReadLocks();
      try {
         E ret = null;
         for (Set<E> shard : shards) {
            E other = ((NavigableSet<E>) shard).lower(e);
            if (ret == null || comp.compare(other, ret) > 0) {
               ret = other;
            }
         }
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public E pollFirst() {
      acquireReadLocks();
      try {
         E ret = null;
         for (Set<E> shard : shards) {
            E other = ((NavigableSet<E>) shard).pollFirst();
            if (other != null && (ret == null || comp.compare(other, ret) < 0)) {
               ret = other;
            }
         }
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public E pollLast() {
      acquireReadLocks();
      try {
         E ret = null;
         for (Set<E> shard : shards) {
            E other = ((NavigableSet<E>) shard).pollLast();
            if (other != null && (ret == null || comp.compare(other, ret) > 0)) {
               ret = other;
            }
         }
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
         boolean toInclusive) {
      // TODO implement me
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      // TODO implement me
      return null;
   }
}
