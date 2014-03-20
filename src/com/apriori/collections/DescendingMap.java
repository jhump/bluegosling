package com.apriori.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;

// TODO: javadoc
class DescendingMap<K, V> implements NavigableMap<K, V> {

   final NavigableMap<K, V> base;

   /**
    * Constructs a new descending view of the specified map.
    *
    * @param base the underlying set
    */
   DescendingMap(NavigableMap<K, V> base) {
      this.base = base;
   }
   
   @Override
   public Comparator<? super K> comparator() {
      return DescendingSet.reverseComparator(base.comparator());
   }

   @Override
   public K firstKey() {
      return base.lastKey();
   }

   @Override
   public K lastKey() {
      return base.firstKey();
   }

   @Override
   public NavigableSet<K> keySet() {
      return base.descendingKeySet();
   }

   @Override
   public Collection<V> values() {
      // keySet() is in proper order thanks to NavigableMap.descendingKeySet()
      // so we create a collection that returns items in the correct order based on it
      return new TransformingCollection<K, V>(keySet(), (k) -> get(k));
   }

   @Override
   public Set<Map.Entry<K, V>> entrySet() {
      return new TransformingSet<K, Map.Entry<K, V>>(keySet(), (k) -> floorEntry(k));
   }

   @Override
   public int size() {
      return base.size();
   }

   @Override
   public boolean isEmpty() {
      return base.isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      return base.containsKey(key);
   }

   @Override
   public boolean containsValue(Object value) {
      return base.containsValue(value);
   }

   @Override
   public V get(Object key) {
      return base.get(key);
   }

   @Override
   public V put(K key, V value) {
      return base.put(key, value);
   }

   @Override
   public V remove(Object key) {
      return base.remove(key);
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      base.putAll(m);
   }

   @Override
   public void clear() {
      base.clear();
   }

   @Override
   public Map.Entry<K, V> lowerEntry(K key) {
      return base.higherEntry(key);
   }

   @Override
   public K lowerKey(K key) {
      return base.higherKey(key);
   }

   @Override
   public Map.Entry<K, V> floorEntry(K key) {
      return base.ceilingEntry(key);
   }

   @Override
   public K floorKey(K key) {
      return base.ceilingKey(key);
   }

   @Override
   public Map.Entry<K, V> ceilingEntry(K key) {
      return base.floorEntry(key);
   }

   @Override
   public K ceilingKey(K key) {
      return base.floorKey(key);
   }

   @Override
   public Map.Entry<K, V> higherEntry(K key) {
      return base.lowerEntry(key);
   }

   @Override
   public K higherKey(K key) {
      return base.lowerKey(key);
   }

   @Override
   public Map.Entry<K, V> firstEntry() {
      return base.lastEntry();
   }

   @Override
   public Map.Entry<K, V> lastEntry() {
      return base.firstEntry();
   }

   @Override
   public Map.Entry<K, V> pollFirstEntry() {
      return base.pollLastEntry();
   }

   @Override
   public java.util.Map.Entry<K, V> pollLastEntry() {
      return base.pollFirstEntry();
   }

   @Override
   public NavigableMap<K, V> descendingMap() {
      return base;
   }

   @Override
   public NavigableSet<K> navigableKeySet() {
      return base.descendingKeySet();
   }

   @Override
   public NavigableSet<K> descendingKeySet() {
      return base.navigableKeySet();
   }

   @Override
   public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return new DescendingMap<K, V>(base.subMap(toKey, toInclusive, fromKey, fromInclusive));
   }

   @Override
   public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return new DescendingMap<K, V>(base.tailMap(toKey, inclusive));
   }

   @Override
   public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return new DescendingMap<K, V>(base.headMap(fromKey,  inclusive));
   }

   @Override
   public NavigableMap<K, V> subMap(K fromKey, K toKey) {
      return new DescendingMap<K, V>(base.subMap(toKey, false, fromKey, true));
   }

   @Override
   public NavigableMap<K, V> headMap(K toKey) {
      return new DescendingMap<K, V>(base.tailMap(toKey, false));
   }

   @Override
   public SortedMap<K, V> tailMap(K fromKey) {
      return new DescendingMap<K, V>(base.headMap(fromKey, true));
   }
   
   @Override
   public boolean equals(Object o) {
      return base.equals(o);
   }
   
   @Override
   public int hashCode() {
      return base.hashCode();
   }
   
   @Override
   public String toString() {
      return MapUtils.toString(this);
   }
}
