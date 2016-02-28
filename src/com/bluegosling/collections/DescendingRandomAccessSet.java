package com.bluegosling.collections;

import java.util.List;
import java.util.ListIterator;

// TODO: javadoc
// TODO: test
// TODO: check indices and give errors (otherwise, exception messages refer to adjusted index)
class DescendingRandomAccessSet<E> extends DescendingSet<E> implements RandomAccessNavigableSet<E> {

   DescendingRandomAccessSet(RandomAccessNavigableSet<E> base) {
      super(base);
   }
   
   RandomAccessNavigableSet<E> base() {
      return (RandomAccessNavigableSet<E>) base;
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
      return CollectionUtils.reverseIterator(base().listIterator(size()));
   }

   @Override
   public ListIterator<E> listIterator(int index) {
      return CollectionUtils.reverseIterator(base().listIterator(size() - index));
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
      return new RandomAccessSetList<E>(this);
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
