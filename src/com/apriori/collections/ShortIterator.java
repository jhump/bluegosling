package com.apriori.collections;

import com.apriori.function.ShortConsumer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/** An {@link Iterator} that can provide primitive (un-boxed) shorts. */
// TODO: more javadoc
public interface ShortIterator extends PrimitiveIterator<Short, ShortConsumer> {
   /**
    * Returns the next short in the iteration. Identical to {@link #next()} except that the value
    * returned is not boxed (and thus cannot be null).
    * 
    * @return the next short in the iteration
    * @throws NoSuchElementException if there are no more elements in the iteration
    */
   short nextShort();
   
   @Override
   default Short next() {
      return nextShort();
   }
   
   @Override
   default void forEachRemaining(ShortConsumer action) {
      Objects.requireNonNull(action);
      while (hasNext()) {
         action.accept(nextShort());
      }
   }
   
   // TODO: javadoc
   default PrimitiveIterator.OfInt asIteratorOfInt() {
      return new PrimitiveIterator.OfInt() {
         @Override
         public boolean hasNext() {
            return ShortIterator.this.hasNext();
         }

         @Override
         public int nextInt() {
            return nextShort();
         }
      };
   }
}
