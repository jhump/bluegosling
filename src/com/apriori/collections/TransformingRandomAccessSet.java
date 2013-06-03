package com.apriori.collections;

import com.apriori.util.Function;

import java.util.List;
import java.util.ListIterator;

//TODO: javadoc (same 1-to-1 mappings required as for TransformingSet)
//TODO: tests
public class TransformingRandomAccessSet<I, O> extends TransformingSet<I, O>
      implements RandomAccessSet<O> {

   public TransformingRandomAccessSet(RandomAccessSet<I> set, Function<I, O> function) {
      super(set, function);
   }

   @Override
   protected RandomAccessSet<I> internal() {
      return (RandomAccessSet<I>) super.internal();
   }

   @Override
   public O get(int index) {
      return apply(internal().get(index));
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
   public ListIterator<O> listIterator() {
      return new TransformingListIterator<I, O>(internal().listIterator(), function());
   }

   @Override
   public ListIterator<O> listIterator(int index) {
      return new TransformingListIterator<I, O>(internal().listIterator(index), function());
   }

   @Override
   public O remove(int index) {
      return apply(internal().remove(index));
   }

   @Override
   public RandomAccessSet<O> subSetByIndices(int fromIndex, int toIndex) {
      return new TransformingRandomAccessSet<I, O>(internal().subSetByIndices(fromIndex, toIndex), function());
   }

   @Override
   public List<O> asList() {
      return new TransformingList<I, O>(internal().asList(), function());
   }
}
