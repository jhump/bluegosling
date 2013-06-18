package com.apriori.collections;

// TODO: javadoc!
public interface BitConverter<T> extends Componentizer<T, Boolean> {
   @Override BitSequence getComponents(T t);
}