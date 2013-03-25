package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.Iterator;
import java.util.NoSuchElementException;

//TODO: javadoc
//TODO: tests
public class FilteringIterator<E> implements Iterator<E> {

   private static final Object NULL_SENTINEL = new Object();
   
   private final Iterator<E> iterator;
   private final Predicate<E> predicate;
   private boolean needNext = true;
   private Object next;
   
   public FilteringIterator(Iterator<E> iterator, Predicate<E> predicate) {
      this.iterator = iterator;
      this.predicate = predicate;
   }
   
   protected Iterator<E> internal() {
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
      return next == null;
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

   @Override
   public void remove() {
      if (!needNext) {
         throw new IllegalStateException("Underlying iterator has advanced and last fetched item can no longer be removed");
      }
      internal().remove();
   }
}
