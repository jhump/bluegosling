package com.bluegosling.collections.queues;

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.collect.MinMaxPriorityQueue;

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
   
   static <E> OrderedDeque<E> fromMinMaxPriorityQueue(MinMaxPriorityQueue<E> queue) {
      return new OrderedDeque<E>() {

         @Override
         public Comparator<? super E> comparator() {
            return queue.comparator();
         }

         @Override
         public boolean add(E e) {
            return queue.add(e);
         }

         @Override
         public boolean offer(E e) {
            return queue.offer(e);
         }

         @Override
         public E remove() {
            return queue.remove();
         }

         @Override
         public E poll() {
            return queue.poll();
         }

         @Override
         public E element() {
            return queue.element();
         }

         @Override
         public E peek() {
            return queue.peek();
         }

         @Override
         public int size() {
            return queue.size();
         }

         @Override
         public boolean isEmpty() {
            return queue.isEmpty();
         }

         @Override
         public boolean contains(Object o) {
            return queue.contains(o);
         }

         @Override
         public Iterator<E> iterator() {
            return queue.iterator();
         }

         @Override
         public Object[] toArray() {
            return queue.toArray();
         }

         @Override
         public <T> T[] toArray(T[] a) {
            return queue.toArray(a);
         }

         @Override
         public boolean remove(Object o) {
            return queue.remove(o);
         }

         @Override
         public boolean containsAll(Collection<?> c) {
            return queue.containsAll(c);
         }

         @Override
         public boolean addAll(Collection<? extends E> c) {
            return queue.addAll(c);
         }

         @Override
         public boolean removeAll(Collection<?> c) {
            return queue.removeAll(c);
         }

         @Override
         public boolean retainAll(Collection<?> c) {
            return queue.retainAll(c);
         }

         @Override
         public void clear() {
            queue.clear();
         }

         @Override
         public E pollFirst() {
            return queue.pollFirst();
         }

         @Override
         public E pollLast() {
            return queue.pollLast();
         }

         @Override
         public E peekFirst() {
            return queue.peekFirst();
         }

         @Override
         public E peekLast() {
            return queue.peekLast();
         }
         
         @Override
         public E removeFirst() {
            return queue.removeFirst();
         }

         @Override
         public E removeLast() {
            return queue.removeLast();
         }
      };
   }
}
