package com.bluegosling.collections;

import java.util.Comparator;

// TODO: javadoc
public interface ImmutableSortedMap<K, V> extends ImmutableMap<K, V> {
   Comparator<? super K> comparator();
   Entry<K, V> firstEntry();
   default K firstKey() {
      Entry<K, V> entry = firstEntry();
      return entry == null ? null : entry.key();
   }
   
   Entry<K, V> lastEntry();
   default K lastKey() {
      Entry<K, V> entry = lastEntry();
      return entry == null ? null : entry.key();
   }
   
   Entry<K, V> floorEntry(K k);
   default K floorKey(K k) {
      Entry<K, V> entry = floorEntry(k);
      return entry == null ? null : entry.key();
   }
   
   Entry<K, V> higherEntry(K k);
   default K higherKey(K k) {
      Entry<K, V> entry = higherEntry(k);
      return entry == null ? null : entry.key();
   }
   
   Entry<K, V> ceilEntry(K k);
   default K ceilKey(K k) {
      Entry<K, V> entry = ceilEntry(k);
      return entry == null ? null : entry.key();
   }
   
   Entry<K, V> lowerEntry(K k);
   default K lowerKey(K k) {
      Entry<K, V> entry = lowerEntry(k);
      return entry == null ? null : entry.key();
   }
   
   
   default ImmutableSortedMap<K, V> subMap(K from, K to) {
      return subMap(from, true, to, false);
   }
   ImmutableSortedMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive);
   
   default ImmutableSortedMap<K, V> headMap(K to) {
      return headMap(to, false);
   }
   ImmutableSortedMap<K, V> headMap(K to, boolean inclusive);
   
   default ImmutableSortedMap<K, V> tailMap(K from) {
      return tailMap(from, true);
   }
   ImmutableSortedMap<K, V> tailMap(K from, boolean inclusive);
   
   @Override ImmutableSortedSet<K> keySet();
}
