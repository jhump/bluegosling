package com.bluegosling.collections.immutable;

import com.bluegosling.collections.MoreIterables;
import com.google.common.collect.Iterators;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * An abstract base class for implementing immutable lists. This overrides all mutable methods
 * with {@code final} implementations that throw {@link UnsupportedOperationException}.
 * 
 * <p>As with its superclass, {@link AbstractList}, the implementations for many operations rely
 * on random access. If implementing an immutable list based on a linked structure (a la
 * {@link LinkedList}), consider sub-classing {@link AbstractLinkedImmutableList} instead.
 *
 * @param <E> the type of element in the collection
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AbstractLinkedImmutableList
 */
public abstract class AbstractImmutableList<E> extends AbstractList<E> {

   @Deprecated
   @Override
   public final boolean add(E e) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean remove(Object o) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean removeIf(Predicate<? super E> filter) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean addAll(Collection<? extends E> c) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final void clear() {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final void add(int index, E element) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final E set(int index, E element) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final E remove(int index) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean addAll(int index, Collection<? extends E> c) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   protected final void removeRange(int fromIndex, int toIndex) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final void replaceAll(UnaryOperator<E> operator) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final void sort(Comparator<? super E> c) {
      throw new UnsupportedOperationException();
   }
   
   @Override
   public List<E> subList(int fromIndex, int toIndex) {
      // wrap the returned sublist, to ensure that no mutable operations are inadvertently exposed
      return Collections.unmodifiableList(super.subList(fromIndex, toIndex));
   }
   
   @Override
   public Iterator<E> iterator() {
      return Iterators.unmodifiableIterator(super.iterator());
   }

   @Override
   public ListIterator<E> listIterator() {
      return MoreIterables.unmodifiableIterator(super.listIterator());
   }

   @Override
   public ListIterator<E> listIterator(int pos) {
      return MoreIterables.unmodifiableIterator(super.listIterator(pos));
   }
}
