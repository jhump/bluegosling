
package com.bluegosling.collections;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

// TODO: javadoc
public interface PersistentMap<K, V> extends ImmutableMap<K, V> {
   PersistentMap<K, V> put(K key, V value);
   PersistentMap<K, V> remove(Object o);
   PersistentMap<K, V> removeAll(Iterable<?> keys);
   PersistentMap<K, V> retainAll(Iterable<?> keys);
   PersistentMap<K, V> putAll(Map<? extends K, ? extends V> items);
   PersistentMap<K, V> putAll(ImmutableMap<? extends K, ? extends V> items);
   PersistentMap<K, V> clear();
   
   default PersistentMap<K, V> remove(Object key, Object value) {
      V v = get(key);
      boolean match = value != null ? value.equals(v) : v == null && containsKey(key);
      return match ? remove(key) : this;
   }

   default PersistentMap<K, V> putIfAbsent(K key, V newValue) {
      return containsKey(key) ? this : put(key, newValue);
   }

   default PersistentMap<K, V> replace(K key, V newValue) {
      return containsKey(key) ? put(key, newValue) : this;
   }
   
   default PersistentMap<K, V> replace(K key, V oldValue, V newValue) {
      V v = get(key);
      boolean match = oldValue != null ? oldValue.equals(v) : v == null && containsKey(key);
      return match ? put(key, newValue) : this;
   }
   
   default PersistentMap<K, V> replaceAll(BiFunction<? super K, ? super V, ? extends V> fn) {
      PersistentMap<K, V> ret = this;
      for (Entry<K, V> entry : this) {
         ret = ret.put(entry.key(), fn.apply(entry.key(), entry.value()));
      }
      return ret;
   }
   
   default PersistentMap<K, V> compute(K key, BiFunction<? super K, ? super V, ? extends V> fn) {
      V newValue = fn.apply(key, get(key));
      return newValue == null ? remove(key) : put(key, newValue);
   }

   default PersistentMap<K, V> computeIfAbsent(K key, Function<? super K, ? extends V> fn) {
      if (containsKey(key)) {
         return this;
      }
      V value = fn.apply(key);
      return value == null ? this : put(key, value);
   }

   default PersistentMap<K, V> computeIfPresent(K key,
         BiFunction<? super K, ? super V, ? extends V> fn) {
      V oldValue = get(key);
      if (oldValue == null && !containsKey(key)) {
         return this;
      }
      V newValue = fn.apply(key, oldValue);
      return newValue == null ? remove(key) : put(key, newValue);
   }

   default PersistentMap<K, V> merge(K key, V value,
         BiFunction<? super V, ? super V, ? extends V> fn) {
      V oldValue = get(key);
      V newValue = oldValue == null ? value : fn.apply(oldValue, value);
      return newValue == null ? remove(key) : put(key, newValue);
   }
}
