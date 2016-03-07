package com.bluegosling.collections.immutable;

import com.bluegosling.collections.CollectionUtils;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An abstract base class for immutable list implementations that use linked data structures
 * so that access is sequential. Random access operations in this sort of list have linear runtime
 * complexity. Sub-classes need only implement {@link #size()}, {@link #first()}, and
 * {@link #rest()}.
 *
 * @param <E> the type of element in the list
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
public abstract class AbstractLinkedImmutableList<E> extends AbstractImmutableList<E>
implements ConsList<E> {

   /**
    * Checks that the specified index is greater than or equal to zero and less than this list's
    * {@link #size}.
    *
    * @param index an index to check
    */
   protected void rangeCheck(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index >= size()) {
         throw new IndexOutOfBoundsException(index + " >= " + size());
      }
   }

   /**
    * Checks that the specified index is greater than or equal to zero and less than or equal to
    * this list's {@link #size}. This is for certain operations where an index equal to the size
    * (<em>after</em> the last valid index in the list) is allowed.
    *
    * @param index an index to check
    */
   protected void rangeCheckWide(int index) {
      if (index < 0) {
         throw new IndexOutOfBoundsException(index + " < 0");
      } else if (index > size()) {
         throw new IndexOutOfBoundsException(index + " > " + size());
      }
   }

   @Override
   public Iterator<E> iterator() {
      return new Iterator<E>() {
         private ConsList<E> current = AbstractLinkedImmutableList.this;
         
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

   private static class IterNode<E> {
      final E value;
      final IterNode<E> prev;
      IterNode<E> next;
      
      IterNode(E value) {
         this(value, null);
      }
      
      IterNode(E value, IterNode<E> prev) {
         this.value = value;
         this.prev = prev;
      }

      IterNode<E> makeNext(E e) {
         IterNode<E> n = new IterNode<>(e, this);
         this.next = n;
         return n;
      }
   }
   
   @Override
   public ListIterator<E> listIterator() {
      // creates an ad-hoc doubly-linked list as we progress through values, in order to iterate
      // backwards to previous values
      return new ListIterator<E>() {
         private ConsList<E> highWater;
         private IterNode<E> next;
         private IterNode<E> prev = null;
         private int nextIndex = 0;
         
         {
            if (isEmpty()) {
               next = null;
            } else {
               next = new IterNode<>(first());
               highWater = rest();
            }
         }
         
         @Override
         public boolean hasNext() {
            return next != null;
         }

         @Override
         public E next() {
            if (next == null) {
               throw new NoSuchElementException();
            }
            prev = next;
            E ret = next.value;
            if (next.next == null && !highWater.isEmpty()) {
               // append new node to doubly-linked list
               next = next.makeNext(highWater.first());
               highWater = highWater.rest();
            } else {
               next = next.next;
            }
            nextIndex++;
            return ret;
         }

         @Override
         public boolean hasPrevious() {
            return prev != null;
         }

         @Override
         public E previous() {
            if (prev == null) {
               throw new NoSuchElementException();
            }
            next = prev;
            E ret = prev.value;
            prev = prev.prev;
            nextIndex--;
            return ret;
         }

         @Override
         public int nextIndex() {
            return nextIndex;
         }

         @Override
         public int previousIndex() {
            return nextIndex - 1;
         }

         @Override
         public void remove() {
            throw new UnsupportedOperationException();
         }

         @Override
         public void set(E e) {
            throw new UnsupportedOperationException();
         }

         @Override
         public void add(E e) {
            throw new UnsupportedOperationException();
         }
      };
   }
   
   @Override
   public ListIterator<E> listIterator(int pos) {
      rangeCheckWide(pos);
      ListIterator<E> iter = listIterator();
      while (pos > 0) {
         iter.next();
         pos--;
      }
      return iter;
   }
   
   @Override
   public E get(int i) {
      rangeCheck(i);
      ConsList<E> current = this;
      for (; i > 0; i--) {
         current = current.rest();
      }
      return current.first();
   }

   @Override
   public int indexOf(Object o) {
      int index = 0;
      ConsList<E> current = this;
      while (!current.isEmpty()) {
         if (Objects.equals(o, current.first())) {
            return index;
         }
         index++;
         current = current.rest();
      }
      return -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      int index = 0;
      int lastIndex = -1;
      ConsList<E> current = this;
      while (!current.isEmpty()) {
         if (Objects.equals(o, current.first())) {
            lastIndex = index;
         }
         index++;
         current = current.rest();
      }
      return lastIndex;
   }

   @Override
   public ConsList<E> subList(int from, int to) {
      rangeCheckWide(from);
      rangeCheckWide(to);
      if (from > to) {
         throw new IndexOutOfBoundsException("from " + from + " > to " + to);
      }
      ConsList<E> current = this;
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
   
   /**
    * A sub-list view of an immutable list that is sequential/linked in nature.
    *
    * @param <E> the type of element in the list
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   protected static class LinkedSubList<E> extends AbstractLinkedImmutableList<E> {
      private final ConsList<E> tail;
      private final int length;
      
      public LinkedSubList(ConsList<E> tail, int length) {
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
      public ConsList<E> rest() {
         return new LinkedSubList<E>(tail.rest(), length - 1);
      }

      @Override
      public int size() {
         return length;
      }
      
      @Override
      public ConsList<E> subList(int from, int to) {
         rangeCheckWide(from);
         rangeCheckWide(to);
         if (from > to) {
            throw new IndexOutOfBoundsException("from " + from + " > to " + to);
         }
         ConsList<E> current = tail;
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
