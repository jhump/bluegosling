package com.apriori.collections;

//TODO: implement me
//TODO: javadoc
//TODO: tests
public abstract class BinomialHeapQueue<E> implements MeldableOrderedQueue<E, BinomialHeapQueue<? extends E>> {

   private static class Node<E> {
      Node<E> next;
      Node<E> previous;
      Node<E> children;
      int order;
      E value;
   }
}
