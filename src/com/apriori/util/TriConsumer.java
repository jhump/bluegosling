package com.apriori.util;

// TODO: javadoc
@FunctionalInterface
public interface TriConsumer<I1, I2, I3> {
   void accept(I1 input1, I2 input2, I3 input3);
}
