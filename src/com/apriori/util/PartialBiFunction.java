package com.apriori.util;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * A partial function that computes a value from two inputs.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I1> the type of the first input
 * @param <I2> the type of the second input
 * @param <O> the output type
 */
@FunctionalInterface
public interface PartialBiFunction<I1, I2, O> extends BiFunction<I1, I2, Optional<O>> {
}