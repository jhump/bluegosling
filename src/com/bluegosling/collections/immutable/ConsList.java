package com.bluegosling.collections.immutable;

import java.util.List;
import java.util.NoSuchElementException;


/**
 * A list that is composed of <a href="https://en.wikipedia.org/wiki/Cons">{@code cons} cells</a>.
 * Implementations are generally simple, singly linked lists.
 *
 * @param <E> the type of element in the list
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface ConsList<E> extends List<E> {
   /**
    * Returns the first element in the list. If this list is empty (aka {@code nil}) then an
    * exception is thrown.
    *
    * @return the first element in the list
    * @throws NoSuchElementException if this list is empty
    */
   E first();
   
   /**
    * Returns the rest of the list, excluding the first element. If this list is empty (aka
    * {@code nil}) then an exception is thrown.
    *
    * @return the rest of the list, not including the first element
    * @throws NoSuchElementException if this list is empty
    */
   ConsList<E> rest();
   
   /**
    * Returns the first of the list. This is a synonym for {@link #first()}.
    *
    * @return the first element in the list
    * @throws NoSuchElementException if this list is empty
    */
   default E car() {
      return first();
   }
   
   /**
    * Returns the rest of the list. This is a synonym for {@link #rest()}.
    *
    * @return the rest of the list, not including the first element
    * @throws NoSuchElementException if this list is empty
    */
   default ConsList<E> cdr() {
      return rest();
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>A sub-list of a {@link ConsList} is also a {@link ConsList}.
    *
    * @see java.util.List#subList(int, int)
    */
   @Override
   ConsList<E> subList(int fromIndex, int toIndex);
}
