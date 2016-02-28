package com.bluegosling.collections;

import com.bluegosling.collections.BiMap.BiEntry;
import com.bluegosling.tuples.Pair;

import java.util.Map;
import java.util.Set;

final class BiMaps {
   private BiMaps() {
   }

   static class InvertedEntry<K, V> implements BiEntry<K, V> {
      private final BiEntry<V, K> entry;

      InvertedEntry(BiEntry<V, K> entry) {
         this.entry = entry;
      }

      @Override
      public K getKey() {
         return entry.getValue();
      }

      @Override
      public V getValue() {
         return entry.getKey();
      }

      @Override
      public V setValue(V value) {
         return entry.setKey(value);
      }

      @Override
      public K setKey(K newKey) {
         return entry.setValue(newKey);
      }
      
      @Override
      public int hashCode() {
         return MapUtils.hashCode(this);
      }
      
      @Override
      public boolean equals(Object o) {
         return MapUtils.equals(this, o);
      }
      
      @Override
      public String toString() {
         return MapUtils.toString(this);
      }
   }
   
   static class InvertedMap<K, V> implements BiMap<K, V> {
      private final BiMap<V, K> map;

      InvertedMap(BiMap<V, K> map) {
         this.map = map;
      }
      
      @Override
      public int size() {
         return map.size();
      }

      @Override
      public boolean isEmpty() {
         return map.isEmpty();
      }

      @Override
      public boolean containsKey(Object key) {
         return map.containsValue(key);
      }

      @Override
      public boolean containsValue(Object value) {
         return map.containsKey(value);
      }

      @Override
      @SuppressWarnings("unchecked")
      public V get(Object key) {
         return map.getKey((K) key);
      }

      @Override
      @SuppressWarnings("unchecked")
      public V remove(Object key) {
         return map.removeValue((K) key);
      }

      @Override
      public void putAll(Map<? extends K, ? extends V> m) {
         m.forEach((k, v) -> map.put(v, k));
      }

      @Override
      public void clear() {
         map.clear();
      }

      @Override
      public Set<K> keySet() {
         return map.values();
      }

      @Override
      public Set<V> values() {
         return map.keySet();
      }

      @Override
      public boolean contains(K key, V value) {
         return map.contains(value, key);
      }

      @Override
      public K getKey(V value) {
         return map.get(value);
      }

      @Override
      public K removeValue(V value) {
         return map.remove(value);
      }

      @Override
      public Set<BiEntry<K, V>> biEntrySet() {
         return new TransformingSet<>(map.biEntrySet(), e -> new InvertedEntry<>(e));
      }

      @Override
      public V put(K key, V value) {
         return map.putValue(key, value);
      }

      @Override
      public V putIfAbsent(K key, V value) {
         return map.putValueIfAbsent(key, value);
      }

      @Override
      public V replace(K key, V value) {
         return map.replaceValue(key, value);
      }

      @Override
      public boolean replace(K key, V oldValue, V newValue) {
         return map.replaceValue(key, oldValue, newValue);
      }

      @Override
      public K putValue(V value, K key) {
         return map.put(value, key);
      }

      @Override
      public K putValueIfAbsent(V value, K key) {
         return map.putIfAbsent(value, key);
      }

      @Override
      public K replaceValue(V value, K key) {
         return map.replace(value, key);
      }

      @Override
      public boolean replaceValue(V value, K oldKey, K newKey) {
         return map.replace(value, oldKey, newKey);
      }
      
      @Override
      public boolean putIfBothAbsent(K key, V value) {
         return map.putIfBothAbsent(value, key);
      }

      @Override
      public Pair<K, V> replaceIfBothPresent(K key, V value) {
         Pair<V, K> result = map.replaceIfBothPresent(value, key);
         return result == null ? null : result.swap();
      }
      
      @Override
      public BiMap<V, K> invert() {
         return map;
      }

      @Override
      public int hashCode() {
         return MapUtils.hashCode(this);
      }
      
      @Override
      public boolean equals(Object o) {
         return MapUtils.equals(this, o);
      }
      
      @Override
      public String toString() {
         return MapUtils.toString(this);
      }
   }
}
