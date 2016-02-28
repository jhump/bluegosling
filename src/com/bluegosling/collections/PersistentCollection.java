package com.bluegosling.collections;

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
public interface PersistentCollection<E> extends ImmutableCollection<E> {
   /**
    * Adds an element to the collection.
    *
    * @param e the element to add
    * @return a new version of the collection that includes the specified item
    */
   PersistentCollection<E> add(E e);
   
   /**
    * Removes an element from the collection.
    *
    * @param o the object to remove
    * @return a new version of the collection with the first item that is equal to the specified
    *       object, if any, removed
    */
   PersistentCollection<E> remove(Object o);
   
   /**
    * Removes all occurrences of an object from the collection.
]   *
    * @param o the object to remove
    * @return a new version of the collection with all items that are equal to the specified object,
    *       if any, removed
    */
   PersistentCollection<E> removeAll(Object o);
   
   /**
    * Removes multiple items from the collection.
    *
    * @param items a sequence of items to remove
    * @return a new version of the collection that does not include any of the specified objects
    */
   PersistentCollection<E> removeAll(Iterable<?> items);
   
   /**
    * Retains only the specified items in the collection.
    *
    * @param items a sequence of items to keep
    * @return a new version of the collection that only includes the specified objects
    */
   PersistentCollection<E> retainAll(Iterable<?> items);
   
   /**
    * Adds multiple elements to the collection.
    *
    * @param items a sequence of items to add
    * @return a new version of the collection that includes all of the specified items
    */
   PersistentCollection<E> addAll(Iterable<? extends E> items);
   
   /**
    * Returns an empty collection.
    *
    * @return an empty persistent collection
    */
   PersistentCollection<E> clear();
}
