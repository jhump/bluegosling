package com.bluegosling.collections;

import java.util.Comparator;
import java.util.NavigableSet;

/**
 * A navigable set that also provides random access to its members. The elements in the set are
 * ordered according to the set's {@link Comparator} or (if there is none) according to the
 * elements' {@linkplain Comparable natural ordering}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the set
 */
public interface RandomAccessNavigableSet<E> extends NavigableSet<E>, RandomAccessSet<E> {

   /**
    * {@inheritDoc}
    * <p>Overrides return type to return a navigable set.
    */
   @Override RandomAccessNavigableSet<E> subSetByIndices(int fromIndex, int toIndex);

   /**
    * {@inheritDoc}
    * <p>Overrides return type to return a random access set.
    */
   @Override RandomAccessNavigableSet<E> descendingSet();

   /**
    * {@inheritDoc}
    * <p>Overrides return type to return a random access set.
    */
   @Override RandomAccessNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
         boolean toInclusive);

   /**
    * {@inheritDoc}
    * <p>Overrides return type to return a random access set.
    */
   @Override RandomAccessNavigableSet<E> headSet(E toElement, boolean inclusive);

   /**
    * {@inheritDoc}
    * <p>Overrides return type to return a random access set.
    */
   @Override RandomAccessNavigableSet<E> tailSet(E fromElement, boolean inclusive);

   /**
    * {@inheritDoc}
    * <p>Overrides return type to return a random access set.
    */
   @Override RandomAccessNavigableSet<E> subSet(E fromElement, E toElement);

   /**
    * {@inheritDoc}
    * <p>Overrides return type to return a random access set.
    */
   @Override RandomAccessNavigableSet<E> headSet(E toElement);

   /**
    * {@inheritDoc}
    * <p>Overrides return type to return a random access set.
    */
   @Override RandomAccessNavigableSet<E> tailSet(E fromElement);
}
