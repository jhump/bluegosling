package com.apriori.collections;

/**
 * A {@link OrderedQueue} that can be merged with another efficiently. Several types of 
 * priority queue implementations support efficient merge operations (such as binomial, skew, and
 * fibonacci heaps). This category of priority queue is often known as a meldable queue.
 *
 * @param <E> the type of element in the queue
 * @param <Q> the type of queue with which this one can be merged
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface MeldableOrderedQueue<E, Q extends MeldableOrderedQueue<? extends E, ?>>
      extends OrderedQueue<E> {
   
   /**
    * Merges the specified queue into this queue. The other queue will be empty when this operation
    * completes and this queue will have the union of its own prior contents and those of the
    * specified queue.
    * 
    * <p>This is effectively the same as the following:<pre>
    * MeldablePriorityQueue&lt;T&gt; queue1, queue2;
    * // merge queue2 into queue1
    * queue1.addAll(queue2);
    * queue2.clear(); 
    * </pre>
    * The main difference is that this specialized method can perform the merge in constant time
    * (possibly amortized constant time). Without this method, such an operation would likely
    * instead take linear or <em>O(n log n)</em> time.
    * 
    * @param other the queue whose contents are merged into this queue
    * @return true if both queues were modified as a result; false if the specified queue was empty
    */
   boolean mergeFrom(Q other);
}
