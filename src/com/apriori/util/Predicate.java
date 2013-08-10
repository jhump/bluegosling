package com.apriori.util;

/**
 * Like a {@link Function}, but returns a primitive boolean value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the argument type
 */
public interface Predicate<T> {
   /**
    * Tests an object.
    * 
    * @param input the single argument
    * @return the predicate's result
    */
   boolean test(T input);
   
   /**
    * A {@link Function.Bivariate function} that returns a boolean value.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T1> the first argument type
    * @param <T2> the second argument type
    */
   interface Bivariate<T1, T2> {
      /**
       * Tests two objects.
       * 
       * @param input1 the first argument
       * @param input2 the second argument
       * @return the predicate's result
       */
      boolean test(T1 input1, T2 input2);
   }
   
   /**
    * A {@link Function.Trivariate function} that returns a boolean value.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T1> the first argument type
    * @param <T2> the second argument type
    * @param <T3> the third argument type
    */
   interface Trivariate<T1, T2, T3> {
      /**
       * Tests three objects.
       * 
       * @param input1 the first argument
       * @param input2 the second argument
       * @param input3 the third argument
       * @return the predicate's result
       */
      boolean test(T1 input1, T2 input2, T3 input3);
   }
}
