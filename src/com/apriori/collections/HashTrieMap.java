package com.apriori.collections;

import java.util.Map;

/**
 * An implementation of {@link Map} that uses a hash array-mapped trie (HAMT). Under the hood, this
 * structure is the same as a {@link ArrayMappedBitwiseTrie} except that the key bits come from a
 * key's hash value. Like a {@link java.util.HashMap HashMap}, a linked list is used to store values
 * whose keys' hash values collide. But collisions are far less likely than in a
 * {@link java.util.HashMap HashMap} because the full 32 bits of hash value are used (vs. hash value
 * modulo array size, which is typically far lower cardinality than the full 32 bits). Since this
 * structure doesn't use a fixed size array for the mappings (unlike a
 * {@link java.util.HashMap HashMap}), it never pays the cost of getting full and requiring an
 * internal table resize. However, arrays are used at individual nodes, and these will need to be
 * resized during insertion and removal operations. But these arrays are very small (never larger
 * than 64 elements) and an insertion or removal operation will never need to resize more than one
 * of them.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <K> the type of keys in the map
 * @param <V> the type of value in the map
 */
//TODO: javadoc
//TODO: implement me and remove abstract modifier (don't forget serialization and cloning)
public abstract class HashTrieMap<K, V> implements Map<K, V> {

}
