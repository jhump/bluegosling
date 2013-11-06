package com.apriori.collections;

// TODO: javadoc
// TODO: tests?
class ReversedBidiIterator<E> implements BidiIterator<E> {
   private final BidiIterator<E> iterator;
   
   ReversedBidiIterator(BidiIterator<E> iterator) {
      this.iterator = iterator;
   }
   
   @Override
   public boolean hasNext() {
      return iterator.hasPrevious();
   }

   @Override
   public E next() {
      return iterator.previous();
   }

   @Override
   public void remove() {
      iterator.remove();
   }

   @Override
   public BidiIterator<E> reverse() {
      return iterator;
   }

   @Override
   public boolean hasPrevious() {
      return iterator.hasNext();
   }

   @Override
   public E previous() {
      return iterator.next();
   }
}
