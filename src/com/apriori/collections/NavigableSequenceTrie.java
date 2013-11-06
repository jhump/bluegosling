package com.apriori.collections;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;

//TODO: javadoc
public interface NavigableSequenceTrie<K, V> extends SequenceTrie<K, V>, NavigableMap<List<K>, V> {

   Comparator<? extends K> componentComparator();
   
   @Override NavigableSequenceTrie<K, V> prefixMap(K prefix);
   
   @Override NavigableSequenceTrie<K, V> prefixMap(List<K> prefix);

   @Override NavigableSequenceTrie<K, V> prefixMap(List<K> prefix, int numComponents);
   
   @Override NavigableSequenceTrie<K, V> descendingMap();

   @Override NavigableSequenceTrie<K, V> subMap(List<K> fromKey, boolean fromInclusive,
         List<K> toKey, boolean toInclusive);

   @Override NavigableSequenceTrie<K, V> headMap(List<K> toKey, boolean inclusive);

   @Override NavigableSequenceTrie<K, V> tailMap(List<K> fromKey, boolean inclusive);

   @Override NavigableSequenceTrie<K, V> subMap(List<K> fromKey, List<K> toKey);

   @Override NavigableSequenceTrie<K, V> headMap(List<K> toKey);

   @Override NavigableSequenceTrie<K, V> tailMap(List<K> fromKey);
}
