package com.bluegosling.testing;

/**
 * Verifies that an object sufficiently matches a reference value.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <T> The type of object verified
 */
public interface ObjectVerifier<T> {
   /**
    * Verifies that an object sufficiently matches a reference value.
    * 
    * <p>
    * The return value will generally be the test object but a different value can be returned
    * instead for various effects. If the verification fails, this method should throw an
    * {@link AssertionError}.
    * 
    * @param test the object to test
    * @param reference the reference object
    * @return a value to return to the calling test, usually {@code test} but could be
    *         {@code reference} or even a different object of the same type
    * @throws AssertionError if the test object does not sufficiently match the reference object
    */
   T verify(T test, T reference);
}
