package com.apriori.collections;

import java.util.Comparator;
import java.util.NavigableSet;

/**
 * An immutable, read-only set that maintains a total ordering over its elements. This is analogous
 * to a {@link NavigableSet} except that it provides no mutation operations.
 *
 * @param <E> the type of element in the set
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public interface ImmutableSortedSet<E> extends ImmutableSet<E> {
   Comparator<? super E> comparator();
   E first();
   ImmutableSortedSet<E> rest();
   E last();
   E floor(E e);
   E higher(E e);
   E ceil(E e);
   E lower(E e);
   ImmutableSortedSet<E> subSet(E from, E to);
   ImmutableSortedSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive);
   ImmutableSortedSet<E> headSet(E to);
   ImmutableSortedSet<E> headSet(E to, boolean inclusive);
   ImmutableSortedSet<E> tailSet(E from);
   ImmutableSortedSet<E> tailSet(E from, boolean inclusive);
}
