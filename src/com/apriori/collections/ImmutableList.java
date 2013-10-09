package com.apriori.collections;

//TODO: javadoc
public interface ImmutableList<E> extends ImmutableCollection<E> {
  E get(int i);
  int indexOf(Object o);
  int lastIndexOf(Object o);
  ImmutableList<E> subList(int from, int to);
  E first();
  ImmutableList<E> rest();
  @Override boolean equals(Object o);
  @Override int hashCode();
}
