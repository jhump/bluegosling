package com.apriori.collections;

/**
 * An abstract base class for implementing an {@link ImmutableSet}. This provides implementations
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
// TODO: tests
public abstract class AbstractImmutableSet<E> extends AbstractImmutableCollection<E>
      implements ImmutableSet<E> {
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this, o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
}
