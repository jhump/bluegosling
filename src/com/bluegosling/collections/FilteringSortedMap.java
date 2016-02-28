package com.bluegosling.collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.function.BiPredicate;

// TODO: javadoc
// TODO: tests
public class FilteringSortedMap<K, V> extends FilteringMap<K, V> implements SortedMap<K, V> {

   public FilteringSortedMap(SortedMap<K, V> internal,
         BiPredicate<? super K, ? super V> predicate) {
      super(internal, predicate);
   }

   @Override
   protected SortedMap<K, V> internal() {
      return (SortedMap<K, V>) super.internal();
   }
   
   @Override
   public SortedMap<K, V> capture() {
      return Collections.unmodifiableSortedMap(new SortedArrayMap<>(this));
   }

   @Override
   public Comparator<? super K> comparator() {
      return internal().comparator();
   }

   @Override
   public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return new FilteringSortedMap<>(internal().subMap(fromKey, toKey), predicate());
   }

   @Override
   public SortedMap<K, V> headMap(K toKey) {
      return new FilteringSortedMap<>(internal().headMap(toKey), predicate());
   }

   @Override
   public SortedMap<K, V> tailMap(K fromKey) {
      return new FilteringSortedMap<>(internal().tailMap(fromKey), predicate());
   }

   @Override
   public K firstKey() {
      return keySet().iterator().next();
   }

   @Override
   public K lastKey() {
      K k = internal().lastKey();
      while (true) {
         V v = internal().get(k);
         if (predicate().test(k, v)) {
            return k;
         }
         // get next highest value and test that
         k = internal().headMap(k).lastKey();
      }
   }
}
