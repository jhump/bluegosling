package com.bluegosling.collections;

import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A double-ended form of an ordered queue. This interface is very similar to mixing an
 * {@link OrderedQueue} with a {@link Deque}. However, deques provide operations for adding
 * elements to either end, whereas an ordered queue just provides the single means to add elements,
 * and their position in the queue will be based on their ordering.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the deque
 */
// TODO: javadoc
public interface OrderedDeque<E> extends OrderedQueue<E> {

   Iterator<E> descendingIterator();

   E pollFirst();

   E pollLast();
   
   @Override
   default E poll() {
      return pollFirst();
   }

   E peekFirst();

   E peekLast();

   @Override
   default E peek() {
      return peekFirst();
   }

   default E removeFirst() {
      E e = pollFirst();
      if (e == null) {
         throw new NoSuchElementException();
      }
      return e;
   }

   default E removeLast() {
      E e = pollLast();
      if (e == null) {
         throw new NoSuchElementException();
      }
      return e;
   }
   
   @Override
   default E remove() {
      return removeFirst();
   }
   
   default E getFirst() {
      E e = peekFirst();
      if (e == null) {
         throw new NoSuchElementException();
      }
      return e;
   }

   default E getLast() {
      E e = peekLast();
      if (e == null) {
         throw new NoSuchElementException();
      }
      return e;
   }
   
   @Override
   default E element() {
      return getFirst();
   }

   default boolean removeFirstOccurrence(Object o) {
      Iterator<E> it = iterator();
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

   default boolean removeLastOccurrence(Object o) {
      return CollectionUtils.removeObject(o, iterator(), true);
   }
   
   @Override default boolean remove(Object o) {
      return CollectionUtils.removeObject(o,descendingIterator(), true);
   }
}
