package com.apriori.collections;

import java.util.NavigableSet;

//TODO: javadoc
public interface RandomAccessNavigableSet<E> extends NavigableSet<E>, RandomAccessSortedSet<E> {

   @Override RandomAccessNavigableSet<E> subList(int fromIndex, int toIndex);

   @Override RandomAccessNavigableSet<E> descendingSet();

   @Override RandomAccessNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
         boolean toInclusive);

   @Override RandomAccessNavigableSet<E> headSet(E toElement, boolean inclusive);

   @Override RandomAccessNavigableSet<E> tailSet(E fromElement, boolean inclusive);

   @Override RandomAccessNavigableSet<E> subSet(E fromElement, E toElement);

   @Override RandomAccessNavigableSet<E> headSet(E toElement);

   @Override RandomAccessNavigableSet<E> tailSet(E fromElement);
}