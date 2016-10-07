package com.bluegosling.streams;

import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A node in a stream pipeline. A pipeline consists of one or more "stages" (aka
 * {@linkplain StreamOperator operators}). Nodes are created for intermediate stages during
 * processing. For a sequential stream, each stage corresponds to one node. But for a parallel
 * stream, there may be multiple nodes for the same stage, each running concurrently.
 * 
 * <p>The primary API of the node is to pull data from the upstream stage and then push the results
 * downstream. This is done one item at a time in a manner very much like traversing the elements in
 * a {@link Spliterator}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of element produced by this node
 * @param <U> the type of upstream element consumed by this node
 */
@FunctionalInterface
public interface StreamNode<T, U> {
   /**
    * Called by a downstream consumer to pull an element from this node. This node uses the given
    * upstream producer to pull data and then given consumer to push a result. This method should
    * invoke the given consumer <em>at most once</em>. If it does invoke the consumer, it must
    * return {@code true}; otherwise it must return {@code false}. Returning false indicates to the
    * downstream consumer that there is no more data to be produced.
    * 
    * @param upstream the upstream producer
    * @param action the downstream consumer
    * @return true if an element was pushed to the consumer or false if there is no more data
    */
   boolean getNext(Upstream<U> upstream, Consumer<? super T> action);

   /**
    * The interface used to pull data from an upstream node.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of data produced by the upstream node
    */
   @FunctionalInterface
   public interface Upstream<T> {
      /**
       * Gets the next element from the upstream producer. If successful, the supplied consumer is
       * invoked and passed the element. Otherwise, the method returns false and the consumer will
       * not be invoked.
       * 
       * <p>It is not a coincidence that this interface has the same lambda shape as the
       * {@link Spliterator#tryAdvance(Consumer)} method. The source of a stream, and thus the
       * upstream producer for the very first stage in the pipeline, is a {@link Spliterator}.
       * 
       * @param action the consumer of the element
       * @return true if data was fetched and supplied to the given consumer; false if there is no
       *       more data
       */
      boolean getUpstream(Consumer<? super T> action);
   }
}
