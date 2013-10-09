package com.apriori.collections;

// TODO: javadoc
public interface ImmutableCollection<E> extends ImmutableIterable<E> {
   int size();
   boolean isEmpty();
   Object[] toArray();
   <T> T[] toArray(T[] array);
   boolean contains(Object o);
   boolean containsAll(Iterable<?> items);
   boolean containsAny(Iterable<?> items);
}
