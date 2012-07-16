package com.apriori.collections;

import com.apriori.util.Function;

import java.util.Set;

// TODO: javadoc -- and explain that this assumes 1-to-1 mappings from the specified
// function or else invariants of interface Set will be seriously violated
public class TransformingSet<I, O> extends TransformingCollection<I, O> implements Set<O> {

   public TransformingSet(Set<I> set, Function<I, O> function) {
      super(set, function);
   }
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this,  o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
   
   public static class ReadOnly<I, O> extends TransformingCollection.ReadOnly<I, O> implements Set<O> {

      public ReadOnly(Set<I> collection, Function<I, O> function) {
         super(collection, function);
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

   public static class Bidi<I, O> extends TransformingCollection.Bidi<I, O> implements Set<O> {

      public Bidi(Set<I> collection, Function<I, O> function1, Function<O, I> function2) {
         super(collection, function1, function2);
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
