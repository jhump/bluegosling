package com.apriori.collections;

import java.util.Iterator;

/**
 * A simple base class for iterators that are read-only. Sub-classes must still implement the
 * {@link #hasNext()} and {@link #next()} methods. But {@link #remove()} need not be implemented,
 * and in fact cannot be overridden. It always throws {@link UnsupportedOperationException}.
 *
 * @param <E> the type of object fetched from the iterator
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class ReadOnlyIterator<E> implements Iterator<E> {
   /**
    * Throws an exception since removals are not allowed.
    * 
    * @throws UnsupportedOperationException always
    */
   @Override public final void remove() {
      throw new UnsupportedOperationException();
   }
}
