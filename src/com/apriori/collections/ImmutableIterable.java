package com.apriori.collections;

//TODO: javadoc
public interface ImmutableIterable<E> extends Iterable<E> {
   ImmutableIterator<E> immutableIterator();
   
   interface Bidi<E> extends ImmutableIterable<E> {
      @Override ImmutableIterator.Bidi<E> immutableIterator();
   }
}
