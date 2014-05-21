package com.apriori.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * Sorts arrays and collections using the <a href="http://en.wikipedia.org/wiki/Smoothsort">
 * Smoothsort</a> algorithm. This algorithm is very similar to heap sort, but it uses a Leonardo
 * heap instead of a binary heap. The way the Leonardo heap is represented in-place in the array
 * (or list) enables sorting operations to take advantage of partially sorted data (data where there
 * are runs of sorted elements). This gives the algorithm a linear best case.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
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
         long lnext = (long) lp2 + lp1 + 1;
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
      for (int i = 0; i < LEONARDO_NUMBERS_LEN; i++) {
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
      
      HeapStructure() {
      }
   }
   
   /**
    * Restores the max-heap property to a given leonardo tree in the array by sifting elements down
    * if necessary.
    *
    * @param array the array
    * @param comp the comparator for comparing two elements
    * @param rootPos the root of the tree that is being repaired
    * @param treeOrder the order of the tree that is being repaired
    */
   private static <T> void siftDown(T[] array, Comparator<? super T> comp, int rootPos, T root,
         int treeOrder) {
      // sift downward until we hit a sub-tree with no children (e.g. order 0 or 1)
      while (treeOrder > 1) {
         int rightChildPos = rootPos - 1;
         T rightChild = array[rightChildPos];
         // right child's order is root's order - 2 (left child is root's order - 1)
         int leftChildPos = rootPos - 1 - LEONARDO_NUMBERS[treeOrder - 2];
         T leftChild = array[leftChildPos];
         T child;
         int childPos, childOrder;
         if (comp.compare(leftChild,  rightChild) > 0) {
            child = leftChild;
            childPos = leftChildPos;
            childOrder = treeOrder - 1;
         } else {
            child = rightChild;
            childPos = rightChildPos;
            childOrder = treeOrder - 2;
         }
         if (comp.compare(root, child) >= 0) {
            // root is not smaller, no need to sift down any further
            return;
         }
         // sift root down to child and then examine that sub-tree
         array[childPos] = root;
         array[rootPos] = child;
         rootPos = childPos;
         treeOrder = childOrder;
      }
   }
   
   /**
    * Rectifies the order of tree roots. This ensures that the smallest/right-most tree has the
    * largest value at its root. Its predecessor (tree to its left) has a root that is less than or
    * equal to it, and so.
    *
    * @param array the array
    * @param comp the comparator for comparing two elements
    * @param last the last list index in the heap
    * @param structureTrees a bitmask that represents the orders of the trees in the heap
    * @param structureOffset the order of the smallest tree, corresponding to the least significant
    *       bit in {@code structureTrees}
    */
   private static <T> void rectifyRoots(T[] array, Comparator<? super T> comp,
         int last, long structureTrees, int structureOffset) {
      int rootPos = last;
      T currentRoot = array[rootPos];
      while (rootPos >= LEONARDO_NUMBERS[structureOffset]) {
         T largest;
         int largestPos;
         int largestOrder;
         if (structureOffset > 1) {
            // this root is not order 0 or 1, so it has two children - make sure we're
            // comparing against the largest out of the root or its two immediate children
            int rightPos = rootPos - 1;
            T rightChild = array[rightPos];
            // right child's order is root's order - 2 (left child is root's order - 1)
            int leftPos = rightPos - LEONARDO_NUMBERS[structureOffset - 2]; 
            T leftChild = array[leftPos];
            if (comp.compare(leftChild,  rightChild) > 0) {
               if (comp.compare(leftChild, currentRoot) > 0) {
                  largest = leftChild;
                  largestPos = leftPos;
                  largestOrder = structureOffset - 1;
               } else {                  
                  largest = currentRoot;
                  largestPos = rootPos;
                  largestOrder = structureOffset;
               }
            } else if (comp.compare(rightChild, currentRoot) > 0) {
               largest = rightChild;
               largestPos = rightPos;
               largestOrder = structureOffset - 2;
            } else {
               largest = currentRoot;
               largestPos = rootPos;
               largestOrder = structureOffset;
            }
         } else {
            largest = currentRoot;
            largestPos = rootPos;
            largestOrder = structureOffset;
         }
         
         int previousRootPos = rootPos - LEONARDO_NUMBERS[structureOffset];
         T previousRoot = array[previousRootPos];
         if (comp.compare(largest, previousRoot) >= 0) {
            // previous root is not bigger, so we're done
            if (largest != currentRoot) {
               // if the root is not right, swap and then sift-down
               array[rootPos] = largest;
               array[largestPos] = currentRoot;
               siftDown(array, comp, largestPos, currentRoot, largestOrder);
            }
            return;
         }
         // swap roots
         array[rootPos] = previousRoot;
         array[previousRootPos] = currentRoot;
         
         // find previous tree
         rootPos = previousRootPos;
         structureTrees &= ~1;
         int shift = Long.numberOfTrailingZeros(structureTrees);
         structureTrees >>= shift;
         structureOffset += shift;
      }

      // shuffle the root down if necessary to maintain heap invariant for this tree
      siftDown(array, comp, rootPos, currentRoot, structureOffset);
   }
   
   /**
    * Adds an element to the heap structure. We heapify the array by starting with the left-most
    * element (a heap with just one item) and expanding towards the right, rectifying the heap
    * structure as we go.
    *
    * @param array the array
    * @param comp the comparator for comparing two elements
    * @param last the last array index in the heap which contains the newly added element
    * @param structure the structure of the heap, describing the order of its constituent trees
    */
   private static <T> void addToHeap(T[] array, Comparator<? super T> comp, int last,
         HeapStructure structure) {
      if ((structure.trees & 1) == 0) {
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
      int len = array.length;
      switch (structure.offset) {
         case 0:
            // if the last heap has order 0, we only rectify if it's the very last element in list
            if (last == len - 1) {
               rectifyRoots(array, comp, last, structure.trees, structure.offset);
            }
            return;
            
         case 1:
            // if the last heap has order 1, we can rectify if it's the last element in the list or
            // if it's the next-to-last element in list and won't be merged when we add last element
            if (last == len - 1 || (last == len - 2 && (structure.trees & 2) == 0)) {
               rectifyRoots(array, comp, last, structure.trees, structure.offset);
            }
            return;
            
         default:
            // otherwise, if there is insufficient room in the list for this tree to be merged
            // into next order tree, then we can rectify
            int elementsRemaining = len - 1 - last;
            int elementsNeededForNextOrderTree = LEONARDO_NUMBERS[structure.offset - 1] + 1;
            if (elementsRemaining < elementsNeededForNextOrderTree) {
               rectifyRoots(array, comp, last, structure.trees, structure.offset);
            } else {
               siftDown(array, comp, last, array[last], structure.offset);
            }
            return;
      }
   }
   
   /**
    * Arranges elements in the array so that they represent a leonardo heap, consisting of one or
    * more leonardo trees.
    *
    * @param array the array
    * @param comp the comparator for comparing two elements
    * @return an object that describes the structure of the leonardo heap -- the order of the
    *       various constituent trees
    */
   private static <T> HeapStructure heapify(T[] array, Comparator<? super T> comp) {
      HeapStructure structure = new HeapStructure();
      for (int i = 0, len = array.length; i < len; i++) {
         addToHeap(array, comp, i, structure);
      }
      return structure;
   }
   
   /**
    * Removes the largest element, the root of the smallest tree, from the heap structure. Once the
    * array is heapified, sorting involves contracting the heap, removing the largest elements as we
    * go. Each remove operation involves rectifying the heap structure to maintain heap invariants.
    *
    * @param array the array
    * @param comp the comparator for comparing two elements
    * @param last the last array index in the heap which contains the newly added element
    * @param structure the structure of the heap, describing the order of its constituent trees
    */
   private static <T> void removeSmallestRoot(T[] array, Comparator<? super T> comp, int last,
         HeapStructure structure) {
      // smallest heap is order 0 or 1, can just drop it
      if (structure.offset <= 1) {
         structure.trees &= ~1;
         if (structure.trees != 0) {
            int shift = Long.numberOfTrailingZeros(structure.trees);
            structure.trees >>= shift;
            structure.offset += shift;
         }
         return;
      }
      
      // break the smallest heap into two smaller order heaps by mapping
      // { tree1, offset } -> { tree011, offset - 2 }
      structure.trees &= ~1;
      structure.trees <<= 2;
      structure.trees |= 3;
      structure.offset -= 2;
      // grab the two immediate children of smallest heap, which are the
      // roots of its two smaller order heaps.
      // last element is the root of the smallest tree; next one over is its right child
      int rightRoot = last - 1;
      // right child's order is root's order - 2 (left child is root's order - 1)
      // we've already subtracted 2 from root's order
      int leftRoot = rightRoot - LEONARDO_NUMBERS[structure.offset];
      // repair roots up to the left child
      rectifyRoots(array, comp, leftRoot, structure.trees >> 1, structure.offset + 1);
      // and then including the right child
      rectifyRoots(array, comp, rightRoot, structure.trees, structure.offset);
   }
   
   /**
    * Sorts the given list according to the elements' {@linkplain Comparable natural ordering}.
    * This is equivalent to using {@link Collections#sort(List)}, except it will utilize the
    * smoothsort algorithm instead of Java's default implementation (mergesort in versions 6 and
    * earlier, timsort in versions 7 and up).
    *
    * @param list the list which will be sorted in place
    */
   public static <T extends Comparable<? super T>> void sort(List<T> list) {
      sort(list, Comparator.naturalOrder());
   }
   
   /**
    * Sorts the given list according to the specified comparator. This is equivalent to using
    * {@link Collections#sort(List, Comparator)}, except it will utilize the smoothsort algorithm
    * instead of Java's default implementation (mergesort in versions 6 and earlier, timsort in
    * versions 7 and up).
    *
    * @param list the list which will be sorted in place
    */
   @SuppressWarnings("unchecked")
   public static <T> void sort(List<T> list, Comparator<? super T> comp) {
      // NB: We could do this without copying the list, if it implements RandomAccess, and get
      // lower memory overhead. However, this has empirically been observed to perform worse.
      // Maybe because of the range checking done on every list access?
      Object[] array = list.toArray();
      sort(array, (Comparator<Object>) comp);
      ListIterator<T> iter = list.listIterator();
      for (int i = 0, len = array.length; i < len; i++) {
         iter.next();
         iter.set((T) array[i]);
      }
   }
   
   /**
    * Sorts the given array according to the elements' {@linkplain Comparable natural ordering}.
    * This is equivalent to using {@link Arrays#sort(Object[])}, except it will utilize the
    * smoothsort algorithm instead of Java's default implementation (mergesort in versions 6 and
    * earlier, timsort in versions 7 and up).
    *
    * @param array the array which will be sorted in place
    */
   public static <T extends Comparable<? super T>> void sort(T[] array) {
      sort(array, Comparator.naturalOrder());
   }

   /**
    * Sorts the given array according to the specified comparator. This is equivalent to using
    * {@link Arrays#sort(Object[], Comparator)}, except it will utilize the smoothsort algorithm
    * instead of Java's default implementation (mergesort in versions 6 and earlier, timsort in
    * versions 7 and up).
    *
    * @param array the array which will be sorted in place
    */
   public static <T> void sort(T[] array, Comparator<? super T> comp) {
      HeapStructure structure = heapify(array, comp);
      for (int i = array.length - 1; i > 0; i--) {
         removeSmallestRoot(array, comp, i, structure);
      }
   }
}
