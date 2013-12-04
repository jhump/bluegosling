package com.apriori.collections;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * An immutable, read-only, ordered collection.  This interface is similar to the standard
 * {@link List} interface except that it defines no mutation operations.
 *
 * @param <E> the type of element in the list
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface ImmutableList<E> extends ImmutableCollection<E> {
  /**
   * Retrieves the element at the specified zero-based index. The item at index zero represents the
   * first item in the list. The item at index {@code size() - 1} represents the last element in
   * the list.
   *
   * @param i the index
   * @return the element at the specified index
   * @throws IndexOutOfBoundsException if the specified index is less than zero or greater than or
   *     equal to the list's size
   */
  E get(int i);
  
  /**
   * Finds the first occurrence of the specified element in the list and returns the corresponding
   * index. The search starts at the beginning of the list, and the index of the first matching
   * element is returned. If no match is found then -1 is returned. A matching item is one that is
   * equal to the specified item. If the specified item is {@code null} then a {@code null} in the
   * list is a match. Otherwise, an item in the list that {@linkplain Object#equals(Object) equals}
   * the specified object is a match.
   *
   * @param o the element to find
   * @return the index of the first occurrence of the specified item or -1 if it is not found.
   */
  int indexOf(Object o);

  /**
   * Finds the last occurrence of the specified element in the list and returns the corresponding
   * index. The search starts at the end of the list, and the index of the first matching element
   * found is returned. If no match is found then -1 is returned.
   *
   * @param o the element to find
   * @return the index of the last occurrence of the specified item or -1 if it is not found.
   * @see #indexOf(Object)
   */
  int lastIndexOf(Object o);
  
  /**
   * Returns a portion of this list as another immutable list. The returned list may be just a view
   * into this list or may represent a separate list with the same elements as the specified
   * sub-list.
   *
   * @param from the starting index, inclusive, of the sub-list
   * @param to the end index, exclusive, of the sub-list
   * @return an immutable list whose elements are the same as those between the specified indices
   * @throws IndexOutOfBoundsException if either index is less than zero or greater than or equal to
   *     the list's size or if the starting index is greater than the end index
   */
  ImmutableList<E> subList(int from, int to);
  
  /**
   * Returns the first element in this list.
   *
   * @return the first element in this list
   * @throws NoSuchElementException if this list is empty
   */
  E first();
  
  /**
   * Returns a sublist that contains the same items as this list except for the first item. The
   * returned list may be just a view into this list or may represent list with the same elements.
   * If this list is empty, an empty list is also returned.
   *
   * @return a sublist that contains the same items as this list except for the first one
   */
  ImmutableList<E> rest();
  
  /**
    * Returns true if this list is equal to the specified object. The objects are equal if both of
    * them are {@link ImmutableList}s and they contain the same elements in the same order. So they
    * are equal if both lists are the same size and if, for every index {@code i}, the item at
    * index {@code i} in this list is equal to the item at index {@code i} in the given list.
    *
    * @param o an object
    * @return true if the specified object is an immutable list with the same elements in the same
    *       order
   */
  @Override boolean equals(Object o);
  
  /**
    * Returns the hash code value for this list. The hash code of a list is defined to be the result
    * of the following calculation:
    * <pre>
    * int hashCode = 1;
    * for (E e : list) {
    *    hashCode = 31 * hashCode + (e==null ? 0 : e.hashCode());
    * }
    * </pre>
    *
    * @return the hash code for this list
    * 
    * @see List#hashCode()
   */
  @Override int hashCode();
}
