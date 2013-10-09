package com.apriori.collections;

import java.util.List;
import java.util.ListIterator;


public abstract class AbstractRandomAccessNavigableMap<K, V> extends AbstractNavigableMap<K, V>
      implements RandomAccessNavigableMap<K, V> {

   @Override
   public int indexOfKey(Object key) {
      return CollectionUtils.findObject(key, keySet().listIterator());
   }
   

   @Override
   public ListIterator<Entry<K, V>> listIterator() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ListIterator<Entry<K, V>> listIterator(int index) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public RandomAccessNavigableMap<K, V> subMapByIndices(int startIndex, int endIndex) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessSet<K> keySet() {
      return navigableKeySet();
   }

   @Override public List<V> values() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessSet<Entry<K, V>> entrySet() {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override public RandomAccessNavigableMap<K, V> descendingMap() {
      return new DescendingRandomAccessMap<K, V>(this);
   }

   @Override public RandomAccessNavigableSet<K> navigableKeySet() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessNavigableSet<K> descendingKeySet() {
      return navigableKeySet().descendingSet();
   }

   @Override public RandomAccessNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey,
         boolean toInclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   @Override public RandomAccessNavigableMap<K, V> subMap(K fromKey, K toKey) {
      return subMap(fromKey, true, toKey, false);
   }

   @Override public RandomAccessNavigableMap<K, V> headMap(K toKey) {
      return headMap(toKey, false);
   }

   @Override public RandomAccessNavigableMap<K, V> tailMap(K fromKey) {
      return tailMap(fromKey, true);
   }
}
