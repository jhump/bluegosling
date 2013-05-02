package com.apriori.collections;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;

/**
 * A navigable map that provides random access over its elements. The entries in the map are
 * ordered according to the map's {@link Comparator} or (if there is none) according to the
 * keys' {@linkplain Comparable natural ordering}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @see RandomAccessNavigableSet
 */
//TODO: javadoc
public interface RandomAccessNavigableMap<K, V> extends NavigableMap<K, V> {

   @Override RandomAccessSet<K> keySet();

   @Override List<V> values(); // returned List also implements RandomAccess

   @Override RandomAccessSet<Map.Entry<K, V>> entrySet();
   
   ListIterator<Map.Entry<K, V>> listIterator();
   
   ListIterator<Map.Entry<K, V>> listIterator(int index);
   
   Map.Entry<K, V> getEntry(int index);
   
   Map.Entry<K, V> removeEntry(int index);
   
   int indexOfKey(Object key);
   
   RandomAccessNavigableMap<K, V> subMapByIndices(int startIndex, int endIndex);
   
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
