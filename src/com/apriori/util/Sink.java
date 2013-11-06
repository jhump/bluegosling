package com.apriori.util;

/**
 * An interface that represents a sink for objects, aka "target" or "consumer".
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of element consumed
 */
public interface Sink<T> {
   /**
    * Consumes the specified object.
    * 
    * @param t an object
    */
   void accept(T t);
}
