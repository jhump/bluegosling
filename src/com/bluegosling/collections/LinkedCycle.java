package com.bluegosling.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

// TODO: javadoc
// TODO: tests
public class LinkedCycle<E> extends AbstractCollection<E> implements Cycle<E>, BidiIterable<E> {
   
   private static class Node<E> {
      E value;
      Node<E> next;
      Node<E> previous;
   }
   
   private Node<E> first;
   private Node<E> current;
   private int size;
   
   @Override
   public int size() {
      return size;
   }

   @Override
   public boolean add(E e) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean remove(Object o) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      // TODO: implement me
      return false;
   }

   @Override
   public void clear() {
      size = 0;
      current = first = null;
   }

   @Override
   public void addFirst(E e) {
      // TODO: implement me
      
   }

   @Override
   public void addLast(E e) {
      // TODO: implement me
      
   }

   @Override
   public E set(E e) {
      // TODO: implement me
      return null;
   }

   @Override
   public E current() {
      if (current == null) {
         throw new NoSuchElementException();
      }
      return current.value;
   }

   @Override
   public int advance() {
      if (current == null) {
         throw new IllegalStateException();
      }
      Node<E> prior = current;
      current = current.next;
      return prior == current ? 0 : 1;
   }

   @Override
   public int advanceBy(int distance) {
      if (current == null) {
         throw new IllegalStateException();
      }
      int ret = distance % size;
      for (int i = 0; i < ret; i++) {
         current = current.next;
      }
      return ret;
   }

   @Override
   public int retreat() {
      if (current == null) {
         throw new IllegalStateException();
      }
      Node<E> prior = current;
      current = current.previous;
      return prior == current ? 0 : 1;
   }

   @Override
   public int retreatBy(int distance) {
      if (current == null) {
         throw new IllegalStateException();
      }
      int ret = distance % size;
      for (int i = 0; i < ret; i++) {
         current = current.previous;
      }
      return ret;
   }

   @Override
   public void reset() {
      current = first;
   }

   @Override
   public E remove() {
      // TODO: implement me
      return null;
   }

   @Override
   public BidiIterator<E> iterator() {
      // TODO: implement me
      return null;
   }

   @Override
   public BidiIterator<E> cycle() {
      // TODO: implement me
      return null;
   }

   @Override
   public Cycle<E> reverse() {
      return new ReversedCycle<E>(this) {
         @Override
         public Iterator<E> iterator() {
            // TODO: implement me
            return null;
         }
      };
   }

   @Override
   public boolean isCyclicOrder(E b, E c) {
      return isCyclicOrder(current(),  b, c);
   }

   @Override
   public boolean isCyclicOrder(E a, E b, E c) {
      // TODO: implement me
      return false;
   }
}
