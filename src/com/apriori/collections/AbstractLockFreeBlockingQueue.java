package com.apriori.collections;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.LockSupport;

/**
 * An abstract blocking queue, useful for building queue implementations that do not rely on locks
 * for thread-safey. This class provides a custom wait/notify mechanism that does not need locks
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
    * The threads that are waiting for an element. These threads are signaled when this blocking
    * queue becomes non-empty. 
    */
   private final ConcurrentLinkedQueue<Thread> awaitingElement =
         new ConcurrentLinkedQueue<>();
   
   /**
    * The threads that are waiting for available capacity. These threads are signaled when this
    * blocking queue is no longer full. 
    */
   private final ConcurrentLinkedQueue<Thread> awaitingCapacity =
         new ConcurrentLinkedQueue<>();

   @SuppressWarnings("unused") // exception is present for overriding sub-classes
   protected E pollInterruptibly() throws InterruptedException {
      return poll();
   }

   @SuppressWarnings("unused") // exception is present for overriding sub-classes
   protected boolean offerInterruptibly(E e) throws InterruptedException {
      return offer(e);
   }
   
   protected final void signalNotEmpty() {
      Thread th = awaitingElement.peek();
      if (th != null) {
         LockSupport.unpark(th);
      }
   }

   protected final void signalNotFull() {
      Thread th = awaitingCapacity.peek();
      if (th != null) {
         LockSupport.unpark(th);
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
      Thread th = Thread.currentThread();
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
         }
      } finally {
         boolean removed = awaitingCapacity.remove(th);
         assert removed;
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
      Thread th = Thread.currentThread();
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
         }
      } finally {
         boolean removed = awaitingCapacity.remove(th);
         assert removed;
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
      Thread th = Thread.currentThread();
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
         }
      } finally {
         boolean removed = awaitingElement.remove(th);
         assert removed;
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
      Thread th = Thread.currentThread();
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
         }
      } finally {
         boolean removed = awaitingElement.remove(th);
         assert removed;
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
   
   protected int tentativeRemainingCapacity() {
      return remainingCapacity();
   }

   protected boolean isTentativelyEmpty() {
      return isEmpty();
   }
}
