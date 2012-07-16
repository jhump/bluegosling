package com.apriori.collections;

import com.apriori.util.Function;

import java.util.ListIterator;

//TODO: javadoc
public class TransformingListIterator<I, O> extends TransformingIterator<I, O> implements ListIterator<O> {

   public TransformingListIterator(ListIterator<I> iterator, Function<I, O> function) {
      super(iterator, function);
   }
   
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
   
   public static class ReadOnly<I, O> extends TransformingListIterator<I, O> {

      public ReadOnly(ListIterator<I> iterator, Function<I, O> function) {
         super(iterator, function);
      }
      
      @Override
      public void remove() {
         throw new UnsupportedOperationException("remove");
      }
   }
   
   public static class Bidi<I, O> extends TransformingListIterator<I, O> {
      
      private final Function<O, I> function;
      
      public Bidi(ListIterator<I> iterator, Function<I, O> function1, Function<O, I> function2) {
         super(iterator, function1);
         this.function = function2;
      }
      
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
