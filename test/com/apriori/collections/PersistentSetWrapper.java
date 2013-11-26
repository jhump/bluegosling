package com.apriori.collections;

import java.util.HashSet;
import java.util.Set;

// TODO: javadoc
class PersistentSetWrapper<E>
      extends PersistentCollectionWrapper<E, Set<E>, ImmutableSetWrapper<E>, PersistentSetWrapper<E>>
      implements PersistentSet<E> {

   PersistentSetWrapper(Set<E> set) {
      super(set);
   }

   @Override
   protected Set<E> copy(Set<E> original) {
      return new HashSet<E>(original);
   }

   @Override
   protected ImmutableSetWrapper<E> wrapImmutable(Set<E> collection) {
      return new ImmutableSetWrapper<E>(collection);
   }

   @Override
   protected PersistentSetWrapper<E> wrapPersistent(Set<E> collection) {
      return new PersistentSetWrapper<E>(collection);
   }
}
