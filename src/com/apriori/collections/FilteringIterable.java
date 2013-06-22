package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * A filtered view of another iterable. This is a wrapper that elides all members of the wrapped
 * iterable that do not match a given predicate. Changes to the underlying iterable will be visible
 * through the filtering iterable (or at least those changes that effect elements that match the
 * predicate).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the iterable
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
   public Collection<E> capture() {
      // can't use copy constructor of ArrayList because that requires Collection, not Iterable
      ArrayList<E> copy = new ArrayList<E>();
      for (E e : this) {
         copy.add(e);
      }
      return Collections.unmodifiableCollection(copy);
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
