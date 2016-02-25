package com.apriori.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A wrapper that adapts a {@link List} to the {@link PersistentList} interface.
 *
 * @param <E> the type of element in the list
 * 
 * @see PersistentCollectionWrapper
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class PersistentListWrapper<E>
      extends PersistentCollectionWrapper<E, List<E>, PersistentListWrapper<E>>
      implements PersistentList<E> {
   
   private final boolean addLast;

   PersistentListWrapper(List<E> list) {
      this(true, list);
   }
   
   PersistentListWrapper(boolean addLast, List<E> list) {
      super(list);
      this.addLast = addLast;
   }

   @Override
   protected List<E> copy(List<E> original) {
      return new ArrayList<E>(original);
   }

   @Override
   protected PersistentListWrapper<E> wrapPersistent(List<E> list) {
      return new PersistentListWrapper<E>(list);
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
   public E first() {
      try {
         return collection.get(0);
      } catch (IndexOutOfBoundsException e) {
         throw new NoSuchElementException();
      }
   }

   @Override
   public PersistentListWrapper<E> add(E e) {
      if (addLast) {
         return super.add(e);
      } else {
         return addFirst(e);
      }
   }

   @Override
   public PersistentListWrapper<E> addAll(Iterable<? extends E> items) {
      if (addLast) {
         return super.addAll(items);
      } else {
         return addAll(0, items);
      }
   }

   @Override
   public PersistentListWrapper<E> add(int i, E e) {
      List<E> newList = new ArrayList<E>(collection);
      newList.add(i, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> addAll(int i, Iterable<? extends E> items) {
      List<E> newList = new ArrayList<E>(collection);
      newList.addAll(i, Iterables.snapshot(items));
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> addFirst(E e) {
      List<E> newList = new ArrayList<E>(collection);
      newList.add(0, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> addLast(E e) {
      List<E> newList = new ArrayList<E>(collection);
      newList.add(e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> set(int i, E e) {
      List<E> newList = new ArrayList<E>(collection);
      newList.set(i, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> remove(int i) {
      List<E> newList = new ArrayList<E>(collection);
      newList.remove(i);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> subList(int from, int to) {
      return new PersistentListWrapper<E>(addLast, collection.subList(from, to));
   }

   @Override
   public PersistentListWrapper<E> rest() {
      return isEmpty() ? this : new PersistentListWrapper<E>(addLast, collection.subList(1, size()));
   }

   @Override
   public PersistentList<E> clear() {
      return new PersistentListWrapper<>(new ArrayList<>(0));
   }
}
