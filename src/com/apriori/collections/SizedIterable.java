package com.apriori.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

// TODO: javadoc
public interface SizedIterable<E> extends Iterable<E> {

   /**
    * Determines the size of the sequence.
    *
    * @return the number of elements in this sequence
    */
   int size();

   /**
    * Determines if the sequence has any elements at all.
    *
    * @return true if the sequence is empty, false otherwise
    */
   default boolean isEmpty() {
      return size() == 0;
   }
   
   static <E> SizedIterable<E> fromCollection(Collection<? extends E> coll) {
      return new SizedIterable<E>() {
         @Override
         public Iterator<E> iterator() {
            return Iterables.cast(coll.iterator());
         }

         @Override
         public int size() {
            return coll.size();
         }

         @Override
         public boolean isEmpty() {
            return coll.isEmpty();
         }
      };
   }
   
   static <E> Collection<E> toCollection(SizedIterable<? extends E> iter) {
      return new AbstractCollection<E>() {
         @Override
         public Iterator<E> iterator() {
            return Iterables.cast(iter.iterator());
         }

         @Override
         public int size() {
            return iter.size();
         }

         @Override
         public boolean isEmpty() {
            return iter.isEmpty();
         }
      };
   }
}
