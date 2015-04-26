package com.apriori.function;

import java.util.function.BinaryOperator;

/**
 * Represents an operation upon two {@code short}-valued operands and producing a
 * {@code short}-valued result. This is the primitive type specialization of
 * {@link BinaryOperator} for {@code short}.
 *
 * @see BinaryOperator
 * @see FloatUnaryOperator
 */
@FunctionalInterface
public interface ShortBinaryOperator {

   /**
    * Applies this operator to the given operands.
    *
    * @param left the first operand
    * @param right the second operand
    * @return the operator result
    */
   short applyAsShort(short left, short right);
}
