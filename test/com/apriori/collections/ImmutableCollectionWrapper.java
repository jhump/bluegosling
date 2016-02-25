package com.apriori.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A wrapper to adapt a {@link Collection} to an {@link ImmutableCollection}. This is used to create
 * "reference implementations" for an {@link ImmutableCollection}, against which the behavior of
 * other implementations can be tested. For example, we wrap an {@link ArrayList} and use the
 * resulting immutable list to verify behavior of other immutable lists.
 *
 * @param <E> the type of element in the collection
 * @param <C> the type of collection being wrapped
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class ImmutableCollectionWrapper<E, C extends Collection<E>>
      implements ImmutableCollection<E> {
   
   protected final C collection;

   ImmutableCollectionWrapper(C collection) {
      this.collection = collection;
   }
   
   @Override
   public Iterator<E> iterator() {
      final Iterator<E> iter = collection.iterator();
      return new Iterator<E>() {
         @Override
         public boolean hasNext() {
            return iter.hasNext();
         }

         @Override
         public E next() {
            return iter.next();
         }
      };
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
   public boolean containsAll(Iterable<?> items) {
      return collection.containsAll(Iterables.snapshot(items));
   }

   @Override
   public boolean containsAny(Iterable<?> items) {
      for (Object item : items) {
         if (collection.contains(item)) {
            return true;
         }
      }
      return false;
   }
}
