package com.apriori.testing;

/**
 * Verifies that an object sufficiently matches a reference value.
 * 
 * @author jhumphries
 *
 * @param <T> The type of object verified
 */
public interface ObjectVerifier<T> {
   /**
    * Verifies that an object sufficiently matches a reference value.
    * 
    * <p>The return value will generally be the test object but can be a
    * different value can be returned instead for various effects. If the
    * verification fails, this method should throw an {@code AssertionFailedError}.
    * 
    * @param test the object to test
    * @param reference the reference object
    * @return a value to return to the calling test, usually {@code test} but
    *             could be {@code reference} or even a different object of the
    *             same type
    * @throws junit.framework.AssertionFailedError if the test object does not
    *             sufficiently match the reference object
    */
   T verify(T test, T reference);
}
