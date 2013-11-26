package com.apriori.collections;

import java.util.List;
import java.util.NoSuchElementException;

// TODO: javadoc
class ImmutableListWrapper<E> extends ImmutableCollectionWrapper<E, List<E>>
      implements ImmutableList<E> {

   
   ImmutableListWrapper(List<E> list) {
      super(list);
   }

   @Override
   public E get(int i) {
      return wrapped().get(i);
   }

   @Override
   public int indexOf(Object o) {
      return wrapped().indexOf(o);
   }

   @Override
   public int lastIndexOf(Object o) {
      return wrapped().lastIndexOf(o);
   }

   @Override
   public ImmutableList<E> subList(int from, int to) {
      return new ImmutableListWrapper<E>(wrapped().subList(from, to));
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
