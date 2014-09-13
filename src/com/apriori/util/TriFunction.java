package com.apriori.util;

import java.util.Objects;
import java.util.function.Function;


/**
 * A functional interface that accepts three arguments and returns a value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <V> the type of the third argument
 * @param <R> the result type
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {
   /**
    * Invokes the function with the specified arguments and returns the result.
    * 
    * @param input1 the first argument
    * @param input2 the second argument
    * @param input3 the third argument
    * @return the function's result
    */
   R apply(T input1, U input2, V input3);

   /**
    * Returns a composed function that first applies this function to its input, and then applies
    * the {@code after} function to the result. If evaluation of either function throws an
    * exception, it is relayed to the caller of the composed function.
    *
    * @param <W> the type of output of the {@code after} function, and of the composed function
    * @param after the function to apply after this function is applied
    * @return a composed function that first applies this function and then applies the
    *         {@code after} function
    * @throws NullPointerException if after is null
    */
   default <W> TriFunction<T, U, V, W> andThen(Function<? super R, ? extends W> after) {
      Objects.requireNonNull(after);
      return (T t, U u, V v) -> after.apply(apply(t, u, v));
   }
}
