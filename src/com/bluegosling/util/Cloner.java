package com.bluegosling.util;

import com.bluegosling.testing.InterfaceVerifier;

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
    * Clones an object. Cloning an object copies it, usually a "deep" copy. Cloning can be performed
    * on arguments passed to an interface during verification by an {@link InterfaceVerifier}. This
    * is necessary in the event that the implementation under test mutates the incoming argument.
    * That way both the test implementation and reference implementation receive proper incoming
    * values (otherwise, one implementation may receive "different" arguments due to the values
    * having been mutated by the other implementation).
    * 
    * <p>
    * It should not be necessary for implementors of this interface to check whether the specified
    * object is null. It should never be null. It is expected that, if invoked to clone a null
    * reference, a {@code NullPointerException} will be thrown.
    * 
    * @param o The object being cloned
    * @return The clone
    * @throws NullPointerException If the specified object is null
    * @throws CloningException If any step in the cloning operation fails
    */
   T clone(T o);
}
