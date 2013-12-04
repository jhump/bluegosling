package com.apriori.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * Sorts arrays and collections using the <a href="http://en.wikipedia.org/wiki/Smoothsort">
 * Smoothsort</a> algorithm. This algorithm is very similar to heap sort, but it uses a Leonardo
 * heap instead of a binary heap. The way the Leonardo heap is represented in-place in the array
 * or list enables sorting operations to take advantage of partially sorted data (data where there
 * are runs of sorted elements). This gives the algorithm a linear best case.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: implement me!
// TODO: javadoc
public final class SmoothSort {

   /** Prevents instantiation. */
   private SmoothSort() {
   }
   
   private static final int LEONARDO_NUMBERS_LEN;
   private static final int[] LEONARDO_NUMBERS;
   
   static {
      int lp2 = 1;
      int lp1 = 1;
      ArrayList<Integer> lnums = new ArrayList<Integer>();
      lnums.add(lp2); lnums.add(lp1);
      while (true) {
         long lnext = lp2 + lp1 + 1;
         if (lnext > Integer.MAX_VALUE) {
            // done
            break;
         }
         int lp0 = (int) lnext;
         lnums.add(lp0);
         lp2 = lp1;
         lp1 = lp0;
      }
      
      LEONARDO_NUMBERS_LEN = lnums.size();
      LEONARDO_NUMBERS = new int[LEONARDO_NUMBERS_LEN];
      for (int i = 0, j = 1; i < LEONARDO_NUMBERS_LEN; i++, j <<= 1) {
         LEONARDO_NUMBERS[i] = lnums.get(i);
      }
   }
   
   /**
    * Describes the structure of the in-place heap in the list. Since a leonardo heap is a
    * set of trees, this structure tracks what order trees are in the heap and where.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class HeapStructure {
      /**
       * Bitmask of which order trees are present in the heap.
       */
      long trees;
      
      /**
       * The order of the least-significant bit in trees mask.
       */
      int offset;
      
      /**
       * Provides visibility w/out need for synthetic accessor.
       */
      HeapStructure() {
      }
   }
   
   private static <T> void siftDown(List<T> list, Comparator<? super T> comp, int root,
         int treeOrder) {
      // TODO
   }
   
   private static <T> void rectifyRoots(List<T> list, Comparator<? super T> comp, int start,
         int end, HeapStructure structure) {
      // TODO
   }
   
   private static <T> void addToHeap(List<T> list, Comparator<? super T> comp, int start, int end,
         int listLength, HeapStructure structure) {
      if ((structure.trees & 1) != 0) {
         // Due to we encode this bitset, if first bit is zero, heap is empty.
         // So we add a leonardo tree of order 1 (size = 1)
         structure.trees |= 1;
         structure.offset = 1;
      } else if ((structure.trees & 3) == 3) {
         // If last two trees in the heap are sequential, we can trivially merge them and
         // and new item is the root of merged tree.
         structure.trees >>= 2; // shift away lowest two orders
         structure.offset += 2;
         structure.trees |= 1; // and replace with next highest order
      } else if (structure.offset == 1) {
         // Otherwise, if smallest tree is order 1 (structure offset is effectively the order of the
         // smallest tree), then we add a tree of order 0.
         structure.trees <<= 1; // make room for new order of tree
         structure.trees |= 1;
         structure.offset = 0;
      } else {
         // Final case: add new tree of order 1
         structure.trees <<= structure.offset - 1; // make room to push in bit at order 1
         structure.trees |= 1;
         structure.offset = 1;
      }
      // If we know this tree is at its final size, then we can rectify the the tree root with the
      // roots of the prior trees in the heap. Otherwise, we just do a sift-down.
      switch (structure.offset) {
         case 0:
            // if the last heap has order 0, we only rectify if it's the very last element in list
            if (end == listLength - 1) {
               rectifyRoots(list, comp, start, end, structure);
               return;
            }
            break;
            
         case 1:
            // if the last heap has order 1, we can rectify if it's the last element in the list or
            // if it's the next-to-last element in list and won't be merged when we add last element
            if (end == listLength - 1 || (end == listLength - 2 && (structure.trees & 2) == 0)) {
               rectifyRoots(list, comp, start, end, structure);
               return;
            }
            break;
            
         default:
            // otherwise, if there is insufficient room in the list for this tree to be merged
            // into next order tree, then we can rectify
            int elementsRemaining = listLength - 1 - end;
            int elementsNeededForNextOrderTree = LEONARDO_NUMBERS[structure.offset - 1] + 1;
            if (elementsRemaining < elementsNeededForNextOrderTree) {
               rectifyRoots(list, comp, start, end, structure);
               return;
            }
            break;
      }
      
      siftDown(list, comp, end, structure.offset);
   }
   
   private static <T> HeapStructure heapify(List<T> list, Comparator<? super T> comp) {
      HeapStructure structure = new HeapStructure();
      for (int i = 0, len = list.size(); i < len; i++) {
         addToHeap(list, comp, 0, i, len, structure);
      }
      return structure;
   }
   
   private static <T> void rebalance(List<T> list, Comparator<? super T> comp,
         HeapStructure structure, int length) {
      // TODO
   }
   
   private static <T> void sortInPlace(List<T> list, Comparator<? super T> comp) {
      HeapStructure structure = heapify(list, comp);
      for (int i = list.size() - 1; i > 0; i--) {
         rebalance(list, comp, structure, i);
      }
   }
   
   public static <T> void sort(List<T> list) {
      sort(list, CollectionUtils.NATURAL_ORDERING);
   }
   
   public static <T> void sort(List<T> list, Comparator<? super T> comp) {
      if (list instanceof RandomAccess) {
         sortInPlace(list, comp);
      } else {
         ArrayList<T> l = new ArrayList<T>(list);
         sortInPlace(list, comp);
         for (ListIterator<T> srcIter = l.listIterator(), destIter = list.listIterator();
               srcIter.hasNext();) {
            destIter.next();
            destIter.set(srcIter.next());
         }
      }
   }
   
   public static <T> void sort(T[] array) {
      sort(Arrays.asList(array));
   }

   public static <T> void sort(T[] array, Comparator<? super T> comp) {
      sort(Arrays.asList(array), comp);
   }

   public static void sort(int[] array) {
      // TODO
   }

   public static void sort(int[] array, Comparator<? super Integer> comp) {
      // TODO
   }

   public static void sort(byte[] array) {
      // TODO
   }
   
   public static void sort(byte[] array, Comparator<? super Byte> comp) {
      // TODO
   }
   
   public static void sort(short[] array) {
      // TODO
   }
   
   public static void sort(short[] array, Comparator<? super Short> comp) {
      // TODO
   }
   
   public static void sort(char[] array) {
      // TODO
   }
   
   public static void sort(char[] array, Comparator<? super Character> comp) {
      // TODO
   }
   
   public static void sort(long[] array) {
      // TODO
   }
   
   public static void sort(long[] array, Comparator<? super Long> comp) {
      // TODO
   }
   
   public static void sort(float[] array) {
      // TODO
   }
   
   public static void sort(float[] array, Comparator<? super Float> comp) {
      // TODO
   }
   
   public static void sort(double[] array) {
      // TODO
   }

   public static void sort(double[] array, Comparator<? super Double> comp) {
      // TODO
   }
}
