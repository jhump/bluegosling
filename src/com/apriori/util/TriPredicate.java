package com.apriori.util;

import java.util.Objects;

/**
 * Like a {@link TriFunction}, but returns a primitive boolean value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the first argument type
 * @param <U> the second argument type
 * @param <V> the third argument type
 */
@FunctionalInterface
public interface TriPredicate<T, U, V> {
   /**
    * Tests three objects.
    * 
    * @param input1 the first argument
    * @param input2 the second argument
    * @param input3 the third argument
    * @return the predicate's result
    */
   boolean test(T input1, U input2, V input3);

   /**
    * Returns a composed predicate that represents a short-circuiting logical AND of this predicate
    * and another. When evaluating the composed predicate, if this predicate is {@code false}, then
    * the {@code other} predicate is not evaluated.
    *
    * <p>
    * Any exceptions thrown during evaluation of either predicate are relayed to the caller; if
    * evaluation of this predicate throws an exception, the {@code other} predicate will not be
    * evaluated.
    *
    * @param other a predicate that will be logically-ANDed with this predicate
    * @return a composed predicate that represents the short-circuiting logical AND of this
    *         predicate and the {@code other} predicate
    * @throws NullPointerException if other is null
    */
   default TriPredicate<T, U, V> and(TriPredicate<? super T, ? super U, ? super V> other) {
      Objects.requireNonNull(other);
      return (t, u, v) -> test(t, u, v) && other.test(t, u, v);
   }

   /**
    * Returns a predicate that represents the logical negation of this predicate.
    *
    * @return a predicate that represents the logical negation of this predicate
    */
   default TriPredicate<T, U, V> negate() {
      return (t, u, v) -> !test(t, u, v);
   }

   /**
    * Returns a composed predicate that represents a short-circuiting logical OR of this predicate
    * and another. When evaluating the composed predicate, if this predicate is {@code true}, then
    * the {@code other} predicate is not evaluated.
    *
    * <p>
    * Any exceptions thrown during evaluation of either predicate are relayed to the caller; if
    * evaluation of this predicate throws an exception, the {@code other} predicate will not be
    * evaluated.
    *
    * @param other a predicate that will be logically-ORed with this predicate
    * @return a composed predicate that represents the short-circuiting logical OR of this predicate
    *         and the {@code other} predicate
    * @throws NullPointerException if other is null
    */
   default TriPredicate<T, U, V> or(TriPredicate<? super T, ? super U, ? super V> other) {
      Objects.requireNonNull(other);
      return (t, u, v) -> test(t, u, v) || other.test(t, u, v);
   }
}
