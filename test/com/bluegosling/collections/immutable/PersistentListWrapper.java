package com.bluegosling.collections.immutable;

import com.bluegosling.collections.MoreIterables;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

/**
 * A wrapper that adapts a {@link List} to the {@link PersistentList} interface.
 *
 * @param <E> the type of element in the list
 * 
 * @see PersistentCollectionWrapper
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class PersistentListWrapper<E>
extends PersistentCollectionWrapper<E, List<E>, PersistentListWrapper<E>>
implements PersistentList<E> {
   
   private final boolean addLast;

   public PersistentListWrapper(List<E> list) {
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
   public ListIterator<E> listIterator() {
      return MoreIterables.unmodifiableIterator(collection.listIterator());
   }

   @Override
   public ListIterator<E> listIterator(int index) {
      return MoreIterables.unmodifiableIterator(collection.listIterator(index));
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
   public PersistentListWrapper<E> with(E e) {
      if (addLast) {
         return withTail(e);
      } else {
         return withHead(e);
      }
   }

   @Override
   public PersistentListWrapper<E> withAll(Iterable<? extends E> items) {
      if (addLast) {
         return super.withAll(items);
      } else {
         return withAll(0, items);
      }
   }

   @Override
   public PersistentListWrapper<E> with(int i, E e) {
      List<E> newList = new ArrayList<E>(collection);
      newList.add(i, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> withAll(int i, Iterable<? extends E> items) {
      List<E> newList = new ArrayList<E>(collection);
      newList.addAll(i, MoreIterables.snapshot(items));
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> withHead(E e) {
      List<E> newList = new ArrayList<E>(collection);
      newList.add(0, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> withTail(E e) {
      List<E> newList = new ArrayList<E>(collection);
      newList.add(e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> withReplacement(int i, E e) {
      List<E> newList = new ArrayList<E>(collection);
      newList.set(i, e);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> withReplacements(UnaryOperator<E> operator) {
      List<E> newList = new ArrayList<E>(collection);
      newList.replaceAll(operator);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> without(int i) {
      List<E> newList = new ArrayList<E>(collection);
      newList.remove(i);
      return new PersistentListWrapper<E>(addLast, newList);
   }

   @Override
   public PersistentListWrapper<E> subList(int from, int to) {
      return new PersistentListWrapper<E>(addLast, collection.subList(from, to));
   }

   @Override
   public PersistentList<E> removeAll() {
      return new PersistentListWrapper<>(new ArrayList<>(0));
   }
}
