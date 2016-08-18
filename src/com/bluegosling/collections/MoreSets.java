package com.bluegosling.collections;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Map.Entry;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * Utility methods for working with and creating instanceos of {@link Set}. These methods complement
 * those in Guava's {@link Sets} class.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class MoreSets {
   private MoreSets() {}

   /**
    * Returns a view of the given collection as a set. The view omits any duplicate values.
    * Iteration order is the same as the underlying collection except that only the first instance
    * of a given value is emitted (subsequent occurrences are elided). Unlike most collection
    * implementations, {@link Set#size()} for the return set runs in {@code O(n)} time, not
    * constant time.
    * 
    * <p>Note that if the given collection <em>is</em> a set, it is returned unchanged.
    * 
    * @param coll a collection
    * @return a view of the given collection as a set
    */
   public static <E> Set<E> fromCollection(Collection<E> coll) {
      return coll instanceof Set ? (Set<E>) coll : new SetFromCollection<>(coll);
   }
   
   /**
    * Returns a sorted set that is backed by the given map. This is the same as
    * {@link Collections#newSetFromMap(java.util.Map)}, except that the returned map implements the
    * {@link SortedSet} interface.
    * 
    * @param map a map
    * @return a sorted set that is backed by the given map
    */
   public static <E> SortedSet<E> newSortedSetFromMap(SortedMap<E, Boolean> map) {
      return new SortedSetFromMap<>(map);
   }

   /**
    * Returns a navigable set that is backed by the given map. This is the same as
    * {@link Collections#newSetFromMap(java.util.Map)}, except that the returned map implements the
    * {@link NavigableSet} interface.
    * 
    * @param map a map
    * @return a navigable set that is backed by the given map
    */
   public static <E> NavigableSet<E> newNavigableSetFromMap(NavigableMap<E, Boolean> map) {
      return new NavigableSetFromMap<>(map);
   }
   
   private static class SetFromCollection<E> implements Set<E> {
      private final Collection<E> coll;
      
      SetFromCollection(Collection<E> coll) {
         this.coll = coll;
      }

      @Override
      public int size() {
         return Iterators.size(iterator());
      }

      @Override
      public boolean isEmpty() {
         return coll.isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return coll.contains(o);
      }

      @Override
      public Iterator<E> iterator() {
         return MoreIterators.unique(coll.iterator());
      }

      @Override
      public Object[] toArray() {
         return CollectionUtils.toArray(this);
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return CollectionUtils.toArray(this, a);
      }

      @Override
      public boolean add(E e) {
         if (contains(e)) {
            return false;
         } else {
            return coll.add(e);
         }
      }

      @Override
      public boolean remove(Object o) {
         boolean ret = false;
         for (Iterator<E> iter = iterator(); iter.hasNext();) {
            E e = iter.next();
            if (Objects.equals(e, o)) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return coll.containsAll(c);
      }

      @Override
      public boolean addAll(Collection<? extends E> c) {
         boolean ret = false;
         for (E e : c) {
            if (add(e)) {
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return coll.retainAll(c);
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return coll.removeAll(c);
      }

      @Override
      public void clear() {
         coll.clear();
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
   
   private static class SortedSetFromMap<E> implements SortedSet<E> {
      private final SortedMap<E, Boolean> map;
      
      SortedSetFromMap(SortedMap<E, Boolean> map) {
         checkArgument(map.isEmpty(), "Specified map must be empty");
         this.map = map;
      }

      protected SortedMap<E, Boolean> underlying() {
         return map;
      }

      @Override
      public Comparator<? super E> comparator() {
         return map.comparator();
      }

      @Override
      public SortedSet<E> subSet(E fromElement, E toElement) {
         return new SortedSetFromMap<E>(map.subMap(fromElement, toElement));
      }

      @Override
      public SortedSet<E> headSet(E toElement) {
         return new SortedSetFromMap<E>(map.headMap(toElement));
      }

      @Override
      public SortedSet<E> tailSet(E fromElement) {
         return new SortedSetFromMap<E>(map.tailMap(fromElement));
      }

      @Override
      public E first() {
         return map.firstKey();
      }

      @Override
      public E last() {
         return map.lastKey();
      }

      @Override
      public Iterator<E> iterator() {
         return map.keySet().iterator();
      }

      @Override
      public int size() {
         return map.size();
      }

      @Override
      public boolean equals(Object o) {
         return map.keySet().equals(o);
      }

      @Override
      public int hashCode() {
         return map.keySet().hashCode();
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return map.keySet().removeAll(c);
      }

      @Override
      public boolean isEmpty() {
         return map.isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return map.containsKey(o);
      }

      @Override
      public Object[] toArray() {
         return map.keySet().toArray();
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return map.keySet().toArray(a);
      }

      @Override
      public boolean add(E e) {
         return map.put(e, Boolean.TRUE) == null;
      }
      
      @Override
      public boolean addAll(Collection<? extends E> c) {
         boolean ret = false;
         for (E e : c) {
            if (add(e)) {
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public boolean remove(Object o) {
         return map.remove(o) != null;
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return map.keySet().containsAll(c);
      }
      

      @Override
      public boolean retainAll(Collection<?> c) {
         return map.keySet().retainAll(c);
      }

      @Override
      public void clear() {
         map.clear();
      }

      @Override
      public String toString() {
         return map.keySet().toString();
      }
   }
   
   private static class NavigableSetFromMap<E> extends SortedSetFromMap<E>
   implements NavigableSet<E> {
      NavigableSetFromMap(NavigableMap<E, Boolean> map) {
         super(map);
      }
      
      @Override
      protected NavigableMap<E, Boolean> underlying() {
         return (NavigableMap<E, Boolean>) super.underlying();
      }

      @Override
      public E lower(E e) {
         return underlying().lowerKey(e);
      }

      @Override
      public E floor(E e) {
         return underlying().floorKey(e);
      }

      @Override
      public E ceiling(E e) {
         return underlying().ceilingKey(e);
      }

      @Override
      public E higher(E e) {
         return underlying().higherKey(e);
      }

      @Override
      public E pollFirst() {
         Entry<E, Boolean> entry = underlying().pollFirstEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public E pollLast() {
         Entry<E, Boolean> entry = underlying().pollLastEntry();
         return entry == null ? null : entry.getKey();
      }

      @Override
      public NavigableSet<E> descendingSet() {
         return new NavigableSetFromMap<>(underlying().descendingMap());
      }

      @Override
      public Iterator<E> descendingIterator() {
         return underlying().navigableKeySet().descendingIterator();
      }

      @Override
      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
            boolean toInclusive) {
         return new NavigableSetFromMap<>(
               underlying().subMap(fromElement, fromInclusive, toElement, toInclusive));
      }

      @Override
      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         return new NavigableSetFromMap<>(underlying().headMap(toElement, inclusive));
      }

      @Override
      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         return new NavigableSetFromMap<>(underlying().tailMap(fromElement, inclusive));
      }
   }
}
