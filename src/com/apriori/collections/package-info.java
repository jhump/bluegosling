/** 
 * New implementations and utilities to complement the Java Collections Framework.
 * 
 * <p>These new collections do <em>not</em> extend the abstract base classes provided
 * in {@code java.util}, primarily as an experiment in implementing the collection
 * interfaces from scratch.
 * 
 * <p>In addition to new implementations for standard collection interfaces, this package also
 * contains some new collection interfaces:
 * <ul>
 * <li>{@link com.apriori.collections.AssociativeList}: A {@link java.util.List List} that supports sparse associative keys
 * and can be viewed as a {@link java.util.Map Map}.</li>
 * <li>{@link com.apriori.collections.RandomAccessSet}: A {@link java.util.Set Set} that supports random access of elements
 * and can be viewed as a {@link java.util.List List}.</li>
 * <li>{@link com.apriori.collections.RandomAccessNavigableMap}: A {@link java.util.NavigableMap} that supports random access of elements
 * via key- and entry-sets that are {@link com.apriori.collections.RandomAccessSet}s and a view of values as a
 * {@link java.util.List List}.</li>
 * <li>{@link com.apriori.collections.BitSequence}: An immutable sequence of bits. This is similar to
 * {@link java.util.BitSet} except that it is immutable and provides additional methods for simpler
 * querying.
 * <li>{@link com.apriori.collections.SequenceTrie}: A {@link java.util.Map Map} that organizes keys that represent
 * sequences and provides views of sub-maps that all share a common prefix. This data structure is
 * typically known as a prefix tree or <a href="#trie">trie</a>.</li>
 * <li>{@link com.apriori.collections.CompositeTrie}: Like a {@link com.apriori.collections.SequenceTrie} but supports keys that are composite
 * objects (which must be "componentized" into a sequence of sub-objects for search and storage).
 * This provides a more convenient API for things like {@link java.lang.String}s, whose components are a
 * sequence of {@code char}s. The {@link java.util.Map} interface is in terms of the composite
 * type, instead of being in terms of {@link java.util.Iterable}s of the component
 * type.</li>
 * </ul>
 * 
 * <a name="trie"/><h3>Tries</h3>
 * Tries are similar to tree structures, except that nodes don't generally contain keys and values.
 * Instead, keys are sequences, and the nodes each contain a component of a key. The nodes are
 * traversed based on the sequence of the requested key. For example, the first item in the key's
 * sequence is matched against a child of the root node. Then we traverse down that node's sub-tree
 * and match the second item in the key's sequence against a child of that node, and so on and so
 * forth. Leaf nodes represent full keys and have values; internal nodes often do not. Internal
 * nodes only have values when the prefix they represent is also a key sequence, with its own
 * associated value.
 * 
 * <p>Searching for an item is O(k) where k is the length of the requested key. Often, k is greater
 * than log n (where n s the number of items in the map), so this would seem to be less efficient
 * than a normal binary search tree. However, searching through a binary search tree requires an
 * O(k) comparison of keys <em>at each node visited</em>. In a trie, comparisons at a given node
 * are O(1) since only a single element must be compared vs. the entire sequence.
 * 
 * <p>Another benefit of a trie is that it makes it trivial to search for all values whose keys have
 * a given prefix. This makes it ideal for suggesting values in text auto-completion.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: kill most of *Utils and just extend java.util.Abstract*. It was a fun experiment, but it's
// probably time to move this code into the mainstream
// TODO: fix nested sublist/subset/submap (most impls here will mistakenly throw
// ConcurrentModificationException from a sub-* view if the collection is modified based from a
// sub-view of that sub-view)
package com.apriori.collections;