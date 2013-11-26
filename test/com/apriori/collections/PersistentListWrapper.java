package com.apriori.collections;

import java.util.ArrayList;
import java.util.List;

import static com.apriori.collections.ImmutableCollectionWrapper.fromIterable;

// TODO: javadoc
class PersistentListWrapper<E>
      extends PersistentCollectionWrapper<E, List<E>, ImmutableListWrapper<E>, PersistentListWrapper<E>>
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
   protected ImmutableListWrapper<E> wrapImmutable(List<E> list) {
      return new ImmutableListWrapper<E>(list);
   }

   @Override
   protected PersistentListWrapper<E> wrapPersistent(List<E> list) {
      return new PersistentListWrapper<E>(list);
   }

   @Override
   public E get(int i) {
      return wrapper.get(i);
   }

   @Override
   public int indexOf(Object o) {
      return wrapper.indexOf(o);
   }

   @Override
   public int lastIndexOf(Object o) {
      return wrapper.lastIndexOf(o);
   }

   @Override
   public E first() {
      return wrapper.first();
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
      List<E> newList = new ArrayList<E>(wrapped());
      newList.add(i, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> addAll(int i, Iterable<? extends E> items) {
      List<E> newList = new ArrayList<E>(wrapped());
      newList.addAll(i, fromIterable(items));
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> addFirst(E e) {
      List<E> newList = new ArrayList<E>(wrapped());
      newList.add(0, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> addLast(E e) {
      List<E> newList = new ArrayList<E>(wrapped());
      newList.add(e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> set(int i, E e) {
      List<E> newList = new ArrayList<E>(wrapped());
      newList.set(i, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> remove(int i) {
      List<E> newList = new ArrayList<E>(wrapped());
      newList.remove(i);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> subList(int from, int to) {
      return new PersistentListWrapper<E>(addLast, wrapped().subList(from, to));
   }

   @Override
   public PersistentListWrapper<E> rest() {
      return isEmpty() ? this : new PersistentListWrapper<E>(addLast, wrapped().subList(1, size()));
   }
}
