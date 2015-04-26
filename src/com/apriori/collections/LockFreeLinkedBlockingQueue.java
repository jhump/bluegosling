package com.apriori.collections;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A lock-free implementation of {@link BlockingQueue}, backed by a {@link ConcurrentLinkedQueue}.
 *
 * <p>Unlike the {@link ConcurrentLinkedQueue} that backs it, this blocking queue provides an O(1)
 * implementation of {@link #size()}. An {@link AtomicInteger} is used to track the size and to also
 * ensure that any configured capacity limit is enforced.
 *  
 * <p>This class handles blocking operations (e.g. {@link #take()}, {@link #put(Object)}, etc.) with
 * custom wait queues instead of using {@link Lock}s and {@link Condition}s.
 * 
 * <p>Use of {@link Iterator#remove()} is discouraged. If invoked and there are multiple, equal
 * values in the queue, the first one will be removed instead of the one last fetched by the
 * iterator.  
 * 
 * @param <E> the type of element stored in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class LockFreeLinkedBlockingQueue<E> extends AbstractLockFreeBlockingQueue<E> {

   /**
    * The number of times to "spin" in a busy-wait loop before yielding. This is used when a
    * tentative insertion or removal operation is waiting for a preceding (but concurrent) insertion
    * or removal operation to complete.
    */
   private final static int SPIN_TIMES = 1000;

   private final ConcurrentLinkedQueue<E> delegate = new ConcurrentLinkedQueue<>();
   private final AtomicInteger size = new AtomicInteger();
   private final int maxCapacity;
   
   public LockFreeLinkedBlockingQueue() {
      this(Integer.MAX_VALUE);
   }
   
   public LockFreeLinkedBlockingQueue(int maxCapacity) {
      this.maxCapacity = maxCapacity;
   }
   
   @Override
   public E poll() {
      E ret;
      for (int i = 0; ; i++) {
         ret = delegate.poll();
         if (ret != null) {
            break;
         } else if (size.get() == 0) {
            return null;
         }
         // Concurrent offer has incremented the size (so not zero) but not yet actually
         // put item in underlying queue. Try again.
         if (i == SPIN_TIMES) {
            Thread.yield();
            i = 0;
         }
      }
      int prevSize = size.getAndDecrement();
      assert prevSize > 0;
      //if (prevSize == maxCapacity) {
         signalNotFull();
      //}
      return ret;
   }
   
   @Override
   public E peek() {
      return delegate.peek();
   }
   
   @Override
   public boolean offer(E e) {
      int prevSize;
      while (true) {
         prevSize = size.get();
         if (prevSize == maxCapacity) {
            return false;
         }
         if (size.compareAndSet(prevSize, prevSize + 1)) {
            break;
         }
      }
      boolean b = delegate.offer(e);
      assert b;
      //if (prevSize == 0) {
         signalNotEmpty();
      //}
      return true;
   }
   
   @Override
   public Iterator<E> iterator() {
      Iterator<E> iter = delegate.iterator();
      return new Iterator<E>() {
         E lastFetched;
         
         @Override
         public boolean hasNext() {
            return iter.hasNext();
         }

         @Override
         public E next() {
            return lastFetched = iter.next();
         }
         
         @Override
         public void remove() {
            if (lastFetched == null) {
               throw new IllegalStateException();
            }
            LockFreeLinkedBlockingQueue.this.remove(lastFetched);
            lastFetched = null;
         }
      };
   }
   
   @Override
   public boolean remove(Object o) {
      if (delegate.remove(o)) {
         int prevSize = size.getAndDecrement();
         assert prevSize > 0;
         if (prevSize == maxCapacity) {
            signalNotFull();
         }
         return true;
      }
      return false;
   }
   
   @Override
   public int remainingCapacity() {
      int c = maxCapacity - size();
      assert c >= 0;
      return c;
   }
   
   @Override
   public int size() {
      return size.get();
   }
}
