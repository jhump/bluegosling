package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

//TODO: javadoc
//TODO: tests
public class FilteringCollection<E> extends FilteringIterable<E> implements Collection<E> {

   public FilteringCollection(Collection<E> collection, Predicate<E> predicate) {
      super(collection, predicate);
   }

   @Override
   public Collection<E> capture() {
      return Collections.unmodifiableCollection(new ArrayList<E>(this));
   }
   
   @Override
   protected Collection<E> internal() {
      return (Collection<E>) super.internal();
   }
   
   @Override
   public int size() {
      int size = 0;
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         iter.next();
         size++;
      }
      return size;
   }

   @Override
   public boolean isEmpty() {
      return iterator().hasNext();
   }

   @Override
   public boolean contains(Object o) {
      for (E element : this) {
         if (o == null ? element == null : o.equals(element)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public Iterator<E> iterator() {
      return new FilteringIterator<E>(internal().iterator(), predicate());
   }

   @Override
   public Object[] toArray() {
      return new ArrayList<E>(this).toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return new ArrayList<E>(this).toArray(a);
   }

   @Override
   public boolean add(E e) {
      if (predicate().apply(e)) {
         return internal().add(e);
      } else {
         throw new IllegalArgumentException("Specified object does not pass filter");
      }
   }

   @Override
   public boolean remove(Object o) {
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         E element = iter.next();
         if (o == null ? element == null : o.equals(element)) {
            if (predicate().apply(element)) {
               iter.remove();
               return true;
            }
         }
      }
      return false;
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      for (Object o : c) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      boolean changed = false;
      for (E e : c) {
         if (add(e)) {
            changed = true;
         }
      }
      return changed;
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      boolean changed = false;
      for (Iterator<E> iter = this.iterator(); iter.hasNext(); ) {
         E e = iter.next();
         if (c.contains(e)) {
            iter.remove();
            changed = true;
         }
      }
      return changed;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      boolean changed = false;
      for (Iterator<E> iter = this.iterator(); iter.hasNext(); ) {
         E e = iter.next();
         if (!c.contains(e)) {
            iter.remove();
            changed = true;
         }
      }
      return changed;
   }

   @Override
   public void clear() {
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         if (predicate().apply(iter.next())) {
            iter.remove();
         }
      }
   }

   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
}
