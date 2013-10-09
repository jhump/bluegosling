package com.apriori.collections;

import java.util.Iterator;

/**
 * A read-only iterator. It is named {@code Immutable} for consistency with other immutable
 * collection interfaces. However, an iterator cannot be truly immutable since its "current
 * location" is state that must be mutated as it moves through the sequence. "Immutable" in this
 * case instead refers to the underlying sequence of values. The main difference between this class
 * and its normal {@link java.util.Iterator} kin is that this interface does not support any
 * mutations to the stream (e.g. no {@link Iterator#remove()} method).
 *
 * @param <E> the type of elements returned from this iterator
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
public interface ImmutableIterator<E> {
   boolean hasNext();
   E next();
   
   interface Bidi<E> extends ImmutableIterator<E> {
      ImmutableIterator.Bidi<E> reverse();
      boolean hasPrevious();
      E previous();
   }
}
