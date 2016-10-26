package com.bluegosling.collections.sets;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import com.bluegosling.collections.CollectionUtils;
import com.bluegosling.collections.MoreIterators;

/**
 * An implementation of {@link List} that delegates to an instance of {@link RandomAccessSet}. This
 * can be conveniently used to implement {@link RandomAccessSet#asList()}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> the type of element in the list
 */
public class RandomAccessSetList<E> implements List<E>, RandomAccess {
   
   private final RandomAccessSet<E> set;
   
   public RandomAccessSetList(RandomAccessSet<E> set) {
      this.set = set;
   }

   @Override
   public int size() {
      return set.size();
   }

   @Override
   public boolean isEmpty() {
      return set.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return set.contains(o);
   }

   @Override
   public Iterator<E> iterator() {
      return set.iterator();
   }

   @Override
   public Object[] toArray() {
      return set.toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return set.toArray(a);
   }

   @Override
   public boolean add(E e) {
      throw new UnsupportedOperationException("add");
   }

   @Override
   public boolean remove(Object o) {
      return set.remove(o);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return set.containsAll(c);
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      throw new UnsupportedOperationException("addAll");
   }

   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
      throw new UnsupportedOperationException("addAll");
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      return set.removeAll(c);
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      return set.retainAll(c);
   }

   @Override
   public void clear() {
      set.clear();
   }

   @Override
   public E get(int index) {
      return set.get(index);
   }

   @Override
   public E set(int index, E element) {
      throw new UnsupportedOperationException("set");
   }

   @Override
   public void add(int index, E element) {
      throw new UnsupportedOperationException("add");
   }

   @Override
   public E remove(int index) {
      return set.remove(index);
   }

   @Override
   public int indexOf(Object o) {
      return set.indexOf(o);
   }

   @Override
   public int lastIndexOf(Object o) {
      return CollectionUtils.findObject(o, MoreIterators.reverseListIterator(listIterator(size())));      
   }

   @Override
   public ListIterator<E> listIterator() {
      return set.listIterator();
   }

   @Override
   public ListIterator<E> listIterator(int index) {
      return set.listIterator(index);
   }

   @Override
   public List<E> subList(int fromIndex, int toIndex) {
      return new RandomAccessSetList<E>(set.subSetByIndices(fromIndex,  toIndex));
   }
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this, o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
   
   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
}
