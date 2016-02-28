package com.bluegosling.collections;


// TODO: javadoc
public abstract class AbstractGrowableArray<E> implements GrowableArray<E> {
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
