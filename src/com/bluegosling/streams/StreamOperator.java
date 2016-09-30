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
public interface StreamOperator<T, U> extends AutoCloseable {
   default void startStream() {
   }

   @Override
   default void close() throws Exception {
   }

   default int spliteratorCharacteristics(int upstreamCharacteristics) {
      return upstreamCharacteristics;
   }
   
   default long spliteratorEstimatedSize(long upstreamEstimatedSize) {
      return upstreamEstimatedSize;
   }

   StreamNode<T, U> createNode();
}
