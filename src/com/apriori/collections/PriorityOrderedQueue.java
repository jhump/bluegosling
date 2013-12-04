package com.apriori.collections;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * A queue that retrieves items in priority order instead of in LIFO order. This sort of queue is
 * generally backed by a <a href="http://en.wikipedia.org/wiki/Heap_(data_structure)">heap</a>.
 * 
 * <p>The Java Collections Framework includes such a queue, {@link PriorityQueue}, that is backed by
 * a binary heap. Although that implementation does not actually implement this interface, it could
 * since it contains all of the necessary methods. This interface exists to provide a contract for
 * other implementations of priority-ordered queues, such as {@link FibonacciHeapQueue}.
 *
 * @param <E> the type of element stored in the queue
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see PriorityQueue
 */
public interface PriorityOrderedQueue<E> extends Queue<E> {
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
}
