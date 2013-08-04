package com.apriori.possible;


//TODO: javadoc
public interface Fulfillable<T> extends Possible<T> {
   boolean fulfill(T value);
}