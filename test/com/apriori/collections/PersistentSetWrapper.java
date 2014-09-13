package com.apriori.collections;

import java.util.HashSet;
import java.util.Set;

// TODO: javadoc
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
}
