package com.bluegosling.collections;

import java.util.List;
import java.util.Map;

/**
 * A <a href="package-summary.html#trie">trie</a> whose keys are lists of values.
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
    * A convenience method for storing mappings and using non-list collections as keys.
    * 
    * <p><strong>Note</strong>, the iteration order of the given key determines where the value is
    * stored in the trie. Keys made up of the same components but in different orders are considered
    * distinct, unequal keys. So using collections with non-deterministic ordering, like a
    * {@code HashSet}, is not advised because you could query the trie later using an equal set key,
    * but not get the right value due to different iteration order.
    * 
    * <p>Instead, this method is intended to be used with iterables whose elements represent a
    * finite, ordered sequence, but that may not implement the full {@link List} interface.
    *
    * @param key the key, relaxed to an iterable of components instead of a list
    * @param value the value to associate with the given key
    * @return the previous value associated with the given key or {@code null} if no such mapping
    *       previously existed
    */
   V put(Iterable<K> key, V value);
   
   /**
    * Generates a view of all mappings whose keys start with the specified value. The returned map's
    * keys will just be the tail sequences: the specified initial value, which prefixes all of its
    * keys will be omitted.
    * 
    * <p>Inserting mappings into this prefix map will also insert them into the underlying trie, but
    * with the prefix implied. For example, suppose we have a trie with the following mappings:
    * <pre>
    * { "a", "b", "c", "d" } -&gt; 123,
    * { "a", "b", "x", "y", "z" } -&gt; 456,
    * { "e", "f", "g", "h" } -&gt; 789
    * </pre>
    * The prefix map, with a prefix of {@code "a"}, would look like so:
    * <pre>
    * { "b", "c", "d" } -&gt; 123,
    * { "b", "x", "y", "z" } -&gt; 456
    * </pre>
    * If the mapping <tt>{ "m", "n", "o" } -&gt; 1001</tt> were added to the prefix map, it would
    * look like the following:
    * <pre>
    * { "b", "c", "d" } -&gt; 123,
    * { "b", "x", "y", "z" } -&gt; 456,
    * { "m", "n", "o" } -&gt; 1001
    * </pre>
    * And the main, underlying map would then have the following mappings:
    * <pre>
    * { "a", "b", "c", "d" } -&gt; 123,
    * { "a", "b", "x", "y", "z" } -&gt; 456,
    * { "a", "m", "n", "o" } -&gt; 1001,
    * { "e", "f", "g", "h" } -&gt; 789
    * </pre>
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
    * <p>This is effectively shorthand for repeated calls to {@link #prefixMap(Object)}, like so:
    * <pre>
    * SequenceTrie&lt;K, V&gt; result = someTrie;
    * for (K value : prefix) {
    *   result = result.prefixMap(value);
    * }
    * </pre>
    * 
    * @param prefix a key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    * 
    * @see #prefixMap(Object)
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
    * 
    * @see #prefixMap(Object)
    */
   SequenceTrie<K, V> prefixMap(Iterable<K> prefix, int numComponents);
}
