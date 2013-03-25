package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.ListIterator;
import java.util.NoSuchElementException;

//TODO: javadoc
//TODO: tests
public class FilteringListIterator<E> implements ListIterator<E> {

   private static final Object NULL_SENTINEL = new Object();
   
   private final ListIterator<E> iterator;
   private final Predicate<E> predicate;
   private boolean needNext = true;
   private Object next;
   private boolean needPrevious = true;
   private Object previous;
   private int nextIndex;
   
   public FilteringListIterator(ListIterator<E> iterator, Predicate<E> predicate) {
      this(iterator, predicate, 0);
   }

   public FilteringListIterator(ListIterator<E> iterator, Predicate<E> predicate, int nextIndex) {
      this.iterator = iterator;
      this.predicate = predicate;
      this.nextIndex = nextIndex;
   }
   
   protected ListIterator<E> internal() {
      return iterator;
   }
   
   protected Predicate<E> predicate() {
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

   @Override
   public void add(E e) {
      if (!needNext || !needPrevious) {
         throw new IllegalStateException("Underlying iterator has moved and a new item can no longer be added");
      } else if (predicate().apply(e)) {
         internal().add(e);
      } else {
         throw new IllegalArgumentException("Specified object does not pass filter");
      }
   }
}
