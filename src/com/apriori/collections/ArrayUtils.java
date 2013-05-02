// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.lang.reflect.Array;

/**
 * Utility methods for working with arrays that back collection implementations.
 * This class includes functionality for manipulating "auto-resizing" arrays,
 * like the ones used to implement array-backed collections.
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
   public static Object[] insertItem(Object element, int index, Object data[], int size) {
      int prevLen = data.length;
      if (prevLen > size) {
         // no need to grow the array
         if (index < size) {
            System.arraycopy(data, index, data, index + 1, size - index);
         }
         data[index] = element;
         return data;
      } else {
         int len = prevLen << 1;
         // arbitrary lower bound on size of new arrays
         if (len < MIN_GROWN_SIZE) {
            len = MIN_GROWN_SIZE;
         }
         // avoid overflow
         if (len <= prevLen) {
            len = Integer.MAX_VALUE - 8;
         }
         Object newData[] = new Object[len];
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
    * Ensure that the specified array has sufficient capacity and creates a new, larger array if
    * it is not.
    * 
    * @param array the array
    * @param capacity the required capacity
    * @return an array with the same component type as the one specified that is large enough to
    *       hold the specified capacity (may be same instance as specified array if it is already
    *       large enough)
    */
   @SuppressWarnings("unchecked")
   public static <T> T[] ensureCapacity(T[] array, int capacity) {
      if (array.length < capacity) {
         return (T[]) Array.newInstance(array.getClass().getComponentType(), capacity);
      }
      return array;
   }

}
