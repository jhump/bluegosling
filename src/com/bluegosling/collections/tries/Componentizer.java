package com.bluegosling.collections.tries;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An interface used to break a composite object up into its constituent components.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the composite object
 * @param <U> the type of the component (sub-object)
 */
@FunctionalInterface
public interface Componentizer<T, U> {
   /**
    * Breaks the specified object into a sequence of components.
    * 
    * @param t the composite object
    * @return the object's sequence of components
    */
   Iterable<U> getComponents(T t);
   
   /**
    * Returns a componentizer that breaks any {@link CharSequence} (for example, a {@link String})
    * into its component characters. Any surrogate pairs remain uninterpreted, resulting in two
    * adjacent values in the components.
    * 
    * @return a componentizer that breaks up {@link CharSequence}s into characters.
    */
   static Componentizer<CharSequence, Character> forCharSequence() {
      return s -> () -> new Iterator<Character>() {
         private int i = 0;
         
         @Override public boolean hasNext() {
            return i < s.length();
         }
         
         @Override public Character next() {
            if (!hasNext()) {
               throw new NoSuchElementException();
            }
            return s.charAt(i);
         }
      };
   }
   
   /**
    * Returns a componentizer that breaks any {@link CharSequence} (for example, a {@link String})
    * into its component unicode code points. Unlike {@link #forCharSequence()}, surrogate pairs are
    * interpreted and combined, as if by {@link Character#toCodePoint(char, char)}. Non-surrogate
    * pairs and other characters are zero-extended to the code point width.
    * 
    * @return a componentizer that breaks up {@link CharSequence}s into characters.
    */
   static Componentizer<CharSequence, Integer> forCodePointSequence() {
      return s -> () -> s.codePoints().iterator();
   }
}
