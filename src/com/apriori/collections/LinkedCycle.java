package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;

// TODO: javadoc
// TODO: tests
public class LinkedCycle<E> implements Cycle<E> {
   @Override
   public int size() {
      // TODO: implement me
      return 0;
   }

   @Override
   public boolean isEmpty() {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean contains(Object o) {
      // TODO: implement me
      return false;
   }

   @Override
   public Object[] toArray() {
      // TODO: implement me
      return null;
   }

   @Override
   public <T> T[] toArray(T[] a) {
      // TODO: implement me
      return null;
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
   public boolean containsAll(Collection<?> c) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      // TODO: implement me
      return false;
   }

   @Override
   public void clear() {
      // TODO: implement me
      
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
      // TODO: implement me
      return null;
   }

   @Override
   public int advance() {
      // TODO: implement me
      return 0;
   }

   @Override
   public int advanceBy(int distance) {
      // TODO: implement me
      return 0;
   }

   @Override
   public int retreat() {
      // TODO: implement me
      return 0;
   }

   @Override
   public int retreatBy(int distance) {
      // TODO: implement me
      return 0;
   }

   @Override
   public void reset() {
      // TODO: implement me
      
   }

   @Override
   public E remove() {
      // TODO: implement me
      return null;
   }

   @Override
   public Iterator<E> iterator() {
      // TODO: implement me
      return null;
   }

   @Override
   public Iterator<E> cycle() {
      // TODO: implement me
      return null;
   }

   @Override
   public Cycle<E> reverse() {
      // TODO: implement me
      return null;
   }

   @Override
   public boolean isCyclicOrder(E b, E c) {
      // TODO: implement me
      return false;
   }

   @Override
   public boolean isCyclicOrder(E a, E b, E c) {
      // TODO: implement me
      return false;
   }
}
