package com.bluegosling.collections.concurrent;

import java.util.Collection;

import com.bluegosling.collections.Stack;

public interface ConcurrentStack<E> extends Stack<E> {
   /**
    * Atomically drains the stack and returns its contents in a new instance. This can be
    * interpreted as a "clone" then "clear" operation, in one.
    *
    * @return a new stack that has all of the contents that were held by this stack, in the same
    *       order
    */
   ConcurrentStack<E> removeAll();

   /**
    * Atomically drains the stack by popping all elements from the stack and adding them to the
    * given collection. The top of the stack will be the first item added to the collection.
    * 
    * <p>If the given collection is thread-safe and published to multiple threads, the additions of
    * elements may not appear atomic.
    *
    * @param coll the collection into which items are drained
    */
   void drainTo(Collection<? super E> coll);
}
