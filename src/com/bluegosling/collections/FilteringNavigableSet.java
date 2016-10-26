package com.bluegosling.collections;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.function.Predicate;

//TODO: javadoc
//TODO: tests
public class FilteringNavigableSet<E> extends FilteringSortedSet<E> implements NavigableSet<E> {

   public FilteringNavigableSet(NavigableSet<E> set, Predicate<? super E> predicate) {
      super(set, predicate);
   }

   @Override
   protected NavigableSet<E> internal() {
      return (NavigableSet<E>) super.internal();
   }
   
   @Override
   public E lower(E e) {
      Iterator<E> iter =
            new FilteringIterator<E>(internal().headSet(e, false).descendingIterator(),
                  predicate());
      return iter.hasNext() ? iter.next() : null;
   }

   @Override
   public E floor(E e) {
      Iterator<E> iter =
            new FilteringIterator<E>(internal().headSet(e, true).descendingIterator(), predicate());
      return iter.hasNext() ? iter.next() : null;
   }

   @Override
   public E ceiling(E e) {
      Iterator<E> iter =
            new FilteringIterator<E>(internal().tailSet(e, true).iterator(), predicate());
      return iter.hasNext() ? iter.next() : null;
   }

   @Override
   public E higher(E e) {
      Iterator<E> iter =
            new FilteringIterator<E>(internal().tailSet(e, false).iterator(), predicate());
      return iter.hasNext() ? iter.next() : null;
   }

   @Override
   public E last() {
      return descendingIterator().next();
   }

   @Override
   public E pollFirst() {
      E first = first();
      boolean removed = internal().remove(first);
      assert removed;
      return first;
   }

   @Override
   public E pollLast() {
      E last = last();
      boolean removed = internal().remove(last);
      assert removed;
      return last;
   }

   @Override
   public NavigableSet<E> descendingSet() {
      return new DescendingSet<>(this);
   }

   @Override
   public Iterator<E> descendingIterator() {
      return new FilteringIterator<>(internal().descendingIterator(), predicate());
   }

   @Override
   public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
         boolean toInclusive) {
      return new FilteringNavigableSet<>(
            internal().subSet(fromElement, fromInclusive, toElement, toInclusive),
            predicate());
   }

   @Override
   public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      return new FilteringNavigableSet<>(internal().headSet(toElement, inclusive), predicate());
   }

   @Override
   public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      return new FilteringNavigableSet<>(internal().tailSet(fromElement, inclusive), predicate());
   }

   @Override
   public NavigableSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
   }

   @Override
   public NavigableSet<E> headSet(E toElement) {
      return headSet(toElement, false);
   }

   @Override
   public NavigableSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
   }
}
