// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.bluegosling.collections;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Utility methods for working with arrays that back collection implementations. This class also
 * includes functionality for manipulating "auto-resizing" arrays, like the ones used to implement
 * array-backed collections.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
final class ArrayUtils {
   
   private static final int MIN_GROWN_SIZE = 8;

   /** Prevents instantiation. */
   private ArrayUtils() {
   }

   /**
    * Insert an item into an array, shifting elements around if necessary. If
    * the array is not large enough to fit another element, a larger array will
    * be allocated and its contents will be populated. The array with the new
    * element will be returned, be that the original object or a new, larger one.
    *
    * @param element the element to insert into the array
    * @param index the index at which the specified item will be inserted
    * @param data the array
    * @param size the actual number of items in the array
    * @return an array with the specified element inserted at the specified index
    */
   public static <T> T[] insertItem(T element, int index, T data[], int size) {
      int prevLen = data.length;
      if (prevLen > size) {
         // no need to grow the array
         if (index < size) {
            System.arraycopy(data, index, data, index + 1, size - index);
         }
         data[index] = element;
         return data;
      } else {
         int len = prevLen + (prevLen << 1);
         len = Math.max(MIN_GROWN_SIZE, Math.max(len, size + 1));
         @SuppressWarnings("unchecked")
         T newData[] = (T[]) Array.newInstance(data.getClass().getComponentType(), len);
         // copy items prior to index
         if (index > 0) {
            System.arraycopy(data, 0, newData, 0, index);
         }
         // new item at index
         newData[index] = element;
         // and items after the index
         if (index < size) {
            System.arraycopy(data, index, newData, index + 1, prevLen - index);
         }
         return newData;
      }
   }

   /**
    * Removes the an item from the specified array, shifting elements around
    * if necessary.
    *
    * @param index the index in the array of the item to remove
    * @param data the array
    * @param size the actual number of items in the array
    */
   public static void removeIndex(int index, Object data[], int size) {
      if (index < size - 1) {
         System.arraycopy(data, index + 1, data, index, size - 1 - index);
      }
      data[size - 1] = null; // clear last reference
   }

   /**
    * Reverses the specified array in place.
    * 
    * @param a the array to be reversed.
    */
   public static void reverse(Object[] a) {
      for (int i = 0, j = a.length - 1; i < j; i++, j--) {
         Object tmp = a[j];
         a[j] = a[i];
         a[i] = tmp;
      }
   }

   /**
    * Returns the specified array if it has sufficient capacity or a new larger array otherwise.
    * 
    * @param array the array
    * @param requiredCapacity the required capacity
    * @return an array with the same component type as the one specified that is large enough to
    *       hold the specified capacity (may be same instance as specified array if it is already
    *       large enough)
    */
   @SuppressWarnings("unchecked")
   public static <T> T[] newArrayIfTooSmall(T[] array, int requiredCapacity) {
      if (array.length < requiredCapacity) {
         return (T[]) Array.newInstance(array.getClass().getComponentType(), requiredCapacity);
      }
      return array;
   }
   
   /**
    * Grows the array if necessary to accommodate additional items. This tries to grow the array by
    * 50%, so that repeated calls will have an amortized runtime complexity of O(1). It will grow
    * the array by more than 50% if necessary to accommodate the indicated extra capacity.
    * 
    * @param array the array
    * @param actualSize the used size of the array, which may be less than its actual length
    * @param extraCapacityNeeded the number of additional items for which room in the array is
    *       needed, above and beyond its used size
    * @return the specified array if it already has sufficient capacity or a new, larger array
    *       that is initialized with the same elements (from index 0 to index {@code actualSize - 1})
    */
   public static <T> T[] maybeGrowBy(T[] array, int actualSize, int extraCapacityNeeded) {
      int totalCapacity = array.length;
      if (actualSize < 0) {
         throw new IllegalArgumentException("actualSize, " + actualSize + ", cannot be negative");
      }
      if (actualSize > totalCapacity) {
         throw new IllegalArgumentException("actualSize, " + actualSize + ", cannot be greater" +
         		" than current array length " + totalCapacity);
      }
      if (extraCapacityNeeded < 0) {
         throw new IllegalArgumentException("extraCapacityNeeded, " + extraCapacityNeeded
               + ", cannot be negative");
      }
      int requiredCapacity = actualSize + extraCapacityNeeded;
      if (totalCapacity >= requiredCapacity) {
         return array;
      }
      // grow by 50%
      totalCapacity += totalCapacity >> 1;
      // if 50% not enough, grow by more
      totalCapacity = Math.max(MIN_GROWN_SIZE, Math.max(totalCapacity, requiredCapacity));

      return Arrays.copyOf(array, totalCapacity);
   }
}
