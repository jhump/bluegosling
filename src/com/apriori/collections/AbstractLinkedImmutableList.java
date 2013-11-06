package com.apriori.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

//TODO: javadoc
//TODO: tests
public abstract class AbstractLinkedImmutableList<E> extends AbstractImmutableCollection<E>
      implements ImmutableList<E> {

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
         private ImmutableList<E> current = AbstractLinkedImmutableList.this;
         
         @Override
         public boolean hasNext() {
            return !current.isEmpty();
         }

         @Override
         public E next() {
            E ret = current.first();
            current = current.rest();
            return ret;
         }
      };
   }

   @Override
   public E get(int i) {
      rangeCheck(i);
      ImmutableList<E> current = this;
      for (; i > 0; i--) {
         current = current.rest();
      }
      return current.first();
   }

   @Override
   public int indexOf(Object o) {
      int index = 0;
      for (E element : this) {
         if (o == null ? element == null : o.equals(element)) {
            return index;
         }
         index++;
      }
      return -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      Object array[] = toArray();
      for (int i = size() - 1; i >= 0; i--) {
         Object element = array[i];
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
      ImmutableList<E> current = this;
      for (; from > 0; from--) {
         current = current.rest();
      }
      if (to == size()) {
         return current;
      } else {
         return new LinkedSubList<E>(current, to - from);
      }
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
   
   protected static class LinkedSubList<E> extends AbstractLinkedImmutableList<E> {
      private final ImmutableList<E> tail;
      private final int length;
      
      public LinkedSubList(ImmutableList<E> tail, int length) {
         this.tail = tail;
         this.length = length;
      }
      
      @Override
      public E first() {
         if (length == 0) {
            throw new NoSuchElementException();
         }
         return tail.first();
      }

      @Override
      public ImmutableList<E> rest() {
         return new LinkedSubList<E>(tail.rest(), length - 1);
      }

      @Override
      public int size() {
         return length;
      }
      
      @Override
      public ImmutableList<E> subList(int from, int to) {
         rangeCheckWide(from);
         rangeCheckWide(to);
         if (from > to) {
            throw new IndexOutOfBoundsException("from " + from + " > to " + to);
         }
         ImmutableList<E> current = tail;
         for (; from > 0; from--) {
            current = current.rest();
         }
         if (to == size()) {
            return current;
         } else {
            return new LinkedSubList<E>(current, to - from);
         }
      }
   }
}
