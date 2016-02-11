package com.apriori.function;

import static java.util.Objects.requireNonNull;

import java.util.function.UnaryOperator;

/**
 * Represents an operation on a single {@code char}-valued operand that produces a
 * {@code char}-valued result. This is the primitive type specialization of {@link UnaryOperator}
 * for {@code char}.
 *
 * @see UnaryOperator
 */
@FunctionalInterface
public interface CharUnaryOperator {

   /**
    * Applies this operator to the given operand.
    *
    * @param operand the operand
    * @return the operator result
    */
   char applyAsChar(char operand);

   /**
    * Returns a composed operator that first applies the {@code before} operator to its input, and
    * then applies this operator to the result. If evaluation of either operator throws an
    * exception, it is relayed to the caller of the composed operator.
    *
    * @param before the operator to apply before this operator is applied
    * @return a composed operator that first applies the {@code before} operator and then applies
    *         this operator
    * @throws NullPointerException if before is null
    *
    * @see #andThen(CharUnaryOperator)
    */
   default CharUnaryOperator compose(CharUnaryOperator before) {
      requireNonNull(before);
      return v -> applyAsChar(before.applyAsChar(v));
   }

   /**
    * Returns a composed operator that first applies this operator to its input, and then applies
    * the {@code after} operator to the result. If evaluation of either operator throws an
    * exception, it is relayed to the caller of the composed operator.
    *
    * @param after the operator to apply after this operator is applied
    * @return a composed operator that first applies this operator and then applies the
    *         {@code after} operator
    * @throws NullPointerException if after is null
    *
    * @see #compose(CharUnaryOperator)
    */
   default CharUnaryOperator andThen(CharUnaryOperator after) {
      requireNonNull(after);
      return v -> after.applyAsChar(applyAsChar(v));
   }

   /**
    * Returns a unary operator that always returns its input argument.
    *
    * @return a unary operator that always returns its input argument
    */
   static CharUnaryOperator identity() {
      return v -> v;
   }
}