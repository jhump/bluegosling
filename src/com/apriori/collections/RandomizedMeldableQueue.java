package com.apriori.collections;

//TODO: implement me
//TODO: javadoc
//TODO: tests
public abstract class RandomizedMeldableQueue<E> implements MeldableOrderedQueue<E, RandomizedMeldableQueue<? extends E>> {

   private static class Node<E> {
      Node<E> left;
      Node<E> right;
      E value;
   }
}
