package com.bluegosling.collections;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import com.bluegosling.collections.views.TransformingSet;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Maps;

/**
 * Utility methods for working with and creating instanceos of {@link Map}. These methods complement
 * those in Guava's {@link Maps} class.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public final class MoreMaps {
   private MoreMaps() {}

   /**
    * Returns a view of the given collection as an identity map. In the map, the value associated
    * with each key is the key itself. Putting new entries in the map is supported, but only if the
    * given key and value are equal. Otherwise, an {@link IllegalArgumentException} is thrown.
    * 
    * <p>If the given collection contains duplicates, they are ignored to preserve the property that
    * keys in a map are distinct.
    * 
    * <p>Unlike most map implementations, the {@link Map#size()} operation (and operations of the
    * same name for collection views returned by the map) may run in linear time instead of constant
    * time (in order to count distinct keys, in case there are duplicates). If the given collection
    * implements {@link Set}, then size can be determined in constant time. Similarly, queries on
    * the map, like {@link Map#containsKey(Object)} or {@link Map#get(Object)}, will take linear
    * time unless the given collection implements {@link Set} (in which case they run in the same
    * time as {@link Set#contains(Object)}, usually sub-linear). 
    * 
    * @param coll a collection
    * @return a view of the collection as an identity map
    */
   public static <T> Map<T, T> fromCollection(Collection<T> coll) {
      return new MapFromCollection<>(coll);
   }

   /**
    * Combines the given iterables into a map, "zipping" the elements into key-value pairs. So a
    * key is taken from the first iterable, and its associated value in the resulting map is the
    * value taken from the second iterable. When either stream is exhausted, the combined stream is
    * also exhausted. So if one stream has more items than the other, the extra items in the longer
    * stream are ignored.
    *
    * <p>The given sequence of keys must be distinct. If duplicates are encountered, an
    * {@link IllegalArgumentException} is thrown.
    * 
    * @param keys the sequence of keys
    * @param values the sequence of values
    * @return a map comprised of the given keys and values
    */
   public static <K, V> Map<K, V> zip(Iterable<? extends K> keys, Iterable<? extends V> values) {
      return Collections.unmodifiableMap(zipInternal(keys, values, LinkedHashMap::new));
   }

   // TODO: doc...
   
   public static <K, V> Map<K, V> zipExact(Iterable<? extends K> keys,
         Iterable<? extends V> values) {
      return Collections.unmodifiableMap(zipExactInternal(keys, values, LinkedHashMap::new));
   }

   public static <K extends Comparable<K>, V> NavigableMap<K, V> zipSorted(
         Iterable<? extends K> keys, Iterable<? extends V> values) {
      return Collections.unmodifiableNavigableMap(zipInternal(keys, values, TreeMap::new));
   }

   public static <K extends Comparable<K>, V> NavigableMap<K, V> zipSortedExact(
         Iterable<? extends K> keys, Iterable<? extends V> values) {
      return Collections.unmodifiableNavigableMap(zipExactInternal(keys, values, TreeMap::new));
   }

   public static <K, V> NavigableMap<K, V> zipSorted(Iterable<? extends K> keys,
         Iterable<? extends V> values, Comparator<? super K> comparator) {
      return Collections.unmodifiableNavigableMap(
            zipInternal(keys, values, () -> new TreeMap<>(comparator)));
   }

   public static <K, V> NavigableMap<K, V> zipSortedExact(Iterable<? extends K> keys,
         Iterable<? extends V> values, Comparator<? super K> comparator) {
      return Collections.unmodifiableNavigableMap(
            zipExactInternal(keys, values, () -> new TreeMap<>(comparator)));
   }
   
   private static <K, V, M extends Map<K, V>> M zipInternal(Iterable<? extends K> keys,
         Iterable<? extends V> values, Supplier<? extends M> factory) {
      M map = factory.get();
      Iterator<? extends K> kIter = keys.iterator();
      Iterator<? extends V> vIter = values.iterator();
      while (kIter.hasNext() && vIter.hasNext()) {
         checkArgument(map.put(kIter.next(), vIter.next()) == null,
               "Keys are not distinct");
      }
      return map;
   }
   
   private static <K, V, M extends Map<K, V>> M zipExactInternal(Iterable<? extends K> keys,
         Iterable<? extends V> values, Supplier<? extends M> factory) {
      OptionalInt kSz = MoreIterables.trySize(keys);
      if (kSz.isPresent()) {
         OptionalInt vSz = MoreIterables.trySize(values);
         if (vSz.isPresent() && kSz.getAsInt() != vSz.getAsInt()) {
            throw new IllegalArgumentException("must have the same number of values as keys");
         }
      }
      M map = factory.get();
      Iterator<? extends K> kIter = keys.iterator();
      Iterator<? extends V> vIter = values.iterator();
      while (kIter.hasNext() && vIter.hasNext()) {
         checkArgument(map.put(kIter.next(), vIter.next()) == null,
               "Keys are not distinct");
      }
      // check again, in case iterables were concurrently modified since above check
      checkArgument(!kIter.hasNext() && !vIter.hasNext(),
            "must have the same number of values as keys");
      return map;
   }
   
   private static class MapFromCollection<T> implements Map<T, T> {
      private final Collection<T> coll;
      private final Set<T> keySet;
      
      MapFromCollection(Collection<T> coll) {
         this.coll = coll;
         this.keySet = MoreSets.fromCollection(coll);
      }
      
      @Override
      public int size() {
         return keySet.size();
      }

      @Override
      public boolean isEmpty() {
         return coll.isEmpty();
      }

      @Override
      public boolean containsKey(Object key) {
         return coll.contains(key);
      }

      @Override
      public boolean containsValue(Object value) {
         return coll.contains(value);
      }

      @Override
      public T get(Object key) {
         for (Iterator<T> iter = coll.iterator(); iter.hasNext();) {
            T t = iter.next();
            if (Objects.equals(t, key)) {
               return t;
            }
         }
         return null;
      }

      @Override
      public T put(T key, T value) {
         if (!Objects.equals(key, value)) {
            throw new IllegalArgumentException("mapped value must be equal to corresponding key");
         }
         return keySet.add(key) ? null : value;
      }

      @Override
      public T remove(Object key) {
         for (Iterator<T> iter = coll.iterator(); iter.hasNext();) {
            T t = iter.next();
            if (Objects.equals(t, key)) {
               iter.remove();
               return t;
            }
         }
         return null;
      }

      @Override
      public void putAll(Map<? extends T, ? extends T> m) {
         for (Entry<? extends T, ? extends T> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
         }
      }

      @Override
      public void clear() {
         coll.clear();
      }

      @Override
      public Set<T> keySet() {
         // view can be used to remove elements, but not add them
         return new ForwardingSet<T>() {
            @Override
            protected Set<T> delegate() {
               return keySet;
            }
            
            @Override
            public boolean add(T t) {
               throw new UnsupportedOperationException("add");
            }
            
            @Override
            public boolean addAll(Collection<? extends T> c) {
               throw new UnsupportedOperationException("addAll");
            }
         };
      }

      @Override
      public Collection<T> values() {
         return keySet();
      }

      @Override
      public Set<Entry<T, T>> entrySet() {
         return new TransformingSet<>(keySet, t -> new AbstractMap.SimpleImmutableEntry<>(t, t));
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
}
