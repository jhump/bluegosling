package com.bluegosling.collections.concurrent;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list that is thread-safe and supports concurrent mutations. In addition to standard list
 * operations, several atomic operations are provided to enable concurrent updates.
 * 
 * <p>{@link Iterator}s returned by this interface will not throw
 * {@link ConcurrentModificationException}s during traversal. However, this exception can still be
 * thrown from modification operations made through the iterator: ({@link Iterator#remove()},
 * {@link ListIterator#add(Object)}, and {@link ListIterator#set(Object)}). Whether or not such
 * exceptions are thrown depends on a given implementation of this interface. But the only
 * operations guaranteed to not throw are traversal/query operations.
 * 
 * <p>This interface does not specify whether {@link Iterator}s are weakly consistent or are
 * strongly consistent snaphots. That detail, as well as the conditions which are considered
 * "conflicting concurrent modifications", should be documented by concrete sub-classes.
 *
 * @param <E> the type of element in the list
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface ConcurrentList<E> extends List<E> {
   
   /**
    * Replaces the given value at the given index. This atomically sets the value at the given index
    * but only if its existing value matches the given value to be replaced.
    *
    * @param index the index of the item to replace
    * @param expectedValue the expected value of the element to replace
    * @param newValue the replacement value
    * @return true if the value was replaced or false if the element at the given index differed
    *       from the expected value and was thus not replaced
    */
   boolean replace(int index, E expectedValue, E newValue);
   
   /**
    * Removes the given value at the given index. This atomically removes the value at the given
    * index if and only if it matches the given value. 
    *
    * @param index the index of the item to remove
    * @param expectedValue the expected value of the element to remove
    * @return true if the value was removed or false if the element at the given index differed from
    *       the expected value and was thus not removed
    */
   boolean remove(int index, E expectedValue);
   
   /**
    * Adds the given value at the given index, after the given value. This atomically adds the value
    * at the given index but only if its predecessor would match the given value.
    * 
    * <p>The given index will be the index of the newly added item. Any items at that index or after
    * will be shifted by one. The expected prior value is compared against the item at
    * {@code index - 1} and the operation is aborted if the actual value doesn't match.
    *
    * @param index the index of the newly added item
    * @param expectedPriorValue the expected value of the new element's predecessor
    * @param addition the value to add at the given index
    * @return true if the value was added or false if the element's predecessor differed from the
    *       expected value
    */
   boolean addAfter(int index, E expectedPriorValue, E addition);

   /**
    * Adds the given value at the given index, before the given value. This atomically adds the
    * value at the given index but only if its successor would match the given value.
    * 
    * <p>The given index will be the index of the newly added item. Any items at that index or after
    * will be shifted by one. The expected next value is compared against the item already at the
    * given index (which will be shifted to {@code index + 1} after the operation completes). The
    * operation is aborted if the actual value doesn't match.
    *
    * @param index the index of the newly added item
    * @param expectedNextValue the expected value of the new element's successor
    * @param addition the value to add at the given index
    * @return true if the value was added or false if the element's succcessor differed from the
    *       expected value
    */
   boolean addBefore(int index, E expectedNextValue, E addition);
   
   /**
    * Adds an element to the beginning of the list. The indices for all other elements in the list,
    * if any, are shifted by one. This method is named for readability and can be used in place of
    * {@link #addAfter(int, Object, Object)} when there is no predecessor.
    * 
    * <p>The default implementation is simply the following:<br>
    * {@link #add(int, Object) add(0, newValue)}.
    *
    * @param newValue the value to add to the front of the list
    */
   default void addFirst(E newValue) {
      add(0, newValue);
   }
   
   /**
    * Adds an element to the end of the list and returns its index, atomically. This method can be
    * used in place of {@link #addBefore(int, Object, Object)} when there is no successor.
    *
    * @param newValue the value to add to the end of the list
    * @return the index of the newly added value
    */
   int addLast(E newValue);
   
   /**
    * Returns a view of the portion of this list between the specified indices. Changes to this
    * list are visible through the returned view, and vice versa.
    * 
    * <p>The returned list is also a concurrent list. Structural changes made to the underlying
    * list can impact the returned list. For example, if an element is added in the given index
    * range in the underlying list, it will effectively displace the last element of the sub-list
    * (since that last item has now been pushed beyond the end of the index range). Similarly,
    * removing an element in the given index range from the underlying list will cause the sub-list
    * to get a new last element (the item that was after the given range that has now been pulled
    * into the range due to an element removal).
    * 
    * <p>The returned sub-list should never throw {@link ConcurrentModificationException} from any
    * of its operations (although its iterators may throw this exception if concurrent modifications
    * conflict with attempts to modify the list via the iterator).
    * 
    * <p>Unlike the standard {@link List} interface, implementations may choose to return an
    * unmodifiable view. In that case, changes made to the underlying list are visible via the
    * sub-list view. But the sub-list view itself may throw {@link UnsupportedOperationException}
    * for structural modifications, like adding and removing elements.
    *
    * @see java.util.List#subList(int, int)
    */
   @Override ConcurrentList<E> subList(int fromIndex, int toIndex);

   /**
    * Returns a view of the tail of this list. Changes to this list are visible through the returned
    * view, and vice versa.
    * 
    * <p>This is similar to using {@link #subList(int, int) subList(fromIndex, size())} except that
    * the returned list can <em>grow as the underlying list grows</em>. If an element is added after
    * the given index, it will not displace the last element of the sub-list but instead extend it.
    * 
    * <p>Like {@link #subList(int, int)}, implementations may choose to return an unmodifiable view,
    * in which case the returned view can throw {@link UnsupportedOperationException} when
    * structural modifications are attempted.
    *
    * @param fromIndex low endpoint (inclusive) of the tail list
    * @return a view of the specified range within this list
    */
   ConcurrentList<E> tailList(int fromIndex);
}
