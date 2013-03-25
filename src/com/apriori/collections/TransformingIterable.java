package com.apriori.collections;

import com.apriori.util.Function;

import java.util.Iterator;

//TODO: javadoc
//TODO: tests
public class TransformingIterable<I, O> implements Iterable<O> {

   private final Iterable<I> iterable;
   private final Function<I, O> function;
   
   public TransformingIterable(Iterable<I> iterable, Function<I, O> function) {
      this.iterable = iterable;
      this.function = function;
   }
   
   protected Iterable<I> internal() {
      return iterable;
   }
   
   protected Function<I, O> function() {
      return function;
   }
   
   protected O apply(I input) {
      return function().apply(input);
   }

   @Override
   public Iterator<O> iterator() {
      return new TransformingIterator<I, O>(internal().iterator(), function());
   }
   
   public static class ReadOnly<I, O> extends TransformingIterable<I, O> {

      public ReadOnly(Iterable<I> iterable, Function<I, O> function) {
         super(iterable, function);
      }
      
      @Override
      public Iterator<O> iterator() {
         return new TransformingIterator.ReadOnly<I, O>(internal().iterator(), function());
      }
   }
}
