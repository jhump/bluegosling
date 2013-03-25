package com.apriori.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

//TODO: implement me! (don't forget serialization and cloning)
//TODO: tests
public class RandomAccessArrayDeque<T> implements Deque<T>, List<T>, RandomAccess {

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
   public boolean addAll(Collection<? extends T> c) {
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
   public boolean addAll(int index, Collection<? extends T> c) {
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
   public T get(int index) {
      return (T) data[computeIndex(index, false)];
   }
   
   @Override
   public T set(int index, T element) {
      int arrayIndex = computeIndex(index, false);
      
      @SuppressWarnings("unchecked") // we know it's a T because we put it there
      T ret = (T) data[arrayIndex];
      
      data[arrayIndex] = element;
      return ret;
   }
   
   @Override
   public void add(int index, T element) {
      // TODO Auto-generated method stub
      modCount++;
   }
   
   @Override
   public T remove(int index) {
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
   public ListIterator<T> listIterator() {
      return listIterator(0);
   }
   
   @Override
   public ListIterator<T> listIterator(int index) {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   public List<T> subList(int fromIndex, int toIndex) {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   public void addFirst(T e) {
      if (!offerFirst(e)) {
         throw new IllegalStateException("deque is full");
      }
   }
   
   @Override
   public void addLast(T e) {
      if (!offerLast(e)) {
         throw new IllegalStateException("deque is full");
      }
   }
   
   @Override
   public boolean offerFirst(T e) {
      // TODO Auto-generated method stub
      modCount++;
      return false;
   }
   
   @Override
   public boolean offerLast(T e) {
      // TODO Auto-generated method stub
      modCount++;
      return false;
   }
   
   @Override
   public T removeFirst() {
      if (size == 0) {
         throw new IllegalStateException("deque is empty");
      }
      return pollFirst();
   }
   
   @Override
   public T removeLast() {
      if (size == 0) {
         throw new IllegalStateException("deque is empty");
      }
      return pollLast();
   }
   
   @Override
   public T pollFirst() {
      // TODO Auto-generated method stub
      modCount++;
      return null;
   }
   
   @Override
   public T pollLast() {
      // TODO Auto-generated method stub
      modCount++;
      return null;
   }
   
   @Override
   public T getFirst() {
      if (size == 0) {
         throw new IllegalStateException("deque is empty");
      }
      return peekFirst();
   }
   
   @Override
   public T getLast() {
      if (size == 0) {
         throw new IllegalStateException("deque is empty");
      }
      return peekLast();
   }
   
   @Override
   @SuppressWarnings("unchecked") // we know it's a T because we put it there
   public T peekFirst() {
      return size == 0 ? null : (T) data[head];
   }
   
   @Override
   @SuppressWarnings("unchecked") // we know it's a T because we put it there
   public T peekLast() {
      return size == 0 ? null : (T) data[tail];
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
   public boolean add(T e) {
      addLast(e);
      return true;
   }
   
   @Override
   public boolean offer(T e) {
      return offerLast(e);
   }
   
   @Override
   public T remove() {
      return removeFirst();
   }
   
   @Override
   public T poll() {
      return pollFirst();
   }
   
   @Override
   public T element() {
      return getFirst();
   }
   
   @Override
   public T peek() {
      return peekFirst();
   }
   
   @Override
   public void push(T e) {
      addFirst(e);
   }
   
   @Override
   public T pop() {
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
   public Iterator<T> iterator() {
      return listIterator();
   }
   
   @Override
   public Iterator<T> descendingIterator() {
      final ListIterator<T> listIter = listIterator(size());
      return new Iterator<T>() {

         @Override
         public boolean hasNext() {
            return listIter.hasPrevious();
         }

         @Override
         public T next() {
            return listIter.previous();
         }

         @Override
         public void remove() {
            listIter.remove();
         }
      };
   }
}
