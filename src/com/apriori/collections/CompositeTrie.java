package com.apriori.collections;

import java.util.Map;

/**
 * A <a href="package-summary.html#trie">trie</a> whose keys are composite objects. These composites
 * are first broken down into a {@linkplain Iterable sequence} of constituent components using a
 * {@link Componentizer}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of keys in the map
 * @param <C> the component type of keys in the map (each key represents a sequence of components)  
 * @param <V> the type of values in the map
 * 
 * @see SequenceTrie
 */
public interface CompositeTrie<K, C, V> extends Map<K, V> {

   /**
    * The object used to break up single key values into a sequence of components.
    * 
    * @return the object used to break up keys into sequences
    */
   Componentizer<? super K, ? extends C> componentizer();
   
   /**
    * Generates a view of all mappings whose keys have the specified prefix. The returned map is a
    * constrained sub-map. Attempts to add mappings to the returned map with keys that do not begin
    * with the prefix will generate {@link IllegalArgumentException}s.
    * 
    * <p>For example, suppose we have a trie that uses strings as keys. The strings are broken into
    * character components. The trie has the following mappings:
    * <pre>
    * { "abcd" } -&gt; 123,
    * { "abxyz" } -&gt; 456,
    * { "efgh" } -&gt; 789
    * </pre>
    * The prefix map, with a prefix of {@code 'a'}, would look like so:
    * <pre>
    * { "abcd" } -&gt; 123,
    * { "abxyz" } -&gt; 456,
    * </pre>
    * If the mapping <tt>{ "mno" } -&gt; 1001</tt> were added to the prefix map, an
    * {@link IllegalArgumentException} would be thrown since it does not begin with the correct
    * prefix. But adding <tt>{ "amno" } -&gt; 1001</tt> would succeed, and the resulting prefix map
    * would then look like the following:
    * <pre>
    * { "abcd" } -&gt; 123,
    * { "abxyz" } -&gt; 456,
    * { "amno" } -&gt; 1001
    * </pre>
    * The new mapping would also be present in the main, underlying map:
    * <pre>
    * { "abcd" } -&gt; 123,
    * { "abxyz" } -&gt; 456,
    * { "amno" } -&gt; 1001,
    * { "efgh" } -&gt; 789
    * </pre>
    * 
    * @param prefix a key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    */
   CompositeTrie<K, C, V> prefixMap(C prefix);
   
   /**
    * Generates a view of all mappings whose keys have the specified prefix.
    * 
    * <p>This is effectively shorthand for repeated calls to {@link #prefixMap(Object)}, like so:
    * <pre>
    * CompositeTrie&lt;K, C, V&gt; result = someTrie;
    * for (C component : prefix) {
    *   result = result.prefixMap(component);
    * }
    * </pre>
    * 
    * @param prefix a key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    * 
    * @see #prefixMap(Object)
    */
   CompositeTrie<K, C, V> prefixMap(Iterable<C> prefix);
   
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
   CompositeTrie<K, C, V> prefixMap(Iterable<C> prefix, int numComponents);
   
   /**
    * Generates a view of all mappings whose keys have the specified prefix. The specified key
    * is broken up into components, and the resulting sequence is used as a prefix.
    * 
    * <p>This is equivalent to the following:
    * <pre>
    * trie.prefixMap(componentizer().getComponents(prefix));
    * </pre>
    * 
    * @param prefix a key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    * 
    * @see #prefixMap(Iterable)
    */
   CompositeTrie<K, C, V> prefixMapByKey(K prefix);
   
   /**
    * Generates a view of all mappings whose keys have the specified prefix. The specified key
    * is broken up into components, and up to the specified number of elements of the resulting
    * sequence are used as a prefix.
    * 
    * <p>This is equivalent to the following:
    * <pre>
    * trie.prefixMap(componentizer().getComponents(prefix), numComponents);
    * </pre>
    * 
    * @param prefix a key prefix
    * @param numComponents the maximum number of components of the prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    * 
    * @see #prefixMap(Iterable, int)
    */
   CompositeTrie<K, C, V> prefixMapByKey(K prefix, int numComponents);
}
