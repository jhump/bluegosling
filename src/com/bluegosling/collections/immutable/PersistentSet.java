package com.bluegosling.collections.immutable;

import java.util.Collection;
import java.util.Set;

/**
 * A fully persistent set. This provides mutation operations that return new sets. Since changes
 * to a persistent data structure preserve their previous versions, persistent sets are also
 * immutable.
 *
 * @param <E> the type of element in the set
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface PersistentSet<E> extends Set<E>, PersistentCollection<E> {

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
   default void clear() {
      throw new UnsupportedOperationException();
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> with(E e);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> without(Object o);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override default PersistentSet<E> withoutAny(Object o) {
      // sets don't have duplicates, so this is the same as without(Object)
      return without(o);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> withoutAny(Iterable<?> items);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> withOnly(Iterable<?> items);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistentSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> withAll(Iterable<? extends E> items);

   /**
    * Returns an empty set.
    *
    * @return an empty persistent set
    */
   @Override PersistentSet<E> removeAll();
   
   /**
    * Returns a persistent set that is backed by the given persistent map. Elements present in the
    * set are the keys that are present in the map. Changes to the map are visible through the
    * returned set and vice versa.
    *
    * @param map a persistent map whose keys are the elements present in the resulting set
    * @return a persistent set backed by the given map
    */
   static <E> PersistentSet<E> newSetFromMap(PersistentMap<E, Boolean> map) {
      return new PersistentSetFromMap<>(map);
   }
}
