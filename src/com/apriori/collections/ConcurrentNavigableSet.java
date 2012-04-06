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

   class DescendingIteratorImpl extends SortedIteratorImpl {
      DescendingIteratorImpl(Set<E> stableShards[]) {
         super(stableShards);
      }
      
      @Override
      Iterator<E> getIterator(Set<E> shard) {
         return ((NavigableSet<E>) shard).descendingIterator();
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
      // TODO implement me
      return null;
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
      // TODO implement me
      return null;
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
      // TODO implement me
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public E lower(E e) {
      // TODO implement me
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public E pollFirst() {
      // TODO implement me
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public E pollLast() {
      // TODO implement me
      return null;
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
