// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

/**
 * An implementation of {@link NavigableSet} that supports concurrent access.
 * This implementation uses the same approach and provides the same consistency
 * and atomicity guarantees as ancestor class {@link ConcurrentSet}.
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
class ConcurrentNavigableSet<E> extends ConcurrentSortedSet<E>
      implements NavigableSet<E> {

   private static final long serialVersionUID = -701598894628602252L;

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
    * A sub-set of the concurrent set.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   class SubSetImpl extends ConcurrentSortedSet<E>.SubSetImpl implements NavigableSet<E> {
      private boolean fromInclusive;
      private boolean toInclusive;
      
      SubSetImpl(E from, boolean fromInclusive, E to, boolean toInclusive) {
         super(from, to);
         this.fromInclusive = fromInclusive;
         this.toInclusive = toInclusive;
      }
      
      @SuppressWarnings("unchecked")
      @Override
      boolean isInRangeLow(Object o, boolean inclusive) {
         return CollectionUtils.isInRangeLow(o, inclusive, from, fromInclusive,
               (Comparator<Object>) comp);
      }
      
      @SuppressWarnings("unchecked")
      @Override
      boolean isInRangeHigh(Object o, boolean inclusive) {
         return CollectionUtils.isInRangeHigh(o, inclusive, to, toInclusive,
               (Comparator<Object>) comp);
      }
      
      @SuppressWarnings("unchecked")
      @Override
      boolean isInRange(Object o) {
         return CollectionUtils.isInRange(o, from, fromInclusive, to, toInclusive,
               (Comparator<Object>) comp);
      }

      @Override
      NavigableSet<E> subSet(SortedSet<E> shard) {
         if (from == null) {
            return ((NavigableSet<E>) shard).headSet(to, toInclusive);
         } else if (to == null) {
            return ((NavigableSet<E>) shard).tailSet(from, fromInclusive);
         } else {
            return ((NavigableSet<E>) shard).subSet(from, fromInclusive, to, toInclusive);
         }
      }

      /** {@inheritDoc} */
      @Override
      public E ceiling(E e) {
         acquireReadLocks();
         try {
            E ret = null;
            for (Set<E> shard : shards) {
               E other = subSet((NavigableSet<E>) shard).ceiling(e);
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
         Set<E> stableShards[] = getStableShards();
         for (int i = 0, len = stableShards.length; i < len; i++) {
            stableShards[i] = subSet((SortedSet<E>) stableShards[i]);
         }
         return new DescendingIteratorImpl(stableShards);
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
               E other = subSet((NavigableSet<E>) shard).floor(e);
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
      public NavigableSet<E> headSet(E toElement) {
         return headSet(toElement, false);
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         checkRangeLow(toElement, false);
         checkRangeHigh(toElement, inclusive);
         return new SubSetImpl(from, fromInclusive, toElement, inclusive);
      }

      /** {@inheritDoc} */
      @Override
      public E higher(E e) {
         acquireReadLocks();
         try {
            E ret = null;
            for (Set<E> shard : shards) {
               E other = subSet((NavigableSet<E>) shard).higher(e);
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
               E other = subSet((NavigableSet<E>) shard).lower(e);
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
               E other = subSet((NavigableSet<E>) shard).pollFirst();
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
               E other = subSet((NavigableSet<E>) shard).pollLast();
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
      public NavigableSet<E> subSet(E fromElement, E toElement) {
         return subSet(fromElement, true, toElement, false);
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> subSet(E fromElement, boolean fromInc, E toElement, boolean toInc) {
         checkRangeLow(fromElement, fromInc);
         checkRangeHigh(toElement, toInc);
         return new SubSetImpl(fromElement, fromInc, toElement, toInc);
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> tailSet(E fromElement) {
         return tailSet(fromElement, true);
      }
      
      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         checkRangeLow(fromElement, inclusive);
         checkRangeHigh(fromElement, false);
         return new SubSetImpl(fromElement, inclusive, to, toInclusive);
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
   public NavigableSet<E> headSet(E toElement) {
      return headSet(toElement, false);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      return new SubSetImpl(null, false, toElement, inclusive);
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
   public NavigableSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
   }
   
   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
         boolean toInclusive) {
      return new SubSetImpl(fromElement, fromInclusive, toElement, toInclusive);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
   }
   
   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      return new SubSetImpl(fromElement, inclusive, null, false);
   }
}
