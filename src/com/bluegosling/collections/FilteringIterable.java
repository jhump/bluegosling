package com.bluegosling.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * A filtered view of another iterable. This is a wrapper that elides all members of the wrapped
 * iterable that do not match a given predicate. Changes to the underlying iterable will be visible
 * through the filtering iterable (or at least those changes that effect elements that match the
 * predicate).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the iterable
 * 
 * @see Iterables#filter(Iterable, com.google.common.base.Predicate)
 */
//TODO: tests
public class FilteringIterable<E> implements Iterable<E> {

   private final Iterable<E> iterable;
   private final Predicate<? super E> predicate;

   /**
    * Constructs a new filtering iterable. Elements in this iterable will include only the elements
    * from the specified iterable that match the specified predicate. An element {@code e} matches
    * the predicate if {@code predicate.apply(e)} returns true. 
    * 
    * @param iterable the wrapped iterable
    * @param predicate the filtering predicate
    */
   public FilteringIterable(Iterable<E> iterable, Predicate<? super E> predicate) {
      this.iterable = iterable;
      this.predicate = predicate;
   }

   /**
    * Capture a snapshot of the filtered elements. The returned collection is not a wrapper and will
    * thusly be disconnected from any future modifications to the wrapped iterable.
    * 
    * @return a snapshot of the filtered elements
    */
   public Collection<E> materialize() {
      return ImmutableList.copyOf(this);
   }

   /**
    * Gets the wrapped iterable.
    * 
    * @return the wrapped iterable
    */
   protected Iterable<E> internal() {
      return iterable;
   }

   /**
    * Gets the predicate that is responsible for filtering elements.
    * 
    * @return the predicate
    */
   protected Predicate<? super E> predicate() {
      return predicate;
   }
   
   @Override
   public Iterator<E> iterator() {
      return new FilteringIterator<E>(internal().iterator(), predicate());
   }

}
