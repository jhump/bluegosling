package com.bluegosling.collections.concurrent;

import static java.util.Objects.requireNonNull;

import com.bluegosling.concurrent.contended.ContendedInteger;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * A blocking queue that is backed by an array and is lock-free. Since it is lock-free, it does not
 * support growing the underlying array, so the capacity must be provided at the time the queue is
 * constructed.
 * 
 * <p>Because it is lock-free, it does <em>not</em> support interior removes. So calls to
 * {@link #remove(Object)}, {@link #removeAll(Collection)}, {@link #retainAll(Collection)},
 * {@link #removeIf(Predicate)}, and {@link Iterator#remove()} will throw
 * {@link UnsupportedOperationException}. This makes the queue most suitable for custom usages that
 * will never attempt an interior remove.
 * 
 * Since {@link ThreadPoolExecutor} has several operations (including the
 * {@link ThreadPoolExecutor#execute(Runnable) execute} method) that may attempt an interior remove,
 * this queue is not quite appropriate for that use. To use as a work queue for an executor, use 
 * {@link #executorWorkQueue(int)}.
 *
 * @param <E> the type of element held in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class LockFreeArrayBlockingQueue<E> extends AbstractLockFreeBlockingQueue<E> {

   /**
    * The number of times to "spin" in a busy-wait loop before yielding. This is used when a
    * tentative insertion or removal operation is waiting for a preceding (but concurrent) insertion
    * or removal operation to complete.
    */
   private final static int SPIN_TIMES = 1000;
   
   private final E data[];
   private final ContendedInteger tentativeHead = new ContendedInteger();
   private final ContendedInteger confirmedHead = new ContendedInteger();
   private final ContendedInteger tentativeTail = new ContendedInteger();
   private final ContendedInteger confirmedTail = new ContendedInteger();
   
   @SuppressWarnings("unchecked")
   public LockFreeArrayBlockingQueue(int capacity) {
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
   }

   @Override
   public E poll() {
      // first, we advance tentative head to determine the element we are removing
      int h, nextH, t;
      while (true) {
         h = tentativeHead.get();
         t = tentativeTail.get();
         if (h != tentativeHead.get()) {
            continue;
         }
         if (h == t) {
            return null;
         }
         nextH = h + 1;
         if (nextH == data.length) {
            nextH = 0;
         }
         if (nextH == confirmedHead.get()) {
            // Too many tentative (unconfirmed) removals plus enough tentative insertions that the
            // queue isn't "technically" empty (tentative operations have not cleared out all
            // elements). But we don't actually have an element to remove until one or more
            // tentative operations complete. This should only happen with very small queues.
            return null;
         }
         if (tentativeHead.compareAndSet(h, nextH)) {
            break;
         }
      }
      // Then we get the element value. We may be racing with the thread that is actually inserting
      // the element so we have to loop until the value is available
      E e;
      for (int i = 0; ; i++) {
         // visibility rules require us to read volatile confirmedTail to see the data
         confirmedTail.get();
         e = data[h];
         if (e != null) {
            break;
         }
         if (i == SPIN_TIMES) {
            Thread.yield();
            i = 0;
         }
      }
      // We may be racing with other threads that are removing prior elements, so we have to wait
      // for them to complete before we can actually update the confirmed head
      int confirmedH;
      for (int i = 0; ; i++) {
         confirmedH = confirmedHead.get();
         if (confirmedH == h) {
            break;
         }
         if (i == SPIN_TIMES) {
            Thread.yield();
            i = 0;
         }
      }
      data[h] = null;
      confirmedHead.set(nextH);
      
      // signal waiting threads if the queue is no longer full
      //if (h == t + 1 || t - h == data.length - 1) {
         signalNotFull();
      //}
      return e;
   }
   
   @Override
   public E peek() {
      while (true) {
         int h = confirmedHead.get();
         int t = confirmedTail.get();
         if (h != confirmedHead.get()) {
            continue;
         }
         if (h == t) {
            return null;
         }
         E e = data[h];
         if (e != null) {
            return e;
         }
      }
   }

   @Override
   public boolean offer(E e) {
      Objects.requireNonNull(e);
      
      // first, we advance tentative tail to reserve the slot where we add the element
      int h, t, nextT;
      while (true) {
         h = tentativeHead.get();
         t = tentativeTail.get();
         if (h != tentativeHead.get()) {
            continue;
         }
         if (h == t + 1 || t - h == data.length - 1) {
            return false;
         }
         nextT = t + 1;
         if (nextT == data.length) {
            nextT = 0;
         }
         if (nextT == confirmedTail.get()) {
            // Too many tentative (unconfirmed) insertions plus enough tentative removals that the
            // queue isn't "technically" full (tentative operations have not pushed us past the
            // capacity limit). But we don't actually have a place to put the element until one or
            // more tentative operations complete. This should only happen with very small queues.
            return false;
         }
         if (tentativeTail.compareAndSet(t, nextT)) {
            break;
         }
      }
      // Then we ensure the existing element is null. We may be racing with the thread that is
      // removing the slot's previous value so we have to loop until the value is gone
      for (int i = 0; ; i++) {
         // visibility rules require us to read volatile confirmedHead to see the data
         confirmedHead.get();
         E prev = data[t];
         if (prev == null) {
            break;
         }
         if (i == SPIN_TIMES) {
            Thread.yield();
            i = 0;
         }
      }
      // We may be racing with other threads that are adding prior elements, so we have to wait
      // for them to complete before we can actually update the confirmed tail
      int confirmedT;
      for (int i = 0; ; i++) {
         confirmedT = confirmedTail.get();
         if (confirmedT == t) {
            break;
         }
         if (i == SPIN_TIMES) {
            Thread.yield();
            i = 0;
         }
      }
      data[t] = e;
      confirmedTail.set(nextT);
      
      // signal waiting threads if the queue is no longer empty
      //if (h == t) {
         signalNotEmpty();
      //}
      return true;
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
   public Iterator<E> iterator() {
      return new IteratorImpl(confirmedHead.get());
   }
   
   @Override
   protected boolean isTentativelyEmpty() {
      while (true) {
         int h = tentativeHead.get();
         int t = tentativeTail.get();
         if (h == tentativeHead.get()) {
            return h == t;
         }
      }
   }

   @Override
   public boolean isEmpty() {
      while (true) {
         int h = confirmedHead.get();
         int t = confirmedTail.get();
         if (h == confirmedHead.get()) {
            return h == t;
         }
      }
   }

   @Override
   public int size() {
      while (true) {
         int h = confirmedHead.get();
         int t = confirmedTail.get();
         if (h == confirmedHead.get()) {
            return h <= t ? t - h : t + data.length - h;
         }
      }
   }
   
   @Override
   protected int tentativeRemainingCapacity() {
      while (true) {
         int h = tentativeHead.get();
         int t = tentativeTail.get();
         if (h == tentativeHead.get()) {
            return h <= t ? data.length - t + h - 1 : h - t - 1;
         }
      }
   }
   
   @Override
   public int remainingCapacity() {
      while (true) {
         int h = confirmedHead.get();
         int t = confirmedTail.get();
         if (h == confirmedHead.get()) {
            return h <= t ? data.length - t + h - 1 : h - t - 1;
         }
      }
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
               int h = confirmedHead.get();
               int t = confirmedTail.get();
               if (h != confirmedHead.get()) {
                  continue;
               }
               if (current == t) {
                  hasNext = 0;
                  return;
               }
               if ((current < h && h < t) || (current > t && t > h)) {
                  current = h;
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
         current++;
         if (current == data.length) {
            current = 0;
         }
         hasNext = -1;
         return ret;
      }
   }
   
   /**
    * Returns a blocking queue that is suitable for use as the work queue for a
    * {@link ThreadPoolExecutor} and is backed by a {@link LockFreeArrayBlockingQueue}. Since
    * interior removes are not supported, attempts to perform such a removal will instead change
    * the task into a no-op. This means that interior removes cannot be used to free up capacity
    * in the queue. Only worker threads de-queueing items can make room for new tasks.
    * 
    * <p>Removed items will be elided when querying the queue, for example from
    * {@link BlockingQueue#peek()}, {@link BlockingQueue#poll()}, or
    * {@link BlockingQueue#iterator()}. However, they will still be reflected in the
    * {@link BlockingQueue#size()} and {@link BlockingQueue#remainingCapacity()}. The no-op tasks
    * are actually cleaned up during operations that remove tasks from the head of the queue.
    *
    * @param capacity the maximum capacity of the work queue, which is allocated all up front
    * @return a blocking queue that is suitable for use as a work queue
    */
   public static LockFreeArrayBlockingQueue<Runnable> executorWorkQueue(int capacity) {
      return new ExecutorWorkQueue(capacity);
   }
   
   private static class ExecutorWorkQueue extends LockFreeArrayBlockingQueue<Runnable> {
      
      ExecutorWorkQueue(int capacity) {
         super(capacity);
      }
      
      @Override
      public boolean offer(Runnable task) {
         return super.offer(new PreemptableTask(task));
      }
      
      @Override
      public Runnable peek() {
         Iterator<Runnable> iter = iterator();
         return iter.hasNext() ? iter.next() : null;
      }
      
      @Override
      public Runnable poll() {
         while (true) {
            PreemptableTask task = (PreemptableTask) super.poll();
            if (task == null) {
               return null;
            }
            Runnable r = task.get();
            if (r != null) {
               return r;
            }
         }
      }
      
      @Override
      public boolean remove(Object o) {
         for (Runnable r : this) {
            PreemptableTask preemptable = (PreemptableTask) r;
            r = preemptable.get();
            if (r != null && r.equals(o) && preemptable.preempt()) {
               return true;
            }
         }
         return false;
      }
      
      @Override
      public boolean removeAll(Collection<?> c) {
         boolean ret = false;
         for (Runnable r : this) {
            PreemptableTask preemptable = (PreemptableTask) r;
            r = preemptable.get();
            if (r != null && c.contains(r) && preemptable.preempt()) {
               ret = true;
            }
         }
         return ret;
      }
      
      @Override
      public boolean retainAll(Collection<?> c) {
         boolean ret = false;
         for (Runnable r : this) {
            PreemptableTask preemptable = (PreemptableTask) r;
            r = preemptable.get();
            if (r != null && !c.contains(r) && preemptable.preempt()) {
               ret = true;
            }
         }
         return ret;
      }
      
      @Override
      public Iterator<Runnable> iterator() {
         Iterator<Runnable> iter = super.iterator();
         
         return new Iterator<Runnable>() {
            boolean needToFindNext = true;
            Runnable next;
            PreemptableTask nextPreemptable;
            PreemptableTask lastFetched;
            
            private void findNext() {
               if (needToFindNext) {
                  needToFindNext = false;
                  while (true) {
                     if (!iter.hasNext()) {
                        next = nextPreemptable = null;
                        return;
                     } else {
                        nextPreemptable = (PreemptableTask) iter.next();
                        next = nextPreemptable.get();
                        if (next != null) {
                           return;
                        }
                     }
                  }
               }
            }
            
            @Override
            public boolean hasNext() {
               findNext();
               return next != null;
            }

            @Override
            public Runnable next() {
               findNext();
               if (next == null) {
                  throw new NoSuchElementException();
               }
               lastFetched = nextPreemptable;
               Runnable ret = next;
               next = null;
               nextPreemptable = null;
               needToFindNext = true;
               return ret;
            }
            
            @Override
            public void remove() {
               if (lastFetched == null) {
                  throw new IllegalStateException();
               }
               lastFetched.preempt();
               lastFetched = null;
            }
            
         };
      }
   }
   
   /**
    * A task that can be preempted, which marks it as a no-op task. Preempted tasks are used as
    * "tombstones" to support interior removes in an {@link ExecutorWorkQueue}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   @SuppressWarnings("serial")
   private static class PreemptableTask extends AtomicReference<Runnable> implements Runnable {
      PreemptableTask(Runnable task) {
         super(requireNonNull(task));
      }
      
      public boolean preempt() {
         return getAndSet(null) != null;
      }
      
      @Override
      public void run() {
         Runnable task = getAndSet(null);
         if (task != null) {
            task.run();
         }
      }
   }
}
