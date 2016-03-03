package com.bluegosling.collections.views;

import com.bluegosling.collections.CollectionUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A filtered view of another set. This is a wrapper that elides all members of the wrapped set that
 * do not match a given predicate. Changes to the underlying set will be visible through the
 * filtering set (or at least those changes that effect elements that match the predicate).
 * 
 * <p>Attempts to add items to the filtering set that do <em>not</em> match the predicate will throw
 * {@link IllegalArgumentException}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <E> the type of element in the sets
 */
//TODO: tests
public class FilteringSet<E> extends FilteringCollection<E> implements Set<E> {

   /**
    * Constructs a new filtering set. Elements in this set will include only the elements from the
    * specified set that match the specified predicate. An element {@code e} matches the predicate
    * if {@code predicate.apply(e)} returns true. 
    * 
    * @param set the wrapped set
    * @param predicate the filtering predicate
    */
   public FilteringSet(Set<E> set, Predicate<? super E> predicate) {
      super(set, predicate);
   }

   @Override
   protected Set<E> internal() {
      return (Set<E>) super.internal();
   }
   
   @Override
   public Set<E> capture() {
      return Collections.unmodifiableSet(new LinkedHashSet<>(this));
   }
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this,  o);
   }

   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
}