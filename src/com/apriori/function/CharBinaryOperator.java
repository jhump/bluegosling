package com.apriori.function;

import java.util.function.BinaryOperator;

/**
 * Represents an operation upon two {@code char}-valued operands and producing a
 * {@code char}-valued result. This is the primitive type specialization of
 * {@link BinaryOperator} for {@code char}.
 *
 * @see BinaryOperator
 * @see CharUnaryOperator
 */
@FunctionalInterface
public interface CharBinaryOperator {

   /**
    * Applies this operator to the given operands.
    *
    * @param left the first operand
    * @param right the second operand
    * @return the operator result
    */
   char applyAsChar(char left, char right);
}
