package com.bluegosling.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

// TODO: doc
// TODO: tests
// TODO: custom queue impl to combine blocking for queue and for rate limiter into a single wait?
public class RateLimitedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

   /**
    * True if we are applying rate limiting when objects are removed from the queue. If false, we
    * instead apply rate limiting when objects are added to the queue.
    */
   private final boolean limitTakes;
   
   private final RateLimiter rateLimiter;
   
   private final BlockingQueue<E> queue;

   private RateLimitedBlockingQueue(boolean limitTakes, RateLimiter rateLimiter,
         BlockingQueue<E> queue) {
      this.limitTakes = limitTakes;
      this.rateLimiter = rateLimiter;
      this.queue = queue;
   }
   
   protected int measure(E element) {
      return 1;
   }
   
   @Override
   public E poll() {
      E ret = queue.poll();
      if (ret != null && limitTakes) {
         rateLimiter.acquire(measure(ret));
      }
      return ret;
   }

   @Override
   public E peek() {
      return queue.peek();
   }

   @Override
   public boolean offer(E e) {
      if (!limitTakes) {
         if (queue.remainingCapacity() == 0) {
            // if we know we can't add it, don't both trying to acquire a permit
            return false;
         }
         rateLimiter.acquire(measure(e));
      }
      return queue.offer(e);
   }

   @Override
   public void put(E e) throws InterruptedException {
      if (!limitTakes) {
         rateLimiter.acquireInterruptibly(measure(e));
      }
      queue.put(e);
   }

   @Override
   public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
      if (limitTakes) {
         return queue.offer(e, timeout, unit);
      }
      
      long start = System.nanoTime();
      long nanos = unit.toNanos(timeout);
      if (!rateLimiter.tryAcquire(measure(e), timeout, unit)) {
         return false;
      }
      long nanosLeft = Math.max(0, nanos - (System.nanoTime() - start));
      return queue.offer(e, nanosLeft, TimeUnit.NANOSECONDS);
   }

   @Override
   public E take() throws InterruptedException {
      E ret = queue.take();
      if (limitTakes) {
         rateLimiter.acquire(measure(ret));
      }
      return ret;
   }

   @Override
   public E poll(long timeout, TimeUnit unit) throws InterruptedException {
      E ret = queue.poll(timeout, unit);
      if (ret != null && limitTakes) {
         rateLimiter.acquire(measure(ret));
      }
      return ret;
   }

   @Override
   public int remainingCapacity() {
      return queue.remainingCapacity();
   }

   @Override
   public int drainTo(Collection<? super E> c) {
      if (c == this || c == queue) {
         throw new IllegalArgumentException("cannot drain queue to itself");
      }

      if (!limitTakes) {
         return queue.drainTo(c);
      }
      
      int ret = 0;
      E e;
      while ((e = poll()) != null) {
         c.add(e);
         ret++;
      }
      return ret;
   }

   @Override
   public int drainTo(Collection<? super E> c, int maxElements) {
      if (c == this || c == queue) {
         throw new IllegalArgumentException("cannot drain queue to itself");
      }

      if (!limitTakes) {
         return queue.drainTo(c, maxElements);
      }
      
      int ret = 0;
      E e;
      while (ret < maxElements && (e = poll()) != null) {
         c.add(e);
         ret++;
      }
      return ret;
   }

   @Override
   public Iterator<E> iterator() {
      return queue.iterator();
   }

   @Override
   public int size() {
      return queue.size();
   }
   
   @Override
   public void clear() {
      queue.clear();
   }
}
