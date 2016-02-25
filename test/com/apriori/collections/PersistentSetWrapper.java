package com.apriori.collections;

import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper that adapts a {@link Set} to the {@link PersistentSet} interface.
 * 
 * @param <E> the type of element in the set
 * 
 * @see PersistentCollectionWrapper
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
class PersistentSetWrapper<E>
      extends PersistentCollectionWrapper<E, Set<E>, PersistentSetWrapper<E>>
      implements PersistentSet<E> {

   PersistentSetWrapper(Set<E> set) {
      super(set);
   }

   @Override
   protected Set<E> copy(Set<E> original) {
      return new HashSet<E>(original);
   }

   @Override
   protected PersistentSetWrapper<E> wrapPersistent(Set<E> coll) {
      return new PersistentSetWrapper<E>(coll);
   }


   @Override
   public PersistentSet<E> clear() {
      return new PersistentSetWrapper<>(new HashSet<>(0));
   }
}
