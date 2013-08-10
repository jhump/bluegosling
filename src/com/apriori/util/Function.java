package com.apriori.util;


/**
 * A functional interface that accepts one argument and returns a value. This can be used in
 * functional style programming and is similar to a lambda (although constrained to one argument
 * and one return value).
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I> the "input" type of the single argument
 * @param <O> the "output" type of the returned value
 */
public interface Function<I, O> {
   /**
    * Invokes the function with the specified argument and returns the result.
    * 
    * @param input the single argument
    * @return the function's result
    */
   O apply(I input);
   
   /**
    * Just like a {@link Function} except that it takes two arguments instead of just one.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I1> the type of the first argument
    * @param <I2> the type of the second argument
    * @param <O> the result type
    */
   interface Bivariate<I1, I2, O> {
      /**
       * Invokes the function with the specified arguments and returns the result.
       * 
       * @param input1 the first argument
       * @param input2 the second argument
       * @return the function's result
       */
      O apply(I1 input1, I2 input2);
   }
   
   /**
    * Just like a {@link Function} except that it takes three arguments instead of just one.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I1> the type of the first argument
    * @param <I2> the type of the second argument
    * @param <I3> the type of the third argument
    * @param <O> the result type
    */
   interface Trivariate<I1, I2, I3, O> {
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
}
