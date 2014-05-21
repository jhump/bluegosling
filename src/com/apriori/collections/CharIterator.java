package com.apriori.collections;

import com.apriori.util.CharConsumer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;

/** An {@link Iterator} that can provide primitive (un-boxed) characters. */
// TODO: more javadoc
public interface CharIterator extends PrimitiveIterator<Character, CharConsumer> {
   /**
    * Returns the next character in the iteration. Identical to {@link #next()} except that the
    * value returned is not boxed (and thus cannot be null).
    * 
    * @return the next character in the iteration
    * @throws NoSuchElementException if there are no more elements in the iteration
    */
   char nextChar();
   
   @Override
   default Character next() {
      return nextChar();
   }
   
   @Override
   default void forEachRemaining(CharConsumer action) {
      Objects.requireNonNull(action);
      while (hasNext()) {
         action.accept(nextChar());
      }
   }
}
