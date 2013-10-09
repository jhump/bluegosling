package com.apriori.collections;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of {@link Map} that uses a hash array-mapped trie (HAMT). Under the hood, this
 * structure is the same as an {@link ArrayMappedBitwiseTrie} except that the key bits come from a
 * key's {@linkplain Object#hashCode() hash value}. Like a {@link java.util.HashMap HashMap}, a
 * linked list is used to store values whose keys' hash values collide. But collisions are far less
 * likely than in a {@link java.util.HashMap HashMap} because the full 32 bits of hash value are
 * used (vs. hash value modulo array size, which is typically far lower cardinality than the full
 * 32 bits).
 * 
 * <p>Since this structure doesn't use a fixed size array for the mappings (unlike a
 * {@link java.util.HashMap HashMap}), it never pays the cost of getting full, performing an
 * internal table resize, and re-hashing all of its contents.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of value in the map
 */
//TODO: javadoc
//TODO: tests
public class HamtMap<K, V> implements Map<K, V> {

   @Override
   public int size() {
      // TODO: implement me
      return 0;
   }

   @Override
   public boolean isEmpty() {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean containsKey(Object key) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean containsValue(Object value) {
      // TODO: implement me
      return false;
   }

   @Override
   public V get(Object key) {
      // TODO: implement me
      return null;
   }

   @Override
   public V put(K key, V value) {
      // TODO: implement me
      return null;
   }

   @Override
   public V remove(Object key) {
      // TODO: implement me
      return null;
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> m) {
      // TODO: implement me
      
   }

   @Override
   public void clear() {
      // TODO: implement me
      
   }

   @Override
   public Set<K> keySet() {
      // TODO: implement me
      return null;
   }

   @Override
   public Collection<V> values() {
      // TODO: implement me
      return null;
   }

   @Override
   public Set<java.util.Map.Entry<K, V>> entrySet() {
      // TODO: implement me
      return null;
   }

}
