package com.apriori.collections;

import java.util.Map;

/**
 * A <a href="package-info.html#trie">trie</a> whose keys are {@linkplain Iterable sequences} of
 * values.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the component type of keys in the map (a key will be a sequence of these components)  
 * @param <V> the type of values in the map
 */
public interface SequenceTrie<K, V> extends Map<Iterable<K>, V> {
   /**
    * Generates a view of all mappings whose keys have the specified prefix.
    * 
    * @param prefix a key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    */
   SequenceTrie<K, V> prefixMap(Iterable<K> prefix);

   /**
    * Generates a view of all mappings whose keys have the specified prefix. A maximum number of
    * components of the specified prefix are considered. So the effective prefix is the specified
    * sequence but truncated to no more than the specified number of elements.
    * 
    * @param prefix a key prefix
    * @param numComponents the maximum number of components of the prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    */
   SequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents);
}
