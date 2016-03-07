package com.bluegosling.collections.immutable;

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
    * Adds an element to the collection. This is analogous to the {@link Collection#add} operation
    * in mutable collections.
    *
    * @param e the element to add
    * @return a new version of the collection that includes the specified item
    */
   PersistentCollection<E> with(E e);
   
   /**
    * Removes an element from the collection. This is analogous to the
    * {@link Collection#remove} operation in mutable collections.
    *
    * @param o the object to remove
    * @return a new version of the collection with the first item that is equal to the specified
    *       object, if any, removed
    */
   PersistentCollection<E> without(Object o);
   
   /**
    * Removes all occurrences of an object from the collection. Unlike
    * {#link {@link #without(Object)}, this may remove multiple elements from the collection if
    * the given object appears more than once it.
]   *
    * @param o the object to remove
    * @return a new version of the collection with all items that are equal to the specified object,
    *       if any, removed
    */
   PersistentCollection<E> withoutAny(Object o);
   
   /**
    * Removes multiple items from the collection. This is analogous to the
    * {@link Collection#removeAll} operation in mutable collections.
    *
    * @param items a sequence of items to remove
    * @return a new version of the collection that does not include any of the specified objects
    */
   PersistentCollection<E> withoutAny(Iterable<?> items);
   
   /**
    * Removes all items from the collection which match the given predicate. This is analogous to
    * the {@link Collection#removeIf} operation in mutable collections.
    *
    * @param predicate a predicate that identifies the elements to remove
    * @return a new version of the collection that does not include any elements that match the
    *       given predicate
    */
   PersistentCollection<E> withoutAny(Predicate<? super E> predicate);
   
   /**
    * Retains only the specified items in the collection. This is analogous to the
    * {@link Collection#retainAll} operation in mutable collections.
    *
    * @param items a sequence of items to keep
    * @return a new version of the collection that only includes the specified objects
    */
   PersistentCollection<E> withOnly(Iterable<?> items);
   
   /**
    * Adds multiple elements to the collection. This is analogous to the {@link Collection#addAll}
    * operation in mutable collections.
    *
    * @param items a sequence of items to add
    * @return a new version of the collection that includes all of the specified items
    */
   PersistentCollection<E> withAll(Iterable<? extends E> items);
   
   /**
    * Returns an empty collection. This is analogous to the {@link Collection#clear()} operation in
    * mutable collections. It is expected to return a new, empty collection of the same type as
    * this collection.
    *
    * @return an empty persistent collection
    */
   PersistentCollection<E> removeAll();
}
