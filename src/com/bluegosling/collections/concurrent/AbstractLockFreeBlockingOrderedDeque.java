package com.bluegosling.collections.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractLockFreeBlockingOrderedDeque<E>
extends AbstractLockFreeBlockingQueue<E> implements BlockingOrderedDeque<E> {

   protected E pollFirstInterruptibly() throws InterruptedException {
      return pollFirst();
   }
   
   protected E pollLastInterruptibly() throws InterruptedException {
      return pollLast();
   }
   
   protected E pollInterruptibly() throws InterruptedException {
      return pollFirstInterruptibly();
   }
   
   @Override
   public E takeFirst() throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      E ret;
      if ((ret = pollFirstInterruptibly()) != null) {
         return ret;
      }
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            if ((ret = pollFirstInterruptibly()) != null) {
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
   public E takeLast() throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      E ret;
      if ((ret = pollLastInterruptibly()) != null) {
         return ret;
      }
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            if ((ret = pollLastInterruptibly()) != null) {
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
   public E take() throws InterruptedException {
      return takeFirst();
   }
   
   @Override
   public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      E ret;
      if ((ret = pollFirstInterruptibly()) != null) {
         return ret;
      }
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            if ((ret = pollFirstInterruptibly()) != null) {
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
   public E pollLast(long timeout, TimeUnit unit) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      E ret;
      if ((ret = pollLastInterruptibly()) != null) {
         return ret;
      }
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            if ((ret = pollLastInterruptibly()) != null) {
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
   public E poll(long timeout, TimeUnit unit) throws InterruptedException {
      return pollFirst(timeout, unit);
   }
}
