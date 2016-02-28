package com.bluegosling.collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.function.Predicate;

//TODO: javadoc
//TODO: tests
public class FilteringSortedSet<E> extends FilteringSet<E> implements SortedSet<E> {

   public FilteringSortedSet(SortedSet<E> set, Predicate<? super E> predicate) {
      super(set, predicate);
   }

   @Override
   protected SortedSet<E> internal() {
      return (SortedSet<E>) super.internal();
   }
   
   @Override
   public SortedSet<E> capture() {
      return Collections.unmodifiableSortedSet(new SortedArraySet<>(this));
   }
   
   @Override
   public Comparator<? super E> comparator() {
      return internal().comparator();
   }

   @Override
   public SortedSet<E> subSet(E fromElement, E toElement) {
      return new FilteringSortedSet<E>(internal().subSet(fromElement, toElement), predicate());
   }

   @Override
   public SortedSet<E> headSet(E toElement) {
      return new FilteringSortedSet<E>(internal().headSet(toElement), predicate());
   }

   @Override
   public SortedSet<E> tailSet(E fromElement) {
      return new FilteringSortedSet<E>(internal().tailSet(fromElement), predicate());
   }

   @Override
   public E first() {
      return iterator().next();
   }

   @Override
   public E last() {
      E e = internal().last();
      while (true) {
         if (predicate().test(e)) {
            return e;
         }
         // get next highest value and test that
         e = internal().headSet(e).last();
      }
   }
}
