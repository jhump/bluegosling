package com.apriori.collections;

import java.util.Iterator;
import java.util.Map;

//TODO: javadoc
public class ImmutableMapWrapper<K, V, M extends Map<K, V>> implements ImmutableMap<K, V> {

   protected final M map;

   ImmutableMapWrapper(M map) {
      this.map = map;
   }
   
   @Override
   public int size() {
      return map.size();
   }

   @Override
   public Iterator<Entry<K, V>> iterator() {
      return entrySet().iterator();
   }

   @Override
   public boolean containsKey(Object o) {
      return map.containsKey(o);
   }

   @Override
   public boolean containsAllKeys(Iterable<?> keys) {
      return map.keySet().containsAll(Iterables.snapshot(keys));
   }

   @Override
   public boolean containsAnyKey(Iterable<?> keys) {
      for (Object k : keys) {
         if (containsKey(k)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean containsValue(Object o) {
      return map.containsValue(o);
   }

   @Override
   public boolean containsAllValues(Iterable<?> values) {
      return map.values().containsAll(Iterables.snapshot(values));
   }

   @Override
   public boolean containsAnyValue(Iterable<?> values) {
      for (Object v : values) {
         if (map.containsValue(v)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public V get(Object key) {
      return map.get(key);
   }

   @Override
   public ImmutableSet<K> keySet() {
      return new ImmutableSetWrapper<>(map.keySet());
   }

   @Override
   public ImmutableCollection<V> values() {
      return new ImmutableCollectionWrapper<>(map.values());
   }

   @Override
   public ImmutableSet<Entry<K, V>> entrySet() {
      return new ImmutableSetWrapper<>(
            new TransformingSet<>(map.entrySet(), Immutables::toImmutableMapEntry));
   }
}
