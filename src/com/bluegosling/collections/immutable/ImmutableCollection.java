package com.bluegosling.collections.immutable;

import com.bluegosling.collections.SizedIterable;

import java.util.Collection;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An immutable, read-only collection. This interface is similar to the standard {@link Collection}
 * interface except that it defines no mutation operations.
 * 
 * <p>The {@link java.util.Iterator#remove() remove()} operation of an immutable collection's
 * {@link #iterator()} always throws {@link UnsupportedOperationException}.
 *
 * @param <E> the type of element in the collection
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface ImmutableCollection<E> extends SizedIterable<E> {
   /**
    * Returns an array with the same elements as this collection. The order of elements in the
    * returned array is the same as the order that elements are returned by the collection's
    * {@link #iterator()}.
    *
    * @return an array with the same elements as this collection
    * 
    * @see Collection#toArray()
    */
   Object[] toArray();
   
   /**
    * Returns an array with the same elements as this collection and the same component type as the
    * specified array. If the elements will fit in the specified array, they are copied there and
    * a {@code null} terminator is stored after the elements if the array can hold more. Otherwise,
    * a new array with the same element type is allocated, and the elements are copied into it.
    *
    * @param array an array 
    * @return an array with the same elements as this collection and the same component type as
    *       the specified array (possibly the same instance as the one specified)
    * @throws ArrayStoreException if any of the elements are not assignable to the component type
    *       of the specified array
    *       
    * @see Collection#toArray(Object[])
    */
   <T> T[] toArray(T[] array);
   
   /**
    * Determines if the specified object is a member of this collection.
    *
    * @param o an object
    * @return true if the specified object is a member of this collection, false otherwise
    */
   boolean contains(Object o);
   
   /**
    * Determines if this collection contains all of the specified items.
    *
    * @param items a sequence of items
    * @return true if this collection contains all of the specified items
    */
   boolean containsAll(Iterable<?> items);
   
   /**
    * Determines if this collection contains any one of the specified items.
    *
    * @param items a sequence of items
    * @return true if this collection contains at least one of the specified items
    */
   boolean containsAny(Iterable<?> items);
   
   // TODO: javadoc
   
   default Stream<E> stream() {
      return StreamSupport.stream(() -> spliterator(), Spliterator.SIZED | Spliterator.SUBSIZED,
            false);
   }

   default Stream<E> parallelStream() {
      return StreamSupport.stream(spliterator(), true);
   }
}
