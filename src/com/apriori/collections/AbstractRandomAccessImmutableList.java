package com.apriori.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

//TODO: javadoc
//TODO: tests
public abstract class AbstractRandomAccessImmutableList<E> extends AbstractImmutableCollection<E>
      implements ImmutableList<E>, RandomAccess {

   protected void rangeCheck(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index >= size()) {
         throw new IndexOutOfBoundsException(index + " >= " + size());
      }
   }

   protected void rangeCheckWide(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index > size()) {
         throw new IndexOutOfBoundsException(index + " > " + size());
      }
   }

   @Override
   public Iterator<E> iterator() {
      return new ReadOnlyIterator<E>() {
         private int index = 0;
         
         @Override
         public boolean hasNext() {
            return index < size();
         }

         @Override
         public E next() {
            if (index >= size()) {
               throw new NoSuchElementException();
            }
            return get(index++);
         }
      };
   }

   @Override
   public int indexOf(Object o) {
      for (int i = 0, len = size(); i < len; i++) {
         Object element = get(i);
         if (o == null ? element == null : o.equals(element)) {
            return i;
         }
      }
      return -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      for (int i = size() - 1; i >= 0; i--) {
         Object element = get(i);
         if (o == null ? element == null : o.equals(element)) {
            return i;
         }
      }
      return -1;
   }

   @Override
   public ImmutableList<E> subList(int from, int to) {
      rangeCheckWide(from);
      rangeCheckWide(to);
      if (from > to) {
         throw new IndexOutOfBoundsException("from " + from + " > to " + to);
      }
      return new SubList<E>(this, from, to - from);
   }

   @Override
   public E first() {
      if (isEmpty()) {
         throw new NoSuchElementException();
      }
      return get(0);
   }

   @Override
   public ImmutableList<E> rest() {
      return isEmpty() ? this : subList(1, size());
   }
   
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this, o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
   
   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
   
   protected static class SubList<E> extends AbstractRandomAccessImmutableList<E> {
      private final ImmutableList<E> main;
      private final int offset;
      private final int length;
      
      public SubList(ImmutableList<E> main, int offset, int length) {
         this.main = main;
         this.offset = offset;
         this.length = length;
      }
      
      @Override
      public int size() {
         return length;
      }

      @Override
      public E get(int i) {
         rangeCheck(i);
         return main.get(i + offset);
      }

      @Override
      public ImmutableList<E> subList(int from, int to) {
         rangeCheckWide(from);
         rangeCheckWide(to);
         if (from > to) {
            throw new IndexOutOfBoundsException("from " + from + " > to " + to);
         }
         return main.subList(from + offset, to + offset);
      }
   }
}
