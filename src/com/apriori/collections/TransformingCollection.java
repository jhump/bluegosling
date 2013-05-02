package com.apriori.collections;

import com.apriori.util.Function;

import java.util.Collection;
import java.util.Iterator;

//TODO: javadoc
//TODO: tests
public class TransformingCollection<I, O> extends TransformingIterable<I, O> implements Collection<O> {

   public TransformingCollection(Collection<I> collection, Function<I, O> function) {
      super(collection, function);
   }
   
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

   public static class ReadOnly<I, O> extends TransformingCollection<I, O> {

      public ReadOnly(Collection<? extends I> collection, Function<I, O> function) {
         super(cast(collection), function);
      }
      
      @SuppressWarnings({ "unchecked", "rawtypes" })
      private static <T> Collection<T> cast(Collection<? extends T> input) {
         return (Collection) input;
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
   
   public static class Bidi<I, O> extends TransformingCollection<I, O> {

      private final Function<O, I> function;
      
      public Bidi(Collection<I> collection, Function<I, O> function1, Function<O, I> function2) {
         super(collection, function1);
         this.function = function2;
      }

      @SuppressWarnings("unchecked")
      protected I unapply(Object input) {
         return function.apply((O) input);
      }
      
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
