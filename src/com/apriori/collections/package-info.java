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
 * <li>{@link com.apriori.collections.AssociativeList}: A {@link java.util.List List} that supports
 * sparse associative keys and can be viewed as a {@link java.util.Map Map}.</li>
 * <li>{@link com.apriori.collections.RandomAccessSet}: A {@link java.util.Set Set} that supports
 * random access of elements and can be viewed as a {@link java.util.List List}.</li>
 * <li>{@link com.apriori.collections.RandomAccessNavigableMap}: A {@link java.util.NavigableMap}
 * that supports random access of elements via key- and entry-sets that are
 * {@link com.apriori.collections.RandomAccessSet}s and a view of values as a
 * {@link java.util.List List}.</li>
 * <li>{@link com.apriori.collections.BitSequence}: An immutable sequence of bits. This is similar
 * to {@link java.util.BitSet} except that it is immutable and provides additional methods for
 * simpler querying.
 * <li>{@link com.apriori.collections.SequenceTrie}: A {@link java.util.Map Map} that organizes keys
 * that represent sequences and provides views of sub-maps that all share a common prefix. This data
 * structure is typically known as a prefix tree or <a href="#trie">trie</a>.</li>
 * <li>{@link com.apriori.collections.CompositeTrie}: Like a
 * {@link com.apriori.collections.SequenceTrie} but supports keys that are composite objects (which
 * must be "componentized" into a sequence of sub-objects for search and storage). This provides a
 * more convenient API for things like {@link java.lang.String}s, whose components are a sequence of
 * {@code char}s. The {@link java.util.Map} interface is in terms of the composite type, instead of
 * being in terms of {@link java.util.Iterable}s.</li>
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
 * <p>There are three main types of structures used in tries:
 * <ol>
 *    <li><strong>Uncompressed</strong>: A normal, or uncompressed, trie uses a tree structure as
 *    described above.</li>
 *    <li><strong>Compact</strong>: A compact trie also uses a tree structure, but removes nodes
 *    that have only one child by combining them with that child. With this type of tree, outgoing
 *    edges from one node have a path that describes all intermediate nodes that were
 *    removed/collapsed. This type of trie is generally better for retrieval but slower for updates
 *    as edges must be merged or split to preserve the compact property.</li>
 *    <li><strong>Array-Mapped</strong>: An array-mapped trie is a special form of compact trie that
 *    requires its keys be sequences of bits. Each node represents 6 bits in the sequence and stores
 *    a long (64-bit) bitmask and a growable array of up to 64 children. To traverse the trie, an
 *    index is computed from the next 6 bits in the sequence (an integer between 0 and 63). The
 *    corresponding bit in the bitmask at that index is queried. If it is set, the trie contains
 *    those bits and traversal continues to a child node. If the bit is unset, the requested key is
 *    not in the map. The array of children only has as many elements as there are bits set in the
 *    bitmask. To determine the index of a child in that array, just count the number of set bits in
 *    the bitmask that are less significant than the one of interest. (Also see
 *    {@link com.apriori.collections.HamtMap}.)</li>
 * </ol>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: kill most of *Utils and just extend java.util.Abstract*. It was a fun experiment, but it's
// probably time to move this code into the mainstream
// TODO: fix nested sublist/subset/submap (most impls here will mistakenly throw
// ConcurrentModificationException from a sub-* view if the collection is modified from a sub-view
// of that sub-view). Probably should add tests for this, too...
package com.apriori.collections;