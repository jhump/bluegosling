package com.apriori.collections;

// TODO: javadoc
public interface PersistentList<E> extends ImmutableList<E>, PersistentCollection<E> {
   @Override PersistentList<E> subList(int from, int to);
   PersistentList<E> add(int i, E e);
   PersistentList<E> addAll(int i, Iterable<? extends E> items);
   PersistentList<E> addFirst(E e);
   PersistentList<E> addLast(E e);
   PersistentList<E> set(int i, E e);
   PersistentList<E> remove(int i);
   @Override PersistentList<E> rest();
   @Override PersistentList<E> add(E e);
   @Override PersistentList<E> remove(Object o);
   @Override PersistentList<E> removeAll(Object o);
   @Override PersistentList<E> removeAll(Iterable<?> items);
   @Override PersistentList<E> retainAll(Iterable<?> items);
   @Override PersistentList<E> addAll(Iterable<? extends E> items);
}
