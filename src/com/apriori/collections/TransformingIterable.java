package com.apriori.collections;

import com.apriori.util.Function;

import java.util.Iterator;

/**
 * An iterable whose elements are the results of applying a function to another iterable. This
 * iterable is simply a wrapper. Changes to the underlying iterable are visible in the
 * transformed iterable. Accessing elements incurs calls to the transforming function.
 * 
 * <p>Functions that perform the transformations should be deterministic so that a stable,
 * unchanging iterable does not appear to be mutating when accessed through this transforming
 * wrapper.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I> the "input" type; the type of the wrapped iterable
 * @param <O> the "output" type; the type of elements in this iterable
 */
//TODO: tests
public class TransformingIterable<I, O> implements Iterable<O> {

   private final Iterable<I> iterable;
   private final Function<? super I, ? extends O> function;
   
   /**
    * Constructs a new transforming iterable.
    * 
    * @param iterable the wrapped iterable
    * @param function the function used to transform elements
    */
   public TransformingIterable(Iterable<I> iterable, Function<? super I, ? extends O> function) {
      this.iterable = iterable;
      this.function = function;
   }
   
   /**
    * Gets the wrapped iterable.
    * 
    * @return the wrapped iterable
    */
   protected Iterable<I> internal() {
      return iterable;
   }
   
   /**
    * Gets the function used to transform elements.
    * 
    * @return the function used to transform elements
    */
   protected Function<? super I, ? extends O> function() {
      return function;
   }
   
   /**
    * Transforms an element. This is done by applying a {@link #function()}.
    * 
    * @param input the element to be transformed
    * @return the transformed result
    */
   protected O apply(I input) {
      return function().apply(input);
   }

   @Override
   public Iterator<O> iterator() {
      return new TransformingIterator<I, O>(internal().iterator(), function());
   }
   
   /**
    * An unmodifiable transforming iterable. Mutation operations ({@link Iterator#remove()}) throw
    * {@link UnsupportedOperationException}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped iterable
    * @param <O> the "output" type; the type of elements in this iterable
    */
   public static class ReadOnly<I, O> extends TransformingIterable<I, O> {

      /**
       * Constructs a new read-only transforming iterable.
       * 
       * @param iterable the wrapped iterable
       * @param function the function used to transform elements
       */
      public ReadOnly(Iterable<I> iterable, Function<? super I, ? extends O> function) {
         super(iterable, function);
      }
      
      @Override
      public Iterator<O> iterator() {
         return new TransformingIterator.ReadOnly<I, O>(internal().iterator(), function());
      }
   }
}
