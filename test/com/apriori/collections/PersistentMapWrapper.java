package com.apriori.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper to adapt a {@link Map} to a {@link PersistentMap}. This is used to create "reference
 * implementations" for a {@link PersistentMap}, against which the behavior of other implementations
 * can be tested. For example, we wrap an {@link ArrayList} and use the resulting persistent list to
 * verify behavior of other persistent lists. These wrapped persistent collections are for testing.
 * They copy the entire wrapped collection during update operations, so they are not expected to
 * perform well.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class PersistentMapWrapper<K, V> extends ImmutableMapWrapper<K, V, Map<K, V>>
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
      newMap.keySet().removeAll(Iterables.snapshot(keys));
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> retainAll(Iterable<?> keys) {
      Map<K, V> newMap = copy(map);
      newMap.keySet().retainAll(Iterables.snapshot(keys));
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
