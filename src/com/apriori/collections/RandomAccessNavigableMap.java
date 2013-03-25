package com.apriori.collections;

import java.util.NavigableMap;

//TODO: javadoc
public interface RandomAccessNavigableMap<K, V> extends RandomAccessSortedMap<K, V>, NavigableMap<K, V> {

   @Override RandomAccessNavigableMap<K, V> descendingMap();

   @Override RandomAccessNavigableSet<K> navigableKeySet();

   @Override RandomAccessNavigableSet<K> descendingKeySet();

   @Override RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

   @Override RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive);

   @Override RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive);

   @Override RandomAccessNavigableMap<K, V> subMap(K fromKey, K toKey);

   @Override RandomAccessNavigableMap<K, V> headMap(K toKey);

   @Override RandomAccessNavigableMap<K, V> tailMap(K fromKey);
}
