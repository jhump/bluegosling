package com.apriori.util;

import java.util.Iterator;

/**
 * Represents a sequence of items produced by a {@link Generator}. This is very similar to the
 * {@link Iterator} interface, but it allows for propagation of exceptions that the generator may
 * throw.
 *
 * @param <T> the type of elements in the sequence
 * @param <X> the type of exception that may be thrown during sequence generation
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Generator
 */
public interface Sequence<T, X extends Throwable> {
   /**
    * Returns the next element in the sequence. This transfers control to the generator and returns
    * when the generator provides the next value. If the generator terminates, either in failure or
    * successfully, then an exception is thrown.
    *
    * @return the next element in the sequence
    * @throws X if the generator fails and throws an exception
    * @throws SequenceFinishedException if the generator completes successfully, indicating that
    *       there are no more elements in the sequence
    */
   T next() throws X;
   
   /**
    * Returns a view of this sequence as an {@link Iterator}. This may transfer control to the
    * generator thread during calls to {@code hasNext()} to determine if there is another item by
    * letting the generator actually produce it. As such, both {@code next()} and {@code hasNext()}
    * methods may throw an (unchecked) exception if the generator fails. 
    *
    * @return a view of this sequence as an {@link Iterator}
    */
   Iterator<T> asIterator();
}
