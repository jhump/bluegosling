package com.bluegosling.collections;

import com.bluegosling.collections.immutable.ImmutableCollection;
import com.bluegosling.collections.immutable.Immutables;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * An iterable that described a fixed number of elements. In addition to providing an iterator or
 * spliterator over the elements, this object also has methods for querying the count of elements.
 *
 * @param <E> the type of elements described by this iterable
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
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
   
   /**
    * Creates a spliterator over the elements described by this iterable. Since this iterable has
    * information about the number of elements, it returns a spliterator with the
    * {@link Spliterator#SIZED} and {@link Spliterator#SUBSIZED} characteristics (but no others).
    *
    * @see java.lang.Iterable#spliterator()
    */
   @Override
   default Spliterator<E> spliterator() {
      return Spliterators.spliterator(iterator(), size(), 0);
   }
   
   /**
    * Adapts a {@link Collection} to this {@link SizedIterable} interface. This interface contains a
    * subset of methods of the collection interface, so each method can simply call through to the
    * collection.
    *
    * @param coll a collection
    * @return a sized iterable that merely wraps the given collection
    */
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
   
   /**
    * Adapts this {@link SizedIterable} interface to the standard {@link Collection} interface.
    * Since the collection interface is a super-set of this interface, this returns an
    * {@link AbstractCollection}, which provides the broader collection interface implemented almost
    * entirely in terms of iterators.
    * 
    * <p>Note that collection operations, like {@link Collection#contains(Object) contains} and
    * {@link Collection#remove(Object) remove}, use the {@link #iterator()} to find and remove
    * elements, so they run in linear time. Many {@link SizedIterable} implementors could provide
    * more efficient implementations of these methods, and will often provide different adaptations
    * to the standard {@link Collection} interface. For example, you can use
    * {@link Immutables#asIfMutable(ImmutableCollection)} to provide a more efficient collection
    * implementation that adapts the {@link ImmutableCollection} interface.
    *
    * @param iter a sized iterable
    * @return a collection that wraps the given sized iterable
    */
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
