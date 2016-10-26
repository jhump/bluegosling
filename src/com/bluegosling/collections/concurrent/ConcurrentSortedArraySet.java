// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.DescendingSet;
import com.bluegosling.collections.sets.SortedArraySet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
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
 * it is possible for an iteration or even a call to {@link #toString()} to reflect only some of
 * the new elements if executing concurrently with a call to {@code addAll(Collection)}, for
 * example.
 * 
 * <p>This implementation is very similar to that returned from the following expression:
 * <pre>ConcurrentSets.withNavigableSet(new SortedArraySet()).create();</pre>
 * The big exception is that this implementation does not provide guarantees of strong consistency
 * or atomicity, so as to allow greater throughput.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> The type of element contained in the set
 */
//TODO: implement me!
//TODO: tests
public class ConcurrentSortedArraySet<E> implements Serializable, Cloneable, NavigableSet<E> {

   private static final long serialVersionUID = -5784539573506639261L;

   private static final int DEFAULT_CONCURRENCY = 10;
   
   private static final int DEFAULT_INITIAL_CAPACITY_PER_SHARD = 10;
   
   /**
    * A builder class for constructing instances of {@code ConcurrentSortedArraySet}.
    * This pattern is used to make construction easier to read since there are so
    * many options that might otherwise take the form of constructor parameters and
    * many overloaded constructors.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    * 
    * @param <E> the type of element in the constructed set
    */
   public static class Builder<E> {
      
      private int concurrency = DEFAULT_CONCURRENCY;
      private boolean fair = false;
      private int initialCapacity = -1;
      private Comparator<? super E> comp = null;
      private Collection<? extends E> contents = null;
      
      /**
       * Sets the concurrency level for the set. This will be the number of shards
       * used in the new set. If unset, this defaults to 10.
       *
       * @param c the expected level of concurrency
       * @return this (for chaining method calls)
       */
      public Builder<E> concurrency(int c) {
         this.concurrency = c;
         return this;
      }
      
      /**
       * Sets whether read-write locks used will be fair or not. If unset, this defaults
       * to using unfair locks.
       * 
       * @param f true if locks are fair
       * @return this (for chaining method calls)
       * 
       * @see ReentrantReadWriteLock#isFair()
       */
      public Builder<E> fair(boolean f) {
         this.fair = f;
         return this;
      }
      
      /**
       * Sets the initial capacity of the set. The new set will be able to contain up to
       * this many items before having to grow internal array buffers. It is possible that
       * a grow operation may be needed when fewer items are in the set if the items added
       * are not evenly distributed amongst the internal shards.
       * 
       * <p>If left unset, the capacity will be the number of items in the set's initial contents.
       * If this capacity is unset and the initial contents are also unset, then this defaults to
       * 10 elements per shard (100 elements if using the default concurrency of 10).
       * 
       * @param capacity the amount of capacity allocated in the new set.
       * @return this (for chaining method calls)
       */
      public Builder<E> initialCapacity(int capacity) {
         this.initialCapacity = capacity;
         return this;
      }
      
      /**
       * Sets the comparator used to sort items in the set. If set to {@code null} or
       * left unset, the set will use the {@linkplain Comparable natural ordering} of items.
       * 
       * @param c the comparator used to sort items in the set
       * @return this (for chaining method calls)
       */
      public Builder<E> comparator(Comparator<? super E> c) {
         this.comp = c;
         return this;
      }
      
      /**
       * Sets the initial contents of the set.
       *
       * @param coll the initial contents of the new set
       * @return this (for chaining method calls)
       */
      public Builder<E> initialContents(Collection<? extends E> coll) {
         this.contents = coll;
         return this;
      }
      
      /**
       * Sets both the initial contents of the set and the comparator used to sort
       * elements to those of the specified {@link SortedSet}.
       *
       * @param set the set whose elements and comparator will be used to build the new set
       * @return this (for chaining method calls)
       */
      public Builder<E> copyOf(SortedSet<E> set) {
         this.contents = set;
         this.comp = set.comparator();
         return this;
      }
      
      /**
       * Constructs a new set with the specified configuration.
       *
       * @return the new set
       */
      public ConcurrentSortedArraySet<E> build() {
         if (contents == null) {
            int capacity = initialCapacity < 0 ? concurrency * DEFAULT_INITIAL_CAPACITY_PER_SHARD
                  : initialCapacity;
            return new ConcurrentSortedArraySet<E>(concurrency, fair, capacity, comp);
         } else {
            int capacity = initialCapacity < 0 ? contents.size() : initialCapacity;
            return new ConcurrentSortedArraySet<E>(contents, concurrency, fair, capacity, comp);
         }
      }
   }
   
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

   /**
    * Constructs a new empty set. All options (like concurrency level, fairness of locks, and
    * sort order of elements) use defaults. This constructor is provided for convenience and using
    * a {@link Builder} is generally recommended instead.
    *
    * @see Builder
    */
   public ConcurrentSortedArraySet() {
      this(DEFAULT_CONCURRENCY, false, DEFAULT_INITIAL_CAPACITY_PER_SHARD * DEFAULT_CONCURRENCY,
            null);
   }
   
   /**
    * Constructs a new set that will use the specified comparator to sort elements. Other options
    * will use default values. This constructor is provided for convenience per the conventions
    * of other implementations of {@link SortedSet}. The use of a {@link Builder} is generally
    * recommended instead.
    *
    * @param comp the comparator used to sort elements or {@code null} to sort elements per their
    *       {@linkplain Comparable natural ordering}
    *       
    * @see Builder
    */
   public ConcurrentSortedArraySet(Comparator<? super E> comp) {
      this(DEFAULT_CONCURRENCY, false, DEFAULT_INITIAL_CAPACITY_PER_SHARD * DEFAULT_CONCURRENCY,
            comp);
   }
   
   /**
    * Constructs a new set with the specified contents. Other options will use default values. This
    * constructor is provided for convenience per the convention recommended for all collection
    * implementations. The use of a {@link Builder} is generally recommended instead.
    *
    * @param coll the initial contents of the new set
    *       
    * @see Builder
    */
   public ConcurrentSortedArraySet(Collection<? extends E> coll) {
      this(coll, DEFAULT_CONCURRENCY, false, coll.size(), null);
   }
   
   /**
    * Constructs a new set that will have the same contents and comparator as the specified set.
    * Other options will use default values. This constructor is provided for convenience per the 
    * conventions of other implementations of {@link SortedSet}. The use of a {@link Builder} is
    * generally recommended instead.
    *
    * @param set the set whose contents will comprise the new set and whose comparator will be
    *       used to sort the elements of the new set
    *       
    * @see Builder
    */
   public ConcurrentSortedArraySet(SortedSet<E> set) {
      this(set, DEFAULT_CONCURRENCY, false, set.size(), set.comparator());
   }

   /**
    * Constructs a new empty set with the specified configuration options.
    *
    * @param concurrency the expected level of concurrency
    * @param fair true if locks are fair
    * @param initialCapacity the amount of capacity allocated in the new set.
    * @param comp the comparator used to sort items in the set
    */
   ConcurrentSortedArraySet(int concurrency, boolean fair, int initialCapacity,
         Comparator<? super E> comp) {
      if (concurrency < 1) {
         throw new IllegalArgumentException("concurrency: " + concurrency + " < 1");
      }
      if (initialCapacity < 0) {
         throw new IllegalArgumentException("capacity: " + initialCapacity + " < 0");
      }
      // new empty set
      if (comp == null) {
         this.comp = CollectionUtils.naturalOrder();
      } else {
         this.comp = comp;
      }
      shards = new Object[concurrency][];
      shardSizes = new int[concurrency];
      shardLocks = new ReentrantReadWriteLock[concurrency];
      int capPerShard = (initialCapacity + concurrency - 1) / concurrency;
      for (int i = 0; i < concurrency; i++) {
         shards[i] = new Object[capPerShard];
         shardSizes[i] = 0;
         shardLocks[i] = new ReentrantReadWriteLock(fair);
      }
   }

   /**
    * Constructs a new set with the specified configuration options and populated with the
    * specified initial contents.
    *
    * @param coll the initial contents of the new set
    * @param concurrency the expected level of concurrency
    * @param fair true if locks are fair
    * @param initialCapacity the amount of capacity allocated in the new set.
    * @param comp the comparator used to sort items in the set
    */
   ConcurrentSortedArraySet(Collection<? extends E> coll, int concurrency, boolean fair,
         int initialCapacity, Comparator<? super E> comp) {
      // new populated set
      this(concurrency, fair, initialCapacity, comp);
      addAll(coll);
   }

   /** {@inheritDoc} */
   @Override
   public Comparator<? super E> comparator() {
      return comp == Comparator.naturalOrder() ? null : comp;
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
   
   /**
    * Trims the internal array buffers to be exactly the required size to accommodate the
    * set's contents. This is generally done after all items are added to the set in order to
    * reduce memory pressure and allow excess buffer space to be reclaimed.
    */
   public void trimToSize() {
      // TODO implement me
   }
   
   private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
      in.defaultReadObject();
      // TODO implement me
   }
   
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      // TODO implement me
   }

   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this, o);
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
