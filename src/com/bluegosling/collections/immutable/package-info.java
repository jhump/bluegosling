/**
 * Immutable and persistent collections. This package contains abstract base classes that can be
 * used to implement immutable and <a href="https://en.wikipedia.org/wiki/Persistent_data_structure">
 * persistent</a> data structures.
 * 
 * <p>Guava's {@link com.google.common.collect.ImmutableCollection} types do not allow sub-typing,
 * explicitly to ensure that they are immutable (e.g. a misbehaved subclass could violate the
 * immutability guarantee). The base types in this package, on the other hand, are intended to be
 * sub-classed to aid implementation of new immutable types.
 * 
 * <p>Also of note: this package contains interfaces for
 * {@linkplain com.bluegosling.collections.immutable.PersistentCollection persistent collections},
 * parallel to the normal mutable interfaces in the JCF. Implementations of these interfaces are
 * also provided.
 */
package com.bluegosling.collections.immutable;