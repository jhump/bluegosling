package com.apriori.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * A double-ended queue that is backed by an array and, in addition to standard {@link Deque}
 * operations, also exposes random access operations on its contents.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the deque
 */
//TODO: implement me! (don't forget serialization and cloning)
//TODO: tests
public class RandomAccessArrayDeque<E> implements Deque<E>, List<E>, RandomAccess {

   private transient int head;
   private transient int tail;
   transient int size;
   private transient Object data[];
   transient int modCount;
   
   @Override
   public boolean isEmpty() {
      return size == 0;
   }
   
   @Override
   public Object[] toArray() {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   public <T> T[] toArray(T[] a) {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   public boolean containsAll(Collection<?> c) {
      // TODO Auto-generated method stub
      return false;
   }
   
   @Override
   public boolean addAll(Collection<? extends E> c) {
      return addAll(size, c);
   }
   
   @Override
   public boolean removeAll(Collection<?> c) {
      // TODO Auto-generated method stub
      return false;
   }
   
   @Override
   public boolean retainAll(Collection<?> c) {
      // TODO Auto-generated method stub
      return false;
   }
   
   @Override
   public void clear() {
      head = tail = size = 0;
      Arrays.fill(data,  null);
      modCount++;
   }
   
   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
      if (c.isEmpty()) {
         return false;
      }
      // TODO Auto-generated method stub
      modCount++;
      return true;
   }
   
   private int computeIndex(int index, boolean includeLast) {
      if (index < 0) {
         throw new IndexOutOfBoundsException("" + index + " < 0");
      } else if (includeLast) {
         if (index > size) {
            throw new IndexOutOfBoundsException("" + index + " > " + size);
         }
      } else if (index >= size) {
         throw new IndexOutOfBoundsException("" + index + " >= " + size);
      }
      return (head + index) % data.length;
   }
   
   @Override
   @SuppressWarnings("unchecked") // we know it's a T because we put it there
   public E get(int index) {
      return (E) data[computeIndex(index, false)];
   }
   
   @Override
   public E set(int index, E element) {
      int arrayIndex = computeIndex(index, false);
      
      @SuppressWarnings("unchecked") // we know it's a T because we put it there
      E ret = (E) data[arrayIndex];
      
      data[arrayIndex] = element;
      return ret;
   }
   
   @Override
   public void add(int index, E element) {
      // TODO Auto-generated method stub
      modCount++;
   }
   
   @Override
   public E remove(int index) {
      // TODO Auto-generated method stub
      modCount++;
      return null;
   }
   
   @Override
   public int indexOf(Object o) {
      // TODO Auto-generated method stub
      return 0;
   }
   
   @Override
   public int lastIndexOf(Object o) {
      // TODO Auto-generated method stub
      return 0;
   }
   
   @Override
   public ListIterator<E> listIterator() {
      return listIterator(0);
   }
   
   @Override
   public ListIterator<E> listIterator(int index) {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   public List<E> subList(int fromIndex, int toIndex) {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   public void addFirst(E e) {
      if (!offerFirst(e)) {
         throw new IllegalStateException("deque is full");
      }
   }
   
   @Override
   public void addLast(E e) {
      if (!offerLast(e)) {
         throw new IllegalStateException("deque is full");
      }
   }
   
   @Override
   public boolean offerFirst(E e) {
      // TODO Auto-generated method stub
      modCount++;
      return false;
   }
   
   @Override
   public boolean offerLast(E e) {
      // TODO Auto-generated method stub
      modCount++;
      return false;
   }
   
   @Override
   public E removeFirst() {
      if (size == 0) {
         throw new IllegalStateException("deque is empty");
      }
      return pollFirst();
   }
   
   @Override
   public E removeLast() {
      if (size == 0) {
         throw new IllegalStateException("deque is empty");
      }
      return pollLast();
   }
   
   @Override
   public E pollFirst() {
      // TODO Auto-generated method stub
      modCount++;
      return null;
   }
   
   @Override
   public E pollLast() {
      // TODO Auto-generated method stub
      modCount++;
      return null;
   }
   
   @Override
   public E getFirst() {
      if (size == 0) {
         throw new IllegalStateException("deque is empty");
      }
      return peekFirst();
   }
   
   @Override
   public E getLast() {
      if (size == 0) {
         throw new IllegalStateException("deque is empty");
      }
      return peekLast();
   }
   
   @Override
   @SuppressWarnings("unchecked") // we know it's a T because we put it there
   public E peekFirst() {
      return size == 0 ? null : (E) data[head];
   }
   
   @Override
   @SuppressWarnings("unchecked") // we know it's a T because we put it there
   public E peekLast() {
      return size == 0 ? null : (E) data[tail];
   }
   
   @Override
   public boolean removeFirstOccurrence(Object o) {
      // TODO Auto-generated method stub
      modCount++;
      return false;
   }
   
   @Override
   public boolean removeLastOccurrence(Object o) {
      // TODO Auto-generated method stub
      modCount++;
      return false;
   }
   
   @Override
   public boolean add(E e) {
      addLast(e);
      return true;
   }
   
   @Override
   public boolean offer(E e) {
      return offerLast(e);
   }
   
   @Override
   public E remove() {
      return removeFirst();
   }
   
   @Override
   public E poll() {
      return pollFirst();
   }
   
   @Override
   public E element() {
      return getFirst();
   }
   
   @Override
   public E peek() {
      return peekFirst();
   }
   
   @Override
   public void push(E e) {
      addFirst(e);
   }
   
   @Override
   public E pop() {
      return removeFirst();
   }
   
   @Override
   public boolean remove(Object o) {
      return removeFirstOccurrence(o);
   }
   
   @Override
   public boolean contains(Object o) {
      // TODO Auto-generated method stub
      return false;
   }
   
   @Override
   public int size() {
      return size;
   }
   
   @Override
   public Iterator<E> iterator() {
      return listIterator();
   }
   
   @Override
   public Iterator<E> descendingIterator() {
      final ListIterator<E> listIter = listIterator(size());
      return new Iterator<E>() {

         @Override
         public boolean hasNext() {
            return listIter.hasPrevious();
         }

         @Override
         public E next() {
            return listIter.previous();
         }

         @Override
         public void remove() {
            listIter.remove();
         }
      };
   }
}
