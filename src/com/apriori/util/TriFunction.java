package com.apriori.util;


/**
 * A functional interface that accepts three arguments and returns a value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I1> the type of the first argument
 * @param <I2> the type of the second argument
 * @param <I3> the type of the third argument
 * @param <O> the result type
 */
@FunctionalInterface
public interface TriFunction<I1, I2, I3, O> {
   /**
    * Invokes the function with the specified arguments and returns the result.
    * 
    * @param input1 the first argument
    * @param input2 the second argument
    * @param input3 the third argument
    * @return the function's result
    */
   O apply(I1 input1, I2 input2, I3 input3);
}
