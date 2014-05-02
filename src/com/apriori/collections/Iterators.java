package com.apriori.collections;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

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
   
   public static <E> Iterator<E> reversed(Iterable<E> iterable) {
      // check for a few special cases where we can reverse the iterable very efficiently
      if (iterable instanceof List) {
         List<E> list = (List<E>) iterable;
         return CollectionUtils.reverseIterator(list.listIterator(list.size()));
      } else if (iterable instanceof ImmutableList && iterable instanceof RandomAccess) {
         final ImmutableList<E> list = (ImmutableList<E>) iterable;
         return new Iterator<E>() {
            int nextIndex = list.size() - 1;
            
            @Override public boolean hasNext() {
               return nextIndex >= 0;
            }

            @Override public E next() {
               if (nextIndex < 0) {
                  throw new NoSuchElementException();
               }
               return list.get(nextIndex--);
            }
         };
      } else if (iterable instanceof Deque) {
         return ((Deque<E>) iterable).descendingIterator();
      } else if (iterable instanceof NavigableSet) {
         return ((NavigableSet<E>) iterable).descendingIterator();
      } else if (iterable instanceof ImmutableSortedSet) {
         return Immutables.descendingIterator((ImmutableSortedSet<E>) iterable);
      } else {
         // No way that we know of to efficiently reverse the given iterable. So we push all of its
         // contents into a stack, so we can then trivially iterate the stack to reverse order
         Iterator<E> iter = iterable.iterator();
         if (!iter.hasNext()) {
            // empty iterator -- no sense doing anything else
            return iter;
         }
         ArrayDeque<E> deque;
         // try to determine best initial size
         if (iterable instanceof Collection) {
            deque = new ArrayDeque<>(((Collection<?>) iterable).size());
         } else if (iterable instanceof ImmutableCollection) {
            deque = new ArrayDeque<>(((ImmutableCollection<?>) iterable).size());
         } else if (iterable instanceof ImmutableMap) {
            deque = new ArrayDeque<>(((ImmutableMap<?, ?>) iterable).size());
         } else if (iterable instanceof PriorityQueue) {
            deque = new ArrayDeque<>(((PriorityQueue<?, ?>) iterable).size());
         } else {
            deque = new ArrayDeque<>();
         }
         while (iter.hasNext()) {
            deque.push(iter.next());
         }
         return deque.iterator();
      }
   }
   
   @SafeVarargs
   public static <E> Iterator<E> concat(Iterator<? extends E>... iterators) {
      // TODO
      return null;
   }
   
   private static class EmptyIterator implements Iterator<Object> {
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

   private static class SingletonIterator<E> implements Iterator<E> {
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
