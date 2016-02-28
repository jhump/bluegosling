package com.bluegosling.collections;

import java.util.ListIterator;
import java.util.function.Function;

/**
 * A list iterator whose elements are the results of applying a function to results from another
 * iterator. This iterator is simply a wrapper.
 *
 * <p>Since transformations can only be done in one direction, some operations are not supported.
 * Namely, {@link #add(Object)} and {@link #set(Object)} throw {@link UnsupportedOperationException}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I> the "input" type; the type of the wrapped iterator
 * @param <O> the "output" type; the type of elements in this iterator
 * 
 * @see TransformingListIterator.ReadOnly
 * @see TransformingListIterator.Bidi
 */
//TODO: tests
public class TransformingListIterator<I, O> extends TransformingIterator<I, O>
      implements ListIterator<O> {

   /**
    * Constructs a new transforming list iterator.
    * 
    * @param iterator the wrapped iterator
    * @param function the function used to transform elements
    */
   public TransformingListIterator(ListIterator<I> iterator,
         Function<? super I, ? extends O> function) {
      super(iterator, function);
   }
   
   /**
    * Gets the wrapped list iterator.
    * 
    * @return the wrapped iterator
    */
   @Override
   protected ListIterator<I> internal() {
      return (ListIterator<I>) super.internal();
   }

   @Override
   public boolean hasPrevious() {
      return internal().hasPrevious();
   }

   @Override
   public O previous() {
      return apply(internal().previous());
   }

   @Override
   public int nextIndex() {
      return internal().nextIndex();
   }

   @Override
   public int previousIndex() {
      return internal().previousIndex();
   }

   @Override
   public void set(O e) {
      throw new UnsupportedOperationException("set");
   }

   @Override
   public void add(O e) {
      throw new UnsupportedOperationException("add");
   }
   
   /**
    * An unmodifiable transforming list iterator. Mutation operations ({@link #remove()},
    * {@link #add(Object)}, and {@link #set(Object)}) throw {@link UnsupportedOperationException}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped iterator
    * @param <O> the "output" type; the type of elements in this iterator
    */
   public static class ReadOnly<I, O> extends TransformingListIterator<I, O> {

      /**
       * Constructs a new read-only transforming list iterator.
       * 
       * @param iterator the wrapped iterator
       * @param function the function used to transform elements
       */
      public ReadOnly(ListIterator<I> iterator, Function<? super I, ? extends O> function) {
         super(iterator, function);
      }
      
      @Override
      public void remove() {
         throw new UnsupportedOperationException("remove");
      }
   }
   
   /**
    * A transforming list iterator that can transform values in both directions. Since this iterator
    * can transform values in the other direction (output type -&gt; input type), all operations are
    * supported, including {@link #add(Object)} and {@link #set(Object)}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped iterator
    * @param <O> the "output" type; the type of elements in this iterator
    */
   public static class Bidi<I, O> extends TransformingListIterator<I, O> {
      
      private final Function<? super O, ? extends I> function;
      
      /**
       * Constructs a new bidirectional transforming list iterator. The two provided functions must
       * be inverses of one another. In other words, for any object {@code i} in the input domain,
       * {@code i} must <em>equal</em> {@code function2.apply(function1.apply(i))}. Similarly, for
       * any object {@code o} in the output domain, {@code o} must <em>equal</em>
       * {@code function1.apply(function2.apply(o))}.
       * 
       * @param iterator the wrapped iterator
       * @param function1 transforms elements; "input" -&gt; "output"
       * @param function2 the inverse of {@code function1}; "output" -&gt; "input"
       */
      public Bidi(ListIterator<I> iterator, Function<? super I, ? extends O> function1,
            Function<? super O, ? extends I> function2) {
         super(iterator, function1);
         this.function = function2;
      }
      
      /**
       * Transforms "output" elements into "input" elements. This is the opposite direction as
       * {@link #apply(Object)}.
       * 
       * @param input the object to transform
       * @return the transformed result
       */
      protected I unapply(O input) {
         return function.apply(input);
      }
      
      @Override
      public void set(O e) {
         internal().set(unapply(e));
      }

      @Override
      public void add(O e) {
         internal().add(unapply(e));
      }
   }
}
