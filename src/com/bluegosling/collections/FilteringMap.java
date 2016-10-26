package com.bluegosling.collections;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// TODO: javadoc
// TODO: FilteringSortedMap, FilteringNavigableMap?
// TODO: tests
public class FilteringMap<K, V> implements Map<K, V> {
   
   public static <K, V> FilteringMap<K, V> filteringValues(Map<K, V> internal,
         Predicate<? super V> predicate) {
      return new FilteringMap<>(internal, (k, v) -> predicate.test(v));
   }

   public static <K, V> FilteringMap<K, V> filteringKeys(Map<K, V> internal,
         Predicate<? super K> predicate) {
      return new FilteringMap<>(internal, (k, v) -> predicate.test(k));
   }

   private final Map<K, V> internal;
   private final BiPredicate<? super K, ? super V> predicate;
   
   public FilteringMap(Map<K, V> internal, BiPredicate<? super K, ? super V> predicate) {
      this.internal = internal;
      this.predicate = predicate;
   }
   
   protected Map<K, V> internal() {
      return internal;
   }
   
   protected BiPredicate<? super K, ? super V> predicate() {
      return predicate;
   }
   
   public Map<K, V> materialize() {
      return ImmutableMap.copyOf(this);
   }

   @Override
   public int size() {
      return entrySet().size();
   }

   @Override
   public boolean isEmpty() {
      return entrySet().isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      V v = internal().get(key);
      if (v == null && !internal().containsKey(key)) {
         return false;
      }
      @SuppressWarnings("unchecked") // could cause ClassCastException below, but that's okay
      K k = (K) key;
      return predicate.test(k, v);
   }

   @Override
   public boolean containsValue(Object value) {
      return values().contains(value);
   }

   @Override
   public V get(Object key) {
      V v = FilteringMap.this.internal().get(key);
      if (v == null && !FilteringMap.this.internal().containsKey(key)) {
         return null;
      }
      @SuppressWarnings("unchecked") // could cause ClassCastException below, but that's okay
      K k = (K) key;
      return predicate().test(k, v) ? v : null;
   }

   @Override
   public V put(K key, V value) {
      if (predicate().test(key, value)) {
         V old = internal().put(key, value);
         if (old == null) {
            return null;
         }
         return predicate().test(key, old) ? old : null;
      } else {
         throw new IllegalArgumentException(
               "Specified entry does not pass filter: " + key + ", " + value);
      }
   }

   @Override
   public V remove(Object key) {
      V v = FilteringMap.this.internal().get(key);
      if (v == null && !FilteringMap.this.internal().containsKey(key)) {
         return null;
      }
      @SuppressWarnings("unchecked") // could cause ClassCastException below, but that's okay
      K k = (K) key;
      return predicate().test(k, v) ? internal().remove(key) : null;
   }
   
   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      LinkedHashMap<K, V> snapshot = new LinkedHashMap<>(m);
      for (Entry<K, V> entry : snapshot.entrySet()) {
         if (!predicate().test(entry.getKey(), entry.getValue())) {
            throw new IllegalArgumentException("Specified entry does not pass filter: "
                  + entry.getKey() + ", " + entry.getValue());
         }
      }
      // all items pass, so we can safely add everything
      internal().putAll(snapshot);
   }

   @Override
   public void clear() {
      entrySet().clear();
   }

   @Override
   public Set<K> keySet() {
      return new TransformingSet<Entry<K, V>, K>(entrySet(), e -> e.getKey()) {
         @Override public boolean contains(Object o) {
            return containsKey(o);
         }

         @Override public boolean remove(Object o) {
            V v = FilteringMap.this.internal().get(o);
            if (v == null && !FilteringMap.this.internal().containsKey(o)) {
               return false;
            }
            @SuppressWarnings("unchecked") // could cause ClassCastException below, but that's okay
            K k = (K) o;
            if (predicate().test(k, v)) {
               FilteringMap.this.internal().remove(o);
               return true;
            }
            return false;
         }
         
         @Override public boolean removeAll(Collection<?> coll) {
            boolean ret = false;
            for (Object o : coll) {
               if (remove(o)) {
                  ret = true;
               }
            }
            return ret;
         }
      };
   }

   @Override
   public Collection<V> values() {
      return new TransformingCollection<>(entrySet(), Entry::getValue);
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return new FilteringSet<>(internal().entrySet(),
            e -> predicate.test(e.getKey(), e.getValue()));
   }
   
   @Override
   public boolean equals(Object o) {
      return MapUtils.equals(this, o);
   }
   
   @Override
   public int hashCode() {
      return MapUtils.hashCode(this);
   }
   
   @Override
   public String toString() {
      return MapUtils.toString(this);
   }
}
