package com.apriori.collections;

import java.util.SortedSet;

//TODO: javadoc
public interface RandomAccessSortedSet<E> extends SortedSet<E>, RandomAccessSet<E> {

   @Override RandomAccessSortedSet<E> subList(int fromIndex, int toIndex);

   @Override RandomAccessSortedSet<E> subSet(E fromElement, E toElement);

   @Override RandomAccessSortedSet<E> headSet(E toElement);

   @Override RandomAccessSortedSet<E> tailSet(E fromElement);
}