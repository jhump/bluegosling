package com.apriori.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//TODO: implement me! (don't forget serialization and cloning)
//TODO: tests
public class RandomAccessArrayBlockingDeque<E> extends RandomAccessArrayDeque<E>
      implements BlockingDeque<E> {

   private transient Lock lock = new ReentrantLock();
   private transient int capacity; // TODO: move this to super-class?
   private transient Condition notEmpty = lock.newCondition();
   private transient Condition notFull = lock.newCondition();
   
   @Override
   public int remainingCapacity() {
      lock.lock();
      try {
         return capacity - size;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public int drainTo(Collection<? super E> c) {
      lock.lock();
      try {
         // TODO Auto-generated method stub
         return 0;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public int drainTo(Collection<? super E> c, int maxElements) {
      lock.lock();
      try {
         // TODO Auto-generated method stub
         return 0;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean isEmpty() {
      lock.lock();
      try {
         return size == 0;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public Object[] toArray() {
      // make a "close enough" copy using weakly-consistent iterator
      ArrayList<E> list = new ArrayList<E>(size());
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         list.add(iter.next());
      }
      return list.toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      // make a "close enough" copy using weakly-consistent iterator
      ArrayList<E> list = new ArrayList<E>(size());
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         list.add(iter.next());
      }
      return list.toArray(a);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      for (E e : c) {
         addLast(e);
      }
      return true;
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      boolean ret = false;
      for (Object o : c) {
         if (removeFirstOccurrence(o)) {
            ret = true;
         }
      }
      return ret;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void clear() {
      lock.lock();
      try {
         super.clear();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E removeFirst() {
      lock.lock();
      try {
         return super.removeFirst();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E removeLast() {
      lock.lock();
      try {
         return super.removeLast();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E pollFirst() {
      lock.lock();
      try {
         return super.pollFirst();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E pollLast() {
      lock.lock();
      try {
         return super.pollLast();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E getFirst() {
      lock.lock();
      try {
         return super.getFirst();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E getLast() {
      lock.lock();
      try {
         return super.getLast();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E peekFirst() {
      lock.lock();
      try {
         return super.peekFirst();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E peekLast() {
      lock.lock();
      try {
         return super.peekLast();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E pop() {
      lock.lock();
      try {
         return super.pop();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public Iterator<E> descendingIterator() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
      lock.lock();
      try {
         boolean ret = false;
         for (E e : c) {
            if (size == capacity) {
               throw new IllegalStateException("deque is full");
            }
            super.add(index++, e);
            ret = true;
         }
         return ret;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E get(int index) {
      lock.lock();
      try {
         return super.get(index);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E set(int index, E element) {
      lock.lock();
      try {
         return super.set(index, element);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void add(int index, E element) {
      lock.lock();
      try {
         super.add(index, element);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E remove(int index) {
      lock.lock();
      try {
         return super.remove(index);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public int indexOf(Object o) {
      lock.lock();
      try {
         return super.indexOf(o);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public int lastIndexOf(Object o) {
      lock.lock();
      try {
         return super.lastIndexOf(o);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public ListIterator<E> listIterator() {
      // TODO Auto-generated method stub
      return null;
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
      lock.lock();
      try {
         super.addFirst(e);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void addLast(E e) {
      lock.lock();
      try {
         super.addLast(e);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offerFirst(E e) {
      lock.lock();
      try {
         return super.offerFirst(e);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offerLast(E e) {
      lock.lock();
      try {
         return super.offerLast(e);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void putFirst(E e) throws InterruptedException {
      lock.lock();
      try {
         notFull.await();
         super.offerFirst(e);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void putLast(E e) throws InterruptedException {
      lock.lock();
      try {
         notFull.await();
         super.offerLast(e);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
      lock.lock();
      try {
         if (notFull.await(timeout, unit)) {
            super.offerFirst(e);
            return true;
         }
         return false;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
      lock.lock();
      try {
         if (notFull.await(timeout, unit)) {
            super.offerLast(e);
            return true;
         }
         return false;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E takeFirst() throws InterruptedException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public E takeLast() throws InterruptedException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean removeFirstOccurrence(Object o) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean removeLastOccurrence(Object o) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean add(E e) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean offer(E e) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void put(E e) throws InterruptedException {
      // TODO Auto-generated method stub
      
   }

   @Override
   public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public E remove() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public E poll() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public E take() throws InterruptedException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public E poll(long timeout, TimeUnit unit) throws InterruptedException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public E element() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public E peek() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean remove(Object o) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean contains(Object o) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public int size() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public Iterator<E> iterator() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void push(E e) {
      // TODO Auto-generated method stub
      
   }

}
