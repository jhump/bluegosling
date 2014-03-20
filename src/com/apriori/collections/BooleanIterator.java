package com.apriori.collections;

import com.apriori.util.BooleanConsumer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/** An {@link Iterator} that can provide primitive (un-boxed) booleans. */
// TODO: more javadoc
public interface BooleanIterator extends PrimitiveIterator<Boolean, BooleanConsumer> {
   /**
    * Returns the next boolean in the iteration. Identical to {@link #next()} except that the value
    * returned is not boxed (and thus cannot be null).
    * 
    * @return the next boolean in the iteration
    * @throws NoSuchElementException if there are no more elements in the iteration
    */
   boolean nextBoolean();
   
   @Override
   default Boolean next() {
      return nextBoolean();
   }
   
   @Override
   default void forEachRemaining(BooleanConsumer action) {
      while (hasNext()) {
         action.accept(nextBoolean());
      }
   }
}
