package com.apriori.collections;

//TODO: javadoc
public interface PersistentSet<E> extends ImmutableSet<E>, PersistentCollection<E> {
   @Override PersistentSet<E> add(E e);
   @Override PersistentSet<E> remove(Object o);
   @Override PersistentSet<E> removeAll(Object o);
   @Override PersistentSet<E> removeAll(Iterable<?> items);
   @Override PersistentSet<E> retainAll(Iterable<?> items);
   @Override PersistentSet<E> addAll(Iterable<? extends E> items);
}
