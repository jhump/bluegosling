package com.apriori.collections;

import java.util.List;
import java.util.Map;

/**
 * A <a href="package-summary.html#trie">trie</a> whose keys are {@linkplain Iterable sequences} of
 * values.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the component type of keys in the map (a key will be a sequence of these components)  
 * @param <V> the type of values in the map
 * 
 * @see CompositeTrie
 */
public interface SequenceTrie<K, V> extends Map<List<K>, V> {
   /**
    * Generates a view of all mappings whose keys start with the specified value. The returned map's
    * keys will just be the tail sequences: the specified initial value, which prefixes all of its
    * keys will be omitted.
    * 
    * @param prefix the key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    */
   SequenceTrie<K, V> prefixMap(K prefix);
   
   /**
    * Generates a view of all mappings whose keys have the specified prefix. The returned map's
    * keys will just be the tail sequences: the specified sequence, which prefixes all of its keys,
    * will be omitted.
    * 
    * <p>This is effectively shorthand for repeated calls to {@link #prefixMap(Object)}, like so:<pre>
    * SequenceTrie&lt;K, V&gt; result = someTrie;
    * for (K value : prefix) {
    *   result = result.prefixMap(value);
    * }
    * </pre>
    * 
    * @param prefix a key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    */
   SequenceTrie<K, V> prefixMap(List<K> prefix);

   /**
    * Generates a view of all mappings whose keys have the specified prefix. A maximum number of
    * components of the specified prefix are considered. So the effective prefix is the specified
    * sequence but truncated to no more than the specified number of elements.
    * 
    * @param prefix a key prefix
    * @param numComponents the maximum number of components of the prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    */
   SequenceTrie<K, V> prefixMap(List<K> prefix, int numComponents);
}
