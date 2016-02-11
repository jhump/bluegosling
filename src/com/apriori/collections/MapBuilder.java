package com.apriori.collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

/**
 * An interface for imperatively constructing a map using a builder pattern and method chaining.
 * 
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * @param <M> the type of map built
 * @param <B> the type of this builder
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface MapBuilder<K, V, M extends Map<K, V>, B extends MapBuilder<K, V, M, B>> {
   /**
    * Adds an item to the map being built.
    *
    * @param key the key of the new entry
    * @param value the value of the new entry
    * @return this builder (for method chaining)
    * @throws IllegalStateException if a map has already been {@linkplain #build() built} but the
    *       builder has not yet been {@linkplain #reset() reset}
    */
   B put(K key, V value);

   /**
    * Adds all items from another map into the map being built.
    *
    * @param otherMap the entries to add to the map
    * @return this builder (for method chaining)
    * @throws IllegalStateException if a map has already been {@linkplain #build() built} but the
    *       builder has not yet been {@linkplain #reset() reset}
    */
   B putAll(Map<? extends K, ? extends V> otherMap);
   
   /**
    * Builds the map. This builder is not usable again after the map is built until {@link #reset()}
    * is invoked.
    *
    * @return the map
    */
   M build();
   
   /**
    * Builds the map, but keeps this builder usable for adding other elements in addition to the
    * already added elements.
    * 
    * <p>Implementations will need to copy the elements added so far into a new map. The extra copy
    * can usually be avoided, as long as the builder is no longer needed, by instead using
    * {@link #build()}.
    * 
    * <p>The default implementation looks like so:<pre>
    * M builtMap = build();
    * reset();          // make builder usable again
    * putAll(builtMap); // seed it with elements already added
    * return builtMap;
    * </pre>
    *
    * @return the map
    */
   default M buildAndContinue() {
      M ret = build();
      reset(); // make builder usable again
      putAll(ret);
      return ret;
   }

   /**
    * Resets the map so it can be re-used to construct another map. Items previously added to the
    * map builder are cleared. This method must be called after {@link #build()} to make more than
    * one map from the same builder.
    */
   void reset();

   /**
    * Returns a map builder that constructs unmodifiable maps. Iteration order of the returned map
    * is deterministic and is the same order as elements are added to the builder.
    *
    * @return a map builder that builds unmodifiable maps
    */
   static <K, V> MapBuilder<K, V, ?, ?> forUnmodifiableMap() {
      return forUnmodifiableMap(MapBuilder.<K, V>forLinkedHashMap());
   }
   
   /**
    * Returns a map builder that constructs unmodifiable maps. The underlying map is built using
    * the given builder and then wrapped via {@link Collections#unmodifiableMap(Map)}.
    *
    * @return a map builder that builds unmodifiable maps
    */
   static <K, V> MapBuilder<K, V, ?, ?> forUnmodifiableMap(MapBuilder<K, V, ?, ?> base) {
      class UnmodifiableMapBuilder implements MapBuilder<K, V, Map<K, V>, UnmodifiableMapBuilder> {
         @Override
         public UnmodifiableMapBuilder put(K key, V value) {
            base.put(key, value);
            return this;
         }

         @Override
         public UnmodifiableMapBuilder putAll(Map<? extends K, ? extends V> otherMap) {
            base.putAll(otherMap);
            return this;
         }

         @Override
         public Map<K, V> build() {
            return Collections.unmodifiableMap(base.build());
         }

         @Override
         public void reset() {
            base.reset();
         }
      }
      return new UnmodifiableMapBuilder();
   }

   /**
    * Returns a map builder that constructs {@link HashMap} instances.
    *
    * @return a map builder that constructs {@link HashMap} instances
    */
   static <K, V> MapBuilder<K, V, HashMap<K, V>, ?> forHashMap() {
      return MapBuilder.<K, V, HashMap<K, V>>withMapConstructor(HashMap::new);
   }

   /**
    * Returns a map builder that constructs {@link LinkedHashMap} instances. Iteration order of the
    * built map is the same as the order that elements are added to the builder.
    *
    * @return a map builder that constructs {@link LinkedHashMap} instances
    */
   static <K, V> MapBuilder<K, V, LinkedHashMap<K, V>, ?> forLinkedHashMap() {
      return MapBuilder.<K, V, LinkedHashMap<K, V>>withMapConstructor(LinkedHashMap::new);
   }

   /**
    * Returns a map builder that constructs {@link TreeMap} instances.
    *
    * @return a map builder that constructs {@link TreeMap} instances
    */
   static <K extends Comparable<K>, V> MapBuilder<K, V, TreeMap<K, V>, ?> forTreeMap() {
      return MapBuilder.<K, V, TreeMap<K, V>>withMapConstructor(TreeMap::new);
   }

   /**
    * Returns a map builder that constructs {@link TreeMap} instances that orders elements per the
    * given comparator.
    *
    * @return a map builder that constructs {@link TreeMap} instances
    */
   static <K, V> MapBuilder<K, V, TreeMap<K, V>, ?> forTreeMap(Comparator<? super K> comparator) {
      return MapBuilder.<K, V, TreeMap<K, V>>withMapConstructor(() -> new TreeMap<>(comparator));
   }

   /**
    * Returns a map builder that constructs {@link HamtMap} instances.
    *
    * @return a map builder that constructs {@link HamtMap} instances
    */
   static <K, V> MapBuilder<K, V, HamtMap<K, V>, ?> forHamtMap() {
      return MapBuilder.<K, V, HamtMap<K, V>>withMapConstructor(HamtMap::new);
   }

   /**
    * Returns a map builder that constructs {@link LinearHashingMap} instances.
    *
    * @return a map builder that constructs {@link LinearHashingMap} instances
    */
   static <K, V> MapBuilder<K, V, LinearHashingMap<K, V>, ?> forLinearHashingMap() {
      return MapBuilder.<K, V, LinearHashingMap<K, V>>withMapConstructor(LinearHashingMap::new);
   }

   /**
    * Returns a map builder that constructs {@link ConcurrentHashMap} instances.
    *
    * @return a map builder that constructs {@link ConcurrentHashMap} instances
    */
   static <K, V> MapBuilder<K, V, ConcurrentHashMap<K, V>, ?> forConcurrentHashMap() {
      return MapBuilder.<K, V, ConcurrentHashMap<K, V>>withMapConstructor(ConcurrentHashMap::new);
   }

   /**
    * Returns a map builder that constructs {@link ConcurrentSkipListMap} instances.
    *
    * @return a map builder that constructs {@link ConcurrentSkipListMap} instances
    */
   static <K extends Comparable<K>, V> MapBuilder<K, V, ConcurrentSkipListMap<K, V>, ?>
   forConcurrentSkipListMap() {
      return MapBuilder.<K, V, ConcurrentSkipListMap<K, V>>withMapConstructor(
            ConcurrentSkipListMap::new);
   }

   /**
    * Returns a map builder that constructs {@link ConcurrentSkipListMap} instances that orders
    * elements per the given comparator.
    *
    * @return a map builder that constructs {@link ConcurrentSkipListMap} instances
    */
   static <K, V> MapBuilder<K, V, ConcurrentSkipListMap<K, V>, ?> forConcurrentSkipListMap(
         Comparator<? super K> comparator) {
      return MapBuilder.<K, V, ConcurrentSkipListMap<K, V>>withMapConstructor(
            () -> new ConcurrentSkipListMap<>(comparator));
   }

   /**
    * Returns a map builder that constructs maps using the given map constructor. The returned map
    * builder will throw {@link IllegalStateException} during a reset if the given supplier returns
    * a non-empty map.
    *
    * @param mapConstructor a supplier of a new, empty map
    * @return a map builder that constructs maps using the given supplier
    * @throws IllegalStateException if the given supplier returns a non-empty map the first time it
    *       is called (when initializing the returned builder)
    */
   static <K, V, M extends Map<K, V>> MapBuilder<K, V, M, ?> withMapConstructor(
         Supplier<M> mapConstructor) {
      class MapBuilderImpl implements MapBuilder<K, V, M, MapBuilderImpl> {
         private boolean valid;
         private M map;
         
         {
            reset();
         }
         
         @Override
         public MapBuilderImpl put(K key, V value) {
            if (!valid) {
               throw new IllegalStateException(
                     "Map already built. Use reset() to re-use this builder");
            }
            map.put(key, value);
            return this;
         }

         @Override
         public MapBuilderImpl putAll(Map<? extends K, ? extends V> otherMap) {
            if (!valid) {
               throw new IllegalStateException(
                     "Map already built. Use reset() to re-use this builder");
            }
            map.putAll(otherMap);
            return this;
         }

         @Override
         public M build() {
            valid = false;
            return map;
         }
         
         @Override
         public void reset() {
            M m = mapConstructor.get();
            if (!m.isEmpty()) {
               throw new IllegalStateException("Map constructor returned non-empty map");
            }
            map = m;
            valid = true;
         }
         
      }
      return new MapBuilderImpl();
   }
}
