package com.bluegosling.collections.immutable;

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
   
   default ImmutableSortedSet<E> subSet(E from, E to) {
      return subSet(from, true, to, false);
   }
   
   ImmutableSortedSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive);
   
   default ImmutableSortedSet<E> headSet(E to) {
      return headSet(to, false);
   }
   
   ImmutableSortedSet<E> headSet(E to, boolean inclusive);
   
   default ImmutableSortedSet<E> tailSet(E from) {
      return tailSet(from, true);
   }
   
   ImmutableSortedSet<E> tailSet(E from, boolean inclusive);
}
