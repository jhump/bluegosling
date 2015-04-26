package com.apriori.collections;

import java.util.AbstractQueue;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An abstract base class for implementations of the {@link Deque} interface. Deques provide a very
 * broad API, including the operations of both a FIFO queue, and a LIFO stack, in addition to the
 * numerous operations that work on either head or tail. Most of these methods are aliases for other
 * methods (for readability in specific use cases), so this base class implements them to delegate
 * to the appropriate method.
 * 
 * <p>Sub-classes need only operate the handful of core operations without the boiler-plate of
 * implementing these other alias methods.
 *
 * @param <E> the type of element stored in the deque
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractDeque<E> extends AbstractQueue<E> implements Deque<E> {

   @Override
   public boolean add(E e) {
      addLast(e);
      return true;
   }
   
   @Override
   public void addFirst(E e) {
      if (!offerFirst(e)) {
         throw new IllegalStateException("Queue full");
      }
   }

   @Override
   public void addLast(E e) {
      if (!offerLast(e)) {
         throw new IllegalStateException("Queue full");
      }
   }

   @Override
   public E remove() {
      return removeFirst();
   }

   @Override
   public E removeFirst() {
      E x = pollFirst();
      if (x != null) {
          return x;
      } else {
          throw new NoSuchElementException();
      }
   }

   @Override
   public E removeLast() {
      E x = pollLast();
      if (x != null) {
          return x;
      } else {
          throw new NoSuchElementException();
      }
   }
   
   @Override
   public E element() {
      return getFirst();
   }

   @Override
   public E getFirst() {
      E x = peekFirst();
      if (x != null) {
          return x;
      } else {
          throw new NoSuchElementException();
      }
   }

   @Override
   public E getLast() {
      E x = peekLast();
      if (x != null) {
          return x;
      } else {
          throw new NoSuchElementException();
      }
   }

   @Override
   public boolean remove(Object o) {
      return removeFirstOccurrence(o);
   }

   @Override
   public boolean removeFirstOccurrence(Object o) {
      return super.remove(o);
   }

   @Override
   public boolean removeLastOccurrence(Object o) {
      // Same implementation as AbstractCollection#remove(Object), except using descendingIterator()
      Iterator<E> it = descendingIterator();
      if (o == null) {
         while (it.hasNext()) {
            if (it.next() == null) {
               it.remove();
               return true;
            }
         }
      } else {
         while (it.hasNext()) {
            if (o.equals(it.next())) {
               it.remove();
               return true;
            }
         }
      }
      return false;
   }

   @Override
   public boolean offer(E e) {
      return offerLast(e);
   }

   @Override
   public E poll() {
      return pollFirst();
   }

   @Override
   public E peek() {
      return peekFirst();
   }

   @Override
   public void push(E e) {
      addFirst(e);
   }

   @Override
   public E pop() {
      return removeFirst();
   }
}
