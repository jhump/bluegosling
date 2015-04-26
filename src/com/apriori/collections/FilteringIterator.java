package com.apriori.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * A filtered view of another iterator. This is a wrapper that elides all members of the wrapped
 * iterator that do not match a given predicate.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the iterator
 */
//TODO: tests
public class FilteringIterator<E> implements Iterator<E> {

   private static final Object NULL_SENTINEL = new Object();
   
   private final Iterator<E> iterator;
   private final Predicate<? super E> predicate;
   private boolean needNext = true;
   private Object next;
   
   /**
    * Constructs a new filtering iterator. Elements in this iterator will include only the elements
    * from the specified iterator that match the specified predicate. An element {@code e} matches
    * the predicate if {@code predicate.apply(e)} returns true. 
    * 
    * @param iterator the wrapped iterator
    * @param predicate the filtering predicate
    */
   public FilteringIterator(Iterator<E> iterator, Predicate<? super E> predicate) {
      this.iterator = iterator;
      this.predicate = predicate;
   }
   
   /**
    * Gets the wrapped iterator.
    * 
    * @return the wrapped iterator
    */
   protected Iterator<E> internal() {
      return iterator;
   }
   
   /**
    * Gets the predicate that is responsible for filtering elements.
    * 
    * @return the predicate
    */
   protected Predicate<? super E> predicate() {
      return predicate;
   }

   private void findNext() {
      while (internal().hasNext()) {
         E candidate = internal().next();
         if (predicate().test(candidate)) {
            next = candidate == null ? NULL_SENTINEL : candidate;
            needNext = false;
            return;
         }
      }
      next = null;
      needNext = false;
   }
   
   @Override
   public boolean hasNext() {
      if (needNext) {
         findNext();
      }
      return next != null;
   }

   @Override
   public E next() {
      if (needNext) {
         findNext();
      }
      if (next == null) {
         throw new NoSuchElementException("next");
      }
      @SuppressWarnings("unchecked")
      E ret = (E) (next == NULL_SENTINEL ? null : next);
      needNext = true;
      return ret;
   }

   /**
    * Removes the last item retrieved from the underlying iterable. This is an optional operation.
    * If {@link #next()} has not yet been called then there is no element to remove and an
    * {@link IllegalStateException} will be thrown.
    * 
    * <p>Due to the filtering logic, a call to {@link #hasNext()} may advance the underlying
    * iterator (to search for the next element that matches the predicate). In this case, removal is
    * no longer possible and an {@link IllegalStateException} will be thrown. So this filtering
    * iterator adds the extra constraint that you cannot interleave a call to {@link #hasNext()}
    * between the calls to {@link #next()} and {@link #remove()}.
    */
   @Override
   public void remove() {
      if (!needNext) {
         throw new IllegalStateException("Underlying iterator has advanced and last fetched item can no longer be removed");
      }
      internal().remove();
   }
}
