package com.bluegosling.collections.views;

import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.MoreIterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;

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
      return Collections.unmodifiableCollection(new ArrayList<>(this));
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

   /**
    * @see com.bluegosling.collections.views.FilteringIterator
    */
   @Override
   public Iterator<E> iterator() {
      return new FilteringIterator<>(internal().iterator(), predicate());
   }

   @Override
   public Object[] toArray() {
      return MoreIterables.toArray(this);
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return MoreIterables.toArray(this, a);
   }

   /**
    * @throws IllegalArgumentException if the specified object does not match the predicate
    */
   @Override
   public boolean add(E e) {
      if (predicate().test(e)) {
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
      ArrayList<E> snapshot = new ArrayList<>(c);
      for (E e : snapshot) {
         if (!predicate().test(e)) {
            throw new IllegalArgumentException("Specified object does not pass filter: " + e);
         }
      }
      // all items pass, so we can safely add everything
      return internal().addAll(snapshot);
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
