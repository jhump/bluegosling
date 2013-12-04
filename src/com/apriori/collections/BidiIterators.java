package com.apriori.collections;

import java.util.List;
import java.util.ListIterator;

/**
 * Utility methods related to bi-directional iterators.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public final class BidiIterators {
   private BidiIterators() {
   }

   /**
    * Adapts a {@link ListIterator} to the {@link BidiIterator} interface.
    *
    * @param listIterator a list iterator
    * @return a bidirectional iterator that delegates to the specified list iterator
    */
   public static <E> BidiListIterator<E> fromListIterator(ListIterator<E> listIterator) {
      return new BidiListIteratorImpl<E>(listIterator);
   }
   
   /**
    * Adapts a {@link List} to the {@link BidiIterable} interface.
    *
    * @param list a list
    * @return a bidirectional iterable that delegates to the specified list
    */
   public static <E> BidiIterable<E> fromList(final List<E> list) {
      return new BidiIterable<E>() {
         @Override public BidiIterator<E> iterator() {
            return fromListIterator(list.listIterator());
         }
      };
   }
   
   /**
    * A bidirectional iterator that wraps a {@link ListIterator}.
    *
    * @param <E> the type of element fetched from the iterator
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class BidiListIteratorImpl<E> implements BidiListIterator<E> {
      private final ListIterator<E> listIterator;
      
      BidiListIteratorImpl(ListIterator<E> listIterator) {
         this.listIterator = listIterator;
      }
      
      @Override
      public boolean hasPrevious() {
         return listIterator.hasPrevious();
      }

      @Override
      public E previous() {
         return listIterator.previous();
     }

      @Override
      public boolean hasNext() {
         return listIterator.hasNext();
      }

      @Override
      public E next() {
         return listIterator.next();
      }

      @Override
      public void remove() {
         listIterator.remove();
      }

      @Override
      public int nextIndex() {
         return listIterator.nextIndex();
      }

      @Override
      public int previousIndex() {
         return listIterator.previousIndex();
      }

      @Override
      public void set(E e) {
         listIterator.set(e);
      }

      @Override
      public void add(E e) {
         listIterator.add(e);
      }

      @Override
      public BidiListIterator<E> reverse() {
         final BidiListIterator<E> self = this;
         ListIterator<E> reverse = CollectionUtils.reverseIterator(listIterator);
         return new BidiListIteratorImpl<E>(reverse) {
            @Override public BidiListIterator<E> reverse() {
               return self;
            }
         };
      }
   }
}
