package com.apriori.collections;

// TODO: javadoc
// TODO: tests
public abstract class AbstractImmutableSet<E> extends AbstractImmutableCollection<E>
      implements ImmutableSet<E> {
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this, o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
   
   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
}
