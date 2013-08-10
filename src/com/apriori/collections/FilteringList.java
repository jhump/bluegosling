package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * A filtered view of another list. This is a wrapper that elides all members of the wrapped
 * list that do not match a given predicate. Changes to the underlying list will be visible through
 * the filtering collection (or at least those changes that effect elements that match the
 * predicate).
 * 
 * <p>Attempts to add/set items in the filtering list that do <em>not</em> match the predicate will
 * throw {@link IllegalArgumentException}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the collection
 */
//TODO: tests
public class FilteringList<E> extends FilteringCollection<E> implements List<E> {

   /**
    * Constructs a new filtering list. Elements in this list will include only the
    * elements from the specified list that match the specified predicate. An element {@code e}
    * matches the predicate if {@code predicate.apply(e)} returns true. 
    * 
    * @param list the wrapped list
    * @param predicate the filtering predicate
    */
   public FilteringList(List<E> list, Predicate<? super E> predicate) {
      super(list, predicate);
   }

   @Override
   public List<E> capture() {
      return Collections.unmodifiableList(new ArrayList<E>(this));
   }
   
   /**
    * Gets the wrapped list.
    * 
    * @return the wrapped list
    */
   @Override
   protected List<E> internal() {
      return (List<E>) super.internal();
   }
   
   /**
    * @throws IllegalArgumentException if any element in the specified collection does not match the
    *       predicate (in which case no element is added)
    */
   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
      // check all of the items before trying to add them
      for (E e : c) {
         if (!predicate().test(e)) {
            throw new IllegalArgumentException("Specified object does not pass filter: " + e);
         }
      }
      ListIterator<E> iter = listIterator(index);
      boolean ret = false;
      for (E element : c) {
         iter.add(element);
         ret = true;
      }
      return ret;
   }

   @Override
   public E get(int index) {
      return listIterator(index + 1).previous();
   }

   /**
    * @throws IllegalArgumentException if the specified object does not match the predicate
    */
   @Override
   public E set(int index, E element) {
      ListIterator<E> iter = listIterator(index + 1);
      E ret = iter.previous();
      iter.set(element);
      return ret;
   }

   /**
    * @throws IllegalArgumentException if the specified object does not match the predicate
    */
   @Override
   public void add(int index, E element) {
      listIterator(index).add(element);
   }

   @Override
   public E remove(int index) {
      ListIterator<E> iter = listIterator(index + 1);
      E ret = iter.previous();
      iter.remove();
      return ret;
   }

   @Override
   public int indexOf(Object o) {
      return CollectionUtils.findObject(o,  listIterator());
   }

   @Override
   public int lastIndexOf(Object o) {
      ListIterator<E> iter = listIterator();
      // advance to end
      int lastOccurrence = -1;
      while (iter.hasNext()) {
         E element = iter.next();
         // keep track of the latest occurrence
         if (o == null ? element == null : o.equals(element)) {
            lastOccurrence = iter.previousIndex();
         }
      }
      return lastOccurrence;
   }

   @Override
   public ListIterator<E> listIterator() {
      return new FilteringListIterator<E>(internal().listIterator(), predicate());
   }

   @Override
   public ListIterator<E> listIterator(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException("" + index + " < 0");
      }
      ListIterator<E> iter = listIterator();
      if (index == 0) {
         return iter;
      }
      while (iter.hasNext()) {
         if (iter.nextIndex() == index) {
            return iter;
         }
         iter.next();
      }
      throw new IndexOutOfBoundsException("" + index + " >= " + iter.nextIndex());
   }

   @Override
   public List<E> subList(final int fromIndex, final int toIndex) {
      // some quick checks to not waste time resolving indices below
      if (fromIndex < 0) {
         throw new IndexOutOfBoundsException("" + fromIndex + " < 0");
      }
      if (fromIndex > toIndex) {
         throw new IndexOutOfBoundsException("from index " + fromIndex + " > to index " + toIndex);
      }
      // compute bounds in terms of underlying list indices
      int startIndex = -1;
      int endIndex = -1;
      int i = -1;
      for (E e : internal()) {
         if (predicate().test(e)) {
            i++;
            if (i == fromIndex) {
               startIndex = i;
            }
            if (i == toIndex - 1) {
               endIndex = i + 1;
            }
         }
      }
      if (startIndex == -1) {
         if (endIndex == -1) {
            // if we never got to this index, then there aren't enough items that match
            throw new IndexOutOfBoundsException("" + fromIndex + " > " + i);
         } else {
            // we set endIndex but not startIndex? we already checked above that toIndex is less
            // than or equal to fromIndex, so the only remaining possibility is that fromIndex 
            // and toIndex both == size. so the sublist will be empty
            startIndex = endIndex;
         }
      }
      if (endIndex == -1) {
         throw new IndexOutOfBoundsException("" + toIndex + " > " + i);
      }
      
      return new FilteringList<E>(internal().subList(startIndex, endIndex), predicate());
   }
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this,  o);
   }

   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
}
