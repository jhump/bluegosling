package com.apriori.collections;

//TODO: javadoc
public interface PersistentCollection<E> extends ImmutableCollection<E> {
   PersistentCollection<E> add(E e);
   PersistentCollection<E> remove(Object o);
   PersistentCollection<E> removeAll(Object o);
   PersistentCollection<E> removeAll(Iterable<?> items);
   PersistentCollection<E> retainAll(Iterable<?> items);
   PersistentCollection<E> addAll(Iterable<? extends E> items);
}
