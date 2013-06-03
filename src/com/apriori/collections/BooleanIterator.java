package com.apriori.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** An {@link Iterator} thqt can provide primitive (un-boxed) booleans. */
public interface BooleanIterator extends Iterator<Boolean> {
   /**
    * Returns the next boolean in the iteration. Identical to {@link #next()} except that the value
    * returned is not boxed (and thus cannot be null).
    * 
    * @return the next boolean in the iteration
    * @throws NoSuchElementException if there are no more elements in the iteration
    */
   boolean nextBoolean();
}
