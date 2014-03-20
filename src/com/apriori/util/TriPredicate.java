package com.apriori.util;

/**
 * Like a {@link TriFunction}, but returns a primitive boolean value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
    * @param <T1> the first argument type
    * @param <T2> the second argument type
    * @param <T3> the third argument type
 */
public interface TriPredicate<T1, T2, T3> {
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
