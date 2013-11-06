package com.apriori.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that can navigate a sequence in both directions.
 *
 * @param <T> the type of element in the sequence
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface BidiIterator<T> extends Iterator<T> {
   /**
    * Returns a view of this iterator in reversed order. Navigating forwards in the returned
    * iterator is the same as navigating backwards in the original and vice versa.
    *
    * @return a reversed view of this iterator
    */
   BidiIterator<T> reverse();
   
   /**
    * Determines if there is a previous element.
    *
    * @return true if there is a previous element, false otherwise
    */
   boolean hasPrevious();
   
   /**
    * Returns the previous element in the sequence.
    *
    * @return the previous element in the sequence
    * @throws NoSuchElementException if there is no previous element
    */
   T previous();
}