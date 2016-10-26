package com.bluegosling.util;

/**
 * An object that can clone other objects.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <T> The type of object that can be cloned
 * 
 * @see Cloners
 */
@FunctionalInterface
public interface Cloner<T> {
   /**
    * Clones an object. Cloning an object copies it, usually a "deep" copy.
    * 
    * @param o The object being cloned
    * @return The clone
    * @throws NullPointerException If the specified object to clone is null
    * @throws CloningException If any step in the cloning operation fails
    */
   T clone(T o);
}
