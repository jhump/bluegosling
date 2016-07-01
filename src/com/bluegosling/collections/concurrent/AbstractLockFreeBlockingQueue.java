package com.bluegosling.collections.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

/**
 * An abstract blocking queue, useful for building queue implementations that do not rely on locks
 * for thread-safety. This class provides a custom wait/notify mechanism that does not need locks
 * (no {@link Condition} queues, no intrinsic locks/monitors).
 * 
 * <p>Sub-classes must invoke {@link #signalNotEmpty()} when their implementation of
 * {@link #offer(Object)} adds an element to a previously empty queue. Similarly, they must invoke
 * {@link #signalNotFull()} when their implementation of {@link #poll()} removes an element from a
 * previously full queue.
 *
 * @param <E> the type of element held in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractLockFreeBlockingQueue<E> extends AbstractQueue<E>
      implements BlockingQueue<E> {
   
   /**
    * Represents a thread waiting to poll or add an element to the queue.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class WaitingThread {
      /**
       * The thread that is waiting.
       */
      final Thread thread;
      
      /**
       * Indicates whether the thread has already been removed from its associated wait queue.
       */
      volatile boolean dequeued;
      
      WaitingThread(Thread thread) {
         this.thread = thread;
      }
   }
   
   /**
    * The threads that are waiting for an element. These threads are signaled when this blocking
    * queue becomes non-empty. 
    */
   private final ConcurrentLinkedQueue<WaitingThread> awaitingElement = new ConcurrentLinkedQueue<>();
   
   /**
    * The threads that are waiting for available capacity. These threads are signaled when this
    * blocking queue is no longer full. 
    */
   private final ConcurrentLinkedQueue<WaitingThread> awaitingCapacity = new ConcurrentLinkedQueue<>();

   /**
    * Removes the first element in the queue, allowing interruption. Since most lock-free queues
    * won't have blocking operations during {@link #poll()} (and thus not be interruptible), this
    * method typically need not be overridden. But if any operations are interruptible, this method
    * should be implemented to propagate that interruption, whereas the {@link #poll()} method
    * (due to its signature) must suppress it. 
    *
    * @return the item removed from the queue or {@code null} if the queue is empty
    * @throws InterruptedException if the operation was interrupted
    */
   protected E pollInterruptibly() throws InterruptedException {
      return poll();
   }

   /**
    * Adds an element to the tail of the queue, allowing interruption. Since most lock-free queues
    * won't have blocking operations during {@link #offer(Object)} (and thus not be interruptible),
    * this method typically need not be overridden. But if any operations are interruptible, this
    * method should be implemented to propagate that interruption, whereas the
    * {@link #offer(Object)} method (due to its signature) must suppress it. 
    *
    * @return true if the element was accepted or false if the queue is full
    * @throws InterruptedException if the operation was interrupted
    */
   protected boolean offerInterruptibly(E e) throws InterruptedException {
      return offer(e);
   }
   
   /**
    * Signals a waiting thread that the queue is no longer empty. An operation that transitions the
    * queue from empty to not empty <em>must</em> call this method in case there are threads waiting
    * to take an element.
    */
   protected final void signalNotEmpty() {
      WaitingThread w = awaitingElement.poll();
      if (w != null) {
         w.dequeued = true;
         LockSupport.unpark(w.thread);
      }
   }

   /**
    * Signals a waiting thread that the queue is no longer full. An operation that transitions the
    * queue from full to not full <em>must</em> call this method in case there are threads waiting
    * to add an element.
    */
   protected final void signalNotFull() {
      WaitingThread w = awaitingCapacity.poll();
      if (w != null) {
         w.dequeued = true;
         LockSupport.unpark(w.thread);
      }
   }

   @Override
   public void put(E e) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      if (offerInterruptibly(e)) {
         return;
      }
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingCapacity.add(th);
      try {
         while (true) {
            if (offerInterruptibly(e)) {
               return;
            }
            LockSupport.park();
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            if (th.dequeued) {
               th.dequeued = false;
               awaitingCapacity.add(th);
            }
         }
      } finally {
         if (!th.dequeued) {
            awaitingCapacity.remove(th);
         }
         if (tentativeRemainingCapacity() > 0) {
            signalNotFull();
         }
      }
   }
   
   @Override
   public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      if (offerInterruptibly(e)) {
         return true;
      }
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingCapacity.add(th);
      try {
         while (true) {
            if (offerInterruptibly(e)) {
               return true;
            }
            if (nanos <= 0) {
               return false;
            }
            LockSupport.parkNanos(nanos);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            long now = System.nanoTime();
            nanos -= now - start;
            start = now;
            if (th.dequeued) {
               th.dequeued = false;
               awaitingCapacity.add(th);
            }
         }
      } finally {
         if (!th.dequeued) {
            awaitingCapacity.remove(th);
         }
         if (tentativeRemainingCapacity() > 0) {
            signalNotFull();
         }
      }
   }
   
   @Override
   public E take() throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      E ret;
      if ((ret = pollInterruptibly()) != null) {
         return ret;
      }
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            if ((ret = pollInterruptibly()) != null) {
               return ret;
            }
            LockSupport.park();
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            if (th.dequeued) {
               th.dequeued = false;
               awaitingElement.add(th);
            }
         }
      } finally {
         if (!th.dequeued) {
            awaitingElement.remove(th);
         }
         if (!isTentativelyEmpty()) {
            signalNotEmpty();
         }
      } 
   }
   
   @Override
   public E poll(long timeout, TimeUnit unit) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      E ret;
      if ((ret = pollInterruptibly()) != null) {
         return ret;
      }
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            if ((ret = pollInterruptibly()) != null) {
               return ret;
            }
            if (nanos <= 0) {
               return null;
            }
            LockSupport.parkNanos(nanos);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            long now = System.nanoTime();
            nanos -= now - start;
            start = now;
            if (th.dequeued) {
               th.dequeued = false;
               awaitingElement.add(th);
            }
         }
      } finally {
         if (!th.dequeued) {
            awaitingElement.remove(th);
         }
         if (!isTentativelyEmpty()) {
            signalNotEmpty();
         }
      }
   }
   
   @Override
   public int drainTo(Collection<? super E> c) {
      int numRemoved = 0;
      E e;
      while ((e = poll()) != null) {
         c.add(e);
         numRemoved++;
      }
      return numRemoved;
   }
   
   @Override
   public int drainTo(Collection<? super E> c, int maxElements) {
      if (maxElements < 0) {
         throw new IllegalArgumentException("max elements to drain must be non-negative");
      }
      int numRemoved = 0;
      E e;
      while (numRemoved < maxElements && (e = poll()) != null) {
         c.add(e);
         numRemoved++;
      }
      return numRemoved;
   }
   
   /**
    * Returns the tentative remaining capacity in the queue. If different from
    * {@link #remainingCapacity()}, then this returns a capacity after considering incomplete
    * operations whereas the other returns "confirmed" remaining capacity, considering only
    * completed operations.
    *
    * @return the tentative remaining capacity in the queue
    */
   protected int tentativeRemainingCapacity() {
      return remainingCapacity();
   }

   /**
    * Returns true if this queue is tentatively empty. If different from {@link #isEmpty()}, then
    * this returns true if the queue is empty after considering incomplete operations whereas the
    * other returns if the queue is "confirmed" empty, considering only completed operations.
    *
    * @return true if this queue is tentatively empty
    */
   protected boolean isTentativelyEmpty() {
      return isEmpty();
   }
}
