package com.apriori.collections;

import java.util.List;
import java.util.ListIterator;

// TODO: javadoc
// TODO: tests
public final class BidiIterators {
   private BidiIterators() {
   }
   
   public static <E> BidiListIterator<E> fromListIterator(ListIterator<E> listIterator) {
      return new BidiListIteratorImpl<E>(listIterator);
   }
   
   public static <E> BidiIterable<E> fromList(final List<E> list) {
      return new BidiIterable<E>() {
         @Override public BidiIterator<E> iterator() {
            return fromListIterator(list.listIterator());
         }
      };
   }
   
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
