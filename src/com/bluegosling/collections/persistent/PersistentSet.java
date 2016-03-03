package com.bluegosling.collections.persistent;

import com.bluegosling.collections.immutable.ImmutableSet;

/**
 * A fully persistent set. This provides mutation operations that return new sets. Since changes
 * to a persistent data structure preserve their previous versions, persistent sets are also
 * immutable.
 *
 * @param <E> the type of element in the set
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface PersistentSet<E> extends ImmutableSet<E>, PersistentCollection<E> {

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> add(E e);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> remove(Object o);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override default PersistentSet<E> removeAll(Object o) {
      // sets don't have duplicates, so this is the same as remove(Object)
      return remove(o);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> removeAll(Iterable<?> items);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> retainAll(Iterable<?> items);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> addAll(Iterable<? extends E> items);

   /**
    * Returns an empty set.
    *
    * @return an empty persistent set
    */
   @Override PersistentSet<E> clear();
   
   /**
    * Returns a persistent set that is backed by the given persistent map. Elements present in the
    * set are the keys that are present in the map. Changes to the map are visible through the
    * returned set and vice versa. If an element is added to the set, it is visible as a new key in
    * the map and mapped to {@link Boolean#TRUE}.
    *
    * @param map a persistent map whose keys are the elements present in the resulting set
    * @return a persistent set backed by the given map
    */
   static <E> PersistentSet<E> newSetFromMap(PersistentMap<E, Boolean> map) {
      return new PersistentSetFromMap<>(map);
   }
}
