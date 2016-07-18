package com.bluegosling.collections;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;

public class NavigableSetFromMap<E> extends SortedSetFromMap<E> implements NavigableSet<E> {
   public NavigableSetFromMap(NavigableMap<E, Boolean> map) {
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
