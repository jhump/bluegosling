package com.apriori.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

// TODO: javadoc
// TODO: tests
public final class Iterators {
   private Iterators() {
   }
   
   public static <E> Iterator<E> emptyIterator() {
      @SuppressWarnings("unchecked")
      Iterator<E> ret = (Iterator<E>) EmptyIterator.INSTANCE;
      return ret;
   }
   
   public static <E> Iterator<E> singletonIterator(E element) {
      return new SingletonIterator<E>(element);
   }
   
   private static class EmptyIterator extends ReadOnlyIterator<Object> {
      static final EmptyIterator INSTANCE = new EmptyIterator();
      
      @Override
      public boolean hasNext() {
         return false;
      }

      @Override
      public Object next() {
         throw new NoSuchElementException();
      }
   }

   private static class SingletonIterator<E> extends ReadOnlyIterator<E> {
      private final E e;
      private boolean used;
      
      SingletonIterator(E e) {
         this.e = e;
      }
      
      @Override
      public boolean hasNext() {
         return !used;
      }

      @Override
      public E next() {
         if (used) {
            throw new NoSuchElementException();
         }
         used = true;
         return e;
      }
   }
}
