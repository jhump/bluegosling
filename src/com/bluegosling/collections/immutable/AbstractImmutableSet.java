package com.bluegosling.collections.immutable;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * An abstract base class for implementing an immutable sets. This provides implementations
 * for all methods except {@link #size()} and {@link #iterator()}.
 *
 * <p>Nothing in this base class enforces set invariants. The only difference between this class
 * and its super-class, {@link AbstractImmutableCollection}, is that this one implements
 * {@link ImmutableSet#equals(Object)} and {@link ImmutableSet#hashCode()}.
 * 
 * <p>Sub-classes will likely override {@link #contains(Object)} to provide better performance. The
 * default implementation (inherited from {@link AbstractImmutableCollection}) performs a linear
 * scan through all elements in the set.
 * 
 * @param <E> the type of element in the collection
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public abstract class AbstractImmutableSet<E> extends AbstractSet<E> {

   @Deprecated
   @Override
   public final boolean add(E e) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean remove(Object o) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean removeIf(Predicate<? super E> filter) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean addAll(Collection<? extends E> c) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
   }

   @Deprecated
   @Override
   public final void clear() {
      throw new UnsupportedOperationException();
   }
}
