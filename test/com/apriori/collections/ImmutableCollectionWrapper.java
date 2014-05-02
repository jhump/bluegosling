package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

// TODO: javadoc
abstract class ImmutableCollectionWrapper<E, C extends Collection<E>>
      implements ImmutableCollection<E> {
   
   static <E> List<E> fromIterable(Iterable<E> iter) {
      List<E> list = new LinkedList<E>();
      for (E item : iter) {
         list.add(item);
      }
      return list;
   }
   
   private final C collection;

   ImmutableCollectionWrapper(C collection) {
      this.collection = collection;
   }
   
   protected C wrapped() {
      return collection;
   }
   
   @Override
   public Iterator<E> iterator() {
      final Iterator<E> iter = wrapped().iterator();
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
      return wrapped().size();
   }

   @Override
   public boolean isEmpty() {
      return wrapped().isEmpty();
   }

   @Override
   public Object[] toArray() {
      return wrapped().toArray();
   }

   @Override
   public <T> T[] toArray(T[] array) {
      return wrapped().toArray(array);
   }

   @Override
   public boolean contains(Object o) {
      return wrapped().contains(o);
   }

   @Override
   public boolean containsAll(Iterable<?> items) {
      return wrapped().containsAll(fromIterable(items));
   }

   @Override
   public boolean containsAny(Iterable<?> items) {
      for (Object item : items) {
         if (wrapped().contains(item)) {
            return true;
         }
      }
      return false;
   }
}
