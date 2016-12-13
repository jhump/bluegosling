/** 
 * Various utilities that extend the Java Collections Framework (JCF). These expand on similar
 * utilities provided by {@linkplain com.google.common.collect Guava}. The various sub-packages
 * include new implementations and collection interfaces. This package contains a few different
 * kinds of utilities, described in the sections below.
 * 
 * <h3>Static Utilities</h3>
 * In the vein of Guava's {@link com.google.common.collect.Lists},
 * {@link com.google.common.collect.Sets}, and {@link com.google.common.collect.Maps}, this package
 * has several classes with a variety of static utility methods. They include more stuff for
 * standard JCF interfaces (such as {@link com.bluegosling.collections.MoreIterables},
 * {@link com.bluegosling.collections.MoreCollections},
 * and {@link com.bluegosling.collections.MoreMaps}). And they also include stuff for interfaces
 * new to Java 8 (such as {@link com.bluegosling.collections.MoreSpliterators} and
 * {@link com.bluegosling.collections.MoreStreams}).
 *
 * <h3>Views</h3>
 * This package includes classes that provide various views over other collection. Most of these
 * views wrap a given collection (or iterable or iterator) and provide a filtered or transformed
 * view of it using a function or a predicate, applied to each element as needed. Updates to the
 * underlying collection are visible (depending on the results of applying the function or
 * predicate) through this wrapper and vice versa.
 * 
 * <p>The filtering and transforming views are very similar to related methods in Guava's
 * {@link com.google.common.collect.Collections2}, {@link com.google.common.collect.Lists},
 * {@link com.google.common.collect.Sets}, and {@link com.google.common.collect.Maps} except that
 * these are public types which can be sub-classed.
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
 * sub-linear performance. This is similar to the functionality provided by Guava's utility
 * methods.
 * <li>{@code ReadOnly}: A read-only transformed view will throw
 * {@link java.lang.UnsupportedOperationException} for all mutation operations. The view will still
 * reflect changes made to the underlying collection. But changes cannot directly be made through
 * the view. This can be convenient when you want to avoid double-wrapping -- e.g. in both a
 * transforming view and an unmodifiable view.</li>
 * <li>{@code Bidi}: A bi-directional transformed view accepts two functions -- a transform and its
 * inverse. This allows the collection to support add operations. Implementations that run in O(n)
 * time with a normal view, such as removing an element from a set, can instead run in sub-linear
 * time if the underlying collection provides sub-linear performance. This variant has no analog in
 * Guava's utility methods.</li>
 * </ol> 
 * Other functionality present here that is not present in Guava's utility methods is the ability
 * to filter lists, transform sets, and transform map keys.
 * 
 * <p>A third category of classes is also in this package: descending views. These are not
 * necessarily useful by themselves, but are intended to be used when implementing new navigable
 * maps and sets. If your map or set implements every operation <em>other than</em> the descending
 * ones (and none <em>in terms of</em> the descending ones), then these classes can be used to
 * easily implement the descending ones.
 * 
 * <h3>Abstract Base Classes</h3>
 * This package includes useful base classes for new implementations of
 * {@link com.bluegosling.collections.AbstractDeque Deque} and
 * {@link com.bluegosling.collections.AbstractNavigableMap NavigableMap}.
 * 
 * <h3>Other</h3>
 * The sub-packages contain a greater variety of new collections, grouped by collection type
 * (e.g. list, set, queue, map, etc). But there are also a few in this package:
 * <ul>
 * <li>{@link com.bluegosling.collections.CircularBuffer}: a circular buffer of reference-type
 * elements. This buffer is much simpler than related primitive buffers in {@code java.nio}.</li>
 * <li>{@link com.bluegosling.collections.NoCopyGrowableArray}: a new collection that is a growable
 * array but does not incur the expense of copying on growth. It implements the
 * {@link java.util.List} interface and provides constant time random access, too.</li>
 * <li>{@link com.bluegosling.collections.Stack}: a new collection interface for LIFO stacks. This
 * is much narrower than existing JCF types used as stacks like {@link java.util.Deque} and
 * {@link java.util.List}.</li>
 * </ul>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: extend java.util.Abstract* where possible.
// TODO: fix nested sublist/subset/submap (most impls here will mistakenly throw
// ConcurrentModificationException from a sub-* view if the collection is modified from a sub-view
// of that sub-view). Probably should add tests for this, too...
package com.bluegosling.collections;
