package com.bluegosling.function;

import static java.util.Objects.requireNonNull;

import java.util.function.UnaryOperator;

/**
 * Represents an operation on a single {@code byte}-valued operand that produces a
 * {@code byte}-valued result. This is the primitive type specialization of {@link UnaryOperator}
 * for {@code byte}.
 *
 * @see UnaryOperator
 */
@FunctionalInterface
public interface ByteUnaryOperator {

   /**
    * Applies this operator to the given operand.
    *
    * @param operand the operand
    * @return the operator result
    */
   byte applyAsByte(byte operand);

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
    * @see #andThen(ByteUnaryOperator)
    */
   default ByteUnaryOperator compose(ByteUnaryOperator before) {
      requireNonNull(before);
      return v -> applyAsByte(before.applyAsByte(v));
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
    * @see #compose(ByteUnaryOperator)
    */
   default ByteUnaryOperator andThen(ByteUnaryOperator after) {
      requireNonNull(after);
      return v -> after.applyAsByte(applyAsByte(v));
   }

   /**
    * Returns a unary operator that always returns its input argument.
    *
    * @return a unary operator that always returns its input argument
    */
   static ByteUnaryOperator identity() {
      return v -> v;
   }
}
