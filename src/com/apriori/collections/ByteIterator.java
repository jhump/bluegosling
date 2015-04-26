package com.apriori.collections;

import com.apriori.function.ByteConsumer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/** An {@link Iterator} that can provide primitive (un-boxed) bytes. */
// TODO: more javadoc
public interface ByteIterator extends PrimitiveIterator<Byte, ByteConsumer> {
   /**
    * Returns the next byte in the iteration. Identical to {@link #next()} except that the value
    * returned is not boxed (and thus cannot be null).
    * 
    * @return the next byte in the iteration
    * @throws NoSuchElementException if there are no more elements in the iteration
    */
   byte nextByte();
   
   @Override
   default Byte next() {
      return nextByte();
   }
   
   @Override
   default void forEachRemaining(ByteConsumer action) {
      Objects.requireNonNull(action);
      while (hasNext()) {
         action.accept(nextByte());
      }
   }
   
   // TODO: javadoc
   default PrimitiveIterator.OfInt asIteratorOfInt() {
      return new PrimitiveIterator.OfInt() {
         @Override
         public boolean hasNext() {
            return ByteIterator.this.hasNext();
         }

         @Override
         public int nextInt() {
            return nextByte();
         }
      };
   }
}
