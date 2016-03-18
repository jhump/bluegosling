/**
 * Various views over collection interfaces. Most of these views wrap a given collection (or
 * iterable or iterator) and provide a filtered or transformed view of it using a function or a
 * predicate, applied to each element as needed. Updates to the underlying collection are visible
 * (depending on the results of applying the function or predicate) through this wrapper and vice
 * versa.
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
 */
package com.bluegosling.collections.views;