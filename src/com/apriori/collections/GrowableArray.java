package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A container type that is like an array except that its capacity can change over time. This
 * interface is intentionally much more narrow than {@link java.util.List}, which is its closest
 * cousin from the Java Collections Framework. The abstract operations basically match that of an
 * array with two exceptions: {@link #growBy(int)} and {@link #shrinkBy(int)}. There are several
 * additional methods with default implementations but notably absent are methods that allow for
 * insertion into or removal from the beginning or middle of the array.
 * 
 * <p>Since this is a generic type, it does not have the type co-variance of actual Java arrays, and
 * so behaves more like a {@link Collection} from that standpoint.
 * 
 * @param <E> the type of element in the array
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: DoubleEndedGrowableArray?
public interface GrowableArray<E> extends SizedIterable<E> {

   /**
    * Retrieves the element at the given index.
    *
    * @param index the index
    * @return the element at the given index
    * @throws IndexOutOfBoundsException if the given index is not a valid index, meaning it is less
    *       than zero or is greater than or equal to {@link #size()}
    */
   E get(int index);
   
   /**
    * Retrieves the first element in this growable array. The first element is the one at index
    * zero.
    *
    * @return the first element in this array
    * @throws NoSuchElementException if this array is empty
    */
   default E first() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return get(0);
   }

   /**
    * Retrieves the last element in this growable array. The last element is the one at index
    * {@code size() - 1}.
    *
    * @return the last element in this array
    * @throws NoSuchElementException if this array is empty
    */
   default E last() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return get(size() - 1);
   }

   /**
    * Sets the value of the element at the given index.
    *
    * @param index the index
    * @param value new value for the given index
    * @throws IndexOutOfBoundsException if the given index is not a valid index, meaning it is less
    *       than zero or is greater than or equal to {@link #size()}
    */
   void set(int index, E value);

   /**
    * Grows the capacity of the array by the given number of elements. The extra capacity is added
    * to the end of the array. The new positions in the array will initially have a value of
    * {@code null}.
    *
    * @param numNewElements the number of new elements by which to grow this array
    * @throws IllegalArgumentException if the given number of elements is negative
    */
   void growBy(int numNewElements);
   
   /**
    * Shrinks the capacity of the array by the given number of elements. The capacity is taken
    * from the end of the array, and any values contained in those last positions are implicitly
    * removed.
    *
    * @param numRemovedElements the number of elements by which to shrink this array
    * @throws IllegalArgumentException if the given number of elements is negative or is greater
    *       than the size of this array
    */
   void shrinkBy(int numRemovedElements);
   
   // TODO: javadoc
   
   default Object[] toArray() {
      return CollectionUtils.toArray(this);
   }
   
   default <T> T[] toArray(T[] array) {
      return CollectionUtils.toArray(this, array);
   }
   
   @Override
   boolean equals(Object o);
   
   @Override
   int hashCode();
   
   /**
    * Removes all elements from this growable array and shrinks its capacity to zero.
    */
   default void clear() {
      shrinkBy(size());
   }
   
   /**
    * Pushes a new value to the end of this growable array. This is effectively the same as growing
    * this array by one element, and then setting that last element to the given value.
    *
    * @param value the value to push to the end of this array
    */
   default void push(E value) {
      growBy(1);
      set(size() - 1, value);
   }
   
   /**
    * Pops the last value from the end of this growable array. This is effectively the same as
    * shrinking the array by one element, but returns the item that was in the last, newly-removed
    * position.
    *
    * @return the value that was removed from the end of this array
    * @throws NoSuchElementException if this array is empty
    */
   default E pop() {
      E ret = last();
      shrinkBy(1);
      return ret;
   }
   
   /**
    * Pushes multiple values to the end of this growable array, in bulk. The order of items pushed
    * is based on the iteration order of the given collection or sequence.
    *
    * @param values the new values to push to the end of this array
    */
   default void pushAll(Iterable<? extends E> values) {
      // mark the initial index, for the first item we'll append
      int i = size();
      // try to pre-allocate the entire amount needed
      OptionalInt otherSize = Iterables.trySize(values);
      if (otherSize.isPresent()) {
         growBy(otherSize.getAsInt());
      }
      for (E t : values) {
         if (i < size()) {
            set(i, t);
         } else {
            push(t);
         }
         i++;
      }
      // in case values collection was concurrently modified to have fewer
      // elements than we initially made room for
      if (i < size()) {
         shrinkBy(size() - i);
      }
   }
   
   /**
    * Returns an iterator over the contents of this growable array. The returned iterator cannot be
    * used to remove elements, and thus {@link Iterator#remove()} will throw
    * {@link UnsupportedOperationException}.
    * 
    * @return an iterator over the contents of this array
    */
   @Override
   default Iterator<E> iterator() {
      return new Iterator<E>() {
         private int cursor = 0;
         
         @Override
         public boolean hasNext() {
            return cursor < size();
         }

         @Override
         public E next() {
            if (cursor < size()) {
               return get(cursor++);
            } else {
               throw new NoSuchElementException();
            }
         }
      };
   }
   
   /**
    * Adjusts the size of this growable array. If the given new capacity is larger than the array's
    * current capacity, the current array grows. Otherwise, it may shrink.
    *
    * @param newCapacity the new capacity for the array
    */
   default void adjustSizeTo(int newCapacity) {
      int sz = size();
      if (newCapacity == sz) {
         return;
      } else if (newCapacity > sz) {
         growBy(newCapacity - sz);
      } else {
         shrinkBy(sz - newCapacity);
      }
   }
   
   @Override
   default Spliterator<E> spliterator() {
      return new GrowableArraySpliterator<E>(this, 0);
   }
   
   /**
    * Returns a sequential stream of the elements in this growable array.
    *
    * @return a new sequential stream
    */
   default Stream<E> stream() {
      return StreamSupport.stream(spliterator(), false);
   }

   /**
    * Returns a parallel stream of the elements in this growable array.
    *
    * @return a new parallel stream
    */
   default Stream<E> parallelStream() {
      return StreamSupport.stream(spliterator(), true);
   }
   
   /**
    * Returns a {@link Collector} that will accumulate elements from a stream into a growable array.
    *
    * @param supplier supplier of a growable array, into which elements are collected
    * @return a new collector that accumulates elements into a growable array
    */
   static <T, A extends GrowableArray<T>> Collector<T, ?, A> collector(Supplier<A> supplier) {
      return Collector.of(supplier, (array, t) -> array.push(t),
            (array1, array2) -> { array1.pushAll(array2); return array1; });
   }
}
