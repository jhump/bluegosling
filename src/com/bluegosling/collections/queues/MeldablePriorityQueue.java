package com.bluegosling.collections.queues;

// TODO: javadoc
public interface MeldablePriorityQueue<E, P, Q extends MeldablePriorityQueue<? extends E, ? extends P, ?>> 
      extends PriorityQueue<E, P> {

   boolean mergeFrom(Q other);
}
