package com.apriori.util;

import com.apriori.possible.Optional;

/**
 * A partial function that computes a value from three inputs.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I1> the type of the first input
 * @param <I2> the type of the second input
 * @param <I3> the type of the three input
 * @param <O> the output type
 */
@FunctionalInterface
public interface PartialTriFunction<I1, I2, I3, O> extends TriFunction<I1, I2, I3, Optional<O>> {
}