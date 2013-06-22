package com.apriori.collections;

import com.apriori.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * A filtered view of another collection. This is a wrapper that elides all members of the wrapped
 * collection that do not match a given predicate. Changes to the underlying collection will be
 * visible through the filtering collection (or at least those changes that effect elements that
 * match the predicate).
 * 
 * <p>Attempts to add items to the filtering collection that do <em>not</em> match the predicate
 * will throw {@link IllegalArgumentException}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the collection
 */
//TODO: tests
public class FilteringCollection<E> extends FilteringIterable<E> implements Collection<E> {

   /**
    * Constructs a new filtering collection. Elements in this collection will include only the
    * elements from the specified collection that match the specified predicate. An element
    * {@code e} matches the predicate if {@code predicate.apply(e)} returns true. 
    * 
    * @param collection the wrapped collection
    * @param predicate the filtering predicate
    */
   public FilteringCollection(Collection<E> collection, Predicate<? super E> predicate) {
      super(collection, predicate);
   }

   @Override
   public Collection<E> capture() {
      return Collections.unmodifiableCollection(new ArrayList<E>(this));
   }
   
   /**
    * Gets the wrapped collection.
    * 
    * @return the wrapped collection
    */
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
      return CollectionUtils.contains(iterator(), o);
   }

   @Override
   public Iterator<E> iterator() {
      return new FilteringIterator<E>(internal().iterator(), predicate());
   }

   @Override
   public Object[] toArray() {
      return CollectionUtils.toArray(this);
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return CollectionUtils.toArray(this, a);
   }

   /**
    * @throws IllegalArgumentException if the specified object does not match the predicate
    */
   @Override
   public boolean add(E e) {
      if (predicate().apply(e)) {
         return internal().add(e);
      } else {
         throw new IllegalArgumentException("Specified object does not pass filter: " + e);
      }
   }

   @Override
   public boolean remove(Object o) {
      return CollectionUtils.removeObject(o, iterator(), true);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return CollectionUtils.containsAll(this, c);
   }

   /**
    * @throws IllegalArgumentException if any element in the specified collection does not match the
    *       predicate (in which case no element is added)
    */
   @Override
   public boolean addAll(Collection<? extends E> c) {
      for (E e : c) {
         if (!predicate().apply(e)) {
            throw new IllegalArgumentException("Specified object does not pass filter: " + e);
         }
      }
      // all items pass, so we can safely add everything
      return internal().addAll(c);
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      return CollectionUtils.filter(c, iterator(), true);
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      return CollectionUtils.filter(c, iterator(), false);
   }

   @Override
   public void clear() {
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         iter.next();
         iter.remove();
      }
   }

   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
}
