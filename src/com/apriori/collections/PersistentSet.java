package com.apriori.collections;

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
    * <p>This method is overridden to covariantly return a {@link PersistenSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> add(E e);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistenSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> remove(Object o);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistenSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> removeAll(Object o);

   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistenSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> removeAll(Iterable<?> items);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistenSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> retainAll(Iterable<?> items);
   
   /**
    * {@inheritDoc}
    * 
    * <p>This method is overridden to covariantly return a {@link PersistenSet} instead of a
    * {@link PersistentCollection}.
    */
   @Override PersistentSet<E> addAll(Iterable<? extends E> items);
}
