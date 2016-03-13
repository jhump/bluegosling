/**
 * Transforming and filtering wrappers for the various collection interfaces. Each of these wraps
 * some other collection (or iterable or iterator) and uses a function or a predicate, applied to
 * each element as needed. Updates to the underlying collection are visible (depending on the
 * results of applying the function or predicate) through this wrapper and vice versa.
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
 * <p>The last category of classes in this package are the descending views. These are not
 * necessarily useful by themselves, but are intended to be used when implementing new navigable
 * maps and sets. If your map or set implements <em>every</em> operation other than the descending
 * ones, then these classes can easily be used to implement the descending ones.
 */
package com.bluegosling.collections.views;