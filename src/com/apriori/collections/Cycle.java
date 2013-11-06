package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A cyclically ordered set. An ordered sequence of objects, similar to a list, but it does not
 * necessarily have a fixed start or fixed end. The sequence is cyclical and thus infinite. At any
 * given time, there is a "current" element which can be thought of as the start of the sequence.
 * To navigate the sequence, though, you can advance or retreat the current element.
 * 
 * <p>The size of the collection is the number of items added to it. This is the number of elements
 * one can traverse before the sequence starts over.
 * 
 * <p>The standard {@link #iterator()} starts iteration at the "current" element and ends before the
 * sequence starts over. An alternate {@link #cycle()} iterator is also provided. This alternate is
 * unending (unless the sequence is empty) and repeats the sequence, following the cycle from end
 * back to beginning.
 *
 * @param <E>
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Cycle<E> extends Collection<E> {
   /**
    * Adds an element to the front of the sequence. This adjusts "current" to be the newly added
    * element. The item that was previously "current" will be the next (second) item in the sequence
    * upon return.
    *
    * @param e the new element to add
    */
   void addFirst(E e);
   
   /**
    * Adds an element to the end of the sequence. The "current" element is not adjusted. Since the
    * sequence is cyclical, this effectively adds an item <em>before</em> the current element.
    *
    * @param e the new element to add
    */
   void addLast(E e);
   
   /**
    * Replaces the current element with the specified value. This is equivalent to calling
    * {@link #remove()} and then calling {@link #addFirst(Object)}. The value returned is the
    * one that was replaced.
    *
    * @param e the new value
    * @return the value that was replaced
    * @throws NoSuchElementException if the sequence is empty
    */
   E set(E e);
   
   /**
    * Retrieves the current element.
    *
    * @return the current element
    * @throws NoSuchElementException if the sequence is empty
    */
   E current();
   
   /**
    * Advances the current element to the next item in the sequence. The value returned is the
    * number of actual positions by which the current element moved. If the sequence contains
    * exactly one element then this will be zero (due to it being a cycle, the next element is also
    * always the current element and the previous element). Otherwise, it will be one.
    * 
    * <p>This is equivalent to calling {@link #advanceBy(int) advanceBy(1)}.
    *
    * @return the effective distance by which the current element was advanced 
    * @throws IllegalStateException if the sequence is empty
    */
   int advance();
   
   /**
    * Advances the current element by the specified distance. The value returned is the number of
    * actual positions by which the current element moved and will be:<pre>
    * Math.min(distance, distance % size())
    * </pre>
    * 
    * <p>This method does nothing and returns zero if the specified distance is zero.
    *
    * @param distance the number of items by which to advance the current element 
    * @return the effective distance by which the current element was advanced
    * @throws IllegalArgumentException if the specified distance is negative 
    * @throws IllegalStateException if the sequence is empty
    */
   int advanceBy(int distance);

   /**
    * Retreats the current element to the previous item in the sequence. The value returned is the
    * number of actual positions by which the current element moved. If the sequence contains
    * exactly one element then this will be zero (due to it being a cycle, the previous element is
    * also always the current element and the next element). Otherwise, it will be one.
    * 
    * <p>This is equivalent to calling {@link #retreatBy(int) retreatBy(1)}.
    *
    * @return the effective distance by which the current element was retreated 
    * @throws IllegalStateException if the sequence is empty
    */
   int retreat();

   /**
    * Retreats the current element by the specified distance. The value returned is the number of
    * actual positions by which the current element moved and will be:<pre>
    * Math.min(distance, distance % size())
    * </pre>
    * 
    * <p>This method does nothing and returns zero if the specified distance is zero.
    *
    * @param distance the number of items by which to retreat the current element 
    * @return the effective distance by which the current element was retreated
    * @throws IllegalArgumentException if the specified distance is negative 
    * @throws IllegalStateException if the sequence is empty
    */
   int retreatBy(int distance);
   
   /**
    * Resets the current element to the first element in the sequence. The first element starts off
    * as the first item added to an empty sequence. When that item is the "current" item and then
    * {@link #addFirst(Object)} is invoked, the newly added item becomes the first element. If the
    * first element is ever removed, the item after it is the first item.
    * 
    * <p>If the sequence is empty, calling this method does nothing.
    */
   void reset();

   /**
    * Removes the current element. After removal, the "current" element will indicate the next item
    * in the sequence.
    *
    * @return the removed element
    */
   E remove();
   
   /**
    * Returns an iterator that moves through the sequence exactly once. The first item returned by
    * this iterator will be the "current" element. The number of items returned by this iterator
    * will be the number of items in the sequence (see {@link #size()}).
    *
    * @return an iterator over a single pass of this sequence
    */
   @Override Iterator<E> iterator();
   
   /**
    * Returns an unending iterator that repeats through the sequence, cyclically. The first item
    * returned by {@link BidiIterator#next()} will be the "current" element.
    *
    * @return an unending iterator
    */
   BidiIterator<E> cycle();
   
   /**
    * Returns a view of this sequence in reverse order. The current element remains unchanged, but
    * the sequence around it will be reversed. What would previously have been the next element will
    * be the previous one in the returned view and vice versa. Calling {@link #reverse()} on the
    * returned cycle will return the original sequence.
    * 
    * <p>Changes made to the view, including moving the "current" cursor, will be visible through
    * the original cycle. For example, calling {@link #advance()} on the returned view has the
    * same effect as having called {@link #retreat()} on the original sequence; calling
    * {@link #addFirst(Object)} on the view has the same affect as calling {@link #addLast(Object)}
    * followed by {@link #retreat()} on the original.
    * 
    * @return a view of the cycle in reverse order
    */
   Cycle<E> reverse();
   
   /**
    * Returns true if the specified elements are in cyclic order with respect to the current
    * element. This is equivalent to calling {@link #isCyclicOrder(Object, Object, Object)
    * isCyclicOrder(current(), b, c)}.
    *
    * @param b the item that comes first, if in cyclic order
    * @param c the item that comes second, if in cyclic order
    * @return true if the specified items are in cyclic order (e.g. {@code b} comes before
    *       {@code c})
    * @throws NoSuchElementException if there is no current element because the sequence is empty
    * @throws IllegalArgumentException if either {@code b} or {@code c} is not in the sequence
    */
   boolean isCyclicOrder(E b, E c);
   
   /**
    * Returns true if the specified elements are in cyclic order. A cyclic order {@code [a, b, c]}
    * means that, after element {@code a}, one encounters element {@code b} before encountering
    * element {@code c}.
    *
    * @param a the starting element
    * @param b the item that comes first, if in cyclic order
    * @param c the item that comes second, if in cyclic order
    * @return true if the specified items are in cyclic order (e.g. after {@code a}, {@code b} comes
    *       before {@code c})
    * @throws IllegalArgumentException if any of {@code a}, {@code b}, or {@code c} is not in the
    *       sequence
    */
   boolean isCyclicOrder(E a, E b, E c);
}
