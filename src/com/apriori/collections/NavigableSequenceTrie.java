package com.apriori.collections;

import java.util.Comparator;
import java.util.NavigableMap;

//TODO: javadoc
public interface NavigableSequenceTrie<K, V> extends SequenceTrie<K, V>, NavigableMap<Iterable<K>, V> {

   Comparator<? extends K> componentComparator();
   
   @Override NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix);

   @Override NavigableSequenceTrie<K, V> descendingMap();

   @Override NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, boolean fromInclusive,
         Iterable<K> toKey, boolean toInclusive);

   @Override NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey, boolean inclusive);

   @Override NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey, boolean inclusive);

   @Override NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, Iterable<K> toKey);

   @Override NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey);

   @Override NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey);
}
