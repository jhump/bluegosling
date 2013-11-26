package com.apriori.collections;

import java.util.Set;

// TODO: javadoc
public class ImmutableSetWrapper<E> extends ImmutableCollectionWrapper<E, Set<E>>
      implements ImmutableSet<E> {

   ImmutableSetWrapper(Set<E> set) {
      super(set);
   }
}
