// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.ArrayUtils;
import com.bluegosling.collections.CollectionUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;

/**
 * An implementation of {@link SortedSet} that supports concurrent access. This
 * implementation uses the same approach and provides the same consistency
 * and atomicity guarantees as its superclass, {@link ShardedConcurrentSet}.
 * 
 * <p>This set does not support {@code null} values, even if the underlying set
 * implementations do.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> the type of element contained in the set
 * 
 * @see ShardedConcurrentSet
 */
class ShardedConcurrentSortedSet<E> extends ShardedConcurrentSet<E>
      implements SortedSet<E> {

   private static final long serialVersionUID = 2826102235941590308L;

   /**
    * Iterates over a snapshot of the set.
    * 
    * <p>The shards are a snapshot and can be assumed to be immutable.
    * 
    * <p>Methods are synchronized so as not to corrupt the structure if
    * used from multiple threads. However, access from multiple threads is
    * discouraged since it could cause {@link #next()} to spuriously throw
    * exceptions, even if the code is first checking {@link #hasNext()}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class SortedIteratorImpl implements Iterator<E> {
      private final PriorityQueue<IteratorAndHead<E>> queue;
      private E lastElement;
      private boolean canRemove;
      
      SortedIteratorImpl(Set<E> stableShards[]) {
         int len = stableShards.length;
         List<IteratorAndHead<E>> iterators = new ArrayList<>(len);
         for (int i = 0; i < len; i++) {
            Iterator<E> iter = getIterator(stableShards[i]);
            if (iter.hasNext()) {
               iterators.add(new IteratorAndHead<E>(getComparator(), iter));
            }
         }
         // we build the values in a list first so we can do O(n) heapify, O(n log n) to insert
         // each item individually (heapify is only done when the queue is constructed with a
         // collection of elements)
         queue = new PriorityQueue<>(iterators);
      }
      
      Iterator<E> getIterator(Set<E> shard) {
         return shard.iterator();
      }

      Comparator<? super E> getComparator() {
         return comp;
      }

      /** {@inheritDoc} */
      @Override
      public boolean hasNext() {
         return !queue.isEmpty();
      }

      /** {@inheritDoc} */
      @Override
      public synchronized E next() {
         IteratorAndHead<E> entry = queue.remove();
         lastElement = entry.head;
         Iterator<E> iter = entry.iter;
         if (iter.hasNext()) {
            entry.head = iter.next();
            queue.add(entry);
         }
         canRemove = true;
         return lastElement;
      }

      /** {@inheritDoc} */
      @Override
      public synchronized void remove() {
         if (!canRemove) {
            throw new IllegalStateException("element already removed");
         } else {
            canRemove = false;
            ShardedConcurrentSortedSet.this.remove(lastElement);
         }
      }
   }
   
   private static class IteratorAndHead<E> implements Comparable<IteratorAndHead<E>> {
      final Comparator<? super E> comparator;
      final Iterator<E> iter;
      E head;
      
      IteratorAndHead(Comparator<? super E> comparator, Iterator<E> iter) {
         this.comparator = comparator;
         this.iter = iter;
         this.head = iter.next();
      }

      @Override
      public int compareTo(IteratorAndHead<E> o) {
         return comparator.compare(head, o.head);
      }
   }
   
   /**
    * A sub-set of the concurrent set. This is not a snapshot but rather a
    * live set that is fully concurrent and provides the same consistency
    * and atomicity guarantees as the backing concurrent set.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class SubSetImpl implements SortedSet<E> {

      E from;
      E to;
      
      SubSetImpl(E from, E to) {
         this.from = from;
         this.to = to;
         if (from != null && to != null &&comp.compare(from, to) > 0) {
            throw new IllegalArgumentException("Invalid subset bounds");
         }
      }
      
      @SuppressWarnings("unchecked")
      boolean isInRangeLow(Object o, boolean inclusive) {
         return CollectionUtils.isInRangeLow(o, inclusive, from, true, (Comparator<Object>) comp);
      }
      
      @SuppressWarnings("unchecked")
      boolean isInRangeHigh(Object o, boolean inclusive) {
         return CollectionUtils.isInRangeHigh(o, inclusive, to, false, (Comparator<Object>) comp);
      }
      
      @SuppressWarnings("unchecked")
      boolean isInRange(Object o) {
         return CollectionUtils.isInRange(o, from, true, to, false, (Comparator<Object>) comp);
      }
      
      void checkRangeHigh(Object o, boolean inclusive) {
         if (!isInRangeHigh(o, inclusive)) {
            throw new IllegalArgumentException("Object outside of subset range");
         }
      }
      
      void checkRangeLow(Object o, boolean inclusive) {
         if (!isInRangeLow(o, inclusive)) {
            throw new IllegalArgumentException("Object outside of subset range");
         }
      }
      
      void checkRange(Object o) {
         if (!isInRange(o)) {
            throw new IllegalArgumentException("Object outside of subset range");
         }
      }
      
      SortedSet<E> subSet(SortedSet<E> shard) {
         if (from == null) {
            return shard.headSet(to);
         } else if (to == null) {
            return shard.tailSet(from);
         } else {
            return shard.subSet(from, to);
         }
      }
      
      /** {@inheritDoc} */
      @Override
      public boolean add(E e) {
         checkRange(e);
         return ShardedConcurrentSortedSet.this.add(e);
      }

      /** {@inheritDoc} */
      @Override
      public boolean addAll(Collection<? extends E> coll) {
         for (E e : coll) {
            checkRange(e);
         }
         return ShardedConcurrentSortedSet.this.addAll(coll);
      }

      /** {@inheritDoc} */
      @Override
      public void clear() {
         acquireWriteLocks();
         try {
            for (Set<E> shard : shards) {
               subSet((SortedSet<E>) shard).clear();
            }
         } finally {
            releaseWriteLocks();
         }
      }

      /** {@inheritDoc} */
      @Override
      public boolean contains(Object o) {
         if (isInRange(o)) {
            return ShardedConcurrentSortedSet.this.contains(o);
         } else {
            return false;
         }
      }

      /** {@inheritDoc} */
      @Override
      public boolean containsAll(Collection<?> coll) {
         for (Object o : coll) {
            if (!isInRange(o)) {
               return false;
            }
         }
         return ShardedConcurrentSortedSet.this.containsAll(coll);
      }

      /** {@inheritDoc} */
      @Override
      public boolean isEmpty() {
         return size() == 0;
      }

      /** {@inheritDoc} */
      @Override
      public Iterator<E> iterator() {
         Set<E> stableShards[] = getStableShards();
         for (int i = 0, len = stableShards.length; i < len; i++) {
            stableShards[i] = subSet((SortedSet<E>) stableShards[i]);
         }
         return makeIterator(stableShards);
      }

      /** {@inheritDoc} */
      @Override
      public boolean remove(Object o) {
         if (!isInRange(o)) {
            return false;
         }
         return ShardedConcurrentSortedSet.this.remove(o);
      }

      /** {@inheritDoc} */
      @Override
      public boolean removeAll(Collection<?> coll) {
         // same logic here as in ConcurrentSet.removeAll
         Collection<?> colls[] = collectionToShards(coll);
         boolean effectedShards[] = getEffectedShards(colls);
         boolean ret = false;
         acquireWriteLocks(effectedShards);
         try {
            for (int i = 0, len = effectedShards.length; i < len; i++) {
               if (effectedShards[i]) {
                  if (subSet((SortedSet<E>) shards[i]).removeAll(colls[i])) {
                     ret = true;
                  }
               }
            }
         } finally {
            releaseWriteLocks(effectedShards);
         }
         return ret;
      }

      /** {@inheritDoc} */
      @Override
      public boolean retainAll(Collection<?> coll) {
         // same logic here as in ConcurrentCopyOnIterationSet.retainAll
         Collection<?> colls[] = collectionToShards(coll);
         boolean effectedShards[] = getEffectedShards(colls);
         boolean ret = false;
         acquireWriteLocks();
         try {
            for (int i = 0, len = effectedShards.length; i < len; i++) {
               Set<E> shard = subSet((SortedSet<E>) shards[i]);
               if (effectedShards[i]) {
                  if (shard.retainAll(colls[i])) {
                     ret = true;
                  }
               } else {
                  if (!shard.isEmpty()) {
                     shard.clear();
                     ret = true;
                  }
               }
            }
         } finally {
            releaseWriteLocks();
         }
         return ret;
      }
      
      private int sizeNoLocks() {
         // up to the caller to acquire locks
         int sz = 0;
         for (Set<E> shard : shards) {
            sz += subSet((SortedSet<E>) shard).size();
         }
         return sz;
      }

      /** {@inheritDoc} */
      @Override
      public int size() {
         acquireReadLocks();
         try {
            return sizeNoLocks();
         } finally {
            releaseReadLocks();
         }
      }

      /** {@inheritDoc} */
      @Override
      public Object[] toArray() {
         acquireReadLocks();
         try {
            Object ret[] = new Object[sizeNoLocks()];
            int len = shards.length;
            @SuppressWarnings("unchecked")
            SortedSet<E> shardSubSets[] = (SortedSet<E>[]) new SortedSet<?>[len];
            for (int i = 0; i < len; i++) {
               shardSubSets[i] = subSet((SortedSet<E>) shards[i]);
            }
            copyToArray(shardSubSets, ret);
            return ret;
         } finally {
            releaseReadLocks();
         }
      }

      /** {@inheritDoc} */
      @Override
      public <T> T[] toArray(T[] a) {
         acquireReadLocks();
         try {
            int sz = sizeNoLocks();
            a = ArrayUtils.newArrayIfTooSmall(a, sz);
            int len = shards.length;
            @SuppressWarnings("unchecked")
            SortedSet<E> shardSubSets[] = (SortedSet<E>[]) new SortedSet<?>[len];
            for (int i = 0; i < len; i++) {
               shardSubSets[i] = subSet((SortedSet<E>) shards[i]);
            }
            copyToArray(shardSubSets, a);
            if (a.length > sz) {
               a[sz] = null;
            }
            return a;
         } finally {
            releaseReadLocks();
         }
      }

      /** {@inheritDoc} */
      @Override
      public Comparator<? super E> comparator() {
         return ShardedConcurrentSortedSet.this.comparator();
      }

      /** {@inheritDoc} */
      @Override
      public E first() {
         acquireReadLocks();
         try {
            E ret = null;
            for (Set<E> shard : shards) {
               SortedSet<E> subShard = subSet((SortedSet<E>) shard);
               if (!subShard.isEmpty()) {
                  E other = subShard.first();
                  if (ret == null || comp.compare(other, ret) < 0) {
                     ret = other;
                  }
               }
            }
            if (ret == null) {
               throw new NoSuchElementException();
            }
            return ret;
         } finally {
            releaseReadLocks();
         }
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> headSet(E toElement) {
         checkRangeLow(toElement, false);
         checkRangeHigh(toElement, false);
         return new SubSetImpl(from, toElement);
      }

      /** {@inheritDoc} */
      @Override
      public E last() {
         acquireReadLocks();
         try {
            E ret = null;
            for (Set<E> shard : shards) {
               SortedSet<E> subShard = subSet((SortedSet<E>) shard);
               if (!subShard.isEmpty()) {
                  E other = subShard.last();
                  if (ret == null || comp.compare(other, ret) > 0) {
                     ret = other;
                  }
               }
            }
            if (ret == null) {
               throw new NoSuchElementException();
            }
            return ret;
         } finally {
            releaseReadLocks();
         }
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> subSet(E fromElement, E toElement) {
         checkRangeLow(fromElement, true);
         checkRangeHigh(toElement, false);
         return new SubSetImpl(fromElement, toElement);
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> tailSet(E fromElement) {
         checkRangeLow(fromElement, true);
         checkRangeHigh(fromElement, false);
         return new SubSetImpl(fromElement, to);
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
   
   /**
    * The comparator used to compare elements for sorting. If natural ordering
    * is used, this will field will be {@link CollectionUtils#NATURAL_ORDERING}
    * instead of {@code null}.
    */
   transient Comparator<? super E> comp;
   
   /**
    * Constructs a new set, based on the provided set implementation.
    * 
    * @param <S> the type of the underlying set implementation
    * @param set the underlying set implementation
    * @param concurrency the number of expected concurrent writers
    * @param fair whether or not fair read-write locks are used
    * 
    * @see ShardedConcurrentSet#ConcurrentSet(Set, int, boolean)
    */
   <S extends SortedSet<E> & Cloneable> ShardedConcurrentSortedSet(S set,
         int concurrency, boolean fair) {
      super(set, concurrency, fair);
      setComparator();
   }
   
   private void setComparator() {
      comp = ((SortedSet<E>) shards[0]).comparator();
      if (comp == null) {
         comp = CollectionUtils.naturalOrder();
      }
   }
   
   @Override
   public boolean contains(Object o) {
      // must override since ordering could be inconsistent w/ equals
      for (int i = 0, len = shards.length; i < len; i++) {
         acquireReadLock(i);
         try {
            if (shards[i].contains(o)) {
               return true;
            }
         } finally {
            releaseReadLock(i);
         }
      }
      return false;
   }

   @Override
   public boolean containsAll(Collection<?> coll) {
      // must override since ordering could be inconsistent w/ equals
      for (Object o : coll) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean remove(Object o) {
      // must override since ordering could be inconsistent w/ equals
      for (int i = 0, len = shards.length; i < len; i++) {
         acquireWriteLock(i);
         try {
            if (shards[i].remove(o)) {
               return true;
            }
         } finally {
            releaseWriteLock(i);
         }
      }
      return false;
   }

   @Override
   public boolean removeAll(Collection<?> coll) {
      // must override since ordering could be inconsistent w/ equals
      boolean ret = false;
      acquireWriteLocks();
      try {
         for (int i = 0, len = shards.length; i < len; i++) {
            if (shards[i].removeAll(coll)) {
               ret = true;
            }
         }
      } finally {
         releaseWriteLocks();
      }
      return ret;
   }

   @Override
   public boolean retainAll(Collection<?> coll) {
      // must override since ordering could be inconsistent w/ equals
      boolean ret = false;
      acquireWriteLocks();
      try {
         for (int i = 0, len = shards.length; i < len; i++) {
            if (shards[i].retainAll(coll)) {
               ret = true;
            }
         }
      } finally {
         releaseWriteLocks();
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public Comparator<? super E> comparator() {
      return comp;
   }

   /** {@inheritDoc} */
   @Override
   public E first() {
      acquireReadLocks();
      try {
         E ret = null;
         for (Set<E> shard : shards) {
            if (!shard.isEmpty()) {
               E other = ((SortedSet<E>) shard).first();
               if (ret == null || comp.compare(other, ret) < 0) {
                  ret = other;
               }
            }
         }
         if (ret == null) {
            throw new NoSuchElementException();
         }
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public SortedSet<E> headSet(E toElement) {
      return new SubSetImpl(null, toElement);
   }
   
   @Override
   protected Iterator<E> makeIterator(Set<E> stableShards[]) {
      return new SortedIteratorImpl(stableShards);
   }

   /** {@inheritDoc} */
   @Override
   public E last() {
      acquireReadLocks();
      try {
         E ret = null;
         for (Set<E> shard : shards) {
            if (!shard.isEmpty()) {
               E other = ((SortedSet<E>) shard).last();
               if (ret == null || comp.compare(other, ret) > 0) {
                  ret = other;
               }
            }
         }
         if (ret == null) {
            throw new NoSuchElementException();
         }
         return ret;
      } finally {
         releaseReadLocks();
      }
   }

   /** {@inheritDoc} */
   @Override
   public SortedSet<E> subSet(E fromElement, E toElement) {
      return new SubSetImpl(fromElement, toElement);
   }

   /** {@inheritDoc} */
   @Override
   public SortedSet<E> tailSet(E fromElement) {
      return new SubSetImpl(fromElement, null);
   }
   
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeObject(comp == Comparator.naturalOrder() ? null : comp);
   }
   
   @SuppressWarnings("unchecked")
   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      comp = (Comparator<? super E>) in.readObject();
      if (comp == null) {
         comp = CollectionUtils.naturalOrder();
      }
   }
}
