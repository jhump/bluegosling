package com.apriori.collections;

// TODO: javadoc
public interface ImmutableSet<E> extends ImmutableCollection<E> {
  @Override boolean equals(Object o);
  @Override int hashCode();
}
