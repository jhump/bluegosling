package com.apriori.collections;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
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
//TODO: use/enforce capacity constraint
public class RandomAccessArrayDeque<E> extends AbstractList<E> implements Deque<E>, RandomAccess {

   private static final int DEFAULT_SIZE = 16;
   private static final int MINIMUM_SIZE = 2;
   
   transient int head;
   transient int tail;
   transient int capacity;
   transient int size;
   transient Object data[];
   
   public RandomAccessArrayDeque() {
      data = new Object[DEFAULT_SIZE];
   }

   public RandomAccessArrayDeque(Collection<? extends E> coll) {
      data = new Object[Math.min(MINIMUM_SIZE, coll.size())];
      addAll(coll);
   }

   public RandomAccessArrayDeque(int initialCapacity, int maximumCapacity) {
      if (maximumCapacity < 0) {
         throw new IllegalArgumentException();
      }
      if (initialCapacity < 0) {
         throw new IllegalArgumentException();
      }
      capacity = maximumCapacity;
      data = new Object[Math.min(MINIMUM_SIZE, initialCapacity)];
   }

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
   protected void removeRange(int fromIndex, int toIndex) {
      // TODO Auto-generated method stub
      modCount++;
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
      if (size == capacity && capacity != 0) {
         return false;
      }
      // TODO Auto-generated method stub
      modCount++;
      return false;
   }
   
   @Override
   public boolean offerLast(E e) {
      if (size == capacity && capacity != 0) {
         return false;
      }
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
      if (size == 0) {
         return null;
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[head];
      if (++head >= data.length) {
         head -= data.length;
      }
      modCount++;
      return ret;
   }
   
   @Override
   public E pollLast() {
      if (size == 0) {
         return null;
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[tail];
      if (--tail < 0) {
         tail += data.length;
      }
      modCount++;
      return ret;
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
      if (CollectionUtils.removeObject(o, iterator(), true)) {
         modCount++;
         return true;
      }
      return false;
   }
   
   @Override
   public boolean removeLastOccurrence(Object o) {
      if (CollectionUtils.removeObject(o, descendingIterator(), true)) {
         modCount++;
         return true;
      }
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
   public int size() {
      return size;
   }
   
   @Override
   public Iterator<E> descendingIterator() {
      ListIterator<E> listIter = listIterator(size());
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
