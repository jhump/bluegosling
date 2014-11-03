package com.apriori.collections;

import java.util.HashMap;
import java.util.Map;

//TODO: javadoc
public class PersistentMapWrapper<K, V> extends ImmutableMapWrapper<K, V, Map<K, V>>
      implements PersistentMap<K, V> {


   PersistentMapWrapper(Map<K, V> map) {
      super(map);
   }

   protected Map<K, V> copy(Map<K, V> original) {
      return new HashMap<>(original);
   }
   
   protected PersistentMapWrapper<K, V> wrapPersistent(Map<K, V> m) {
      return new PersistentMapWrapper<>(m);
   }

   @Override
   public PersistentMap<K, V> put(K key, V value) {
      Map<K, V> newMap = copy(map);
      newMap.put(key, value);
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> remove(Object o) {
      Map<K, V> newMap = copy(map);
      newMap.remove(o);
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> removeAll(Iterable<?> keys) {
      Map<K, V> newMap = copy(map);
      newMap.keySet().removeAll(ImmutableCollectionWrapper.fromIterable(keys));
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> retainAll(Iterable<?> keys) {
      Map<K, V> newMap = copy(map);
      newMap.keySet().retainAll(ImmutableCollectionWrapper.fromIterable(keys));
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> putAll(Map<? extends K, ? extends V> items) {
      Map<K, V> newMap = copy(map);
      newMap.putAll(items);
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> items) {
      Map<K, V> newMap = copy(map);
      newMap.putAll(Immutables.asIfMutable(items));
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> clear() {
      Map<K, V> newMap = copy(map);
      newMap.clear();
      return wrapPersistent(newMap);
   }
}
