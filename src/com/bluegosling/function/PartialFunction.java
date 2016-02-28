package com.bluegosling.function;

import com.bluegosling.possible.Optionals;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A function that does not fully map the input domain to the output domain. This is reflected in
 * the API by returning {@link Optional#empty() no value} when a result cannot be computed for a
 * given input.
 * 
 * <p>Note that the use of {@link Optional} means that {@code null} results for a valid input are
 * not allowed.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the input type
 * @param <R> the result type
 */
@FunctionalInterface
public interface PartialFunction<T, R> extends Function<T, Optional<R>> {

   /**
    * Returns a composed function that first applies the {@code before}
    * function to its input, and then applies this function to the result.
    * If evaluation of either function throws an exception, it is relayed to
    * the caller of the composed function.
    *
    * @param <V> the type of input to the {@code before} function, and to the
    *           composed function
    * @param before the function to apply before this function is applied
    * @return a composed function that first applies the {@code before}
    * function and then applies this function
    * @throws NullPointerException if before is null
    *
    * @see #andThen(Function)
    */
   @Override
   default <V> PartialFunction<V, R> compose(Function<? super V, ? extends T> before) {
       Objects.requireNonNull(before);
       return (V v) -> apply(before.apply(v));
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
    * @see #compose(Function)
    */
   default <V> PartialFunction<T, V> maybeThen(Function<? super R, ? extends V> after) {
       Objects.requireNonNull(after);
       return (T t) -> {
          Optional<R> r = apply(t);
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
    * @see #compose(Function)
    */
   default <V> PartialFunction<T, V> maybeThen(PartialFunction<? super R, ? extends V> after) {
       Objects.requireNonNull(after);
       return (T t) -> {
          Optional<R> r = apply(t);
          return r.isPresent() ? Optionals.upcast(after.apply(r.get())) : Optional.empty();
       };
   }
}
