package com.apriori.collections;

import com.apriori.concurrent.NonReentrantLock;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

/**
 * A double-ended blocking queue backed by an array. This class uses two locks, one each for the
 * head and tail of the deque. It is not lock-free, but extends {@link AbstractLockFreeBlockingDeque}
 * in order to inherit its custom wait/notify mechanisms. Typical lock {@link Condition} queues
 * won't work since there are two locks which could generate "not empty" and "not full" notices.
 * 
 * <p>Operations on one side of the queue typically only need to acquire one of the locks. But
 * removing the last element requires both locks since the last element is both the head and tail of
 * the queue. The first element added will be the head and the tail, too, but adding the first
 * element can actually be done safely from either end without holding both locks. This is because
 * the two additions don't actually contend over the same slot in the underlying array: adding via
 * tail uses the slot <em>after</em> the one used by adding via head.
 *
 * <p>Because there is no single lock for the whole array, operations can only operate on the head
 * or tail (holding the respective lock). Therefore this implementation does <em>not</em> support
 * interior removes. So calls to {@link #remove(Object)}, {@link #removeAll(Collection)},
 * {@link #retainAll(Collection)}, {@link #removeIf(Predicate)}, and {@link Iterator#remove()} will
 * throw {@link UnsupportedOperationException}. This makes the queue most suitable for custom usages
 * that will never attempt an interior remove.
 * 
 * <p>The lack of support for interior removals rules out usage with a {@link ThreadPoolExecutor},
 * which does attempt interior removes for some operations (including
 * {@link ThreadPoolExecutor#execute(Runnable)}). For a high-performance executor work queue that is
 * backed by an array, instead consider {@link LockFreeArrayBlockingQueue#executorWorkQueue(int)}.
 *
 * @param <E> the type of elements held in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ArrayBlockingDeque<E> extends AbstractLockFreeBlockingDeque<E> {

   /**
    * The contents of the queue.
    */
   private final E data[];
   
   /**
    * The number of elements in the queue.
    */
   private final AtomicInteger size = new AtomicInteger();

   /**
    * The index (inclusive) of the head of the queue. If the head is equal to the tail then the
    * queue is empty. Modifying this field requires holding the {@link #headLock}. But the field is
    * volatile so other query operations (iteration, peek) can occur concurrently without acquiring
    * the lock.
    */
   private volatile int head;

   /**
    * The lock that guards changes to {@link #head}.
    */
   private final Lock headLock;

   /**
    * The index (exclusive) of the tail of the queue. If the head is equal to the tail then the
    * queue is empty. Modifying this field requires holding the {@link #tailLock}. But the field is
    * volatile so other query operations (iteration, peek) can occur concurrently without acquiring
    * the lock.
    */
   private volatile int tail;

   /**
    * The lock that guards changes to {@link #tail}.
    */
   private final Lock tailLock;

   @SuppressWarnings("unchecked")
   public ArrayBlockingDeque(int capacity) {
      if (capacity <= 0) {
         throw new IllegalArgumentException("capacity must be positive");
      }
      if (capacity == Integer.MAX_VALUE) {
         throw new IllegalArgumentException(
               "capacity must be less than Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
      }
      // we need the array to be one bigger than actual capacity so we can always disambiguate
      // head == tail condition as empty (if array length == capacity, then head == tail could
      // also mean full)
      capacity++;
      this.data = (E[]) new Object[capacity];
      this.headLock = new NonReentrantLock(); 
      this.tailLock = new NonReentrantLock(); 
   }
   
   @Override
   public E pollFirst() {
      try {
         return pollFirst(false);
      } catch (InterruptedException e) {
         // we pass "false" to indicate not interruptible, so this shouldn't be possible
         throw new AssertionError(e);
      }
   }
   
   @Override
   protected E pollFirstInterruptibly() throws InterruptedException {
      return pollFirst(true);
   }
   
   private E pollFirst(boolean interruptible) throws InterruptedException {
      boolean releaseTail = false;
      boolean signalWaiters = false;
      if (interruptible) {
         headLock.lockInterruptibly();
      } else {
         headLock.lock();
      }
      try {
         // claim the element by decrementing size
         int prevSize;
         while (true) {
            prevSize = size.get();
            if (prevSize == 0) {
               return null;
            }
            if (size.compareAndSet(prevSize, prevSize - 1)) {
               break;
            }
         }
         // need both locks to remove the last element
         if (prevSize == 1) {
            if (interruptible) {
               tailLock.lockInterruptibly();
            } else {
               tailLock.lock();
            }
            releaseTail = true;
         }
         int h = head;
         int nextH = h + 1;
         if (nextH == data.length) {
            nextH = 0;
         }
         E ret = data[h];
         assert ret != null;
         data[h] = null;
         head = nextH;
         signalWaiters = prevSize == data.length - 1;
         return ret;
      } finally {
         if (releaseTail) {
            tailLock.unlock();
         }
         headLock.unlock();
         if (signalWaiters) {
            signalNotFull();
         }
      }
   }
   
   @Override
   public E pollLast() {
      try {
         return pollLast(false);
      } catch (InterruptedException e) {
         // we pass "false" to indicate not interruptible, so this shouldn't be possible
         throw new AssertionError(e);
      }
   }

   @Override
   protected E pollLastInterruptibly() throws InterruptedException {
      return pollLast(true);
   }
   
   private E pollLast(boolean interruptible) throws InterruptedException {
      boolean releaseTail = false;
      boolean releaseHead = false;
      boolean signalWaiters = false;
      if (interruptible) {
         tailLock.lockInterruptibly();
      } else {
         tailLock.lock();
      }
      releaseTail = true;
      try {
         // try to claim the element by decrementing size
         int prevSize;
         while (true) {
            prevSize = size.get();
            if (prevSize == 0) {
               return null;
            }
            if (prevSize == 1) {
               // need both locks to remove the last element
               if (headLock.tryLock()) {
                  releaseHead = true;
               } else {
                  // to avoid deadlock, must always acquire head before tail when we need both
                  tailLock.unlock();
                  releaseTail = false;
                  if (interruptible) {
                     headLock.lockInterruptibly();
                  } else {
                     headLock.lock();
                  }
                  releaseHead = true;
                  if (interruptible) {
                     tailLock.lockInterruptibly();
                  } else {
                     tailLock.lock();
                  }
                  releaseTail = true;
               }
               // re-read size
               prevSize = size.get();
               if (prevSize == 0) {
                  return null;
               }
               // don't need CAS since we hold both locks
               size.set(prevSize - 1);
               // if we don't actually need both locks, release head
               if (prevSize != 1) {
                  headLock.unlock();
                  releaseHead = false;
               }
               break;
            }
            if (size.compareAndSet(prevSize, prevSize - 1)) {
               break;
            }
         }

         int t = tail;
         int nextT = t == 0 ? data.length - 1 : t - 1;
         E ret = data[nextT];
         assert ret != null;
         data[nextT] = null;
         tail = nextT;
         signalWaiters = prevSize == data.length - 1;
         return ret;
      } finally {
         if (releaseTail) {
            tailLock.unlock();
         }
         if (releaseHead) {
            headLock.unlock();
         }
         if (signalWaiters) {
            signalNotFull();
         }
      }
   }

   @Override
   public E peekFirst() {
      while (true) {
         if (size.get() == 0) {
            return null;
         }
         int h = head;
         if (h != head) {
            continue;
         }
         E ret = data[h];
         if (ret != null) {
            return ret;
         }
      }
   }

   @Override
   public E peekLast() {
      while (true) {
         if (size.get() == 0) {
            return null;
         }
         int t = tail;
         t = t == 0 ? data.length - 1 : t - 1;
         E ret = data[t];
         if (ret != null) {
            return ret;
         }
      }
   }
   
   @Override
   public boolean offerFirst(E e) {
      try {
         return offerFirst(e, false);
      } catch (InterruptedException ie) {
         // we pass "false" to indicate not interruptible, so this shouldn't be possible
         throw new AssertionError(ie);
      }
   }

   @Override
   protected boolean offerFirstInterruptibly(E e) throws InterruptedException {
      return offerFirst(e, true);
   }

   private boolean offerFirst(E e, boolean interruptible) throws InterruptedException {
      boolean signalWaiters = false;
      if (interruptible) {
         headLock.lockInterruptibly();
      } else {
         headLock.lock();
      }
      try {
         // claim the element by decrementing size
         int prevSize;
         while (true) {
            prevSize = size.get();
            if (prevSize == data.length - 1) {
               return false;
            }
            if (size.compareAndSet(prevSize, prevSize + 1)) {
               break;
            }
         }
         // adjust head pointer and store element
         int h = head;
         int nextH = h == 0 ? data.length - 1 : h - 1;
         assert data[nextH] == null;
         data[nextH] = e;
         head = nextH;
         signalWaiters = prevSize == 0;
         return true;
      } finally {
         headLock.unlock();
         if (signalWaiters) {
            signalNotEmpty();
         }
      }
   }
   
   @Override
   public boolean offerLast(E e) {
      try {
         return offerLast(e, false);
      } catch (InterruptedException ie) {
         // we pass "false" to indicate not interruptible, so this shouldn't be possible
         throw new AssertionError(ie);
      }
   }
   
   @Override
   protected boolean offerLastInterruptibly(E e) throws InterruptedException {
      return offerLast(e, true);
   }

   private boolean offerLast(E e, boolean interruptible) throws InterruptedException {
      boolean signalWaiters = false;
      if (interruptible) {
         tailLock.lockInterruptibly();
      } else {
         tailLock.lock();
      }
      try {
         // claim the element by decrementing size
         int prevSize;
         while (true) {
            prevSize = size.get();
            if (prevSize == data.length - 1) {
               return false;
            }
            if (size.compareAndSet(prevSize, prevSize + 1)) {
               break;
            }
         }
         // adjust head pointer and store element
         int t = tail;
         int nextT = t + 1;
         if (nextT == data.length) {
            nextT = 0;
         }
         assert data[t] == null;
         data[t] = e;
         tail = nextT;
         signalWaiters = prevSize == 0;
         return true;
      } finally {
         tailLock.unlock();
         if (signalWaiters) {
            signalNotEmpty();
         }
      }
   }

   @Override
   public Iterator<E> iterator() {
      return new IteratorImpl(head);
   }

   @Override
   public Iterator<E> descendingIterator() {
      int t = tail;
      t = t == 0 ? data.length - 1 : t - 1;
      return new DescendingIteratorImpl(t);
   }

   @Override
   public boolean remove(Object o) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
   }
   
   @Override
   public int remainingCapacity() {
      return data.length - 1 - size();
   }

   @Override
   public int size() {
      return size.get();
   }
   
   private class IteratorImpl implements Iterator<E> {
      private int current;
      private int hasNext = -1;
      private E next;
      
      IteratorImpl(int h) {
         this.current = h;
      }
      
      @SuppressWarnings("synthetic-access")
      private void findNext() {
         if (hasNext == -1) {
            while (true) {
               if (size.get() == 0) {
                  hasNext = 0;
                  return;
               }
               next = data[current];
               int t;
               if (next != null) {
                  hasNext = 1;
                  return;
               } else if (current == (t = tail)) {
                  hasNext = 0;
                  return;
               }
               int h = head;
               if ((current < h && h < t) || (current > t && t > h)) {
                  current = h;
               }
            }
         }
      }

      @Override
      public boolean hasNext() {
         findNext();
         return hasNext == 1;
      }

      @SuppressWarnings("synthetic-access")
      @Override
      public E next() {
         findNext();
         if (hasNext == 0) {
            throw new NoSuchElementException();
         }
         E ret = next;
         next = null;
         current++;
         if (current == data.length) {
            current = 0;
         }
         hasNext = -1;
         return ret;
      }
   }

   private class DescendingIteratorImpl implements Iterator<E> {
      private int current;
      private int hasNext = -1;
      private E next;
      
      DescendingIteratorImpl(int t) {
         this.current = t;
      }
      
      @SuppressWarnings("synthetic-access")
      private void findNext() {
         if (hasNext == -1) {
            while (true) {
               int h = head;
               int t = tail;
               if (h != head) {
                  continue;
               }
               int end = h == 0 ? data.length - 1 : h - 1;
               if (current == end) {
                  hasNext = 0;
                  return;
               }
               if ((current < h && h < t) || (current > t && t > h)) {
                  current = t == 0 ? data.length - 1 : t - 1;
               }
               next = data[current];
               if (next != null) {
                  hasNext = 1;
                  return;
               }
            }
         }
      }

      @Override
      public boolean hasNext() {
         findNext();
         return hasNext == 1;
      }

      @SuppressWarnings("synthetic-access")
      @Override
      public E next() {
         findNext();
         if (hasNext == 0) {
            throw new NoSuchElementException();
         }
         E ret = next;
         next = null;
         current = current == 0 ? data.length - 1 : current - 1;
         hasNext = -1;
         return ret;
      }
   }
}
