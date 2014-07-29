package com.apriori.collections;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;

//TODO: javadoc
public interface NavigableSequenceTrie<K, V> extends SequenceTrie<K, V>, NavigableMap<List<K>, V> {

   Comparator<? super K> componentComparator();
   
   @Override NavigableSequenceTrie<K, V> prefixMap(K prefix);
   
   @Override NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix);

   @Override NavigableSequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents);
   
   @Override NavigableSequenceTrie<K, V> descendingMap();

   @Override NavigableSequenceTrie<K, V> subMap(List<K> fromKey, boolean fromInclusive,
         List<K> toKey, boolean toInclusive);
   @Override NavigableSequenceTrie<K, V> headMap(List<K> toKey, boolean inclusive);
   @Override NavigableSequenceTrie<K, V> tailMap(List<K> fromKey, boolean inclusive);
   @Override NavigableSequenceTrie<K, V> subMap(List<K> fromKey, List<K> toKey);
   @Override NavigableSequenceTrie<K, V> headMap(List<K> toKey);
   @Override NavigableSequenceTrie<K, V> tailMap(List<K> fromKey);

   NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, boolean fromInclusive,
         Iterable<K> toKey, boolean toInclusive);
   NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey, boolean inclusive);
   NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey, boolean inclusive);
   NavigableSequenceTrie<K, V> subMap(Iterable<K> fromKey, Iterable<K> toKey);
   NavigableSequenceTrie<K, V> headMap(Iterable<K> toKey);
   NavigableSequenceTrie<K, V> tailMap(Iterable<K> fromKey);

   Entry<List<K>, V> lowerEntry(Iterable<K> keys);
   List<K> lowerKey(Iterable<K> keys);
   Entry<List<K>, V> higherEntry(Iterable<K> keys);
   List<K> higherKey(Iterable<K> keys);
   Entry<List<K>, V> ceilingEntry(Iterable<K> keys);
   List<K> ceilingKey(Iterable<K> keys);
   Entry<List<K>, V> floorEntry(Iterable<K> keys);
   List<K> floorKey(Iterable<K> keys);
}
