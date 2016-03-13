package com.bluegosling.collections.concurrent;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A producer of queues and deques that all share a single bound. An individual queue could fill up
 * the total capacity if all of the others are empty. Similarly, if other queues produced by the
 * factory have a large number of items, then the next queue would have a smaller capacity. The
 * capacity is not fixed at the time the queue is generated but rather is determined at runtime by
 * the total number of items across all queues at any given moment in time.
 * 
 * <p>Queues created by this factory can be closed, which releases their share of the capacity (and
 * also makes them inoperable).
 * 
 * <p>This factory provides no new implementations of {@link Queue} or {@link Deque}. Instead,
 * callers provide the implementation, and this factory wraps it so that mutation attempts will have
 * the total capacity enforced.
 * 
 * <p>The queues returned by this factory will generally have all of the properties of the
 * implementation that is wrapped. The exception to this rule is that some methods on the returned
 * queues are <strong>not atomic</strong>, even if the underlying implementation provides an atomic
 * version of the operation. This is done so as to accurately track the total size of all
 * queues and to enforce the total capacity. The methods impacted by this exception follow:
 * <ul>
 * <li>{@link Collection#addAll(Collection) addAll}</li>
 * <li>{@link Collection#removeAll(Collection) removeAll}</li>
 * <li>{@link Collection#retainAll(Collection) retainAll}</li>
 * <li>{@link Collection#clear() clear}</li>
 * </ul> 
 * 
 * <p>The queues returned by this factory also have the same concurrency as the implementation that
 * is wrapped. Notably, <strong>if the underlying queue is not thread-safe, neither is the wrapped
 * queue provided by this factory</strong>. If a queue that is not thread-safe is modified
 * concurrently, the results are undefined (but expect that the structure could be unrecoverably
 * corrupted and that the factory's capacity could be exceeded).  
 * 
 * <p><strong>NOTE:</strong> The {@link Iterator#remove()} method returns no information as to
 * whether the removal succeeded (i.e. modified the backing collection). If used with an
 * implementation of queue or deque that uses weakly consistent iterators, there is no way to know
 * with certainty when an item is actually removed. This means the information about the combined
 * size of all queues provided by this factory could become incorrect if this method is used. Since
 * it behaves when the underlying implementation has strongly consistent iterators, the wrapped
 * queues provided by this factory <em>do</em> support this operation. <em>But</em>, since it
 * doesn't behave properly for all underlying implementations, removing elements via an iterator is
 * <strong>strongly discouraged</strong>. 
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests!
public class SharedCapacityDequeFactory {
   
   /**
    * A {@link Queue} that can be closed. Closing the queue means it can no longer be used.
    * Its share of the total capacity is immediately reclaimed so other queues created by the same
    * factory can use it.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element contained in the queue
    */
   public interface CloseableQueue<T> extends Queue<T> {
      /**
       * Closes the queue, releasing its capacity, and rendering it inoperable. Subsequent methods
       * invoked on the queue will throw {@link IllegalStateException}.
       */
      void close();
   }

   /**
    * A {@link BlockingQueue} that can be closed.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element contained in the queue
    * 
    * @see CloseableQueue
    */
   public interface CloseableBlockingQueue<T> extends CloseableQueue<T>, BlockingQueue<T> {
   }

   /**
    * A {@link Deque} that can be closed.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element contained in the queue
    * 
    * @see CloseableQueue
    */
   public interface CloseableDeque<T> extends CloseableQueue<T>, Deque<T> {
   }

   /**
    * A {@link BlockingDeque} that can be closed.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element contained in the deque
    * 
    * @see CloseableQueue
    */
   public interface CloseableBlockingDeque<T> extends CloseableDeque<T>, BlockingDeque<T> {
   }

   private final int totalCapacity;
   final Semaphore allowedItems;

   /**
    * Constructs a new factory.
    * 
    * @param totalCapacity the total capacity shared across all queues provided by this factory
    */
   public SharedCapacityDequeFactory(int totalCapacity) {
      this.totalCapacity = totalCapacity;
      this.allowedItems = new Semaphore(totalCapacity);
   }

   /**
    * Returns the total capacity for this factory.
    * 
    * @return the total capacity
    */
   public int totalCapacity() {
      return totalCapacity;
   }

   /**
    * Returns the combined size of all queues. This is a snapshot of the total number of elements in
    * all queues provided by this factory.
    * 
    * @return the combined size of all queues
    */
   public int combinedSize() {
      return totalCapacity - allowedItems.availablePermits();
   }

   /**
    * Returns the remaining capacity for this factory. This is the same as:
    * <pre>
    * totalCapacity() - combinedSize()
    * </pre>
    * 
    * @return the remaining capacity
    */
   public int remainingCapacity() {
      return allowedItems.availablePermits();
   }

   /**
    * Creates a new {@link BlockingDeque}. The returned deque uses the factory's shared capacity to
    * limit its number of elements. The actual implementation underlying the returned deque is
    * {@link LinkedBlockingDeque}. If you need an alternate implementation, use one of the various
    * {@link #shareCapacityWith} methods.
    * 
    * @return a new {@link BlockingDeque}.
    */
   public <T> CloseableBlockingDeque<T> createDeque() {
      return new SharedCapacityBlockingDeque<T>(new LinkedBlockingDeque<T>());
   }

   /**
    * Wraps the specified queue so that it shares the factory's capacity. The specified queue must
    * be empty.
    * 
    * <p>After this method returns, no items should be added or removed using the original
    * queue reference (until it is closed). All such operations should be performed using the
    * returned object. If operations are performed directly against the queue, the results are
    * undefined (but expect that the factory's total capacity could be exceeded and other strange
    * runtime exceptions could be thrown). The safest way to use this method is with a queue that
    * has <em>no</em> other references to it. This can be done like so:
    * <pre>{@code
    * CloseableQueue<MyObject> queue = factory.shareCapacityWith(new LinkedList<MyObject>()); 
    * }</pre> 
    * 
    * <p>If the queue already has a bound on capacity then it will be observed, too. At any given
    * time, the number of elements in the queue will not exceed the specified queue's limit. At the
    * same time, the number of elements across all queues provided by this factory, including the
    * one returned from this method, will not exceed the factory's total capacity.
    * 
    * @param queue the queue to wrap
    * @return a queue that shares capacity with this factory
    * @throws IllegalArgumentException if the specified queue is not empty
    */
   public <T> CloseableQueue<T> shareCapacityWith(Queue<T> queue) {
      return new SharedCapacityQueue<T>(queue);
   }

   /**
    * Wraps the specified deque so that it shares the factory's capacity.
    * 
    * @param deque the deque to wrap
    * @return a deque that shares capacity with this factory
    * @throws IllegalArgumentException if the specified deque is not empty
    * 
    * @see #shareCapacityWith(Queue)
    */
   public <T> CloseableDeque<T> shareCapacityWith(Deque<T> deque) {
      return new SharedCapacityDeque<T>(deque);
   }
   
   /**
    * Wraps the specified queue so that it shares the factory's capacity.
    * 
    * @param queue the queue to wrap
    * @return a queue that shares capacity with this factory
    * @throws IllegalArgumentException if the specified deque is not empty
    * 
    * @see #shareCapacityWith(Queue)
    */
   public <T> CloseableBlockingQueue<T> shareCapacityWith(BlockingQueue<T> queue) {
      return new SharedCapacityBlockingQueue<T>(queue);
   }

   /**
    * Wraps the specified deque so that it shares the factory's capacity.
    * 
    * @param deque the deque to wrap
    * @return a deque that shares capacity with this factory
    * @throws IllegalArgumentException if the specified deque is not empty
    * 
    * @see #shareCapacityWith(Queue)
    */
   public <T> CloseableBlockingDeque<T> shareCapacityWith(BlockingDeque<T> deque) {
      return new SharedCapacityBlockingDeque<T>(deque);
   }

   /**
    * An iterator that wraps another but tries to enforce the factory's total capacity.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of element retrieved by the iterator
    */
   private class SharedCapacityIterator<T> implements Iterator<T> {
      private final SharedCapacityQueue<T> queue;
      private final Iterator<T> delegate;
      
      SharedCapacityIterator(SharedCapacityQueue<T> queue, Iterator<T> delegate) {
         this.queue = queue;
         this.delegate = delegate;
      }

      @Override
      public boolean hasNext() {
         queue.checkClosed();
         return delegate.hasNext();
      }

      @Override
      public T next() {
         queue.checkClosed();
         return delegate.next();
      }

      @Override
      public void remove() {
         queue.lock();
         try {
            queue.checkClosed();
            int beforeSize = queue.size();
            delegate.remove();
            // Sadly, no real way to tell if item was actually removed. For blocking queues
            // with weakly consistent iterators, this can cause us to release a permit when
            // we shouldn't and subsequently allow total size of queues to exceed the max :(
            if (queue.size() < beforeSize) {
               allowedItems.release();
            }
         } finally {
            queue.unlock();
         }
      }
   }
   
   private class SharedCapacityQueue<T> implements CloseableQueue<T> {
      final Queue<T> delegate;
      // mutations are guarded by this lock to prevent race conditions with closing the queue
      private final ReadWriteLock lock = new ReentrantReadWriteLock();
      private final AtomicBoolean closed = new AtomicBoolean();
      
      SharedCapacityQueue(Queue<T> delegate) {
         if (!delegate.isEmpty()) {
            throw new IllegalArgumentException("can only share capacity with empty queue");
         }
         this.delegate = delegate;
      }
      
      Queue<T> delegate() {
         return delegate;
      }
      
      void checkClosed() {
         if (closed.get()) {
            throw new IllegalStateException("queue has been closed");
         }
      }
      
      void lock() {
         lock.readLock().lock();
      }
      
      void unlock() {
         lock.readLock().unlock();
      }
      
      @Override
      public boolean add(T e) {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            if (!allowedItems.tryAcquire()) {
               throw new IllegalArgumentException("shared capacity is full");
            }
            boolean release = true;
            try {
               boolean ret = delegate().add(e);
               release = false; // successful add, so keep the permit
               return ret;
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public boolean offer(T e) {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            if (!allowedItems.tryAcquire()) {
               return false;
            }
            if (delegate().offer(e)) {
               return true;
            }
            // offer failed so release permit
            allowedItems.release();
            return false;
         } finally {
            unlock();
         }
      }

      @Override
      public T remove() {
         lock();
         try {
            checkClosed();
            T ret = delegate().remove();
            allowedItems.release();
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T poll() {
         lock();
         try {
            checkClosed();
            T ret = delegate().poll();
            if (ret != null) {
               allowedItems.release();
            }
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T element() {
         checkClosed();
         return delegate().element();
      }

      @Override
      public T peek() {
         checkClosed();
         return delegate().peek();
      }

      @Override
      public int size() {
         checkClosed();
         return delegate().size();
      }

      @Override
      public boolean isEmpty() {
         checkClosed();
         return delegate().isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         checkClosed();
         return delegate().contains(o);
      }

      @Override
      public Iterator<T> iterator() {
         checkClosed();
         return new SharedCapacityIterator<T>(this, delegate().iterator());
      }

      @Override
      public Object[] toArray() {
         checkClosed();
         return delegate().toArray();
      }

      @Override
      public <E> E[] toArray(E[] a) {
         checkClosed();
         return delegate().toArray(a);
      }

      @Override
      public boolean remove(Object o) {
         lock();
         try {
            checkClosed();
            if (delegate().remove(o)) {
               allowedItems.release();
               return true;
            }
            return false;
         } finally {
            unlock();
         }
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         checkClosed();
         return delegate().containsAll(c);
      }

      @Override
      public boolean addAll(Collection<? extends T> c) {
         checkClosed();
         // need to do this one at a time in order to handle permits correctly
         boolean ret = false;
         for (T t : c) {
            if (add(t)) {
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         checkClosed();
         // need to do this one at a time in order to handle permits correctly
         boolean ret = false;
         for (Object o : c) {
            if (remove(o)) {
               ret = true;
            }
         }
         return ret;
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         checkClosed();
         if (c.size() > 100 && !(c instanceof Set)) {
            // try to improve performance of contains query
            c = new HashSet<Object>(c);
         }
         // need to do this one at a time in order to handle permits (mostly) correctly
         for (Iterator<T> iter = iterator(); iter.hasNext();) {
            if (!c.contains(iter.next())) {
               // Iterator.remove() is flawed, but no realistic alternative
               iter.remove();
            }
         }
         return false;
      }

      @Override
      public void clear() {
         checkClosed();
         // need to do this one at a time in order to handle permits correctly
         while (!isEmpty()) {
            remove();
         }
      }

      @Override
      public void close() {
         lock.writeLock().lock();
         try {
            if (closed.compareAndSet(false, true)) {
               allowedItems.release(delegate().size());
               delegate().clear();
            }
            // else, already closed
         } finally {
            lock.writeLock().unlock();
         }
      }
   }

   private class SharedCapacityDeque<T> extends SharedCapacityQueue<T>
         implements CloseableDeque<T> {
      
      SharedCapacityDeque(Deque<T> delegate) {
         super(delegate);
      }

      @Override Deque<T> delegate() {
         return (Deque<T>) delegate;
      }

      @Override
      public void addFirst(T e) {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            if (!allowedItems.tryAcquire()) {
               throw new IllegalArgumentException("shared capacity is full");
            }
            boolean release = true;
            try {
               delegate().addFirst(e);
               release = false; // successful add, so keep the permit
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public void addLast(T e) {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            if (!allowedItems.tryAcquire()) {
               throw new IllegalArgumentException("shared capacity is full");
            }
            boolean release = true;
            try {
               delegate().addLast(e);
               release = false; // successful add, so keep the permit
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public boolean offerFirst(T e) {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            if (!allowedItems.tryAcquire()) {
               return false;
            }
            if (delegate().offerFirst(e)) {
               return true;
            }
            // offer failed so release permit
            allowedItems.release();
            return false;
         } finally {
            unlock();
         }
      }

      @Override
      public boolean offerLast(T e) {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            if (!allowedItems.tryAcquire()) {
               return false;
            }
            if (delegate().offerLast(e)) {
               return true;
            }
            // offer failed so release permit
            allowedItems.release();
            return false;
         } finally {
            unlock();
         }
      }

      @Override
      public T removeFirst() {
         lock();
         try {
            checkClosed();
            T ret = delegate().removeFirst();
            allowedItems.release();
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T removeLast() {
         lock();
         try {
            checkClosed();
            T ret = delegate().removeLast();
            allowedItems.release();
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T pollFirst() {
         lock();
         try {
            checkClosed();
            T ret = delegate().pollFirst();
            if (ret != null) {
               allowedItems.release();
            }
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T pollLast() {
         lock();
         try {
            checkClosed();
            T ret = delegate().pollLast();
            if (ret != null) {
               allowedItems.release();
            }
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T getFirst() {
         checkClosed();
         return delegate().getFirst();
      }

      @Override
      public T getLast() {
         checkClosed();
         return delegate().getLast();
      }

      @Override
      public T peekFirst() {
         checkClosed();
         return delegate().peekFirst();
      }

      @Override
      public T peekLast() {
         checkClosed();
         return delegate().peekLast();
      }

      @Override
      public boolean removeFirstOccurrence(Object o) {
         lock();
         try {
            checkClosed();
            if (delegate().removeFirstOccurrence(o)) {
               allowedItems.release();
               return true;
            }
            return false;
         } finally {
            unlock();
         }
      }

      @Override
      public boolean removeLastOccurrence(Object o) {
         lock();
         try {
            checkClosed();
            if (delegate().removeLastOccurrence(o)) {
               allowedItems.release();
               return true;
            }
            return false;
         } finally {
            unlock();
         }
      }

      @Override
      public void push(T e) {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            if (!allowedItems.tryAcquire()) {
               throw new IllegalArgumentException("shared capacity is full");
            }
            boolean release = true;
            try {
               delegate().push(e);
               release = false; // successful add, so keep the permit
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public T pop() {
         lock();
         try {
            checkClosed();
            T ret = delegate().pop();
            allowedItems.release();
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public Iterator<T> descendingIterator() {
         checkClosed();
         return new SharedCapacityIterator<T>(this, delegate().descendingIterator());
      }
}

   private class SharedCapacityBlockingQueue<T> extends SharedCapacityQueue<T>
         implements CloseableBlockingQueue<T> {
      
      SharedCapacityBlockingQueue(BlockingQueue<T> delegate) {
         super(delegate);
      }

      @Override BlockingQueue<T> delegate() {
         return (BlockingQueue<T>) delegate;
      }

      @Override
      public void put(T e) throws InterruptedException {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            allowedItems.acquire();
            boolean release = true;
            try {
               delegate().put(e);
               release = false; // successful add, so keep the permit
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            long start = System.nanoTime();
            if (!allowedItems.tryAcquire(timeout, unit)) {
               return false;
            }
            boolean release = true;
            try {
               // compute remaining time
               long elapsed = System.nanoTime() - start;
               if (delegate().offer(e, unit.toNanos(timeout) - elapsed, TimeUnit.NANOSECONDS)) {
                  release = false; // successful add, so keep the permit
                  return true;
               }
               return false;
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public T take() throws InterruptedException {
         lock();
         try {
            checkClosed();
            T ret = delegate().take();
            allowedItems.release();
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T poll(long timeout, TimeUnit unit) throws InterruptedException {
         lock();
         try {
            checkClosed();
            T ret = delegate().poll(timeout, unit);
            if (ret != null) {
               allowedItems.release();
            }
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public int remainingCapacity() {
         checkClosed();
         return Math.min(SharedCapacityDequeFactory.this.remainingCapacity(),
               delegate().remainingCapacity());
      }

      @Override
      public int drainTo(Collection<? super T> c) {
         lock();
         try {
            checkClosed();
            int numItems = delegate().drainTo(c);
            allowedItems.release(numItems);
            return numItems;
         } finally {
            unlock();
         }
      }

      @Override
      public int drainTo(Collection<? super T> c, int maxElements) {
         lock();
         try {
            checkClosed();
            int numItems = delegate().drainTo(c, maxElements);
            allowedItems.release(numItems);
            return numItems;
         } finally {
            unlock();
         }
      }
   }

   private class SharedCapacityBlockingDeque<T> extends SharedCapacityDeque<T>
         implements CloseableBlockingDeque<T> {
      
      SharedCapacityBlockingDeque(BlockingDeque<T> delegate) {
         super(delegate);
      }
      
      @Override BlockingDeque<T> delegate() {
         return (BlockingDeque<T>) delegate;
      }

      @Override
      public void put(T e) throws InterruptedException {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            allowedItems.acquire();
            boolean release = true;
            try {
               delegate().put(e);
               release = false; // successful add, so keep the permit
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            long start = System.nanoTime();
            if (!allowedItems.tryAcquire(timeout, unit)) {
               return false;
            }
            boolean release = true;
            try {
               // compute remaining time
               long elapsed = System.nanoTime() - start;
               if (delegate().offer(e, unit.toNanos(timeout) - elapsed, TimeUnit.NANOSECONDS)) {
                  release = false; // successful add, so keep the permit
                  return true;
               }
               return false;
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public T take() throws InterruptedException {
         lock();
         try {
            checkClosed();
            T ret = delegate().take();
            allowedItems.release();
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T poll(long timeout, TimeUnit unit) throws InterruptedException {
         lock();
         try {
            checkClosed();
            T ret = delegate().poll(timeout, unit);
            if (ret != null) {
               allowedItems.release();
            }
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public int remainingCapacity() {
         checkClosed();
         return Math.min(SharedCapacityDequeFactory.this.remainingCapacity(),
               delegate().remainingCapacity());
      }

      @Override
      public int drainTo(Collection<? super T> c) {
         lock();
         try {
            checkClosed();
            int numItems = delegate().drainTo(c);
            allowedItems.release(numItems);
            return numItems;
         } finally {
            unlock();
         }
      }

      @Override
      public int drainTo(Collection<? super T> c, int maxElements) {
         lock();
         try {
            checkClosed();
            int numItems = delegate().drainTo(c, maxElements);
            allowedItems.release(numItems);
            return numItems;
         } finally {
            unlock();
         }
      }

      @Override
      public void putFirst(T e) throws InterruptedException {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            allowedItems.acquire();
            boolean release = true;
            try {
               delegate().putFirst(e);
               release = false; // successful add, so keep the permit
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public void putLast(T e) throws InterruptedException {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            allowedItems.acquire();
            boolean release = true;
            try {
               delegate().putLast(e);
               release = false; // successful add, so keep the permit
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public boolean offerFirst(T e, long timeout, TimeUnit unit) throws InterruptedException {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            long start = System.nanoTime();
            if (!allowedItems.tryAcquire(timeout, unit)) {
               return false;
            }
            boolean release = true;
            try {
               // compute remaining time
               long elapsed = System.nanoTime() - start;
               if (delegate().offerFirst(e, unit.toNanos(timeout) - elapsed, TimeUnit.NANOSECONDS)) {
                  release = false; // successful add, so keep the permit
                  return true;
               }
               return false;
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public boolean offerLast(T e, long timeout, TimeUnit unit) throws InterruptedException {
         lock();
         try {
            checkClosed();
            if (e == null) {
               throw new NullPointerException();
            }
            long start = System.nanoTime();
            if (!allowedItems.tryAcquire(timeout, unit)) {
               return false;
            }
            boolean release = true;
            try {
               // compute remaining time
               long elapsed = System.nanoTime() - start;
               if (delegate().offerLast(e, unit.toNanos(timeout) - elapsed, TimeUnit.NANOSECONDS)) {
                  release = false; // successful add, so keep the permit
                  return true;
               }
               return false;
            } finally {
               if (release) {
                  allowedItems.release();
               }
            }
         } finally {
            unlock();
         }
      }

      @Override
      public T takeFirst() throws InterruptedException {
         lock();
         try {
            checkClosed();
            T ret = delegate().takeFirst();
            allowedItems.release();
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T takeLast() throws InterruptedException {
         lock();
         try {
            checkClosed();
            T ret = delegate().takeLast();
            allowedItems.release();
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T pollFirst(long timeout, TimeUnit unit) throws InterruptedException {
         lock();
         try {
            checkClosed();
            T ret = delegate().pollFirst(timeout, unit);
            if (ret != null) {
               allowedItems.release();
            }
            return ret;
         } finally {
            unlock();
         }
      }

      @Override
      public T pollLast(long timeout, TimeUnit unit) throws InterruptedException {
         lock();
         try {
            checkClosed();
            T ret = delegate().pollLast(timeout, unit);
            if (ret != null) {
               allowedItems.release();
            }
            return ret;
         } finally {
            unlock();
         }
      }
   }
}
