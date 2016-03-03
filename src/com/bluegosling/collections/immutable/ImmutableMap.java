package com.bluegosling.collections.immutable;

import com.bluegosling.collections.SizedIterable;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * An immutable, read-only set of key+value mappings. This interface is similar to the standard
 * {@link Map} interface except that it defines no mutation operations.
 * 
 * <p>The {@link java.util.Iterator#remove() remove()} operation of an immutable map's
 * {@link #iterator()} always throws {@link UnsupportedOperationException}. Similarly the
 * {@link #iterator()} for any of the collection views of the map will also always throw
 * {@link UnsupportedOperationException}.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public interface ImmutableMap<K, V> extends SizedIterable<ImmutableMap.Entry<K, V>> {
   
   /** 
    * A key-value-pair entry in an immutable map.
    *
    * @param <K> the type of the key
    * @param <V> the type of the value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Entry<K, V> {
      /**
       * Retrieves the key associated with this entry.
       *
       * @return the key for this entry
       */
      K key();

      /**
       * Retrieves the value associated with this entry.
       *
       * @return the value for this entry
       */
      V value();
      
      /**
       * Determines if the given object is equal to this entry. This returns true if the given
       * object is also an {@link ImmutableMap.Entry} and has an equal key and equal value. Two
       * objects are considered equal if the following expression is true:<br>
       * {@code o1 == null ? o2 == null : o1.equals(o2)}.
       *
       * @param o the object to test
       * @return true if the given object is equal to this entry
       */
      @Override boolean equals(Object o);
      
      /**
       * Computes the hash code for this entry. The hash code should be the result of XOR'ing the
       * hash code of the key with the hash code of the value. If the key or value is null, its hash
       * code is considered zero for the purpose of this computation.
       *
       * @return the hash code for this entry
       */
      @Override int hashCode();
   }
   
   /**
    * Determines if this map contains a mapping for the given key.
    *
    * @param key a key
    * @return true if this map contains a mapping for the given key
    */
   boolean containsKey(Object key);

   /**
    * Determines if this map contains mappings for all of the given keys.
    *
    * @param keys zero or more keys
    * @return true if this map contains a mapping for every one of the given keys
    */
   boolean containsAllKeys(Iterable<?> keys);

   /**
    * Determines if this map contains a mapping for any of the given keys.
    *
    * @param keys zero or more keys
    * @return true if this map contains a mapping for at least one of the given keys
    */
   boolean containsAnyKey(Iterable<?> keys);

   /**
    * Determines if this map contains the given value in any of its mappings.
    *
    * @param value a value
    * @return true if the given value is present in any of the contained mappings
    */
   boolean containsValue(Object value);

   /**
    * Determines if this map contains all of the given values in its mappings.
    *
    * @param values zero or more values
    * @return true if every one of the given values are present in the contained mappings
    */
   boolean containsAllValues(Iterable<?> values);

   /**
    * Determines if this map contains any of the given values in its mappings.
    *
    * @param values zero or more values
    * @return true if at least one of the given values is present in the contained mappings
    */
   boolean containsAnyValue(Iterable<?> values);

   /**
    * Retrieves the value associated with the given key.
    *
    * @param key a key
    * @return the value associated with the given key or {@code null} if no mapping exists for the
    *       given key
    */
   V get(Object key);
   
   /**
    * Retrieves the value associated with the given key or a default value in the given key is not
    * present in the map.
    *
    * @param key a key
    * @param defaultValue a default value to return if the given key is not present
    * @return the value associated with the given key or the given default if no mapping exists for
    *       the given key
    */
   default V getOrDefault(Object key, V defaultValue) {
      V v = get(key);
      if (v != null) {
         return v;
      }
      return containsKey(key) ? v : defaultValue;
   }
   
   /**
    * Returns a view of this map's keys as an immutable set.
    *
    * @return a view of this map's keys as an immutable set
    */
   ImmutableSet<K> keySet();

   /**
    * Returns a view of this map's values as an immutable collection.
    *
    * @return a view of this map's values as an immutable collection
    */
   ImmutableCollection<V> values();
   
   /**
    * Returns a view of this map's mappings as an immutable set of entries.
    *
    * @return a view of this map's mappings as an immutable set of entries
    */
   ImmutableSet<Entry<K, V>> entrySet();
   
   /**
    * Determines if the given object is equal to this map. The given object is equal if it is also
    * an {@link ImmutableMap} and contains exactly the same mappings (same keys and same values) as
    * this map. This is similar to {@link Map#equals(Object)}.
    *
    * @param o an object
    * @return true if the given object is equal to this map
    */
   @Override boolean equals(Object o);
   
   /**
    * Computes a hash code for this map. The hash code for an immutable map is the same as the hash
    * code of its {@link #entrySet()} (using the definition for {@link ImmutableSet#hashCode()} and
    * {@link Entry#hashCode()}).
    *
    * @return a hash code for this map
    */
   @Override int hashCode();

   default Stream<Entry<K, V>> stream() {
      return entrySet().stream();
   }

   default Stream<Entry<K, V>> parallelStream() {
      return entrySet().parallelStream();
   }
   
   @Override
   default Iterator<Entry<K, V>> iterator() {
      return entrySet().iterator();
   }
   
   @Override
   default Spliterator<Entry<K, V>> spliterator() {
      return entrySet().spliterator();
   }
   
   default void forEach(BiConsumer<? super K, ? super V> action) {
      for (Entry<K, V> entry : this) {
         action.accept(entry.key(), entry.value());
      }
   }
}
