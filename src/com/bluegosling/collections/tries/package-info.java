/**
 * General trie container types for Java. This package contains a couple of new sub-interfaces of
 * {@link java.util.Map}:
 * <dl>
 * <dt>{@link com.bluegosling.collections.tries.SequenceTrie}</dt>
 *    <dd>A {@link java.util.Map} that organizes keys that represent sequences and provides views of
 *    sub-maps that all share a common prefix. This data structure is typically known as a
 *    <strong><a href="https://en.wikipedia.org/wiki/Trie">trie</a></strong> and
 *    sometimes called a prefix tree or radix tree..</dd>
 * <dt>{@link com.bluegosling.collections.tries.CompositeTrie}</dt>
 *    <dd>Like a {@link com.bluegosling.collections.tries.SequenceTrie} but supports keys that are
 *    composite objects (which must be "componentized" into a sequence of sub-objects for search and
 *    storage). This provides a more convenient API for things like {@link java.lang.String}s, whose
 *    components are a sequence of {@code char}s. The {@link java.util.Map} interface is in terms of
 *    the composite type, instead of being in terms of {@link java.lang.Iterable}s.</dd>
 * </dl>
 * 
 * Tries are similar to tree structures, except that nodes don't generally contain keys and values.
 * Instead, keys are sequences, and the nodes each contain a component of a key. The nodes are
 * traversed based on the sequence of the requested key. For example, the first item in the key's
 * sequence is matched against a child of the root node. Then we traverse down that node's sub-tree
 * and match the second item in the key's sequence against a child of that node, and so on and so
 * forth. Leaf nodes represent full keys and have values; internal nodes often do not. Internal
 * nodes only have values when the prefix they represent is also a key sequence, with its own
 * associated value.
 * 
 * <p>Searching for an item is <em>O(k)</em> where {@code k} is the length of the requested key.
 * Often, {@code k} is greater than {@code log n} (where {@code n} is the number of items in the
 * map), so this would seem to be less efficient than a normal binary search tree. However,
 * searching through a binary search tree requires an <em>O(k)</em> comparison of keys <em>at each
 * node visited</em>. In a trie, comparisons at a given node are <em>O(1)</em> since only a single
 * element must be compared, as opposed to the entire sequence.
 * 
 * <p>Another benefit of a trie is that it makes it trivial to search for all values whose keys have
 * a given prefix. This makes it ideal for suggesting values in text auto-completion.
 * 
 * <p>There are three main types of structures used in tries:
 * <ol>
 *    <li><strong>Uncompressed</strong>: A normal, or uncompressed, trie uses a tree structure as
 *    described above.</li>
 *    <li><strong>Compact</strong>: A compact trie also uses a tree structure, but removes nodes
 *    that have no mapped value and exactly one child edge. With this type of tree, outgoing
 *    edges represent a path that describes all intermediate nodes that were removed/collapsed. This
 *    type of trie is generally better for retrieval but slower for updates as edges must be merged
 *    or split to preserve the compact property.</li>
 *    <li><strong>Array-Mapped</strong>: An array-mapped trie is a special form of compact trie that
 *    looks more like a sparse vector. Its keys must be sequences of bits, which can also be
 *    interpreted as numeric array indices (hence the name). Each node represents 6 bits in the
 *    key/index sequence and stores a long (64-bit) bitmask and a growable array of up to 64
 *    children. To traverse the trie, an index is computed from the next 6 bits in the sequence (an
 *    integer between 0 and 63). The corresponding bit in the bitmask at that index is queried. If
 *    it is set, the trie contains those bits and traversal continues to a child node. If the bit is
 *    unset, the requested key is not in the map. The array of children only has as many elements as
 *    there are bits set in the bitmask. To determine the index of a child in that array, just count
 *    the number of set bits in the bitmask that are less significant than the one of interest.
 *    (Also see {@link com.bluegosling.collections.immutable.AmtPersistentList},
 *    {@link com.bluegosling.collections.maps.HamtMap}, and
 *    {@link com.bluegosling.collections.immutable.HamtPersistentMap}.)</li>
 * </ol>
 */
package com.bluegosling.collections.tries;