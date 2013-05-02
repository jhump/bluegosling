package com.apriori.collections;

import java.util.Comparator;
import java.util.NavigableMap;

//TODO: javadoc
public interface NavigableCompositeTrie<K, C, V>
      extends CompositeTrie<K, C, V>, NavigableMap<K, V> {

   Comparator<? extends C> componentComparator();
   
   @Override NavigableCompositeTrie<K, C, V> prefixMap(K prefix);
   
   @Override NavigableCompositeTrie<K, C, V> prefixMap(K prefix, int numComponents);
   
   @Override NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> prefix);
   
   @Override NavigableCompositeTrie<K, C, V> prefixMap(Iterable<C> prefix, int numComponents);

   @Override NavigableCompositeTrie<K, C, V> descendingMap();

   @Override NavigableCompositeTrie<K, C, V> subMap(K fromKey, boolean fromInclusive,
         K toKey, boolean toInclusive);

   @Override NavigableCompositeTrie<K, C, V> headMap(K toKey, boolean inclusive);

   @Override NavigableCompositeTrie<K, C, V> tailMap(K fromKey, boolean inclusive);

   @Override NavigableCompositeTrie<K, C, V> subMap(K fromKey, K toKey);

   @Override NavigableCompositeTrie<K, C, V> headMap(K toKey);

   @Override NavigableCompositeTrie<K, C, V> tailMap(K fromKey);
}
