package com.bluegosling.collections.queues;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.RandomAccess;

import com.bluegosling.collections.CollectionUtils;

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

   static final int DEFAULT_SIZE = 16;
   private static final int MINIMUM_SIZE = 2;
   
   transient int head;
   final transient int capacity;
   transient int size;
   transient Object data[];
   
   public RandomAccessArrayDeque() {
      data = new Object[DEFAULT_SIZE];
      capacity = 0;
   }

   public RandomAccessArrayDeque(Collection<? extends E> coll) {
      data = new Object[Math.min(MINIMUM_SIZE, coll.size())];
      capacity = 0;
      addAll(coll);
   }

   public RandomAccessArrayDeque(int initialCapacity, int maximumCapacity) {
      if (maximumCapacity <= 0) {
         throw new IllegalArgumentException();
      }
      if (initialCapacity < 0) {
         throw new IllegalArgumentException();
      }
      data = new Object[Math.min(MINIMUM_SIZE, initialCapacity)];
      capacity = maximumCapacity;
   }

   @Override
   public boolean isEmpty() {
      return size == 0;
   }
   
   @Override
   public Object[] toArray() {
      Object d[] = data;
      int h = head, s = size, l = d.length;
      Object ret[] = new Object[s];
      for (int i = 0, c = h; i < s; i++) {
         ret[i] = d[c];
         if (++c == l) {
            c = 0;
         }
      }
      return ret;
   }
   
   @SuppressWarnings("unchecked")
   @Override
   public <T> T[] toArray(T[] a) {
      Object d[] = data;
      int h = head, s = size, l = d.length;
      Object ret[] =
            a.length >= s ? a : (Object[]) Array.newInstance(a.getClass().getComponentType(), s);
      for (int i = 0, c = h; i < s; i++) {
         ret[i] = d[c];
         if (++c == l) {
            c = 0;
         }
      }
      if (ret.length > s) {
         ret[s] = null;
      }
      return (T[]) ret;
   }
   
   @Override
   protected void removeRange(int fromIndex, int toIndex) {
      if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
         throw new IllegalArgumentException("Invalid indices: " + fromIndex + " -> " + toIndex);
      }
      if (fromIndex == toIndex) {
         // nothing to do
         return;
      }
      int h = head, s = size, t = h + s, l = data.length;
      if (fromIndex == 0 && toIndex == s) {
         // completely cleared
         if (t > data.length) {
            Arrays.fill(data, h, l, null);
            Arrays.fill(data, 0, t - l, null);
         } else {
            Arrays.fill(data, h, t, null);
         }
         size = head = 0;
      } else {
         int trailingCount = s - toIndex;
         int numRemoved = toIndex - fromIndex;
         if (trailingCount > fromIndex) {
            // fewer items at the head to shift
            int newH = h + numRemoved;
            if (newH >= l) {
               newH -= l;
            }
            if (newH + fromIndex <= s) {
               // new destination range is contiguous
               if (h + fromIndex <= s) {
                  // source range is, too! easy
                  System.arraycopy(data, h, data, newH, fromIndex);
               } else {
                  int prefix = s - h;
                  System.arraycopy(data, 0, data, newH + prefix, fromIndex - prefix);
                  System.arraycopy(data, h, data, newH, s - h);
               }
            } else {
               // destination range is *not* contiguous
               if (h + fromIndex <= s) {
                  // source range is contiguous
                  int prefix = size - newH;
                  System.arraycopy(data, h + prefix, data, 0, fromIndex - prefix);
                  System.arraycopy(data, h, data, newH, prefix);
               } else {
                  assert h < newH;
                  int d = newH - h;
                  int prefix = s - newH;
                  int suffix = h + fromIndex - s;
                  System.arraycopy(data, 0, data, d, suffix);
                  System.arraycopy(data, s - d, data, 0, d);
                  System.arraycopy(data, h, data, newH, prefix);
               }
            }
            // nullify removed data
            if (h < newH) {
               Arrays.fill(data, h, newH, null);
            } else {
               Arrays.fill(data, h, s, null);
               Arrays.fill(data, 0, newH, null);
            }
         } else {
            // fewer items at the tail to shift
            int newT = t - numRemoved;
            if (newT < 0) {
               newT += l;
            }
            if (newT - trailingCount >= 0) {
               // new destination range is contiguous
               // TODO

            } else {
               // source range is contiguous
               // TODO
               
            }
            // nullify removed data
            if (newT < t) {
               Arrays.fill(data, newT, t, null);
            } else {
               Arrays.fill(data, newT, s, null);
               Arrays.fill(data, 0, t, null);
            }
            
         }
         size -= numRemoved;
      }
      modCount++;
   }
   
   @Override
   public void clear() {
      int h = head, s = size, l = data.length, t = h + s;
      if (t > l) {
         Arrays.fill(data, h, l, null);
         Arrays.fill(data, 0, t - l, null);
      } else {
         Arrays.fill(data, h, t, null);
      }
      size = head = 0;
      modCount++;
   }
   
   @Override
   public boolean addAll(Collection<? extends E> c) {
      if (c.isEmpty()) {
         return false;
      }
      Object[] a = c.toArray();
      maybeGrowArray(a.length);
      // TODO Auto-generated method stub
      modCount++;
      return true;
   }
   
   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
      if (c.isEmpty()) {
         return false;
      }
      if (size + c.size() > capacity && capacity != 0) {
         throw new IllegalStateException("deque cannot fit given collection");
      }
      Object[] a = c.toArray();
      maybeGrowArray(a.length);
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
      int ret = head + index;
      int len = data.length;
      if (ret >= len) {
         ret -= len;
         assert ret < len;
      }
      return ret;
   }
   
   @Override
   @SuppressWarnings("unchecked") // we know it's an E because we put it there
   public E get(int index) {
      return (E) data[computeIndex(index, false)];
   }
   
   @Override
   public E set(int index, E element) {
      int arrayIndex = computeIndex(index, false);
      
      @SuppressWarnings("unchecked") // we know it's an E because we put it there
      E ret = (E) data[arrayIndex];
      
      data[arrayIndex] = element;
      return ret;
   }
   
   private void maybeGrowArray(int capacityNeeded) {
      int l = data.length, s = size, c = capacity;
      int neededSize = s + capacityNeeded;
      if (neededSize > c && c != 0) {
         throw new IllegalStateException("deque cannot fit given elements");
      }
      if (neededSize <= l) {
         return; // nothing to do
      }
      int newLen = l * 2; // try doubling
      if (newLen < l) {
         // overflow
         newLen = Integer.MAX_VALUE;
      }
      if (newLen < neededSize) {
         newLen = neededSize;
      }
      if (newLen > c && c != 0) { 
         newLen = c;
      }
      Object newData[] = new Object[newLen];
      int h = head, t = tail();
      if (h < t) {
         System.arraycopy(data, h, newData, 0, s);
      } else {
         int prefix = l - h;
         System.arraycopy(data, h, newData, 0, prefix);
         System.arraycopy(data, 0, newData, prefix, s - prefix);
      }
      data = newData;
      head = 0;
      modCount++;
   }
   
   @Override
   public void add(int index, E element) {
      // TODO Auto-generated method stub
      maybeGrowArray(1);
      size++;
      modCount++;
   }
   
   @Override
   public E remove(int index) {
      @SuppressWarnings("unchecked")
      E ret = (E) data[computeIndex(index, false)];
      removeRange(index, index + 1);
      return ret;
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
      maybeGrowArray(1);
      int h = head - 1;
      if (h < 0) {
         h += data.length;
      }
      data[h] = e;
      head = h;
      size++;
      modCount++;
      return true;
   }
   
   @Override
   public boolean offerLast(E e) {
      if (size == capacity && capacity != 0) {
         return false;
      }
      maybeGrowArray(1);
      int t = head + size, l = data.length;
      if (t >= l) {
         t -= l;
      }
      data[t] = e;
      size++;
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
      int h = head, l = data.length;
      @SuppressWarnings("unchecked")
      E ret = (E) data[h++];
      
      if (h >= l) {
         h -= l;
      }
      head = h;
      size--;
      modCount++;
      return ret;
   }
   
   private int tail() {
      int tail = head + size;
      int len = data.length;
      if (tail >= len) {
         tail -= len;
         assert tail < len;
      }
      return tail;
   }
   
   @Override
   public E pollLast() {
      if (size == 0) {
         return null;
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[tail()];
      size--;
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
      return size == 0 ? null : (E) data[tail()];
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
   public Iterator<E> iterator() {
      return listIterator(0);
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
