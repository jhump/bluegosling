package com.apriori.collections;

import com.apriori.util.Cloners;

import java.security.SecureRandom;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

// TODO: javadoc
// TODO: implement me!
// TODO: tests
// TODO: serialization?
// TODO: fixed capacity w/ random sampling/culling?
public class RandomizedQueue<E> extends AbstractCollection<E> implements Queue<E> {

   private final Random random;
   final List<E> elements;
   int peekIndex = -1;
   
   public <T extends List<E> & Cloneable> RandomizedQueue(T elements) {
      this(elements, new SecureRandom());
   }

   public <T extends List<E> & Cloneable> RandomizedQueue(T elements, Random random) {
      this.elements = Cloners.<T>forCloneable().clone(elements);
      this.random = random;
   }
   
   @Override
   public Iterator<E> iterator() {
      final Iterator<E> iter = elements.iterator();
      return new Iterator<E>() {
         @Override
         public boolean hasNext() {
            return iter.hasNext();
         }

         @Override
         public E next() {
            return iter.next();
         }

         @Override
         public void remove() {
            iter.remove();
            peekIndex = -1;
         }
      };
   }

   @Override
   public int size() {
      return elements.size();
   }

   @Override
   public boolean isEmpty() {
      return elements.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return elements.contains(o);
   }

   @Override
   public boolean remove(Object o) {
      if (elements.remove(o)) {
         peekIndex = -1;
         return true;
      }
      return false;
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return elements.containsAll(c);
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      if (elements.removeAll(c)) {
         peekIndex = -1;
         return true;
      }
      return false;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      if (elements.retainAll(c)) {
         peekIndex = -1;
         return true;
      }
      return false;
   }
   
   @Override
   public boolean add(E e) {
      return elements.add(e);
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      return elements.addAll(c);
   }

   @Override
   public boolean offer(E e) {
      return add(e);
   }
   
   private int pickRandom() {
      return random.nextInt(elements.size());
   }
   
   private int headIndex() {
      return peekIndex == -1 ? (peekIndex = pickRandom()) : peekIndex;
   }
   
   private E removeHead() {
      int idx = headIndex();
      peekIndex = -1;
      int lastIndex = elements.size() - 1;
      if (idx == lastIndex) {
         return elements.remove(lastIndex);
      } else {
         E last = elements.remove(lastIndex);
         E ret = elements.get(idx);
         elements.set(idx, last);
         return ret;
      }
   }

   @Override
   public E remove() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return removeHead();
   }

   @Override
   public E poll() {
      return isEmpty() ? null : removeHead();
   }

   private E peekHead() {
      return elements.get(headIndex());
   }
   
   @Override
   public E element() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return peekHead();
   }

   @Override
   public E peek() {
      return isEmpty() ? null : peekHead();
   }
}
