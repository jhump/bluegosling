package com.bluegosling.collections.queues;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link BlockingDeque} implementation based on a {@link RandomAccessArrayDeque}. Like its
 * super-class, it is based on a growable array and exposes random access operations on its
 * contents.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the deque
 */
//TODO: implement me! (don't forget serialization and cloning)
//TODO: tests
public class RandomAccessArrayBlockingDeque<E> extends RandomAccessArrayDeque<E>
      implements BlockingDeque<E> {

   private transient Lock lock = new ReentrantLock();
   private transient Condition notEmpty = lock.newCondition();
   private transient Condition notFull = lock.newCondition();
   
   public RandomAccessArrayBlockingDeque() {
      super();
   }

   public RandomAccessArrayBlockingDeque(Collection<? extends E> coll) {
      super(coll);
   }

   public RandomAccessArrayBlockingDeque(int initialCapacity, int maximumCapacity) {
      super(initialCapacity, maximumCapacity);
   }

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
   public int drainTo(Collection<? super E> coll) {
      lock.lock();
      try {
         Object d[] = data;
         int h = head, s = size, l = d.length;
         int i = 0;
         for (int c = h; i < s; i++) {
            @SuppressWarnings("unchecked")
            E e = (E) d[c];
            coll.add(e);
            if (++c == l) {
               c = 0;
            }
         }
         return i;
      } finally {
         if (size < capacity) {
            notFull.signalAll();
         }
         lock.unlock();
      }
   }

   @Override
   public int drainTo(Collection<? super E> coll, int maxElements) {
      lock.lock();
      try {
         Object d[] = data;
         int h = head, s = size, l = d.length;
         int i = 0;
         for (int c = h; i < s && i < maxElements; i++) {
            @SuppressWarnings("unchecked")
            E e = (E) d[c];
            coll.add(e);
            if (++c == l) {
               c = 0;
            }
         }
         return i;
      } finally {
         if (size < capacity) {
            notFull.signalAll();
         }
         lock.unlock();
      }
   }

   @Override
   public Object[] toArray() {
      lock.lock();
      try {
         return super.toArray();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public <T> T[] toArray(T[] a) {
      lock.lock();
      try {
         return super.toArray(a);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      lock.lock();
      try {
         return super.addAll(c);
      } finally {
         if (size > 0) {
            notEmpty.signalAll();
         }
         lock.unlock();
      }
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      lock.lock();
      try {
         return super.removeAll(c);
      } finally {
         if (size < capacity) {
            notFull.signalAll();
         }
         lock.unlock();
      }
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      lock.lock();
      try {
         return super.retainAll(c);
      } finally {
         if (size < capacity) {
            notFull.signalAll();
         }
         lock.unlock();
      }
   }

   @Override
   public void clear() {
      lock.lock();
      try {
         super.clear();
      } finally {
         if (size < capacity) {
            notFull.signalAll();
         }
         lock.unlock();
      }
   }

   @Override
   public E removeFirst() {
      lock.lock();
      try {
         // calls pollFirst, which signals notFull
         return super.removeFirst();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E removeLast() {
      lock.lock();
      try {
         // calls pollLast, which signals notFull
         return super.removeLast();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E pollFirst() {
      lock.lock();
      try {
         E ret = super.pollFirst();
         notFull.signal();
         return ret;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E pollLast() {
      lock.lock();
      try {
         E ret = super.pollLast();
         notFull.signal();
         return ret;
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
   public boolean addAll(int index, Collection<? extends E> c) {
      lock.lock();
      try {
         return super.addAll(index, c);
      } finally {
         if (size > 0) {
            notEmpty.signalAll();
         }
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
         notEmpty.signal();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E remove(int index) {
      lock.lock();
      try {
         E ret = super.remove(index);
         notFull.signal();
         return ret;
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
         // calls offerFirst, which signals notEmpty
         super.addFirst(e);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void addLast(E e) {
      lock.lock();
      try {
         // calls offerLast, which signals notEmpty
         super.addLast(e);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offerFirst(E e) {
      lock.lock();
      try {
         if (super.offerFirst(e)) {
            notEmpty.signal();
            return true;
         }
         return false;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offerLast(E e) {
      lock.lock();
      try {
         if (super.offerLast(e)) {
            notEmpty.signal();
            return true;
         }
         return false;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void putFirst(E e) throws InterruptedException {
      lock.lock();
      try {
         while (size >= capacity) {
            notFull.await();
         }
         boolean result = super.offerFirst(e);
         assert result;
         notEmpty.signal();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void putLast(E e) throws InterruptedException {
      lock.lock();
      try {
         while (size >= capacity) {
            notFull.await();
         }
         boolean result = super.offerLast(e);
         assert result;
         notEmpty.signal();
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
      lock.lock();
      try {
         long wait = unit.toNanos(timeout);
         while (size >= capacity) {
            if ((wait = notFull.awaitNanos(wait)) <= 0) {
               return false;
            }
         }
         boolean result = super.offerFirst(e);
         assert result;
         notEmpty.signal();
         return true;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
      lock.lock();
      try {
         long wait = unit.toNanos(timeout);
         while (size >= capacity) {
            if ((wait = notFull.awaitNanos(wait)) <= 0) {
               return false;
            }
         }
         boolean result = super.offerLast(e);
         assert result;
         notEmpty.signal();
         return true;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E takeFirst() throws InterruptedException {
      lock.lock();
      try {
         while (size == 0) {
            notEmpty.await();
         }
         E ret = super.pollFirst();
         notFull.signal();
         return ret;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E takeLast() throws InterruptedException {
      lock.lock();
      try {
         while (size == 0) {
            notEmpty.await();
         }
         E ret = super.pollLast();
         notFull.signal();
         return ret;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
      lock.lock();
      try {
         long wait = unit.toNanos(timeout);
         while (size == 0) {
            if ((wait = notEmpty.awaitNanos(wait)) <= 0) {
               return null;
            }
         }
         E ret = super.pollFirst();
         notFull.signal();
         return ret;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
      lock.lock();
      try {
         long wait = unit.toNanos(timeout);
         while (size == 0) {
            if ((wait = notEmpty.awaitNanos(wait)) <= 0) {
               return null;
            }
         }
         E ret = super.pollLast();
         notFull.signal();
         return ret;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean removeFirstOccurrence(Object o) {
      lock.lock();
      try {
         if (super.removeFirstOccurrence(o)) {
            notFull.signal();
            return true;
         }
         return false;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean removeLastOccurrence(Object o) {
      lock.lock();
      try {
         if (super.removeLastOccurrence(o)) {
            notFull.signal();
            return true;
         }
         return false;
      } finally {
         lock.unlock();
      }
   }

   @Override
   public void put(E e) throws InterruptedException {
      putLast(e);
   }

   @Override
   public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
      return offerLast(e, timeout, unit);
   }

   @Override
   public E take() throws InterruptedException {
      return takeFirst();
   }

   @Override
   public E poll(long timeout, TimeUnit unit) throws InterruptedException {
      return pollFirst(timeout, unit);
   }

   @Override
   public boolean remove(Object o) {
      lock.lock();
      try {
         return remove(o);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public boolean contains(Object o) {
      lock.lock();
      try {
         return contains(o);
      } finally {
         lock.unlock();
      }
   }

   @Override
   public int size() {
      lock.lock();
      try {
         return size;
      } finally {
         lock.unlock();
      }
   }
}
