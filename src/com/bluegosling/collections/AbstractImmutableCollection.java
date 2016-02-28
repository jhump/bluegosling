package com.bluegosling.collections;

/**
 * An abstract base class for implementing {@link ImmutableCollection} and its sub-interfaces. This
 * provides implementations for all methods except {@link #size()} and {@link #iterator()}.
 *
 * @param <E> the type of element in the collection
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
// TODO: move some of these into default methods on ImmutableCollection
public abstract class AbstractImmutableCollection<E> implements ImmutableCollection<E> {
   @Override
   public boolean isEmpty() {
      return size() == 0;
   }

   @Override
   public Object[] toArray() {
      return CollectionUtils.toArray(this);
   }

   @Override
   public <T> T[] toArray(T[] array) {
      return CollectionUtils.toArray(this, array);
   }

   @Override
   public boolean contains(Object o) {
      return CollectionUtils.contains(iterator(), o);
   }

   @Override
   public boolean containsAll(Iterable<?> items) {
      for (Object o : items) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean containsAny(Iterable<?> items) {
      for (Object o : items) {
         if (contains(o)) {
            return true;
         }
      }
      return false;
   }
   
   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }
}
