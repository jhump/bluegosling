package com.bluegosling.collections.tries;

import com.bluegosling.collections.bits.BitSequence;
import com.bluegosling.collections.bits.BitSequences;

/**
 * Decomposes objects into a sequence of bits.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the composite object
 */
@FunctionalInterface
public interface BitConverter<T> extends Componentizer<T, Boolean> {
   /**
    * {@inheritDoc}
    * 
    * <p>This returns a {@link BitSequence}, which is an {@link Iterable} but also provides methods
    * for navigating the bits as a stream.
    * 
    * @see BitSequences
    */
   @Override BitSequence getComponents(T t);
}
