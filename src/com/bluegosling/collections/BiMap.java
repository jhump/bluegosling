package com.bluegosling.collections;

import com.bluegosling.tuples.Pair;

import java.util.Map;
import java.util.Set;

/**
 * A map that defines two-way mappings and whose values are unique. The values are effectively a
 * set. Storing a mapping with a value that is already present may implicitly remove the mapping
 * with which it was previously associated.
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of values in the map
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface BiMap<K, V> extends Map<K, V> {
   
   /**
    * An entry in a {@code BiMap}. Since mappings are bi-directional, this allows changing the key,
    * which is effectively just like {@link #setValue(Object)} but for the other direction.
    *
    * @param <K> the type of keys in the map
    * @param <V> the type of values in the map
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface BiEntry<K, V> extends Entry<K, V> {
      /**
       * Changes the key for this entry.
       *
       * @param newKey the new key
       * @return the previous key
       */
      K setKey(K newKey);
   }
   
   /**
    * Returns true if the map contains the given mapping. If the map contains the given key, but
    * mapped to a different value, or it contains the given value, but associated with a different
    * key, then this returns false.
    *
    * @param key a key
    * @param value a value
    * @return true if this map contains the given key and it is mapped to the given value
    */
   boolean contains(K key, V value);
   
   /**
    * Gets the key that is associated with the given value.
    *
    * @param value a value
    * @return the key associated with the given value or {@code null} if value is not present
    */
   K getKey(V value);
   
   /**
    * Removes the mapping with the given value.
    *
    * @param value a value
    * @return the key previously associated with the removed value or {@code null} if value was
    *       not present
    */
   K removeValue(V value);

   /**
    * Returns the entry set. This is effectively the same as 
    * TODO: document me!
    *
    * @return
    */
   Set<BiEntry<K, V>> biEntrySet();
   
   // TODO: doc...
   
   @Override
   Set<V> values();
   
   @Override
   default Set<Entry<K, V>> entrySet() {
      // Since the returned set is effectively read-only, up-casting the element type is safe
      @SuppressWarnings("rawtypes")
      Set rawEntries = biEntrySet();
      @SuppressWarnings("unchecked")
      Set<Entry<K, V>> entries = rawEntries;
      return entries;
   }
   
   @Override
   V put(K key, V value);
   
   K putValue(V value, K key);
   
   @Override
   V putIfAbsent(K key, V value);
   
   K putValueIfAbsent(V value, K key);
   
   @Override
   V replace(K key, V value);
   
   K replaceValue(V value, K key);

   @Override
   boolean replace(K key, V oldValue, V newValue);
   
   boolean replaceValue(V value, K oldKey, K newKey);
   
   boolean putIfBothAbsent(K key, V value);
   
   Pair<K, V> replaceIfBothPresent(K key, V value);

   default BiMap<V, K> invert() {
      return new BiMaps.InvertedMap<>(this);
   }
}
