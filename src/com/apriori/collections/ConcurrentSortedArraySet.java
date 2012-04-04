// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link NavigableSet} that allows concurrent access. The set's elements are stored in multiple
 * sorted arrays, each of which represents a subset (shard) of this set's elements. Multiple
 * internal arrays are used to support concurrency with less thread contention, so an estimate for
 * the expected level of concurrency is needed for this set to perform properly. The level of
 * concurrency is the expected peak number of threads modifying the set simultaneously. If too small
 * a value is used then there will be extra contention over modifying the set. If too large a value
 * is used then there will be wasteful memory overhead.
 * 
 * <p>To allow in-process iteration to happen in the face of concurrent modifications, this set
 * relies on the fact that the set is sorted. So calling {@link Iterator#next() next()} on an
 * {@link Iterator} is effectively the same as calling {@link #first()} for the first element and
 * then calling {@link #higher(Object) higher(lastItemFetchedViaIterator)} for subsequent elements.
 * To prevent {@code Iterator.next()} from taking <em>O(log<sub>2</sub>n)</em> time, iterators track
 * version numbers for each shard and only perform <em>O(log<sub>2</sub>n)</em> look-ups in a shard
 * if it has been modified since the last time it was observed by the iterator.
 * 
 * <p>Many operations that are typically constant-time operations will run in <em>O(s)</em> time
 * with this set implementation, where <em>s</em> is the number of shards. This includes calls to
 * methods such as {@link #first()}, {@link #last()}, {@link #size()}, and {@code Iterator.next()}.
 * 
 * <p>Similarly, query performance runs in <em>O(s * log<sub>2</sub>(n / s))</em>. This applies to
 * various methods that query the set, including {@link #contains(Object)}, {@link #floor(Object)},
 * {@link #ceiling(Object)}, {@link #lower(Object)}, {@link #higher(Object)}, {@link #headSet},
 * {@link #tailSet}, and {@link #subSet}. So, as the number of shards is increased from 1 to n (or
 * even higher), query performance degrades from <em>O(log<sub>2</sub>n)</em> to <em>O(
 * greater(s,n) )</em>.
 * 
 * <p>It is also worth pointing out that some operations that run in <em>O(n)</em> in a
 * {@link SortedArraySet}, like {@link SortedArraySet#add(Object) add} and
 * {@link SortedArraySet#remove(Object) remove}, run in <em>O(n / s)</em> time in this
 * implementation. So using a large number of shards can actually improve the performance of
 * mutative operations (even without considering thread contention issues), but at the cost of
 * reducing the performance of non-mutative operations.
 *
 * <p>Generally, iteration will reflect up-to-the-moment changes to the underlying set
 * <strong>except</strong> when a change happens between the calls to {@code hasNext()} and
 * {@code next()}. This minor staleness is necessary to prevent {@code next()} from throwing an
 * exception after a call to {@code hasNext()} returned true since, if the contents returned by
 * {@code next()} were <em>never</em> stale, it would be possible to get such an exception if, for
 * example, the last item in the set were removed between these two calls.
 * 
 * <p>Like many other concurrent collection implementations, operations in this set are
 * weakly consistent. Bulk operations, like {@link #addAll(Collection)} and
 * {@link #removeAll(Collection)} for example, are not guaranteed to execute atomically. So
 * it is possible for an iteration or even a call to {#link #toString()} to reflect only some of
 * the new elements if executing concurrently with a call to {@code addAll(Collection)}, for
 * example.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> The type of element contained in the set
 */
public class ConcurrentSortedArraySet<E> implements Serializable, Cloneable, NavigableSet<E> {

   private static final int DEFAULT_CONCURRENCY = 10;
   
   private static final int DEFAULT_INITIAL_CAPACITY_PER_SHARD = 10;
   
   /**
    * Two-dimensional array of elements. Each item represents a single shard. Each shard is itself
    * a sorted array of elements.
    */
   transient Object shards[][];
   
   /**
    * An array of shard sizes. There is one element here for each shard. The total number of
    * elements in the set is the sum of values in this array.
    */
   transient int shardSizes[];
   
   /**
    * An array of shard revision numbers. There is one element here for each shard. These are used
    * to invalidate iterator states for a shard when it is modified via mutative operations.
    */
   transient int modCounts[];
   
   /**
    * An array of locks that guard access to the shards.
    */
   transient ReentrantReadWriteLock shardLocks[];
   
   /**
    * The comparator used to sort elements in the set or {@code null} to indicate that items are
    * sorted by their {@linkplain Comparable natural order}.
    */
   transient Comparator<? super E> comp;

   public ConcurrentSortedArraySet() {
      this(DEFAULT_CONCURRENCY);
   }
   
   public ConcurrentSortedArraySet(Comparator<? super E> comp) {
      this(DEFAULT_CONCURRENCY, comp);
   }
   
   public ConcurrentSortedArraySet(int concurrency) {
      this(concurrency, false);
   }
   
   public ConcurrentSortedArraySet(int concurrency, boolean fair) {
      this(concurrency, fair, concurrency * DEFAULT_INITIAL_CAPACITY_PER_SHARD);
   }
   
   public ConcurrentSortedArraySet(int concurrency, Comparator<? super E> comp) {
      this(concurrency, concurrency * DEFAULT_INITIAL_CAPACITY_PER_SHARD, comp);
   }
   
   public ConcurrentSortedArraySet(int concurrency, boolean fair, Comparator<? super E> comp) {
      this(concurrency, fair, concurrency * DEFAULT_INITIAL_CAPACITY_PER_SHARD, comp);
   }
   
   public ConcurrentSortedArraySet(int concurrency, int initialCapacity) {
      this(concurrency, false, initialCapacity);
   }
   
   public ConcurrentSortedArraySet(int concurrency, boolean fair, int initialCapacity) {
      this(concurrency, fair, initialCapacity, null);
   }
   
   public ConcurrentSortedArraySet(int concurrency, int initialCapacity,
         Comparator<? super E> comp) {
      this(concurrency, false, initialCapacity, comp);
   }
   
   public ConcurrentSortedArraySet(int concurrency, boolean fair, int initialCapacity,
         Comparator<? super E> comp) {
      // new empty set
      this.comp = comp;
      shards = new Object[concurrency][];
      shardSizes = new int[concurrency];
      shardLocks = new ReentrantReadWriteLock[concurrency];
      int capPerShard = (initialCapacity + concurrency - 1) / concurrency;
      for (int i = 0, len = shards.length; i < len; i++) {
         shards[i] = new Object[capPerShard];
         shardSizes[i] = 0;
         shardLocks[i] = new ReentrantReadWriteLock(fair);
      }
   }
   
   public ConcurrentSortedArraySet(Collection<? extends E> coll) {
      this(coll, DEFAULT_CONCURRENCY);
   }
   
   public ConcurrentSortedArraySet(Collection<? extends E> coll,
         Comparator<? super E> comp) {
      this(coll, DEFAULT_CONCURRENCY, comp);
   }
   
   public ConcurrentSortedArraySet(Collection<? extends E> coll, int concurrency) {
      this(coll, concurrency, false);
   }

   public ConcurrentSortedArraySet(Collection<? extends E> coll, int concurrency,
         boolean fair) {
      this(coll, concurrency, fair, null);
   }

   public ConcurrentSortedArraySet(Collection<? extends E> coll, int concurrency,
         Comparator<? super E> comp) {
      this(coll, concurrency, false, comp);
   }
   
   public ConcurrentSortedArraySet(Collection<? extends E> coll, int concurrency, boolean fair,
         Comparator<? super E> comp) {
      // new populated set
      this(concurrency, fair, coll.size(), comp);
      addAll(coll);
   }

   /** {@inheritDoc} */
   @Override
   public Comparator<? super E> comparator() {
      return comp;
   }

   /** {@inheritDoc} */
   @Override
   public E first() {
      // TODO implement me
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public E last() {
      // TODO implement me
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public boolean add(E e) {
      // TODO implement me
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean addAll(Collection<? extends E> coll) {
      boolean ret = false;
      for (E e : coll) {
         if (add(e)) {
            ret = true;
         }
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public void clear() {
      // TODO implement me
      
   }

   /** {@inheritDoc} */
   @Override
   public boolean contains(Object o) {
      // TODO implement me
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean containsAll(Collection<?> coll) {
      for (Object o : coll) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean isEmpty() {
      return size() == 0;
   }

   /** {@inheritDoc} */
   @Override
   public boolean remove(Object o) {
      // TODO implement me
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean removeAll(Collection<?> coll) {
      boolean ret = false;
      for (Object o : coll) {
         if (remove(o)) {
            ret = true;
         }
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public boolean retainAll(Collection<?> coll) {
      return CollectionUtils.filter(coll, iterator(), false);
   }

   /** {@inheritDoc} */
   @Override
   public int size() {
      int sz = 0;
      for (int i = 0, len = shardSizes.length; i < len; i++) {
         sz += shardSizes[i];
      }
      return sz;
   }

   private Collection<E> snapshot() {
      // using ArrayList as a "fast growable array" for storing
      // a snapshot of contents
      ArrayList<E> ret = new ArrayList<E>(size());
      ret.addAll(this);
      return ret;
   }
   
   /** {@inheritDoc} */
   @Override
   public Object[] toArray() {
      return snapshot().toArray();
   }

   /** {@inheritDoc} */
   @Override
   public <T> T[] toArray(T[] a) {
      return snapshot().toArray(a);
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
      // TODO implement me
      return null;
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
   public NavigableSet<E> headSet(E to) {
      return headSet(to, false);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> headSet(E to, boolean inclusive) {
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
   public Iterator<E> iterator() {
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
   public NavigableSet<E> subSet(E from, E to) {
      return subSet(from, true, to, false);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
      // TODO implement me
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E from) {
      return tailSet(from, true);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E from, boolean inclusive) {
      // TODO implement me
      return null;
   }

   @Override
   public boolean equals(Object o) {
      // Due to the concurrent nature of this set, Utils.equals() isn't
      // strong enough (even for weak consistency guarantees of this set).
      // So we us this alternate implementation, which is slightly less
      // efficient (since it does ~2x as many set queries) but more correct.
      if (o instanceof Set) {
         Set<?> other = (Set<?>) o;
         return other.containsAll(this) && containsAll(other);
      } else {
         return false;
      }
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }

   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
}