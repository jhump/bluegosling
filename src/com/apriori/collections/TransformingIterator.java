package com.apriori.collections;

import com.apriori.util.Function;

import java.util.Iterator;

// TODO: javadoc
public class TransformingIterator<I, O> implements Iterator<O> {

   private final Iterator<I> iterator;
   private final Function<I, O> function;
   
   public TransformingIterator(Iterator<I> iterator, Function<I, O> function) {
      this.iterator = iterator;
      this.function = function;
   }
   
   protected Iterator<I> internal() {
      return iterator;
   }
   
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
   
   public static class ReadOnly<I, O> extends TransformingIterator<I, O> {

      public ReadOnly(Iterator<I> iterator, Function<I, O> function) {
         super(iterator, function);
      }
      
      @Override
      public void remove() {
         throw new UnsupportedOperationException("remove");
      }
   }
}
