package com.apriori.collections;

import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public abstract class AbstractLockFreeBlockingDeque<E> extends AbstractDeque<E>
      implements BlockingDeque<E> {

   private static class WaitingThread {
      final Thread thread;
      volatile boolean dequeued;
      
      WaitingThread(Thread thread) {
         this.thread = thread;
      }
   }
   
   private final ConcurrentLinkedQueue<WaitingThread> awaitingElement =
         new ConcurrentLinkedQueue<>();
   private final ConcurrentLinkedQueue<WaitingThread> awaitingCapacity =
         new ConcurrentLinkedQueue<>();

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
   
   @SuppressWarnings("unused") // exception is present for overriding sub-classes
   protected E pollFirstInterruptibly() throws InterruptedException {
      return pollFirst();
   }

   @SuppressWarnings("unused") // exception is present for overriding sub-classes
   protected E pollLastInterruptibly() throws InterruptedException {
      return pollLast();
   }

   @SuppressWarnings("unused") // exception is present for overriding sub-classes
   protected boolean offerFirstInterruptibly(E e) throws InterruptedException {
      return offerFirst(e);
   }

   @SuppressWarnings("unused") // exception is present for overriding sub-classes
   protected boolean offerLastInterruptibly(E e) throws InterruptedException {
      return offerLast(e);
   }

   protected final void signalNotEmpty() {
      WaitingThread w = awaitingElement.poll();
      if (w != null) {
         w.dequeued = true;
         LockSupport.unpark(w.thread);
      }
   }

   protected final void signalNotFull() {
      WaitingThread w = awaitingCapacity.poll();
      if (w != null) {
         w.dequeued = true;
         LockSupport.unpark(w.thread);
      }
   }
   
   @Override
   public void putFirst(E e) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      boolean added = offerFirstInterruptibly(e);
      if (added) {
         return;
      }
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingCapacity.add(th);
      try {
         while (true) {
            added = offerFirstInterruptibly(e);
            if (added) {
               return;
            }
            LockSupport.park(th);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            if (th.dequeued) {
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
   public void putLast(E e) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      boolean added = offerLastInterruptibly(e);
      if (added) {
         return;
      }
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingCapacity.add(th);
      try {
         while (true) {
            added = offerLastInterruptibly(e);
            if (added) {
               return;
            }
            LockSupport.park(th);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            if (th.dequeued) {
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
   public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      boolean added = offerFirstInterruptibly(e);
      if (added) {
         return true;
      }
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingCapacity.add(th);
      try {
         while (true) {
            added = offerFirstInterruptibly(e);
            if (added) {
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
   public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      boolean added = offerLastInterruptibly(e);
      if (added) {
         return true;
      }
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingCapacity.add(th);
      try {
         while (true) {
            added = offerLastInterruptibly(e);
            if (added) {
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
   public E takeFirst() throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      E ret = pollFirstInterruptibly();
      if (ret != null) {
         return ret;
      }
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            ret = pollFirstInterruptibly();
            if (ret != null) {
               return ret;
            }
            LockSupport.park(th);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            if (th.dequeued) {
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
      E ret = pollLastInterruptibly();
      if (ret != null) {
         return ret;
      }
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            ret = pollLastInterruptibly();
            if (ret != null) {
               return ret;
            }
            LockSupport.park(th);
            if (Thread.interrupted()) {
               throw new InterruptedException();
            }
            if (th.dequeued) {
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
   public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      }
      E ret = pollFirstInterruptibly();
      if (ret != null) {
         return ret;
      }
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            ret = pollFirstInterruptibly();
            if (ret != null) {
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
      E ret = pollLastInterruptibly();
      if (ret != null) {
         return ret;
      }
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      WaitingThread th = new WaitingThread(Thread.currentThread());
      awaitingElement.add(th);
      try {
         while (true) {
            ret = pollLastInterruptibly();
            if (ret != null) {
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
      while ((e = pollFirst()) != null) {
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
      while (numRemoved < maxElements && (e = pollFirst()) != null) {
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
