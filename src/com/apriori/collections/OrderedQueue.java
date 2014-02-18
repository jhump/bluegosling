package com.apriori.collections;

import java.util.Comparator;
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
}
