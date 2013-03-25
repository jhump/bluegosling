package com.apriori.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;

/**
 * A {@link NavigableSet} that is a view of another set, but in opposite (descending) order.
 * Most operations are reversed. For example {@link #first()} returns the
 * {@linkplain SortedSet#last() last} item in the underlying set, {@link #iterator()} visits
 * elements in descending order (and {@link #descendingIterator()} visits them in ascending order!),
 * etc.
 * 
 * <p>This implementation is designed to be used to implement {@link NavigableSet#descendingSet()}.
 * The underlying set must implement all other interface methods <em>without</em> using this
 * implementation. For example, you can't use a {@code DescendingSet} to help you implement a
 * descending iterator since the {@code DescendingSet} relies on your implementation of
 * {@link NavigableSet#descendingIterator()} to implement its {@link #iterator()} method. 
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> the type of element in the set
 */
class DescendingSet<E> implements NavigableSet<E> {

   private final NavigableSet<E> base;

   /**
    * Constructs a new descending view of the specified set.
    *
    * @param base the underlying set
    */
   public DescendingSet(NavigableSet<E> base) {
      this.base = base;
   }
   
   @Override
   public Comparator<? super E> comparator() {
      final Comparator<? super E> comp = base.comparator();
      if (comp == null) {
         return null;
      }
      return new Comparator<E>() {
         @Override
         public int compare(E o1, E o2) {
            // reverse it
            return comp.compare(o2, o1);
         }
      };
   }

   @Override
   public E first() {
      return base.last();
   }

   @Override
   public E last() {
      return base.first();
   }

   @Override
   public boolean add(E e) {
      return base.add(e);
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      return base.addAll(c);
   }

   @Override
   public void clear() {
      base.clear();
   }

   @Override
   public boolean contains(Object o) {
      return base.contains(o);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return base.containsAll(c);
   }

   @Override
   public boolean isEmpty() {
      return base.isEmpty();
   }

   @Override
   public boolean remove(Object o) {
      return base.remove(o);
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      return base.removeAll(c);
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      return base.retainAll(c);
   }

   @Override
   public int size() {
      return base.size();
   }

   @Override
   public Object[] toArray() {
      Object ret[] = base.toArray();
      ArrayUtils.reverse(ret);
      return ret;
   }

   @Override
   public <T> T[] toArray(T[] a) {
      T ret[] = base.toArray(a);
      ArrayUtils.reverse(ret);
      return ret;
   }

   @Override
   public E ceiling(E e) {
      return base.floor(e);
   }

   @Override
   public Iterator<E> descendingIterator() {
      return base.iterator();
   }

   @Override
   public NavigableSet<E> descendingSet() {
      return base;
   }

   @Override
   public E floor(E e) {
      return base.ceiling(e);
   }

   @Override
   public NavigableSet<E> headSet(E toElement) {
      return headSet(toElement, false);
   }

   @Override
   public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      return new DescendingSet<E>(base.tailSet(toElement, inclusive));
   }

   @Override
   public E higher(E e) {
      return base.lower(e);
   }

   @Override
   public Iterator<E> iterator() {
      return base.descendingIterator();
   }

   @Override
   public E lower(E e) {
      return base.higher(e);
   }

   @Override
   public E pollFirst() {
      return base.pollLast();
   }

   @Override
   public E pollLast() {
      return base.pollFirst();
   }

   @Override
   public NavigableSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
   }

   @Override
   public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
         boolean toInclusive) {
      return new DescendingSet<E>(base.subSet(toElement, toInclusive, fromElement,
            fromInclusive));
   }

   @Override
   public NavigableSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
   }

   @Override
   public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      return new DescendingSet<E>(base.headSet(fromElement, inclusive));
   }
   
   @Override
   public boolean equals(Object o) {
      return base.equals(o);
   }
   
   @Override
   public int hashCode() {
      return base.hashCode();
   }
   
   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
}