// Copyright (C) 2012 - Apriori Enterprises - All Rights Reserved
package com.apriori.collections;

import java.util.Comparator;

/**
 * Utility methods for working with arrays that back collection implementations.
 * This class includes functionality for manipulating "auto-resizing" arrays,
 * like the ones used to implement array-backed collections.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ArrayUtils {

   /** Prevents instantiation. */
   private ArrayUtils() {
   }
   
   /**
    * Binary searches a range in of a sorted array to find the specified item. If the item is not
    * found, an encoded negative value is returned that indicates where the item <em>would</em> be.
    * So if the return value is negative then the following expression indicates the index into the
    * array where the item would go:
    * <pre>
    * -findIndex(o, data, lo, hi, comp) - 1
    * </pre>
    * 
    * <p>The specified comparator cannot be {@code null}. If natural ordering was used to sort
    * the array, then pass {@link CollectionUtils#NATURAL_ORDERING}.
    *
    * @param o the item to find
    * @param data the sorted array to search
    * @param lo the minimum index that will be searched
    * @param hi the maximum index that will be searched
    * @param comp the comparator used to sort the items in the array
    * @return the index in the list where the specified item was found or, if not found, a negative
    *       value that can be decoded to determine the index where the item would go
    */
   public static int findIndex(Object o, Object data[], int lo, int hi, Comparator<Object> comp) {
      if (hi < lo) {
         return -1;
      }
      while (true) {
         int mid = (hi + lo) >> 1;
         int c = comp.compare(data[mid], o);
         if (c == 0) {
            return mid;
         }
         else if (c < 0) {
            if (hi == mid) {
               return -(hi + 2);
            }
            lo = mid + 1;
         }
         else {
            if (lo == mid) {
               return -(lo + 1);
            }
            hi = mid - 1;
         }
      }
   }

   /**
    * Insert an item into an array, shifting elements around if necessary. If
    * the array is not large enough to fit another element, a larger array will
    * be allocated and its contents will be populated. The array with the new
    * element will be returned, be that the original object or a new, large one.
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
}
