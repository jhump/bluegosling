package com.bluegosling.collections.persistent;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * A fully persistent collection. This provides mutation operations that return new collections.
 * Since changes to a persistent data structure preserve their previous versions, persistent
 * collections are also immutable.
 *
 * <p>Since a persistent collection is immutable, the {@link java.util.Iterator#remove() remove()}
 * operation of its {@link #iterator()} always throws {@link UnsupportedOperationException}.
 * 
 * @param <E> the type of element in the collection
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface PersistentCollection<E> extends Collection<E> {
   
   @Deprecated
   @Override
   default boolean add(E e) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean addAll(Collection<? extends E> coll) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean remove(Object o) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean removeAll(Collection<?> coll) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default boolean retainAll(Collection<?> coll) {
      throw new UnsupportedOperationException();
   }
   
   @Deprecated
   @Override
   default boolean removeIf(Predicate<? super E> predicate) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   default void clear() {
      throw new UnsupportedOperationException();
   }

   /**
    * Adds an element to the collection.
    *
    * @param e the element to add
    * @return a new version of the collection that includes the specified item
    */
   PersistentCollection<E> with(E e);
   
   /**
    * Removes an element from the collection.
    *
    * @param o the object to remove
    * @return a new version of the collection with the first item that is equal to the specified
    *       object, if any, removed
    */
   PersistentCollection<E> without(Object o);
   
   /**
    * Removes all occurrences of an object from the collection.
]   *
    * @param o the object to remove
    * @return a new version of the collection with all items that are equal to the specified object,
    *       if any, removed
    */
   PersistentCollection<E> withoutAny(Object o);
   
   /**
    * Removes multiple items from the collection.
    *
    * @param items a sequence of items to remove
    * @return a new version of the collection that does not include any of the specified objects
    */
   PersistentCollection<E> withoutAny(Iterable<?> items);
   
   /**
    * Retains only the specified items in the collection.
    *
    * @param items a sequence of items to keep
    * @return a new version of the collection that only includes the specified objects
    */
   PersistentCollection<E> withOnly(Iterable<?> items);
   
   /**
    * Adds multiple elements to the collection.
    *
    * @param items a sequence of items to add
    * @return a new version of the collection that includes all of the specified items
    */
   PersistentCollection<E> withAll(Iterable<? extends E> items);
   
   /**
    * Returns an empty collection.
    *
    * @return an empty persistent collection
    */
   PersistentCollection<E> removeAll();
}
