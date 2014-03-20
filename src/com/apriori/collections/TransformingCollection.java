package com.apriori.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

/**
 * A collection whose elements are the results of applying a function to another collection. This
 * collection is simply a wrapper. Changes to the underlying collection are visible in the
 * transformed collection. Accessing elements incurs calls to the transforming function.
 * 
 * <p>Functions that perform the transformations should be deterministic so that a stable,
 * unchanging collection does not appear to be mutating when accessed through this transforming
 * wrapper.
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
 * @param <I> the "input" type; the type of the wrapped collection
 * @param <O> the "output" type; the type of elements in this collection
 */
//TODO: tests
public class TransformingCollection<I, O> extends TransformingIterable<I, O>
      implements Collection<O> {

   /**
    * Constructs a new transforming collection.
    * 
    * @param collection the wrapped collection
    * @param function the function used to transform elements
    */
   public TransformingCollection(Collection<I> collection,
         Function<? super I, ? extends O> function) {
      super(collection, function);
   }
   
   /**
    * Gets the wrapped collection.
    * 
    * @return the wrapped collection
    */
   @Override
   protected Collection<I> internal() {
      return (Collection<I>) super.internal();
   }

   @Override
   public int size() {
      return internal().size();
   }

   @Override
   public boolean isEmpty() {
      return internal().isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return CollectionUtils.contains(iterator(),  o);
   }

   @Override
   public Object[] toArray() {
      return CollectionUtils.toArray(this);
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return CollectionUtils.toArray(this, a);
   }

   @Override
   public boolean add(O e) {
      throw new UnsupportedOperationException("add");
   }

   @Override
   public boolean remove(Object o) {
      return CollectionUtils.removeObject(o,  iterator(), true);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return CollectionUtils.containsAll(this,  c);
   }

   @Override
   public boolean addAll(Collection<? extends O> c) {
      throw new UnsupportedOperationException("addAll");
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      return CollectionUtils.filter(c, iterator(), true);
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      return CollectionUtils.filter(c, iterator(), false);
   }
   
   @Override
   public void clear() {
      internal().clear();
   }
   
   @Override
   public String toString() {
      return CollectionUtils.toString(this);
   }

   /**
    * An unmodifiable transforming collection. All mutation operations throw
    * {@link UnsupportedOperationException}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped collection
    * @param <O> the "output" type; the type of elements in this collection
    */
   public static class ReadOnly<I, O> extends TransformingCollection<I, O> {

      /**
       * Constructs a new read-only transforming collection.
       * 
       * @param collection the wrapped collection
       * @param function the function used to transform elements
       */
      public ReadOnly(Collection<I> collection,
            Function<? super I, ? extends O> function) {
         super(collection, function);
      }
      
      @Override
      public Iterator<O> iterator() {
         return new TransformingIterator.ReadOnly<I, O>(internal().iterator(), function());
      }
      
      @Override
      public boolean remove(Object o) {
         throw new UnsupportedOperationException("remove");
      }
      
      @Override
      public boolean removeAll(Collection<?> c) {
         throw new UnsupportedOperationException("removeAll");
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         throw new UnsupportedOperationException("retainAll");
      }

      @Override
      public void clear() {
         throw new UnsupportedOperationException("clear");
      }
   }
   
   /**
    * A transforming collection that can transform values in both directions. Since this collection
    * can transform values in the other direction (output type -&gt; input type), all operations are
    * supported, including {@link #add(Object)} and {@link #addAll(Collection)}.
    * 
    * <p>Several methods are overridden to use the reverse function before delegating to the
    * underlying list. Since some of these interface methods actually accept any type of object
    * (instead of requiring the list's element type), this implementation could result in
    * {@link ClassCastException}s if objects with incorrect types are passed to them. These
    * methods are {@link #contains(Object)}, {@link #containsAll(Collection)},
    * {@link #remove(Object)}, {@link #removeAll(Collection)}, and {@link #retainAll(Collection)}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped collection
    * @param <O> the "output" type; the type of elements in this collection
    */
   public static class Bidi<I, O> extends TransformingCollection<I, O> {

      private final Function<? super O, ? extends I> function;
      
      /**
       * Constructs a new bidirectional transforming collection. The two provided functions must be
       * inverses of one another. In other words, for any object {@code i} in the input domain,
       * {@code i} must <em>equal</em> {@code function2.apply(function1.apply(i))}. Similarly, for
       * any object {@code o} in the output domain, {@code o} must <em>equal</em>
       * {@code function1.apply(function2.apply(o))}.
       * 
       * @param collection the wrapped collection
       * @param function1 transforms elements; "input" -&gt; "output"
       * @param function2 the inverse of {@code function1}; "output" -&gt; "input"
       */
      public Bidi(Collection<I> collection, Function<? super I, ? extends O> function1,
            Function<? super O, ? extends I> function2) {
         super(collection, function1);
         this.function = function2;
      }
      
      /**
       * Gets the function used to transform elements from the "output" domain to the "input"
       * domain. This is the inverse of {@link #function()}.
       * 
       * @return the inverse function
       */
      protected Function<? super O, ? extends I> inverseFunction() {
         return function;
      }

      /**
       * Transforms "output" elements into "input" elements. This is the opposite direction as
       * {@link #apply(Object)}.
       * 
       * @param input the object to transform, which should be of type {@code O}
       * @return the transformed result
       */
      @SuppressWarnings("unchecked")
      protected I unapply(Object input) {
         return function.apply((O) input);
      }
      
      /**
       * Transforms a collection of "output" elements into "input" elements.
       * 
       * @param c the collection to transform, whose elements should be of type {@code O}
       * @return the transformed results
       */
      @SuppressWarnings("unchecked")
      protected Collection<I> unapplyAll(Collection<?> c) {
         @SuppressWarnings("rawtypes")
         Collection rawCollection = c;
         return new TransformingCollection<O, I>(rawCollection, function);
      }
      
      @Override
      public boolean add(O e) {
         return internal().add(unapply(e));
      }
      
      @Override
      public boolean remove(Object o) {
         return internal().remove(unapply(o));
      }
      
      @Override
      public boolean contains(Object o) {
         return internal().contains(unapply(o));
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return internal().containsAll(unapplyAll(c));
      }
      
      @Override
      public boolean removeAll(Collection<?> c) {
         return internal().removeAll(unapplyAll(c));
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return internal().retainAll(unapplyAll(c));
      }

      @Override
      public boolean addAll(Collection<? extends O> c) {
         return internal().addAll(unapplyAll(c));
      }
   }
}
