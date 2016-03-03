package com.bluegosling.collections.immutable;

import java.util.Set;

// TODO: javadoc
public class ImmutableSetWrapper<E> extends ImmutableCollectionWrapper<E, Set<E>>
      implements ImmutableSet<E> {

   public ImmutableSetWrapper(Set<E> set) {
      super(set);
   }
}
