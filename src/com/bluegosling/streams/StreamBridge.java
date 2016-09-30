package com.bluegosling.streams;

import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bluegosling.choice.Either;
import com.bluegosling.collections.MoreSpliterators;

/**
 * A stream bridge provides a non-trivial intermediate operation, or stage, in a stream. Some kinds
 * of operations cannot be easily modeled using a {@link StreamOperator}, so this interface is more
 * flexible and open-ended. For example, this stage allows conversion from parallel to sequential
 * that preserves the property of previous operations. In other words, the stages up to this one
 * can run in parallel and then this and subsequent streams can be sequential (or vice versa). This
 * kind of stage also allows for transformation of the encounter order (like sorting), which is not
 * easily accomplished with a {@link StreamOperator}.  
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of element produced by the stage
 * @param <U> the type of upstream element consumed by the stage
 */
@FunctionalInterface
public interface StreamBridge<T, U> {
   /**
    * Accepts the given source of data and returns the results of the operation as either another
    * stream or as a spliterator.
    * 
    * <p>It is recommended that the returned stream or spliterator <em>defer</em> access to its
    * data source. This can be accomplished by using a {@link Supplier supplier} as the backing
    * source for the {@linkplain StreamSupport#stream(Supplier, int, boolean) stream} or
    * {@linkplain MoreSpliterators#lazySpliterator(Supplier, int) spliterator}.
    *  
    * @param source the data provided by the upstream stage
    * @return the resulting data that is fed to downstream stages
    */
   Either<Stream<T>, Spliterator<T>> bridgeFrom(Spliterator<U> source);
}
