/** 
 * New implementations and utilities to complement the Java Collections Framework (JCF).
 * 
 * <p>In addition to new implementations for standard collection interfaces, this package also
 * contains some new collection interfaces:
 * <dl>
 * <dt>{@link com.bluegosling.collections.AssociativeList}</dt>
 *    <dd>A {@link java.util.List} that supports sparse associative keys and can be viewed as a
 *    {@link java.util.Map}.</dd>
 * <dt>{@link com.bluegosling.collections.RandomAccessSet}</dt>
 *    <dd>A {@link java.util.Set} that supports random access of elements and can be viewed as a
 *    {@link java.util.List}.</dd>
 * <dt>{@link com.bluegosling.collections.RandomAccessNavigableMap}</dt>
 *    <dd>A {@link java.util.NavigableMap} that supports random access of elements via key- and
 *    entry-sets that are {@link com.bluegosling.collections.RandomAccessSet}s and a view of values as a
 *    {@link java.util.List}.</dd>
 * <dt>{@link com.bluegosling.collections.BitSequence}</dt>
 *    <dd>An immutable sequence of bits. This is similar to {@link java.util.BitSet} except that it
 *    is immutable and provides additional methods for simpler querying.</dd>
 * <dt>{@link com.bluegosling.collections.GrowableArray}</dt>
 *    <dd>An abstraction that represents a growable array. This is very similar to the standard
 *    {@link java.util.List} interface, but has a narrower API more like arrays. Implementations can
 *    be used to back lists, queues, hash tables, and other structures that use arrays as their
 *    backing stores.</dd>
 * <dt>{@link com.bluegosling.collections.SequenceTrie}</dt>
 *    <dd>A {@link java.util.Map} that organizes keys that represent sequences and provides views of
 *    sub-maps that all share a common prefix. This data structure is typically known as a prefix
 *    tree or <a href="#trie">trie</a>.</dd>
 * <dt>{@link com.bluegosling.collections.CompositeTrie}</dt>
 *    <dd>Like a {@link com.bluegosling.collections.SequenceTrie} but supports keys that are composite
 *    objects (which must be "componentized" into a sequence of sub-objects for search and storage).
 *    This provides a more convenient API for things like {@link java.lang.String}s, whose
 *    components are a sequence of {@code char}s. The {@link java.util.Map} interface is in terms of
 *    the composite type, instead of being in terms of {@link java.lang.Iterable}s.</dd>
 * <dt>{@link com.bluegosling.collections.ImmutableCollection}</dt>
 *    <dd>This interface is the root of a hierarchy of <a href="#immutable-persistent">immutable
 *    collections</a>, parallel to the normal mutable interfaces in the JCF.</dd>
 * <dt>{@link com.bluegosling.collections.PersistentCollection}</dt>
 *    <dd>The root interface of a hierarchy of fully <a href="#immutable-persistent">persistent
 *    collections</a>. They also happen to extend their immutable counter-parts since a persistent
 *    data structure is also immutable.</dd>
 * <dt>{@link com.bluegosling.collections.Cycle}</dt>
 *    <dd>A collection that represents a finite cycle.</dd>
 * <dt>{@link com.bluegosling.collections.PriorityQueue}</dt>
 *    <dd>A priority queue. This interface is a little different than the JRE's class of the same
 *    name in that it exposes additional operations of a classical priority-queue ADT, mainly
 *    {@code reduce-key}. This decouples an element's priority from its intrinsic value, making it
 *    more useful for certain types of graph algorithms.</dd>
 * <dt>{@link com.bluegosling.collections.SizedIterable}</dt>
 *    <dd>A sized iterable is just an iterable that also has {@code size()} and {@code isEmpty}
 *    methods. Several collection-like interfaces in this package, do not actually extend the
 *    {@link java.util.Collection} interface, but they do provide size information in addition to
 *    their {@code iterator}.
 * </dl>
 * 
 * <h3>Concurrent Collections</h3>
 * This package contains additional implementations of concurrent data structures.
 * 
 * <p>One such class is a factory for {@linkplain com.bluegosling.collections.ShardedConcurrentSets
 * concurrent sets}. It operates using normal not-thread-safe set implementations and makes them
 * thread-safe using sharding, for parallelism, and read-write locks, for thread-safety.
 * 
 * <p>Also present is a {@link com.bluegosling.collections.ConcurrentList} interface, along with a
 * single implementation that is backed by a persistent list:
 * {@link com.bluegosling.collections.PersistentListBackedConcurrentList}.
 * 
 * <p>Another concurrent structure is a map that is backed by a persistent map:
 * {@link com.bluegosling.collections.PersistentMapBackedConcurrentMap}.
 * 
 * <h3><a name="immutable-persistent"></a>Immutable and Persistent Collections</h3>
 * This package contains a family of interfaces to represent immutable collections, and their close
 * cousins, persistent collections. Also present are interfaces for immutable and persistent maps.
 * 
 * <p>The utility class {@link com.bluegosling.collections.Immutables} provides numerous methods for
 * creating immutable collections or adapting them to standard collection and map interfaces. Also
 * present are abstract base classes for implementing new immutable collection implementations.
 * 
 * <h3>Transforming and Filtering Collections</h3>
 * This package contains transforming and filtering implementations of the various collection
 * interfaces. Each of these wraps some other collection (or iterable or iterator) and uses a
 * function or a predicate, applied to each element as needed. Updates to the underlying collection
 * are visible (depending on the results of applying the function or predicate) through this
 * wrapper and vice versa.
 * 
 * <p>The filtering implementations provide O(n) performance for several methods that might usually
 * run in constant time for other collection implementations, for example {@code size()}. This is
 * because, to provide an up-to-date view of the backing collection, the filter must be applied to
 * each element when these methods are used.
 * 
 * <p>The transforming implementations have a few variants worth noting:
 * <ol>
 * <li>Normal: The standard transformed views accept a single function. Since the inverse of the
 * function is not available, new elements cannot be added (since the actual element added to the
 * underlying collection would need to be "un-transformed" into the source type). But other mutation
 * operations will work. Some operation implementations, for example removing an element from a
 * transformed set, will have O(n) performance, even when the underlying implementation may provide
 * sub-linear performance.
 * <li>{@code ReadOnly}: A read-only transformed view will throw
 * {@link java.lang.UnsupportedOperationException} for all mutation operations. The view will still
 * reflect changes made to the underlying collection. But changes cannot directly be made through
 * the view.</li>
 * <li>{@code Bidi}: A bi-directional transformed view accepts two functions -- a transform and its
 * inverse. This allows the collection to support add operations. Implementations that run in O(n)
 * time with a normal view, such as removing an element from a set, can instead run in sub-linear
 * time if the underlying collection provides sub-linear performance.</li>
 * </ol>  
 *  
 * <h3><a name="trie"></a>Tries</h3>
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
 *    looks more like a sparse vector. Its keys must be sequences of bits, which can also be
 *    interpreted as numeric array indices (hence the name). Each node represents 6 bits in the
 *    key/index sequence and stores a long (64-bit) bitmask and a growable array of up to 64
 *    children. To traverse the trie, an index is computed from the next 6 bits in the sequence (an
 *    integer between 0 and 63). The corresponding bit in the bitmask at that index is queried. If
 *    it is set, the trie contains those bits and traversal continues to a child node. If the bit is
 *    unset, the requested key is not in the map. The array of children only has as many elements as
 *    there are bits set in the bitmask. To determine the index of a child in that array, just count
 *    the number of set bits in the bitmask that are less significant than the one of interest.
 *    (Also see {@link com.bluegosling.collections.AmtPersistentList}, {@link
 *    com.bluegosling.collections.HamtMap}, and {@link com.bluegosling.collections.HamtPersistentMap}.)</li>
 * </ol>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: extend java.util.Abstract* where possible.
// TODO: fix nested sublist/subset/submap (most impls here will mistakenly throw
// ConcurrentModificationException from a sub-* view if the collection is modified from a sub-view
// of that sub-view). Probably should add tests for this, too...
package com.bluegosling.collections;
