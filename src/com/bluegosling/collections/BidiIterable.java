package com.bluegosling.collections;

/**
 * An iterable that supports iteration over its elements in both directions
 *
 * @param <T> the type of element in the sequence
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface BidiIterable<T> extends Iterable<T> {
   /**
    * {@inheritDoc}
    * 
    * <p>The returned iterator supports iteration both forwards and backwards through the sequence.
    */
   @Override BidiIterator<T> iterator();
}
