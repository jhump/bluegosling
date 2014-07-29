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
   
   static CharIterator from(CharSequence str) {
      return new CharIterator() {
         char next;
         int hasNext = -1;
         int i = 0;
         
         private void findNext() {
            // For CharSequences that are thread-safe (e.g. StringBuffer), we cache next value so we
            // can return consistently from hasNext() and next(), even if sequence is concurrently
            // modified.
            if (hasNext == -1) {
               try {
                  if (i < str.length()) {
                     hasNext = 1;
                     next = str.charAt(i);
                  } else {
                     hasNext = 0;
                  }
               } catch (IndexOutOfBoundsException e) {
                  // could happen if sequence is concurrently modified between the length check
                  // and actually accessing element above
                  hasNext = 0;
               }
            }
         }
         
         @Override
         public boolean hasNext() {
            findNext();
            return hasNext == 1;
         }

         @Override
         public char nextChar() {
            findNext();
            if (hasNext == 0) {
               throw new NoSuchElementException();
            }
            hasNext = -1;
            i++;
            return next;
         }
      };
   }
}
