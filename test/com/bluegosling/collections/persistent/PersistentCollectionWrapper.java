package com.bluegosling.collections.persistent;

import com.bluegosling.collections.MoreIterables;
import com.bluegosling.collections.persistent.PersistentCollection;
import com.google.common.collect.Iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A wrapper to adapt a {@link Collection} to a {@link PersistentCollection}. This is used to create
 * "reference implementations" for a {@link PersistentCollection}, against which the behavior of
 * other implementations can be tested. For example, we wrap an {@link ArrayList} and use the
 * resulting persistent list to verify behavior of other persistent lists. These wrapped persistent
 * collections are for testing. They copy the entire wrapped collection during update operations, so
 * they are not expected to perform well.
 *
 * @param <E> the type of element in the collection
 * @param <C> the type of collection being wrapped
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class PersistentCollectionWrapper<E, C extends Collection<E>,
                                           P extends PersistentCollectionWrapper<E, C, P>>
implements PersistentCollection<E> {

   protected final C collection;

   public PersistentCollectionWrapper(C collection) {
      this.collection = collection;
   }
   
   protected abstract C copy(C original);
   
   protected abstract P wrapPersistent(C coll);
   
   @Override
   public Iterator<E> iterator() {
      return Iterators.unmodifiableIterator(collection.iterator());
   }

   @Override
   public int size() {
      return collection.size();
   }

   @Override
   public boolean isEmpty() {
      return collection.isEmpty();
   }

   @Override
   public Object[] toArray() {
      return collection.toArray();
   }

   @Override
   public <T> T[] toArray(T[] array) {
      return collection.toArray(array);
   }

   @Override
   public boolean contains(Object o) {
      return collection.contains(o);
   }

   @Override
   public boolean containsAll(Collection<?> items) {
      return collection.containsAll(MoreIterables.snapshot(items));
   }
   
   @Override
   public P with(E e) {
      C newCollection = copy(collection);
      newCollection.add(e);
      return wrapPersistent(newCollection);
   }

   @Override
   public P without(Object o) {
      C newCollection = copy(collection);
      newCollection.remove(o);
      return wrapPersistent(newCollection);
   }

   @Override
   public P withoutAny(Object o) {
      C newCollection = copy(collection);
      for (Iterator<E> iter = newCollection.iterator(); iter.hasNext();) {
         E e = iter.next();
         if (o == null ? e == null : o.equals(e)) {
            iter.remove();
         }
      }
      return wrapPersistent(newCollection);
   }

   @Override
   public P withoutAny(Iterable<?> items) {
      C newCollection = copy(collection);
      newCollection.removeAll(MoreIterables.snapshot(items));
      return wrapPersistent(newCollection);
   }

   @Override
   public P withOnly(Iterable<?> items) {
      C newCollection = copy(collection);
      newCollection.retainAll(MoreIterables.snapshot(items));
      return wrapPersistent(newCollection);
   }

   @Override
   public P withAll(Iterable<? extends E> items) {
      C newCollection = copy(collection);
      newCollection.addAll(MoreIterables.snapshot(items));
      return wrapPersistent(newCollection);
   }
}
