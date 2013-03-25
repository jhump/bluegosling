package com.apriori.util;

//TODO: javadoc
public interface BitSequence {
   int length();
   boolean isBitSet(int index);
   int firstSetBit();
   int nextSetBit(int index);
}