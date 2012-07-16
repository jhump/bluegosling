package com.apriori.collections;

import com.apriori.util.Function;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

//TODO: javadoc
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
      for (O element : this) {
         if (o == null ? element == null : o.equals(element)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public Object[] toArray() {
      Object ret[] = new Object[size()];
      int i = 0;
      for (O o : this) {
         ret[i++] = o;
      }
      return ret;
   }

   @SuppressWarnings("unchecked")
   @Override
   public <T> T[] toArray(T[] a) {
      if (a.length < size()) {
         a = (T[]) Array.newInstance(a.getClass().getComponentType(), size());
      }
      int i = 0;
      for (O o : this) {
         a[i++] = (T) o;
      }
      return a;
   }

   @Override
   public boolean add(O e) {
      throw new UnsupportedOperationException("add");
   }

   @Override
   public boolean remove(Object o) {
      for (Iterator<O> iter = this.iterator(); iter.hasNext(); ) {
         O element = iter.next();
         if (o == null ? element == null : o.equals(element)) {
            iter.remove();
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      for (Object o : c) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public boolean addAll(Collection<? extends O> c) {
      throw new UnsupportedOperationException("addAll");
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      boolean changed = false;
      for (Iterator<O> iter = this.iterator(); iter.hasNext(); ) {
         O o = iter.next();
         if (c.contains(o)) {
            iter.remove();
            changed = true;
         }
      }
      return changed;
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      boolean changed = false;
      for (Iterator<O> iter = this.iterator(); iter.hasNext(); ) {
         O o = iter.next();
         if (!c.contains(o)) {
            iter.remove();
            changed = true;
         }
      }
      return changed;
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

      public ReadOnly(Collection<I> collection, Function<I, O> function) {
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
   
   public static class Bidi<I, O> extends TransformingCollection<I, O> {

      private final Function<O, I> function;
      
      public Bidi(Collection<I> collection, Function<I, O> function1, Function<O, I> function2) {
         super(collection, function1);
         this.function = function2;
      }
      
      protected I unapply(O input) {
         return function.apply(input);
      }
      
      @Override
      public boolean add(O e) {
         return internal().add(unapply(e));
      }
      
      // TODO: override remove(), contains(), removeAll(), retainAll(), containsAll() to use unapply

      // Cast should be safe assuming underlying collection's implementation of addAll() doesn't
      // try to add elements to the specified collection. Which seems like a safe assumption since
      // such behavior would strongly violate the contract of Collection.addAll()!! If something
      // like that does happen, a ClassCastException could be thrown.
      @SuppressWarnings("unchecked")
      @Override
      public boolean addAll(Collection<? extends O> c) {
         @SuppressWarnings("rawtypes")
         Collection rawCollection = c;
         return internal().addAll(new TransformingCollection<O, I>(rawCollection, function));
      }
   }
}
