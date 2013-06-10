package com.apriori.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** An {@link Iterator} that can provide primitive (un-boxed) longs. */
public interface LongIterator extends Iterator<Long> {
   /**
    * Returns the next long in the iteration. Identical to {@link #next()} except that the value
    * returned is not boxed (and thus cannot be null).
    * 
    * @return the next long in the iteration
    * @throws NoSuchElementException if there are no more elements in the iteration
    */
   long nextLong();
}
