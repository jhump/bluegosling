package com.bluegosling.collections.immutable;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * An abstract base class for implementing immutable collections. This overrides all mutable methods
 * with {@code final} implementations that throw {@link UnsupportedOperationException}.
 *
 * @param <E> the type of element in the collection
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see AbstractImmutableSet
 * @see AbstractImmutableList
 */
public abstract class AbstractImmutableCollection<E> extends AbstractCollection<E> {

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
