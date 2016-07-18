package com.bluegosling.collections;

import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;

public class SortedSetFromMap<E> extends AbstractSet<E> implements SortedSet<E> {
   private final SortedMap<E, Boolean> map;
   
   public SortedSetFromMap(SortedMap<E, Boolean> map) {
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
}
