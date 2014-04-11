package com.apriori.collections;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;


/**
 * A Spliterator designed for use by sources that traverse and split elements maintained in a
 * {@link GrowableArray}.
 */
class GrowableArraySpliterator<T> implements Spliterator<T> {
   
   /**
    * The source array.
    */
   private final GrowableArray<T> array;
    
   /**
    * Current index, modified on advance/split.
    */
   private int index;
    
   /**
    * One past last index.
    */
   private final int fence;
    
   private final int characteristics;

   /**
    * Creates a spliterator covering all of the given array.
    * 
    * @param array the array, assumed to be unmodified during use
    * @param additionalCharacteristics additional spliterator characteristics of this spliterator's
    *      source or elements beyond {@code SIZED} and {@code SUBSIZED} which are are always
    *      reported
    */
   GrowableArraySpliterator(GrowableArray<T> array, int additionalCharacteristics) {
      this(array, 0, array.size(), additionalCharacteristics);
   }

   /**
    * Creates a spliterator covering the given array and range.
    * 
    * @param array the array, assumed to be unmodified during use
    * @param origin the least index (inclusive) to cover
    * @param fence one past the greatest index to cover
    * @param additionalCharacteristics additional spliterator characteristics of this spliterator's
    *      source or elements beyond {@code SIZED} and {@code SUBSIZED} which are are always
    *      reported
    */
   public GrowableArraySpliterator(GrowableArray<T> array, int origin, int fence,
         int additionalCharacteristics) {
      this.array = array;
      this.index = origin;
      this.fence = fence;
      this.characteristics = additionalCharacteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
   }

   @Override
   public Spliterator<T> trySplit() {
      int lo = index;
      int mid = (lo + fence) >>> 1;
      return lo >= mid ? null
            : new GrowableArraySpliterator<>(array, lo, index = mid, characteristics);
   }
   
   @Override
   public void forEachRemaining(Consumer<? super T> action) {
      if (action == null) {
         throw new NullPointerException();
      }
      int i = index, limit = fence;
      assert i >= 0;
      assert array.size() >= limit;
      index = fence;
      while (i < limit) {
         action.accept(array.get(i++));
      }
   }

   @Override
   public boolean tryAdvance(Consumer<? super T> action) {
      if (action == null) {
         throw new NullPointerException();
      }
      if (index >= 0 && index < fence) {
         action.accept(array.get(index++));
         return true;
      }
      return false;
   }

   @Override
   public long estimateSize() {
      return fence - index;
   }

   @Override
   public int characteristics() {
      return characteristics;
   }

   @Override
   public Comparator<? super T> getComparator() {
      if (hasCharacteristics(Spliterator.SORTED)) return null;
      throw new IllegalStateException();
   }
}
