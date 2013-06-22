package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A filtered view of another list iterator. This is a wrapper that elides all members of the
 * wrapped list iterator that do not match a given predicate.
 * 
 * <p>Attempts to add/set items in the filtering list iterator that do <em>not</em> match the
 * predicate will throw {@link IllegalArgumentException}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the collection
 */
//TODO: fix tracking of state -- currently, findNext and findPrevious can get state messed up if
//    calls are made to hasNext() *and* hasPrevious() without otherwise calling next() or previous()
//    between... also make sure we handle repositioning underlying iterator prior to add()
//TODO: tests
public class FilteringListIterator<E> implements ListIterator<E> {

   private static final Object NULL_SENTINEL = new Object();
   
   private final ListIterator<E> iterator;
   private final Predicate<? super E> predicate;
   private boolean needNext = true;
   private Object next;
   private boolean needPrevious = true;
   private Object previous;
   private int nextIndex;
   
   /**
    * Constructs a new filtering list iterator. Elements in this list iterator will include only the
    * elements from the specified iterator that match the specified predicate. An element {@code e}
    * matches the predicate if {@code predicate.apply(e)} returns true.
    * 
    * <p>The wrapped iterator must be initialized to the beginning of the list. In other words a
    * call {@link ListIterator#nextIndex()} should return zero.
    * 
    * @param iterator the wrapped iterator, which should be initialized to the beginning of the
    *       list
    * @param predicate the filtering predicate
    * 
    * @throws IllegalArgumentException if the specified iterator is not initialized to the beginning
    *       of the list
    */
   public FilteringListIterator(ListIterator<E> iterator, Predicate<? super E> predicate) {
      if (iterator.nextIndex() != 0) {
         // if we're not at the beginning, we might incorrectly compute list indices of the
         // filtered list, so don't allow that
         throw new IllegalArgumentException("The specified iterator is not at the beginning of list");
      }
      this.iterator = iterator;
      this.predicate = predicate;
   }
   
   /**
    * Gets the wrapped list iterator.
    * 
    * @return the wrapped iterator
    */
   protected ListIterator<E> internal() {
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
         if (predicate().apply(candidate)) {
            next = candidate == null ? NULL_SENTINEL : candidate;
            needNext = false;
            needPrevious = true;
            return;
         }
      }
      next = null;
      needNext = false;
      needPrevious = true;
   }
   
   private void findPrevious() {
      while (internal().hasPrevious()) {
         E candidate = internal().previous();
         if (predicate().apply(candidate)) {
            previous = candidate == null ? NULL_SENTINEL : candidate;
            needPrevious = false;
            needNext = true;
            return;
         }
      }
      previous = null;
      needPrevious = false;
      needNext = true;
   }
   
   @Override
   public boolean hasNext() {
      if (needNext) {
         findNext();
      }
      return next == null;
   }

   @Override
   public boolean hasPrevious() {
      if (needPrevious) {
         findPrevious();
      }
      return previous == null;
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
      nextIndex++;
      return ret;
   }

   @Override
   public E previous() {
      if (needPrevious) {
         findPrevious();
      }
      if (previous == null) {
         throw new NoSuchElementException("previous");
      }
      @SuppressWarnings("unchecked")
      E ret = (E) (previous == NULL_SENTINEL ? null : previous);
      needPrevious = true;
      nextIndex--;
      return ret;
   }

   /**
    * Removes the last item retrieved from the underlying iterable. If neither {@link #next()} nor
    * {@link #previous()} has been called then there is no element to remove and an
    * {@link IllegalStateException} will be thrown.
    * 
    * <p>Due to the filtering logic, a call to {@link #hasNext()} or {@link #hasPrevious()} may
    * move the underlying iterator (to search for the next or previous element that matches the
    * predicate). In this case, removal is no longer possible and an {@link IllegalStateException}
    * will be thrown. So this filtering iterator adds the extra constraint that you cannot
    * interleave a call to {@link #hasNext()}/{@link #hasPrevious()} between the calls to
    * {@link #next()}/{@link #previous()} and {@link #remove()}.
    */
   @Override
   public void remove() {
      if (!needNext || !needPrevious) {
         throw new IllegalStateException("Underlying iterator has moved and last fetched item can no longer be removed");
      }
      internal().remove();
   }

   @Override
   public int nextIndex() {
      return nextIndex;
   }

   @Override
   public int previousIndex() {
      return nextIndex - 1;
   }

   /**
    * @throws IllegalArgumentException if the specified object does not match the predicate
    */
   @Override
   public void set(E e) {
      if (!needNext || !needPrevious) {
         throw new IllegalStateException("Underlying iterator has moved and last fetched item can no longer be set");
      } else if (predicate().apply(e)) {
         internal().set(e);
      } else {
         throw new IllegalArgumentException("Specified object does not pass filter");
      }
   }

   /**
    * @throws IllegalArgumentException if the specified object does not match the predicate
    */
   @Override
   public void add(E e) {
      if (!predicate().apply(e)) {
         throw new IllegalArgumentException("Specified object does not pass filter");
      }
      // if we've already moved underlying iterator to search for next or previous item, reposition
      // it to the right place for the insertion
      if (!needNext) {
         findPrevious();
      } else if (!needPrevious) {
         // reposition underlying iterator back to next item
         findNext();
      }
      internal().add(e);
   }
}
