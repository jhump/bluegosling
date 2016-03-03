package com.bluegosling.collections.immutable;

import java.util.List;
import java.util.NoSuchElementException;

// TODO: javadoc
public class ImmutableListWrapper<E> extends ImmutableCollectionWrapper<E, List<E>>
      implements ImmutableList<E> {

   
   public ImmutableListWrapper(List<E> list) {
      super(list);
   }

   @Override
   public E get(int i) {
      return collection.get(i);
   }

   @Override
   public int indexOf(Object o) {
      return collection.indexOf(o);
   }

   @Override
   public int lastIndexOf(Object o) {
      return collection.lastIndexOf(o);
   }

   @Override
   public ImmutableList<E> subList(int from, int to) {
      return new ImmutableListWrapper<E>(collection.subList(from, to));
   }

   @Override
   public E first() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return get(0);
   }

   @Override
   public ImmutableList<E> rest() {
      return isEmpty() ? this : subList(1, size());
   }
}
