package com.bluegosling.collections.sets;

import java.util.List;
import java.util.ListIterator;

import com.bluegosling.collections.DescendingSet;
import com.bluegosling.collections.MoreIterators;

// TODO: javadoc
// TODO: test
// TODO: check indices and give errors (otherwise, exception messages refer to adjusted index)
public class DescendingRandomAccessSet<E> extends DescendingSet<E>
implements RandomAccessNavigableSet<E> {

   public DescendingRandomAccessSet(RandomAccessNavigableSet<E> base) {
      super(base);
   }
   
   @Override protected RandomAccessNavigableSet<E> base() {
      return (RandomAccessNavigableSet<E>) super.base();
   }

   int convertIndex(int index) {
      return size() - index - 1;
   }
   
   @Override
   public E get(int index) {
      return base().get(convertIndex(index));
   }

   @Override
   public int indexOf(Object o) {
      int index = base().indexOf(o);
      return index == -1 ? -1 : convertIndex(index);
   }

   @Override
   public ListIterator<E> listIterator() {
      return MoreIterators.reverseListIterator(base().listIterator(size()));
   }

   @Override
   public ListIterator<E> listIterator(int index) {
      return MoreIterators.reverseListIterator(base().listIterator(size() - index));
   }

   @Override
   public E remove(int index) {
      return base().remove(convertIndex(index));
   }

   @Override
   public RandomAccessNavigableSet<E> subSetByIndices(int fromIndex, int toIndex) {
      return new DescendingRandomAccessSet<E>(base().subSetByIndices(size() - toIndex, size() - fromIndex));
   }

   @Override
   public RandomAccessNavigableSet<E> descendingSet() {
      return base();
   }
   
   @Override
   public List<E> asList() {
      return new RandomAccessSetList<>(this);
   }

   @Override
   public RandomAccessNavigableSet<E> headSet(E toElement) {
      return headSet(toElement, false);
   }

   @Override
   public RandomAccessNavigableSet<E> headSet(E toElement, boolean inclusive) {
      return new DescendingRandomAccessSet<E>(base().tailSet(toElement, inclusive));
   }
   
   @Override
   public RandomAccessNavigableSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
   }

   @Override
   public RandomAccessNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
         boolean toInclusive) {
      return new DescendingRandomAccessSet<E>(base().subSet(toElement, toInclusive, fromElement,
            fromInclusive));
   }

   @Override
   public RandomAccessNavigableSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
   }

   @Override
   public RandomAccessNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      return new DescendingRandomAccessSet<E>(base().headSet(fromElement, inclusive));
   }
}
