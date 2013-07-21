package com.apriori.util;

/**
 * An interface that represents a source of data. No contract is made regarding the items provided
 * by this source. It could always provide the same object or it could supply a different object for
 * each invocation, maybe ordered, maybe not. It's just a generic interface from which an object
 * can be retrieved.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of element provided
 */
public interface Source<T> {
   /**
    * Gets an object from this source.
    * 
    * @return an object
    */
   T get();
}
