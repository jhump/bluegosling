package com.apriori.collections;

import java.util.List;
import java.util.NavigableMap;

/**
 * A structure that can be used to perform radix sort and can be used to efficiently represent very
 * sparse arrays. Under the hood, this use a <a href="package-summary.html#trie">trie</a> structure,
 * and each level of the trie represents a nybble (4 bits) of the value with the most-significant
 * bits in the top nodes and least-significant in the leaves.
 * 
 * <p>Since the number of bits in each key is fixed (always 64 bits), insertion and removal
 * operations run in constant time. Inserting keys into this map implicitly sorts the keys,
 * and this results in constant time sort -- effectively a radix sort. Like any sorted map with
 * integer keys, this structure can be used to model sparse arrays or lists. To make this use even
 * simpler, this class provides a {@linkplain #denseValues() list view of the values}. This view
 * has constraints on mutations due to the fact that it is backed by a map, and it treats list
 * indices with no value (i.e. no value mapped to that key) as {@code null}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of values in the map
 */
//TODO: javadoc
//TODO: implement me and remove abstract modifier (don't forget serialization and cloning)
public abstract class RadixTrieMap<V> implements NavigableMap<Long, V> {

   /**
    * Gets the value at the specified key. This is a convenience method that eliminates any
    * auto-boxing/unboxing required by {@link #get(Object) get(Long)}.
    * 
    * @param key the key
    * @return the corresponding value or {@code null} if no such key is mapped 
    */
   public V get(long key) {
      // TODO
      return null;
   }
   
   @Override public V get(Object key) {
      // ClassCastException if key is not a Number is okay
      return get(((Number) key).longValue());
   }

   /**
    * Puts a value at the specified key. This is a convenience method that eliminates any
    * auto-boxing/unboxing required by {@link #put(Long, Object) put(Long, V)}.
    *  
    * @param key the key
    * @param value the value for the specified key
    * @return the previous value maped to the key or {@code null} if there was no previous mapping
    */
   public V put(long key, V value) {
      // TODO
      return null;
   }
   
   @Override public V put(Long key, V value) {
      return put(key.longValue(), value);
   }

   /**
    * Removes a value from the specified key. This is a convenience method that eliminates any
    * auto-boxing/unboxing required by {@link #remove(Object) remove(Long)}.
    * 
    * @param key the key
    * @return the value that was removed or {@code null} if no such key was mapped
    */
   public V remove(long key) {
      // TODO
      return null;
   }
   
   @Override public V remove(Object key) {
      // ClassCastException if key is not a Number is okay
      return remove(((Number) key).longValue());
   }

   /**
    * Returns a view of this map as a dense list. Values in the list will only be non-null at
    * indices that correspond to keys in this map. The size of the list is equal to the largest
    * mapped key plus one. Since the list is just a view, it is memory-efficient for very sparse
    * lists that have items at very large indices.
    * 
    * <p>The returned map does not support any add or remove operations. But does allow setting
    * elements (which results in insertions into the underlying map). The iterator also allows
    * setting values but disallows removing or adding elements.
    * 
    * @return a view of this map as a dense list of values
    * @throws IllegalStateException if the map contains any keys that are less than zero or are
    *    greater than or equal to {@link Integer#MAX_VALUE}
    */
   public List<V> denseValues() {
      // TODO
      return null;
   }

   /**
    * Returns a view of this map as a dense list. Values in the list will only be non-null at
    * indices that correspond to keys in this map. Since the list is just a view, it is
    * memory-efficient for very sparse lists that have items at very large indices.
    * 
    * <p>Any keys in this map that are less than zero or are greater than or equal to the specified
    * size will be absent from the returned list.
    * 
    * <p>The returned map has the same restrictions regarding adding and removing elements as the
    * one returned from {@link #denseValues()}.
    *
    * @param size the size of the returned list
    * @return a view of this map as a dense list of values
    * @throws IllegalStateException if the map contains any keys less than zero
    */
   public List<V> denseValues(int size) {
      // TODO
      return null;
   }
}
