package com.apriori.util;

import java.util.Set;

//TODO: javadoc
public interface Fulfillable<T> {
   
   boolean isFulfilled();
   
   boolean fulfill(T value);
   
   T get();
   
   T getOr(T other);
   
   Set<T> asSet();
   
}