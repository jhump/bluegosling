package com.bluegosling.collections.immutable;

import com.bluegosling.collections.MoreIterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
public class PersistentMapWrapper<K, V> implements PersistentMap<K, V> {

   protected final Map<K, V> map;

   public PersistentMapWrapper(Map<K, V> map) {
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
   public boolean containsKey(Object o) {
      return map.containsKey(o);
   }

   @Override
   public boolean containsValue(Object o) {
      return map.containsValue(o);
   }

   @Override
   public V get(Object key) {
      return map.get(key);
   }

   @Override
   public Set<K> keySet() {
      return map.keySet();
   }

   @Override
   public Collection<V> values() {
      return map.values();
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return map.entrySet();
   }

   protected Map<K, V> copy(Map<K, V> original) {
      return new HashMap<>(original);
   }
   
   protected PersistentMapWrapper<K, V> wrapPersistent(Map<K, V> m) {
      return new PersistentMapWrapper<>(m);
   }

   @Override
   public PersistentMap<K, V> with(K key, V value) {
      Map<K, V> newMap = copy(map);
      newMap.put(key, value);
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> withoutKey(Object o) {
      Map<K, V> newMap = copy(map);
      newMap.remove(o);
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> withoutKeys(Iterable<?> keys) {
      Map<K, V> newMap = copy(map);
      newMap.keySet().removeAll(MoreIterables.snapshot(keys));
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> withoutKeys(Predicate<? super K> predicate) {
      Map<K, V> newMap = copy(map);
      newMap.keySet().removeIf(predicate);
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> withOnlyKeys(Iterable<?> keys) {
      Map<K, V> newMap = copy(map);
      newMap.keySet().retainAll(MoreIterables.snapshot(keys));
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> withAll(Map<? extends K, ? extends V> items) {
      Map<K, V> newMap = copy(map);
      newMap.putAll(items);
      return wrapPersistent(newMap);
   }

   @Override
   public PersistentMap<K, V> removeAll() {
      Map<K, V> newMap = copy(map);
      newMap.clear();
      return wrapPersistent(newMap);
   }
}
