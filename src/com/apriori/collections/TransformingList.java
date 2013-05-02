package com.apriori.collections;

import com.apriori.util.Function;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

//TODO: javadoc
//TODO: tests
public class TransformingList<I, O> extends TransformingCollection<I, O> implements List<O> {

   public TransformingList(List<I> list, Function<I, O> function) {
      super(list, function);
   }

   @Override
   protected List<I> internal() {
      return (List<I>) super.internal();
   }
   
   @Override
   public boolean addAll(int index, Collection<? extends O> c) {
      throw new UnsupportedOperationException("addAll");
   }

   @Override
   public O get(int index) {
      return apply(internal().get(index));
   }

   @Override
   public O set(int index, O element) {
      throw new UnsupportedOperationException("set");
   }

   @Override
   public void add(int index, O element) {
      throw new UnsupportedOperationException("add");
   }

   @Override
   public O remove(int index) {
      return apply(internal().remove(index));
   }

   @Override
   public int indexOf(Object o) {
      int i = 0;
      for (O element : this) {
         if (o == null ? element == null : o.equals(element)) {
            return i;
         }
         i++;
      }
      return -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      int i = size() - 1;
      for (ListIterator<O> iterator = listIterator(size()); iterator.hasPrevious(); ) {
         O element = iterator.previous();
         if (o == null ? element == null : o.equals(element)) {
            return i;
         }
         i--;
      }
      return -1;
   }

   @Override
   public ListIterator<O> listIterator() {
      return new TransformingListIterator<I, O>(internal().listIterator(), function());
   }

   @Override
   public ListIterator<O> listIterator(int index) {
      return new TransformingListIterator<I, O>(internal().listIterator(index), function());
   }

   @Override
   public List<O> subList(int fromIndex, int toIndex) {
      return new TransformingList<I, O>(internal().subList(fromIndex, toIndex), function());
   }
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this,  o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
   
   public static class RandomAccess<I, O> extends TransformingList<I, O> {
      public <L extends List<I> & java.util.RandomAccess> RandomAccess(L list,
            Function<I, O> function) {
         super(list, function);
      }
   }

   public static class ReadOnly<I, O> extends TransformingList<I, O> {

      public ReadOnly(List<? extends I> list, Function<I, O> function) {
         super(cast(list), function);
      }
      
      @SuppressWarnings({ "unchecked", "rawtypes" })
      private static <T> List<T> cast(List<? extends T> input) {
         return (List) input;
      }
      
      @Override
      public Iterator<O> iterator() {
         return new TransformingIterator.ReadOnly<I, O>(internal().iterator(), function());
      }
      
      @Override
      public O remove(int index) {
         throw new UnsupportedOperationException("remove");
      }
      
      @Override
      public ListIterator<O> listIterator() {
         return new TransformingListIterator.ReadOnly<I, O>(internal().listIterator(), function());
      }

      @Override
      public ListIterator<O> listIterator(int index) {
         return new TransformingListIterator.ReadOnly<I, O>(internal().listIterator(index), function());
      }

      @Override
      public List<O> subList(int fromIndex, int toIndex) {
         return new TransformingList.ReadOnly<I, O>(internal().subList(fromIndex, toIndex), function());
      }
      
      public static class RandomAccess<I, O> extends TransformingList.ReadOnly<I, O> {
         public <L extends List<? extends I> & java.util.RandomAccess> RandomAccess(L list,
               Function<I, O> function) {
            super(list, function);
         }
      }
   }
   
   public static class Bidi<I, O> extends TransformingList<I, O> {

      private final Function<O, I> function;
      
      public Bidi(List<I> list, Function<I, O> function1, Function<O, I> function2) {
         super(list, function1);
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
      public boolean addAll(Collection<? extends O> c) {
         return internal().addAll(unapplyAll(c));
      }
      
      @Override
      public O set(int index, O element) {
         return apply(internal().set(index, unapply(element)));
      }

      @Override
      public void add(int index, O element) {
         internal().add(index, unapply(element));
      }
      
      // See TransformingCollection.Bidi.addAll() for explanation
      @SuppressWarnings("unchecked")
      @Override
      public boolean addAll(int index, Collection<? extends O> c) {
         @SuppressWarnings("rawtypes")
         Collection rawCollection = c;
         return internal().addAll(index, new TransformingCollection<O, I>(rawCollection, function));
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
      public ListIterator<O> listIterator() {
         return new TransformingListIterator.Bidi<I, O>(internal().listIterator(), function(), function);
      }

      @Override
      public ListIterator<O> listIterator(int index) {
         return new TransformingListIterator.Bidi<I, O>(internal().listIterator(index), function(), function);
      }
      
      @Override
      public List<O> subList(int fromIndex, int toIndex) {
         return new TransformingList.Bidi<I, O>(internal().subList(fromIndex, toIndex), function(), function);
      }
      
      public static class RandomAccess<I, O> extends TransformingList.Bidi<I, O> {
         public <L extends List<I> & java.util.RandomAccess> RandomAccess(L list,
               Function<I, O> function1, Function<O, I> function2) {
            super(list, function1, function2);
         }
      }
   }
}
