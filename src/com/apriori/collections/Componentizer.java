package com.apriori.collections;

/**
 * An interface used to break a composite object up into its constituent components.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the composite object
 * @param <U> the type of the component (sub-object)
 */
public interface Componentizer<T, U> {
   /**
    * Breaks the specified object into a sequence of components.
    * 
    * @param t the composite object
    * @return the object's sequence of components
    */
   Iterable<U> getComponents(T t);
}