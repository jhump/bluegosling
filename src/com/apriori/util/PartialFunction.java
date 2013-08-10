package com.apriori.util;

import com.apriori.possible.Optional;

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
public interface PartialFunction<I, O> extends Function<I, Optional<O>> {

   /**
    * A partial function that computes a value from two inputs.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I1> the type of the first input
    * @param <I2> the type of the second input
    * @param <O> the output type
    */
   interface Bivariate<I1, I2, O> extends Function.Bivariate<I1, I2, Optional<O>> {
   }

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
   interface Trivariate<I1, I2, I3, O> extends Function.Trivariate<I1, I2, I3, Optional<O>> {
   }
}
