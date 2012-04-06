// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

/**
 * An implementation of {@link SortedSet} that supports concurrent access. This
 * implementation uses the same approach and provides the same consistency
 * and atomicity guarantees as its superclass, {@link ConcurrentSet}.
 * 
 * <p>This set does not support {@code null} values, even if the underlying set
 * implementations do.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> the type of element contained in the set
 * 
 * @see ConcurrentSet
 */
class ConcurrentSortedSet<E> extends ConcurrentSet<E>
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
      private Iterator<E> iterators[];
      private E nextElements[];
      private boolean haveNext;
      private E lastElement;
      private boolean removed;
      
      @SuppressWarnings("unchecked")
      SortedIteratorImpl(Set<E> stableShards[]) {
         int len = stableShards.length;
         iterators = new Iterator[len];
         for (int i = 0; i < len; i++) {
            Iterator<E> iter = getIterator(stableShards[i]);
            if (iter.hasNext()) {
               haveNext = true;
               nextElements[i] = iter.next();
            } else {
               // avoid pinning unused object to the heap by
               // not keeping references to empty iterators
               iter = null;
            }
            iterators[i] = iter;
         }
      }
      
      Iterator<E> getIterator(Set<E> shard) {
         return shard.iterator();
      }

      /** {@inheritDoc} */
      @Override
      public synchronized boolean hasNext() {
         return haveNext;
      }

      /** {@inheritDoc} */
      @Override
      public synchronized E next() {
         if (haveNext) {
            int idx = -1;
            int nextCount = 0;
            lastElement = null; // store the element to return in this field
            // find least element already fetched
            for (int i = 0, len = nextElements.length; i < len; i++) {
               E other = nextElements[i];
               if (other != null) {
                  nextCount++;
                  if (lastElement == null || comp.compare(other, lastElement) < 0) {
                     lastElement = other;
                     idx = i; // save index
                  }
               }
            }
            // replace the element we're about to return so subsequent
            // call has the right set of values to look at
            if (iterators[idx].hasNext()) {
               nextElements[idx] = iterators[idx].next();
            } else {
               // clear references
               nextElements[idx] = null;
               iterators[idx] = null;
               // decrement this since we just cleared out one of them
               nextCount--;
            }
            haveNext = nextCount > 0;
            removed = false;
            return lastElement;
         } else {
            throw new NoSuchElementException();
         }
      }

      /** {@inheritDoc} */
      @Override
      public synchronized void remove() {
         if (removed) {
            throw new IllegalStateException("element already removed");
         } else if (lastElement == null) {
            throw new IllegalStateException("no element to remove");
         } else {
            ConcurrentSortedSet.this.remove(lastElement);
         }
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
      }
      
      @SuppressWarnings("unchecked")
      boolean isInRange(Object o) {
         return CollectionUtils.isInRange(o, from, true, to, false, (Comparator<Object>) comp);
      }
      
      private void checkRange(Object o) {
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
         return ConcurrentSortedSet.this.add(e);
      }

      /** {@inheritDoc} */
      @Override
      public boolean addAll(Collection<? extends E> coll) {
         for (E e : coll) {
            checkRange(e);
         }
         return ConcurrentSortedSet.this.addAll(coll);
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
            return ConcurrentSortedSet.this.contains(o);
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
         return ConcurrentSortedSet.this.containsAll(coll);
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
         return ConcurrentSortedSet.this.remove(o);
      }

      /** {@inheritDoc} */
      @Override
      public boolean removeAll(Collection<?> coll) {
         // same logic here as in ConcurrentCopyOnIterationSet.removeAll
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
                  if (shard.removeAll(colls[i])) {
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
            SortedSet<E> shardSubSets[] = new SortedSet[len];
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
      @SuppressWarnings("unchecked")
      @Override
      public <T> T[] toArray(T[] a) {
         acquireReadLocks();
         try {
            int sz = sizeNoLocks();
            if (a.length < sz) {
               a = (T[]) Array.newInstance(a.getClass().getComponentType(), sz);
            }
            int len = shards.length;
            SortedSet<E> shardSubSets[] = new SortedSet[len];
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
         return ConcurrentSortedSet.this.comparator();
      }

      /** {@inheritDoc} */
      @Override
      public E first() {
         acquireReadLocks();
         try {
            E ret = null;
            for (Set<E> shard : shards) {
               E other = subSet((SortedSet<E>) shard).first();
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
      public SortedSet<E> headSet(E toElement) {
         checkRange(toElement);
         return new SubSetImpl(from, toElement);
      }

      /** {@inheritDoc} */
      @Override
      public E last() {
         acquireReadLocks();
         try {
            E ret = null;
            for (Set<E> shard : shards) {
               E other = subSet((SortedSet<E>) shard).last();
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
      public SortedSet<E> subSet(E fromElement, E toElement) {
         checkRange(fromElement);
         checkRange(toElement);
         return new SubSetImpl(fromElement, toElement);
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> tailSet(E fromElement) {
         checkRange(fromElement);
         return new SubSetImpl(fromElement, to);
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
    * @see ConcurrentSet#ConcurrentSet(Set, int, boolean)
    */
   <S extends SortedSet<E> & Cloneable> ConcurrentSortedSet(S set,
         int concurrency, boolean fair) {
      super(set, concurrency, fair);
      setComparator();
   }
   
   private void setComparator() {
      comp = ((SortedSet<E>) shards[0]).comparator();
      if (comp == null) {
         comp = CollectionUtils.NATURAL_ORDERING;
      }
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
            E other = ((SortedSet<E>) shard).first();
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
            E other = ((SortedSet<E>) shard).last();
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
      out.writeObject(comp == CollectionUtils.NATURAL_ORDERING ? null : comp);
   }
   
   @SuppressWarnings("unchecked")
   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
      in.defaultReadObject();
      comp = (Comparator<? super E>) in.readObject();
      if (comp == null) {
         comp = CollectionUtils.NATURAL_ORDERING;
      }
   }
}
