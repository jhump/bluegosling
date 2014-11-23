package com.apriori.util;

import java.util.function.BinaryOperator;

/**
 * Represents an operation upon two {@code float}-valued operands and producing a
 * {@code float}-valued result. This is the primitive type specialization of
 * {@link BinaryOperator} for {@code float}.
 *
 * @see BinaryOperator
 * @see FloatUnaryOperator
 */
@FunctionalInterface
public interface FloatBinaryOperator {

   /**
    * Applies this operator to the given operands.
    *
    * @param left the first operand
    * @param right the second operand
    * @return the operator result
    */
   float applyAsFloat(float left, float right);
}
