package com.apriori.util;

import com.apriori.possible.Optional;

import java.util.function.Function;

/**
 * A function that does not fully map the input domain to the output domain. This is reflected in
 * the API by returning {@link Optional#none() no value} when a result cannot be computed for a
 * given input.
 * 
 * <p>Note that the use of {@link Optional} means that {@code null} results for a valid input are
 * not allowed.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I> the input type
 * @param <O> the output type
 */
@FunctionalInterface
public interface PartialFunction<I, O> extends Function<I, Optional<O>> {
}
