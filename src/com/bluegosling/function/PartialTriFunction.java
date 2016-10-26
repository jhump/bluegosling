package com.bluegosling.function;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A partial function that computes a value from three inputs.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the first input
 * @param <U> the type of the second input
 * @param <V> the type of the three input
 * @param <R> the result type
 */
@FunctionalInterface
public interface PartialTriFunction<T, U, V, R> extends TriFunction<T, U, V, Optional<R>> {
   /**
    * Returns a composed function that first applies this function to
    * its input, and then applies the {@code after} function to the result.
    * If evaluation of either function throws an exception, it is relayed to
    * the caller of the composed function.
    *
    * @param <W> the type of output of the {@code after} function, and of the
    *           composed function
    * @param after the function to apply after this function is applied
    * @return a composed function that first applies this function and then
    * applies the {@code after} function
    * @throws NullPointerException if after is null
    */
   default <W> PartialTriFunction<T, U, V, W> maybeThen(Function<? super R, ? extends W> after) {
       Objects.requireNonNull(after);
       return (T t, U u, V v) -> {
          Optional<R> r = apply(t, u, v);
          return r.isPresent() ? Optional.ofNullable(after.apply(r.get())) : Optional.empty();
       };
   }

   /**
    * Returns a composed function that first applies this function to
    * its input, and then applies the {@code after} function to the result.
    * If evaluation of either function throws an exception, it is relayed to
    * the caller of the composed function.
    *
    * @param <W> the type of output of the {@code after} function, and of the
    *           composed function
    * @param after the function to apply after this function is applied
    * @return a composed function that first applies this function and then
    * applies the {@code after} function
    * @throws NullPointerException if after is null
    */
   default <W> PartialTriFunction<T, U, V, W> maybeChain(
         PartialFunction<? super R, ? extends W> after) {
       Objects.requireNonNull(after);
       return (T t, U u, V v) -> {
          Optional<R> r = apply(t, u, v);
          return r.isPresent() ? PartialFunctions.cast(after.apply(r.get())) : Optional.empty();
       };
   }
}
