package com.apriori.collections;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * A set that also provides for random access to its members. This interface does not provide
 * methods for inserting items at arbitrary indices since the order of elements, only for querying
 * and removing elements at arbitrary indices. This interface imposes no constraints on ordering of
 * elements in the set -- they could be ordered arbitrarily, in sorted order, based on most recently
 * added or most recently accessed, etc. It is left up to concrete implementations to decide how to
 * order elements (and they could even provide methods for inserting at arbitrary indices if they
 * allow arbitrary ordering).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the set
 */
public interface RandomAccessSet<E> extends Set<E> {

   /**
    * Retrieves the item at the specified index. The index is zero based, so zero is the first item
    * in the set and the last element is at index {@code size - 1}.
    * 
    * @param index the index of the element to retrieve
    * @return the item at the specified index
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than or
    *       equal to the set's size
    */
   E get(int index);

   /**
    * Retrieves the index of the specified element in the set.
    * 
    * @param o the element
    * @return the index of the specified element or -1 if the specified object is not in the set
    */
   int indexOf(Object o);
   
   /**
    * Returns a {@list ListIterator} for iterating over the set. The iterator will start off before
    * the first element so the first call to {@link ListIterator#next() next()} returns the first
    * element in the set. Iteration is in the same order as that of {@link #iterator()}, but allows
    * for navigating both forwards and backwards in the set and for retrieving the indices of
    * elements.
    * 
    * <p>Elements cannot be set or added using the returned iterator. These operations will throw
    * {@link UnsupportedOperationException}s.
    * 
    * @return an iterator
    */
   ListIterator<E> listIterator();
   
   /**
    * Returns a {@list ListIterator} for iterating over the set. The iterator will start off before
    * the specified index. Iteration is in the same order as that of {@link #iterator()}, but allows
    * for navigating both forwards and backwards in the set and for retrieving the indices of
    * elements.
    * 
    * <p>Elements cannot be set or added using the returned iterator. These operations will throw
    * {@link UnsupportedOperationException}s.
    * 
    * @return an iterator
    */
   ListIterator<E> listIterator(int index);
   
   /**
    * Removes the item at the specified index.
    * 
    * @param index the index of the element to remove
    * @return the removed element
    * @throws IndexOutOfBoundsException if the specified index is negative or is greater than or
    *       equal to the set's size
    */
   E remove(int index);

   /**
    * Returns the sub-set of elements between the specified starting index (inclusive) and ending
    * index (exclusive). The returned set is a view so updates to this set are reflected in the
    * sub-set view and vice versa. Items cannot be added using the sub-set view.
    * 
    * @param fromIndex the starting index
    * @param toIndex the ending index
    * @return the sub-set of elements
    */
   RandomAccessSet<E> subSetByIndices(int fromIndex, int toIndex);

   /**
    * Returns a view of this set as a list. Similar to the sub-set view returned by
    * {@link #subSetByIndices(int, int)}, the returned view does not support adding or setting
    * elements.
    * 
    * @return a view of this set as a list
    */
   List<E> asList();
}