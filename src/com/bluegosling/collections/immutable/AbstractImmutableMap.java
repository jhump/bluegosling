package com.bluegosling.collections.immutable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An abstract base class for implementing {@link ImmutableMap}s. Sub-classes only need to provide
 * implementations for {@link #size()} and {@link #iterator()}. However, sub-classes are advised to
 * also provide implementations for {@link #get(Object)} and {@link #containsKey(Object)} as the
 * default implementations run in linear time whereas map's are usually expected to be more
 * efficient than that.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractImmutableMap<K, V> extends AbstractMap<K, V> {

   @Deprecated
   @Override
   public final V put(K key, V value) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final V remove(Object key) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final void putAll(Map<? extends K, ? extends V> m) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final void clear() {
      throw new UnsupportedOperationException();
   }
   
   @Deprecated
   @Override
   public final void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final V putIfAbsent(K key, V value) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean remove(Object key, Object value) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean replace(K key, V oldValue, V newValue) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final V replace(K key, V value) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final V computeIfPresent(K key,
         BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final V merge(K key, V value,
         BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
      throw new UnsupportedOperationException();
   }
}
