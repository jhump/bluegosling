package com.apriori.collections;

import java.util.Map;

/**
 * A <a href="package-info.html#trie">trie</a> whose keys are composite objects. These composites
 * are first broken down into a {@linkplain Iterable sequence} of constituent components.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of keys in the map
 * @param <C> the component type of keys in the map (a key represents a sequence of these components)  
 * @param <V> the type of values in the map
 */
public interface CompositeTrie<K, C, V> extends Map<K, V> {

   /**
    * An interface used to break a key value up into its constituent components.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <K> the type of the key
    * @param <C> the type of the component
    */
   interface Componentizer<K, C> {
      /**
       * Breaks the specified key into a sequence of components.
       * 
       * @param key the key
       * @return the key's sequence of components
       */
      Iterable<C> getComponents(K key);
   }

   /**
    * The object used to break up single key values into a sequence of components. This can be
    * null for {@link CompositeTrie}s that have their own internal mechanism for breaking a key
    * into a sequence.
    * 
    * @return the object used to break up keys into sequences or {@code null} if that behavior is
    *       intrinsic to the trie implementation
    */
   Componentizer<K, C> componentizer();
   
   /**
    * Generates a view of all mappings whose keys have the specified prefix. The specified key
    * is broken up into components, and the resulting sequence is used as a prefix.
    * 
    * @param prefix a key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    * 
    * @see #prefixMap(Iterable)
    */
   CompositeTrie<K, C, V> prefixMap(K prefix);
   
   /**
    * Generates a view of all mappings whose keys have the specified prefix. The specified key
    * is broken up into components, and the resulting sequence is used as a prefix.
    * 
    * @param prefix a key prefix
    * @param numComponents the maximum number of components of the prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
    * 
    * @see #prefixMap(Iterable, int)
    */
   CompositeTrie<K, C, V> prefixMap(K prefix, int numComponents);
   
   /**
    * Generates a view of all mappings whose keys have the specified prefix.
    * 
    * @param prefix a key prefix
    * @return a view of this map that represents the subset of keys with the specified prefix
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
    */
   CompositeTrie<K, C, V> prefixMap(Iterable<C> prefix, int numComponents);
}
