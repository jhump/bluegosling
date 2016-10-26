/** 
 * New implementations and utilities to complement the Java Collections Framework (JCF).
 * 
 * <p>In addition to new implementations for standard collection interfaces, this package also
 * contains some new collection interfaces:
 * <dl>
 * <dt>{@link com.bluegosling.collections.lists.AssociativeList}</dt>
 *    <dd>A {@link java.util.List} that supports sparse associative keys and can be viewed as a
 *    {@link java.util.Map}.</dd>
 * <dt>{@link com.bluegosling.collections.sets.RandomAccessSet}</dt>
 *    <dd>A {@link java.util.Set} that supports random access of elements and can be viewed as a
 *    {@link java.util.List}.</dd>
 * <dt>{@link com.bluegosling.collections.maps.RandomAccessNavigableMap}</dt>
 *    <dd>A {@link java.util.NavigableMap} that supports random access of elements via key- and
 *    entry-sets that are {@link com.bluegosling.collections.sets.RandomAccessSet}s and a view of values as a
 *    {@link java.util.List}.</dd>
 * <dt>{@link com.bluegosling.collections.bits.BitSequence}</dt>
 *    <dd>An immutable sequence of bits. This is similar to {@link java.util.BitSet} except that it
 *    is immutable and provides additional methods for simpler querying.</dd>
 * <dt>{@link com.bluegosling.collections.queues.PriorityQueue}</dt>
 *    <dd>A priority queue. This interface is a little different than the JRE's class of the same
 *    name in that it exposes additional operations of a classical priority-queue ADT, mainly
 *    {@code reduce-key}. This decouples an element's priority from its intrinsic value, making it
 *    more useful for certain types of graph algorithms.</dd>
 * </dl>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: extend java.util.Abstract* where possible.
// TODO: fix nested sublist/subset/submap (most impls here will mistakenly throw
// ConcurrentModificationException from a sub-* view if the collection is modified from a sub-view
// of that sub-view). Probably should add tests for this, too...
package com.bluegosling.collections;
