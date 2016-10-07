package com.bluegosling.function;

import com.bluegosling.possible.Optionals;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A partial function that computes a value from two inputs.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the first input
 * @param <U> the type of the second input
 * @param <R> the result type
 */
@FunctionalInterface
public interface PartialBiFunction<T, U, R> extends BiFunction<T, U, Optional<R>> {
   /**
    * Returns a composed function that first applies this function to
    * its input, and then applies the {@code after} function to the result.
    * If evaluation of either function throws an exception, it is relayed to
    * the caller of the composed function.
    *
    * @param <V> the type of output of the {@code after} function, and of the
    *           composed function
    * @param after the function to apply after this function is applied
    * @return a composed function that first applies this function and then
    * applies the {@code after} function
    * @throws NullPointerException if after is null
    *
    * @see #andThen(Function)
    */
   default <V> PartialBiFunction<T, U, V> maybeThen(Function<? super R, ? extends V> after) {
       Objects.requireNonNull(after);
       return (T t, U u) -> {
          Optional<R> r = apply(t, u);
          return r.isPresent() ? Optional.ofNullable(after.apply(r.get())) : Optional.empty();
       };
   }

   /**
    * Returns a composed function that first applies this function to
    * its input, and then applies the {@code after} function to the result.
    * If evaluation of either function throws an exception, it is relayed to
    * the caller of the composed function.
    *
    * @param <V> the type of output of the {@code after} function, and of the
    *           composed function
    * @param after the function to apply after this function is applied
    * @return a composed function that first applies this function and then
    * applies the {@code after} function
    * @throws NullPointerException if after is null
    *
    * @see #andThen(Function)
    */
   default <V> PartialBiFunction<T, U, V> maybeChain(PartialFunction<? super R, ? extends V> after) {
       Objects.requireNonNull(after);
       return (T t, U u) -> {
          Optional<R> r = apply(t, u);
          return r.isPresent() ? Optionals.upcast(after.apply(r.get())) : Optional.empty();
       };
   }
}
