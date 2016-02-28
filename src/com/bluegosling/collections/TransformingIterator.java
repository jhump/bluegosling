package com.bluegosling.collections;

import java.util.Iterator;
import java.util.function.Function;

/**
 * An iterator whose elements are the results of applying a function to results from another
 * iterator. This iterator is simply a wrapper.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I> the "input" type; the type of the wrapped iterator
 * @param <O> the "output" type; the type of elements in this iterator
 */
//TODO: tests
public class TransformingIterator<I, O> implements Iterator<O> {

   private final Iterator<I> iterator;
   private final Function<? super I, ? extends O> function;
   
   /**
    * Constructs a new transforming iterator.
    * 
    * @param iterator the wrapped iterator
    * @param function the function used to transform elements
    */
   public TransformingIterator(Iterator<I> iterator, Function<? super I, ? extends O> function) {
      this.iterator = iterator;
      this.function = function;
   }
   
   /**
    * Gets the wrapped iterator.
    * 
    * @return the wrapped iterator
    */
   protected Iterator<I> internal() {
      return iterator;
   }
   
   /**
    * Transforms an element by applying a function.
    * 
    * @param input the element to be transformed
    * @return the transformed result
    */
   protected O apply(I input) {
      return function.apply(input);
   }
   
   @Override
   public boolean hasNext() {
      return internal().hasNext();
   }

   @Override
   public O next() {
      return apply(internal().next());
   }

   @Override
   public void remove() {
      internal().remove();
   }
   
   /**
    * An unmodifiable transforming iterator. Mutation operations ({@link #remove()}) throw
    * {@link UnsupportedOperationException}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped iterator
    * @param <O> the "output" type; the type of elements in this iterator
    */
   public static class ReadOnly<I, O> extends TransformingIterator<I, O> {

      /**
       * Constructs a new read-only transforming iterator.
       * 
       * @param iterator the wrapped iterator
       * @param function the function used to transform elements
       */
      public ReadOnly(Iterator<I> iterator, Function<? super I, ? extends O> function) {
         super(iterator, function);
      }
      
      @Override
      public void remove() {
         throw new UnsupportedOperationException("remove");
      }
   }
}
