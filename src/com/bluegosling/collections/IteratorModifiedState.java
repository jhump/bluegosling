package com.bluegosling.collections;

/**
 * Enumeration of possible modification states of the iterator. This is used to detect illegal
 * states for {@code remove()} and {@code set()} methods. If the state of the current element for
 * the iterator is anything other than {@code NONE}, calls to {@code remove()} and {@code set()}
 * will result in {@code IllegalStateException}s.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
enum IteratorModifiedState {
   /**
    * Indicates that no structural modifications have been made at the current location from this
    * iterator. The {@code set()} method does not count as a structural modification.
    */
   NONE,

   /**
    * Indicates that the item at the iterator's current location was removed via the iterator's
    * {@code remove()} method.
    */
   REMOVED,

   /**
    * Indicates that at least one item has has been added at the iterator's current location
    * removed via the iterator's {@code add()} method.
    */
   ADDED
}
