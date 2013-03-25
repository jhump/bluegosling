package com.apriori.collections;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

//TODO: javadoc
public interface RandomAccessSortedMap<K, V> extends SortedMap<K, V> {

   @Override RandomAccessSortedMap<K, V> subMap(K fromKey, K toKey);

   @Override RandomAccessSortedMap<K, V> headMap(K toKey);

   @Override RandomAccessSortedMap<K, V> tailMap(K fromKey);

   @Override RandomAccessSet<K> keySet();

   @Override List<V> values(); // returned List also implements RandomAccess

   @Override RandomAccessSet<Map.Entry<K, V>> entrySet();
}
