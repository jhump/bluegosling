package com.apriori.possible;

/**
 * A possible value that can be set exactly once. Setting a value when not already present is
 * called "fulfilling" the value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of fulfilled value
 */
public interface Fulfillable<T> extends Possible<T> {
   /**
    * Fulfills the value.
    * 
    * @param value the fulfilled value
    * @return true if the value was set to the specified value; false if the value was already
    *       fulfilled
    */
   boolean fulfill(T value);
}