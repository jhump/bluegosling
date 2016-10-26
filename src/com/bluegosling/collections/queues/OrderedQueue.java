package com.bluegosling.collections.queues;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;

/**
 * A queue that retrieves items in priority order instead of in LIFO order. This sort of queue is
 * generally backed by a <a href="http://en.wikipedia.org/wiki/Heap_(data_structure)">heap</a>.
 * 
 * <p>The Java Collections Framework includes such a queue, {@link java.util.PriorityQueue}, that is
 * backed by a binary heap. Although that class does not actually implement this interface, it could
 * since it contains all of the necessary methods. This interface exists to provide a contract for
 * other implementations of ordered queues.
 * 
 * <p>Note that this interface is a simple extension of the standard {@link Queue} interface. This
 * package also includes a separate {@link PriorityQueue} interface, that provides an alternate
 * contract with additional operations, that is incompatible with that standard interface.
 *
 * @param <E> the type of element stored in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see java.util.PriorityQueue
 * @see PriorityQueue
 */
public interface OrderedQueue<E> extends Queue<E> {
   /**
    * The comparator used to compare elements and determine their order. If two elements are equal
    * according to the comparator, the order of when the elements will be retrieved from the queue
    * is undefined.
    * 
    * <p>For queues that order elements according to their {@linkplain Comparable natural ordering},
    * this method will return {@code null}.
    *
    * @return the comparator used to compare elements or null if elements are compared according to
    *       their natural ordering
    */
   Comparator<? super E> comparator();
   
   /**
    * Adapts the JRE's {@link java.util.PriorityQueue PriorityQueue} to this interface.
    *
    * @param queue a priority queue
    * @return a view of the given priority queue as an {@link OrderedQueue}
    */
   static <E> OrderedQueue<E> fromJrePriorityQueue(java.util.PriorityQueue<E> queue) {
      return new OrderedQueue<E>() {
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
         public Comparator<? super E> comparator() {
            return queue.comparator();
         }
      };
   }
}
