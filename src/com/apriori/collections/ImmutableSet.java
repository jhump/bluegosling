package com.apriori.collections;

import java.util.Set;

/**
 * An immutable, read-only collection that does not contain duplicate elements. This is analogous
 * to the standard {@link Set} interface except that it defines no mutation operations.
 *
 * @param <E> the type of element in the set
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface ImmutableSet<E> extends ImmutableCollection<E> {
   /**
    * Returns true if this set is equal to the specified object. The objects are equal if both of
    * them are {@link ImmutableSet}s and they contain the same elements. They contain the same
    * elements when this set {@linkplain #containsAll(Iterable) contains all} of the items in the
    * specified set and vice versa.
    *
    * @param o an object
    * @return true if the specified object is an immutable set with the same elements
    */
   @Override boolean equals(Object o);
   
   /**
    * Returns the hash code value for this set. The hash code of a set is defined to be the sum of
    * the hash codes of the elements in the set, where the hash code of a {@code null} element is
    * defined to be zero. This ensures that {@code s1.equals(s2)} implies that
    * {@code s1.hashCode()==s2.hashCode()} for any two sets {@code s1} and {@code s2}, as required
    * by the general contract of {@link Object#hashCode}.
    *
    * @return the hash code for this set
    * 
    * @see Set#hashCode()
    */
   @Override int hashCode();
}
