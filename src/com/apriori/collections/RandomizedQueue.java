package com.apriori.collections;

import java.security.SecureRandom;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

// TODO: javadoc
// TODO: tests
// TODO: serialization and cloning
// TODO: fixed capacity w/ random sampling/culling?
public class RandomizedQueue<E> extends AbstractCollection<E> implements Queue<E> {
   private static final int DEFAULT_INITIAL_CAPACITY = 16;
   
   private final Random random;
   Object[] elements;
   int size;
   int peekIndex = -1;
   int modCount;
   
   public RandomizedQueue() {
      this(DEFAULT_INITIAL_CAPACITY);
   }

   public RandomizedQueue(int initialCapacity) {
      this(initialCapacity, new SecureRandom());
   }

   public RandomizedQueue(int initialCapacity, Random random) {
      this.random = random;
      this.elements = new Object[initialCapacity];
      this.size = 0;
   }

   public RandomizedQueue(Collection<? extends E> elements) {
      this(elements, new SecureRandom());
   }

   public RandomizedQueue(Collection<? extends E> elements, Random random) {
      this.random = random;
      this.elements = elements.toArray();
      this.size = this.elements.length;
   }
   
   @Override
   public Iterator<E> iterator() {
      return new Iterator<E>() {
         int index = 0;
         int lastFetched = -1;
         int myModCount = modCount;
         
         private void checkModCount() {
            if (myModCount != modCount) {
               throw new ConcurrentModificationException();
            }
         }

         @Override
         public boolean hasNext() {
            checkModCount();
            return index < size;
         }

         @SuppressWarnings("unchecked")
         @Override
         public E next() {
            checkModCount();
            if (index >= size) {
               throw new NoSuchElementException();
            }
            lastFetched = index;
            return (E) elements[index++];
         }

         @Override
         public void remove() {
            checkModCount();
            if (lastFetched == -1) {
               throw new IllegalStateException();
            }
            ArrayUtils.removeIndex(index, elements, size);
            if (lastFetched == peekIndex) {
               peekIndex = -1;
            } else if (lastFetched < peekIndex) {
               peekIndex--;
            }
            lastFetched = -1;
            index--;
            myModCount = ++modCount;
         }
      };
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public boolean add(E e) {
      maybeGrowBy(1);
      elements[size++] = e;
      modCount++;
      return true;
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      maybeGrowBy(c.size());
      for (Iterator<? extends E> iter = c.iterator(); iter.hasNext();) {
         elements[size++] = iter.next();
      }
      modCount++;
      return true;
   }
   
   private void maybeGrowBy(int spaceNeeded) {
      elements = ArrayUtils.maybeGrowBy(elements, size, spaceNeeded);
   }

   @Override
   public boolean offer(E e) {
      return add(e);
   }
   
   private int pickRandom() {
      return random.nextInt(size);
   }
   
   private int headIndex() {
      return peekIndex == -1 ? (peekIndex = pickRandom()) : peekIndex;
   }
   
   private E removeHead() {
      int idx = headIndex();
      peekIndex = -1;
      size--;
      @SuppressWarnings("unchecked")
      E ret = (E) elements[idx];
      if (idx != size) {
         elements[idx] = elements[size];
      }
      elements[size] = null;
      modCount++;
      return ret;
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

   @SuppressWarnings("unchecked")
   private E peekHead() {
      return (E) elements[headIndex()];
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

   @Override
   public void clear() {
      size = 0;
      peekIndex = -1;
      modCount++;
      Arrays.fill(elements,  null);
   }
}
