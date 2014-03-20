package com.apriori.collections;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * A set whose elements are the results of applying a function to another collection. This set is
 * simply a wrapper. Changes to the underlying set are visible in the transformed set. Accessing
 * elements incurs calls to the transforming function.
 * 
 * <p>Functions that perform the transformations should be deterministic so that a stable,
 * unchanging set does not appear to be mutating when accessed through this transforming wrapper.
 * 
 * <p><strong>Note</strong>: The function should produce a 1-to-1 mapping from elements of the input
 * type to elements of the output type. If the function should ever produce the same output value
 * for two <em>unequal</em> input values, then the invariant that is core to the set -- that
 * elements therein are unique -- could be broken for the transformed set. Failure to comply with
 * this constraint may yield sets that appear to have duplicates. Because of this  requirement,
 * caution is advised when using a {@link TransformingSet}. A safer way to transform a set is to
 * make a snapshot that is transformed and dump those into a new set which can enforce uniqueness.
 * That could be done with code like the following:<pre>
 * Set&lt;Type1&gt; input;
 * Function&lt;Type1, Type2&gt; function;
 * // take a snapshot to produce a transformed set that will never
 * // violate set invariants
 * Set&lt;Type2&gt; transformed = new HashSet&lt;Type2&gt;(
 *       new TransformingCollection&lt;Type1, Type2&gt;(input, function));
 * </pre>
 * 
 * <p>Since transformations can only be done in one direction, some operations are not supported.
 * Namely, {@link #add(Object)} and {@link #addAll(Collection)} throw
 * {@link UnsupportedOperationException}.
 * 
 * <p>Also due to transformations only working in one direction, some methods are implemented in
 * terms of the collection's {@linkplain #iterator() transforming iterator} and thus may have worse
 * performance than the underlying collection's implementation. These methods include
 * {@link #contains(Object)}, {@link #containsAll(Collection)}, {@link #remove(Object)},
 * {@link #removeAll(Collection)}, and {@link #retainAll(Collection)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I> the "input" type; the type of the wrapped set
 * @param <O> the "output" type; the type of elements in this set
 */
// TODO: tests
public class TransformingSet<I, O> extends TransformingCollection<I, O> implements Set<O> {

   /**
    * Constructs a new transforming set.
    * 
    * @param set the wrapped set
    * @param function the function used to transform elements, which must provide a 1-to-1 mapping
    *       from the input domain to the output domain
    */
   public TransformingSet(Set<I> set, Function<? super I, ? extends O> function) {
      super(set, function);
   }
   
   /**
    * Gets the wrapped set.
    * 
    * @return the wrapped set
    */
   @Override
   protected Set<I> internal() {
      return (Set<I>) super.internal();
   }
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this,  o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
   
   /**
    * An unmodifiable transforming set. All mutation operations throw
    * {@link UnsupportedOperationException}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped set
    * @param <O> the "output" type; the type of elements in this set
    */
   public static class ReadOnly<I, O> extends TransformingCollection.ReadOnly<I, O> implements Set<O> {

      /**
       * Constructs a new read-only transforming set.
       * 
       * @param set the wrapped set
       * @param function the function used to transform elements, which must provide a 1-to-1
       *    mapping from the input domain to the output domain
       */
      public ReadOnly(Set<I> set, Function<? super I, ? extends O> function) {
         super(set, function);
      }
      
      /**
       * Gets the wrapped set.
       * 
       * @return the wrapped set
       */
      @Override
      protected Set<I> internal() {
         return (Set<I>) super.internal();
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

   /**
    * A transforming set that can transform values in both directions. Since this set can transform
    * values in the other direction (output type -&gt; input type), all operations are supported,
    * including {@link #add(Object)} and {@link #addAll(Collection)}.
    * 
    * <p>Several methods are overridden to use the reverse function before delegating to the
    * underlying set. Since some of these interface methods actually accept any type of object
    * (instead of requiring the set's element type), this implementation could result in
    * {@link ClassCastException}s if objects with incorrect types are passed to them. These
    * methods are {@link #contains(Object)}, {@link #containsAll(Collection)},
    * {@link #remove(Object)}, {@link #removeAll(Collection)}, and {@link #retainAll(Collection)}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped set
    * @param <O> the "output" type; the type of elements in this set
    */
   public static class Bidi<I, O> extends TransformingCollection.Bidi<I, O> implements Set<O> {

      /**
       * Constructs a new bidirectional transforming set. The two provided functions must be
       * inverses of one another. In other words, for any object {@code i} in the input domain,
       * {@code i} must <em>equal</em> {@code function2.apply(function1.apply(i))}. Similarly, for
       * any object {@code o} in the output domain, {@code o} must <em>equal</em>
       * {@code function1.apply(function2.apply(o))}.
       * 
       * @param set the wrapped set
       * @param function1 transforms elements; "input" -&gt; "output"
       * @param function2 the inverse of {@code function1}; "output" -&gt; "input"
       */
      public Bidi(Set<I> set, Function<? super I, ? extends O> function1,
            Function<? super O, ? extends I> function2) {
         super(set, function1, function2);
      }
      
      /**
       * Gets the wrapped set.
       * 
       * @return the wrapped set
       */
      @Override
      protected Set<I> internal() {
         return (Set<I>) super.internal();
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
}
