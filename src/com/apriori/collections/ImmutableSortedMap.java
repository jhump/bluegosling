package com.apriori.collections;

import java.util.Comparator;

// TODO: javadoc
public interface ImmutableSortedMap<K, V> extends ImmutableMap<K, V> {
   Comparator<? super K> comparator();
   Entry<K, V> firstEntry();
   Entry<K, V> lastEntry();
   Entry<K, V> floorEntry(K k);
   Entry<K, V> higherEntry(K k);
   Entry<K, V> ceilEntry(K k);
   Entry<K, V> lowerEntry(K k);
   ImmutableSortedMap<K, V> subMap(K from, K to);
   ImmutableSortedMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive);
   ImmutableSortedMap<K, V> headMap(K to);
   ImmutableSortedMap<K, V> headMap(K to, boolean inclusive);
   ImmutableSortedMap<K, V> tailMap(K from);
   ImmutableSortedMap<K, V> tailMap(K from, boolean inclusive);
   @Override ImmutableSortedSet<K> keySet();
}
