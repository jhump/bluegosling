package com.apriori.collections;

import com.apriori.util.Function;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;

//TODO: javadoc
//TODO: test
class DescendingRandomAccessMap<K, V> extends DescendingMap<K, V>
      implements RandomAccessNavigableMap<K, V> {

   DescendingRandomAccessMap(RandomAccessNavigableMap<K, V> base) {
      super(base);
   }
   
   RandomAccessNavigableMap<K, V> base() {
      return (RandomAccessNavigableMap<K, V>) base;
   }

   int convertIndex(int index) {
      return size() - index - 1;
   }

   @Override
   public ListIterator<Map.Entry<K, V>> listIterator() {
      return CollectionUtils.reverseIterator(base().listIterator(size()));
   }

   @Override
   public ListIterator<Map.Entry<K, V>> listIterator(int index) {
      return CollectionUtils.reverseIterator(base().listIterator(size() - index));
   }

   @Override
   public Map.Entry<K, V> getEntry(int index) {
      return base().getEntry(convertIndex(index));
   }

   @Override
   public Map.Entry<K, V> removeEntry(int index) {
      return base().removeEntry(index);
   }
   
   @Override
   public int indexOfKey(Object key) {
      return convertIndex(base().indexOfKey(key));
   }
   
   @Override
   public RandomAccessNavigableSet<K> keySet() {
      return navigableKeySet();
   }

   @Override
   public List<V> values() {
      // keySet() is in proper order thanks to RandomAccessNavigableMap.descendingKeySet()
      // so we create a list that returns items in the correct order based on it
      return new TransformingList<K, V>(keySet().asList(), new Function<K, V>() {
         @Override public V apply(K input) {
            return get(input);
         }
      });
   }

   @Override
   public RandomAccessSet<Map.Entry<K, V>> entrySet() {
      // can probably do something more efficient, like a specialized descending RandomAccessSet
      // (DescendingRandomAccessSet is actually a RandomAccess*Navigable*Set so doesn't quite work)
      return new TransformingRandomAccessSet<K, Map.Entry<K, V>>(keySet(),
            new Function<K, Map.Entry<K, V>>() {
               @Override public Map.Entry<K, V> apply(K input) {
                  return floorEntry(input);
               }
            });
   }
   
   @Override
   public RandomAccessNavigableMap<K, V> descendingMap() {
      return base();
   }

   @Override
   public RandomAccessNavigableSet<K> navigableKeySet() {
      return base().descendingKeySet();
   }

   @Override
   public RandomAccessNavigableSet<K> descendingKeySet() {
      return base().navigableKeySet();
   }
   
   @Override
   public RandomAccessNavigableMap<K, V> subMapByIndices(int startIndex, int endIndex) {
      return new DescendingRandomAccessMap<K, V>(
            base().subMapByIndices(convertIndex(endIndex), size() - startIndex));
   }

   @Override
   public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return new DescendingRandomAccessMap<K, V>(base().subMap(toKey, toInclusive, fromKey, fromInclusive));
   }

   @Override
   public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return new DescendingRandomAccessMap<K, V>(base().tailMap(toKey, inclusive));
   }

   @Override
   public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return new DescendingRandomAccessMap<K, V>(base().headMap(fromKey,  inclusive));
   }

   @Override
   public RandomAccessNavigableMap<K, V> subMap(K fromKey, K toKey) {
      return new DescendingRandomAccessMap<K, V>(base().subMap(toKey, false, fromKey, true));
   }

   @Override
   public RandomAccessNavigableMap<K, V> headMap(K toKey) {
      return new DescendingRandomAccessMap<K, V>(base().tailMap(toKey, false));
   }

   @Override
   public RandomAccessNavigableMap<K, V> tailMap(K fromKey) {
      return new DescendingRandomAccessMap<K, V>(base().headMap(fromKey, true));
   }
}