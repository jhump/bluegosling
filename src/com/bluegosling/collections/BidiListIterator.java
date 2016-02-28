package com.bluegosling.collections;

import java.util.ListIterator;

/**
 * A list iterator that also implements {@link BidiIterator}. List iterators already support
 * iteration in both directions. Implementing this interface only adds one new operation:
 * {@link #reverse()}.
 *
 * @param <T> the type of element in the list
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface BidiListIterator<T> extends BidiIterator<T>, ListIterator<T> {
   @Override BidiListIterator<T> reverse();
}
