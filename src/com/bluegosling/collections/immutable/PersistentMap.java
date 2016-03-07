
package com.bluegosling.collections.immutable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

// TODO: javadoc
public interface PersistentMap<K, V> extends Map<K, V> {
   
   @Deprecated
   @Override
   default V put(K k, V v) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default V putIfAbsent(K k, V v) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default V remove(Object k) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean remove(Object k, Object v) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default V replace(K k, V v) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean replace(K k, V oldV, V newV) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default void putAll(Map<? extends K, ? extends V> map) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default void clear() {
      throw new UnsupportedOperationException();
   }
   
   @Deprecated
   @Override
   default V compute(K k, BiFunction<? super K, ? super V, ? extends V> fn) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default V computeIfAbsent(K k, Function<? super K, ? extends V> fn) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> fn) {
      throw new UnsupportedOperationException();
   }
   
   @Deprecated
   @Override
   default V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> fn) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default void replaceAll(BiFunction<? super K, ? super V, ? extends V> fn) {
      throw new UnsupportedOperationException();
   }
   
   /**
    * Returns a view of this map's keys as an immutable set. No changes are allowed through the
    * returned view.
    *
    * @return a view of this map's keys as an immutable set
    */
   @Override
   Set<K> keySet();

   /**
    * Returns a view of this map's values as an immutable collection. No changes are allowed through
    * the returned view.
    *
    * @return a view of this map's values as an immutable collection
    */
   @Override
   Collection<V> values();
   
   /**
    * Returns a view of this map's mappings as an immutable set of entries. No changes are allowed
    * through the returned view.
    *
    * @return a view of this map's mappings as an immutable set of entries
    */
   @Override
   Set<Entry<K, V>> entrySet();
   
   PersistentMap<K, V> with(K key, V value);
   PersistentMap<K, V> withoutKey(Object o);
   PersistentMap<K, V> withoutKeys(Iterable<?> keys);
   PersistentMap<K, V> withoutKeys(Predicate<? super K> predicate);
   PersistentMap<K, V> withOnlyKeys(Iterable<?> keys);
   PersistentMap<K, V> withAll(Map<? extends K, ? extends V> items);
   PersistentMap<K, V> removeAll();
   
   default PersistentMap<K, V> without(Object key, Object value) {
      V v = get(key);
      boolean match = value != null ? value.equals(v) : v == null && containsKey(key);
      return match ? withoutKey(key) : this;
   }

   default PersistentMap<K, V> withNewKey(K key, V newValue) {
      return containsKey(key) ? this : with(key, newValue);
   }

   default PersistentMap<K, V> withExistingKey(K key, V newValue) {
      return containsKey(key) ? with(key, newValue) : this;
   }
   
   default PersistentMap<K, V> withReplacement(K key, V oldValue, V newValue) {
      V v = get(key);
      boolean match = oldValue != null ? oldValue.equals(v) : v == null && containsKey(key);
      return match ? with(key, newValue) : this;
   }
   
   default PersistentMap<K, V> withReplacements(BiFunction<? super K, ? super V, ? extends V> fn) {
      PersistentMap<K, V> ret = this;
      for (Entry<K, V> entry : this.entrySet()) {
         ret = ret.with(entry.getKey(), fn.apply(entry.getKey(), entry.getValue()));
      }
      return ret;
   }
   
   default PersistentMap<K, V> withComputedValue(K key,
         BiFunction<? super K, ? super V, ? extends V> fn) {
      V newValue = fn.apply(key, get(key));
      return newValue == null ? withoutKey(key) : with(key, newValue);
   }

   default PersistentMap<K, V> withNewKeyComputedValuey(K key,
         Function<? super K, ? extends V> fn) {
      if (containsKey(key)) {
         return this;
      }
      V value = fn.apply(key);
      return value == null ? this : with(key, value);
   }

   default PersistentMap<K, V> withNewComputedValue(K key,
         BiFunction<? super K, ? super V, ? extends V> fn) {
      V oldValue = get(key);
      if (oldValue == null && !containsKey(key)) {
         return this;
      }
      V newValue = fn.apply(key, oldValue);
      return newValue == null ? withoutKey(key) : with(key, newValue);
   }

   default PersistentMap<K, V> withMergedValue(K key, V value,
         BiFunction<? super V, ? super V, ? extends V> fn) {
      V oldValue = get(key);
      V newValue = oldValue == null ? value : fn.apply(oldValue, value);
      return newValue == null ? withoutKey(key) : with(key, newValue);
   }
}
