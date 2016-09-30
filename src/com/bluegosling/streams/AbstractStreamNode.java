package com.bluegosling.streams;

import java.util.function.Consumer;

/**
 * A useful base class for {@link StreamNode} implementations. This simplifies the task of
 * interacting with upstream producers for the common pattern of querying upstream and then acting
 * on the produced item.
 * 
 * <p>Concrete instances implement {@link #getNext(Upstream, Consumer)}, which is given both the
 * upstream producer from which to pull data and the downstream consumer to which to push data.
 * Sub-classes then use {@link #nextUpstream(Upstream)} to pull items, followed by
 * {@link #latestUpstream()} to query for the pulled item.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of element produced by this node
 * @param <U> the type of upstream element consumed by this node
 */
public abstract class AbstractStreamNode<T, U> implements StreamNode<T, U> {
   private final Consumer<U> upstreamReceiver;
   private U latestUpstream;
   
   /**
    * Constructs a new node.
    */
   protected AbstractStreamNode() {
      this.upstreamReceiver = this::setUpstream;
   }
   
   private void setUpstream(U value) {
      this.latestUpstream = value;
   }
   
   /**
    * Queries for the latest element pulled from the upstream node in the stream pipeline.
    * 
    * @return the latest element pulled from upstream
    */
   protected U latestUpstream() {
      return latestUpstream;
   }
   
   /**
    * Pulls data from the given upstream producer, returning whether or not data was pulled. If no
    * data was pulled, that means that the upstream producer is finished and has no more data.
    * 
    * @param upstream the upstream producer
    * @return true if an element was pulled or false if the upstream producer is finished
    * @see #latestUpstream()
    */
   protected boolean nextUpstream(Upstream<U> upstream) {
      return upstream.getUpstream(upstreamReceiver);
   }
}
