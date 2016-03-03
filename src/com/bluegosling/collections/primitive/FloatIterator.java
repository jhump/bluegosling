package com.bluegosling.collections.primitive;

import com.bluegosling.function.FloatConsumer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/** An {@link Iterator} that can provide primitive (un-boxed) floats. */
// TODO: more javadoc
public interface FloatIterator extends PrimitiveIterator<Float, FloatConsumer> {
   /**
    * Returns the next float in the iteration. Identical to {@link #next()} except that the value
    * returned is not boxed (and thus cannot be null).
    * 
    * @return the next short in the iteration
    * @throws NoSuchElementException if there are no more elements in the iteration
    */
   float nextFloat();
   
   @Override
   default Float next() {
      return nextFloat();
   }
   
   @Override
   default void forEachRemaining(FloatConsumer action) {
      Objects.requireNonNull(action);
      while (hasNext()) {
         action.accept(nextFloat());
      }
   }
   
   // TODO: javadoc
   default PrimitiveIterator.OfDouble asIteratorOfDouble() {
      return new PrimitiveIterator.OfDouble() {
         @Override
         public boolean hasNext() {
            return FloatIterator.this.hasNext();
         }

         @Override
         public double nextDouble() {
            return nextFloat();
         }
      };
   }
}
