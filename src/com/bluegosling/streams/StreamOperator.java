package com.bluegosling.streams;

/**
 * A stream operator provides a simople intermediate operation, or stage, in a stream. These are
 * non-terminal operations that accept data produced by the upstream operation, perform some sort
 * of transformation on the data, and then send the results to downstream operations.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of element produced by the operation
 * @param <U> the type of upstream element consumed by the operation
 */
@FunctionalInterface
@SuppressWarnings("try") // for javac, since #close() could throw InterruptedException
public interface StreamOperator<T, U> extends AutoCloseable {
   /**
    * Performs initialization needed prior to processing the stream. The default implementation
    * does nothing.
    */
   default void startStream() {
   }

   /**
    * Performs clean-up needed after processing of the stream completes. The default implementation
    * does nothing.
    */
   @Override
   default void close() throws Exception {
   }

   /**
    * Adjusts the input characteristics, returning the output characteristics. The given value
    * represents the characteristics of the upstream stage of the stream. If this operator modifies
    * the stream in a way that alters the characteristics it emits to downstream stages, it should
    * return an adjusted set of characteristics.
    * 
    * <p>The default implementation returns the given characteristics unchanged.
    * 
    * @param upstreamCharacteristics the characteristics of the upstream stage
    * @return the characteristics of the data that this stage provides
    */
   default int spliteratorCharacteristics(int upstreamCharacteristics) {
      return upstreamCharacteristics;
   }
   
   /**
    * Adjusts the estimated size of the data source. The given value represents the estimated size
    * of the data source provided by the upstream stage. If this operator changes the number of
    * elements emitted to downstream stages, it can adjust the size estimate via this method.
    * 
    * <p>The default implementation returns the given estimate unchanged.
    * 
    * @param upstreamEstimatedSize the estimated size of the upstream stage
    * @return the estimated size of the data that this stage provides
    */
   default long spliteratorEstimatedSize(long upstreamEstimatedSize) {
      return upstreamEstimatedSize;
   }

   /**
    * Constructs a node that will process data in the stream. For a sequential stream, this will be
    * invoked once to produce a single processing node. For a parallel stream, this will be invoked
    * once per thread that processes the stream, each node being a single-threaded component of
    * processing.
    * 
    * @return a node that will process data in the stream
    */
   StreamNode<T, U> createNode();
}
