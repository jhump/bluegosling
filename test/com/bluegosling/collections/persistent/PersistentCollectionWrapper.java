package com.bluegosling.collections.persistent;

import com.bluegosling.collections.Iterables;
import com.bluegosling.collections.immutable.ImmutableCollectionWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A wrapper to adapt a {@link Collection} to a {@link PersistentCollection}. This is used to create
 * "reference implementations" for a {@link PersistentCollection}, against which the behavior of
 * other implementations can be tested. For example, we wrap an {@link ArrayList} and use the
 * resulting persistent list to verify behavior of other persistent lists. These wrapped persistent
 * collections are for testing. They copy the entire wrapped collection during update operations, so
 * they are not expected to perform well.
 *
 * @param <E> the type of element in the collection
 * @param <C> the type of collection being wrapped
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class PersistentCollectionWrapper<E, C extends Collection<E>,
                                           P extends PersistentCollectionWrapper<E, C, P>>
extends ImmutableCollectionWrapper<E, C>
implements PersistentCollection<E> {
   
   public PersistentCollectionWrapper(C collection) {
      super(collection);
   }
   
   protected abstract C copy(C original);
   protected abstract P wrapPersistent(C coll);

   @Override
   public P add(E e) {
      C newCollection = copy(collection);
      newCollection.add(e);
      return wrapPersistent(newCollection);
   }

   @Override
   public P remove(Object o) {
      C newCollection = copy(collection);
      newCollection.remove(o);
      return wrapPersistent(newCollection);
   }

   @Override
   public P removeAll(Object o) {
      C newCollection = copy(collection);
      for (Iterator<E> iter = newCollection.iterator(); iter.hasNext();) {
         E e = iter.next();
         if (o == null ? e == null : o.equals(e)) {
            iter.remove();
         }
      }
      return wrapPersistent(newCollection);
   }

   @Override
   public P removeAll(Iterable<?> items) {
      C newCollection = copy(collection);
      newCollection.removeAll(Iterables.snapshot(items));
      return wrapPersistent(newCollection);
   }

   @Override
   public P retainAll(Iterable<?> items) {
      C newCollection = copy(collection);
      newCollection.retainAll(Iterables.snapshot(items));
      return wrapPersistent(newCollection);
   }

   @Override
   public P addAll(Iterable<? extends E> items) {
      C newCollection = copy(collection);
      newCollection.addAll(Iterables.snapshot(items));
      return wrapPersistent(newCollection);
   }
}
