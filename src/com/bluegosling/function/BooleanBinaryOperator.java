package com.bluegosling.function;

import java.util.function.BinaryOperator;

/**
 * Represents an operation upon two {@code boolean}-valued operands and producing a
 * {@code boolean}-valued result. This is the primitive type specialization of
 * {@link BinaryOperator} for {@code boolean}.
 *
 * @see BinaryOperator
 * @see BooleanUnaryOperator
 */
@FunctionalInterface
public interface BooleanBinaryOperator {

   /**
    * Applies this operator to the given operands.
    *
    * @param left the first operand
    * @param right the second operand
    * @return the operator result
    */
   boolean applyAsBoolean(boolean left, boolean right);
}
