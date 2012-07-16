package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

//TODO: javadoc
public class FilteringList<E> extends FilteringCollection<E> implements List<E> {

   public FilteringList(List<E> list, Predicate<E> predicate) {
      super(list, predicate);
   }

   @Override
   public List<E> capture() {
      return Collections.unmodifiableList(new ArrayList<E>(this));
   }
   
   @Override
   protected List<E> internal() {
      return (List<E>) super.internal();
   }
   
   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
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

   @Override
   public E set(int index, E element) {
      ListIterator<E> iter = listIterator(index + 1);
      E ret = iter.previous();
      iter.set(element);
      return ret;
   }

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
      for (ListIterator<E> iter = listIterator(); iter.hasNext();) {
         E element = iter.next();
         if (o == null ? element == null : o.equals(element)) {
            return iter.previousIndex();
         }
      }
      return -1;
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
      }
      throw new IndexOutOfBoundsException("" + index + " >= " + iter.nextIndex());
   }

   @Override
   public List<E> subList(int fromIndex, int toIndex) {
      // TODO: fix this! fromIndex and toIndex need to be converted from filterd indices to
      // underlying indices before calling subList()
      return new FilteringList<E>(internal().subList(fromIndex, toIndex), predicate());
   }

}
