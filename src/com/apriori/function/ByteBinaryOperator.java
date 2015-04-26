package com.apriori.function;

import java.util.function.BinaryOperator;

/**
 * Represents an operation upon two {@code byte}-valued operands and producing a
 * {@code byte}-valued result. This is the primitive type specialization of
 * {@link BinaryOperator} for {@code byte}.
 *
 * @see BinaryOperator
 * @see ByteUnaryOperator
 */
@FunctionalInterface
public interface ByteBinaryOperator {

   /**
    * Applies this operator to the given operands.
    *
    * @param left the first operand
    * @param right the second operand
    * @return the operator result
    */
   byte applyAsByte(byte left, byte right);
}
