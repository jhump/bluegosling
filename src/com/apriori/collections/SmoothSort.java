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
// TODO: implement sorting of arrays -- will necessitate ugly copy+paste to handle primitives :(
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
    * Restores the max-heap property to a given leonardo tree in the list by sifting elements down
    * if necessary.
    *
    * @param list the list
    * @param comp the comparator for comparing two elements
    * @param rootPos the root of the tree that is being repaired
    * @param treeOrder the order of the tree that is being repaired
    */
   private static <T> void siftDown(List<T> list, Comparator<? super T> comp, int rootPos, T root,
         int treeOrder) {
      // sift downward until we hit a sub-tree with no children (e.g. order 0 or 1)
      while (treeOrder > 1) {
         int rightChildPos = rootPos - 1;
         T rightChild = list.get(rightChildPos);
         // right child's order is root's order - 2 (left child is root's order - 1)
         int leftChildPos = rootPos - 1 - LEONARDO_NUMBERS[treeOrder - 2];
         T leftChild = list.get(leftChildPos);
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
         list.set(childPos, root);
         list.set(rootPos, child);
         rootPos = childPos;
         treeOrder = childOrder;
      }
   }
   
   /**
    * Rectifies the order of tree roots. This ensures that the smallest/right-most tree has the
    * largest value at its root. Its predecessor (tree to its left) has a root that is less than or
    * equal to it, and so.
    *
    * @param list the list
    * @param comp the comparator for comparing two elements
    * @param last the last list index in the heap
    * @param structureTrees a bitmask that represents the orders of the trees in the heap
    * @param structureOffset the order of the smallest tree, corresponding to the least significant
    *       bit in {@code structureTrees}
    */
   private static <T> void rectifyRoots(List<T> list, Comparator<? super T> comp,
         int last, long structureTrees, int structureOffset) {
      int rootPos = last;
      T currentRoot = list.get(rootPos);
      while (rootPos >= LEONARDO_NUMBERS[structureOffset]) {
         T largest;
         int largestPos;
         int largestOrder;
         if (structureOffset > 1) {
            // this root is not order 0 or 1, so it has two children - make sure we're
            // comparing against the largest out of the root or its two immediate children
            int rightPos = rootPos - 1;
            T rightChild = list.get(rightPos);
            // right child's order is root's order - 2 (left child is root's order - 1)
            int leftPos = rightPos - LEONARDO_NUMBERS[structureOffset - 2]; 
            T leftChild = list.get(leftPos);
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
         T previousRoot = list.get(previousRootPos);
         if (comp.compare(largest, previousRoot) >= 0) {
            // previous root is not bigger, so we're done
            if (largest != currentRoot) {
               // if the root is not right, swap and then sift-down
               list.set(rootPos, largest);
               list.set(largestPos, currentRoot);
               siftDown(list, comp, largestPos, currentRoot, largestOrder);
            }
            return;
         }
         // swap roots
         list.set(rootPos, previousRoot);
         list.set(previousRootPos, currentRoot);
         
         // find previous tree
         rootPos = previousRootPos;
         structureTrees &= ~1;
         int shift = Long.numberOfTrailingZeros(structureTrees);
         structureTrees >>= shift;
         structureOffset += shift;
      }

      // shuffle the root down if necessary to maintain heap invariant for this tree
      siftDown(list, comp, rootPos, currentRoot, structureOffset);
   }
   
   /**
    * Adds an element to the heap structure. We heapify the list by starting with the left-most
    * element (a heap with just one item) and expanding towards the right, rectifying the heap
    * structure as we go.
    *
    * @param list the list
    * @param comp the comparator for comparing two elements
    * @param last the last list index in the heap which contains the newly added element
    * @param structure the structure of the heap, describing the order of its constituent trees
    */
   private static <T> void addToHeap(List<T> list, Comparator<? super T> comp, int last,
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
      int len = list.size();
      switch (structure.offset) {
         case 0:
            // if the last heap has order 0, we only rectify if it's the very last element in list
            if (last == len - 1) {
               rectifyRoots(list, comp, last, structure.trees, structure.offset);
            }
            return;
            
         case 1:
            // if the last heap has order 1, we can rectify if it's the last element in the list or
            // if it's the next-to-last element in list and won't be merged when we add last element
            if (last == len - 1 || (last == len - 2 && (structure.trees & 2) == 0)) {
               rectifyRoots(list, comp, last, structure.trees, structure.offset);
            }
            return;
            
         default:
            // otherwise, if there is insufficient room in the list for this tree to be merged
            // into next order tree, then we can rectify
            int elementsRemaining = len - 1 - last;
            int elementsNeededForNextOrderTree = LEONARDO_NUMBERS[structure.offset - 1] + 1;
            if (elementsRemaining < elementsNeededForNextOrderTree) {
               rectifyRoots(list, comp, last, structure.trees, structure.offset);
            } else {
               siftDown(list, comp, last, list.get(last), structure.offset);
            }
            return;
      }
   }
   
   /**
    * Arranges elements in the list so that they represent a leonardo heap, consisting of multiple
    * leonardo trees, each having max-heap property.
    *
    * @param list the list
    * @param comp the comparator for comparing two elements
    * @return an object that describes the structure of the leonardo heap -- the order of the
    *       various constituent trees
    */
   private static <T> HeapStructure heapify(List<T> list, Comparator<? super T> comp) {
      HeapStructure structure = new HeapStructure();
      for (int i = 0, len = list.size(); i < len; i++) {
         addToHeap(list, comp, i, structure);
      }
      return structure;
   }
   
   /**
    * Removes the largest element, the root of the smallest tree, from the heap structure. Once the
    * list is heapified, sorting involves contracting the heap, removing the largest elements as we
    * go. Each remove operation involves rectifying the heap structure to maintain heap invariants.
    *
    * @param list the list
    * @param comp the comparator for comparing two elements
    * @param last the last list index in the heap which contains the newly added element
    * @param structure the structure of the heap, describing the order of its constituent trees
    */
   private static <T> void removeSmallestRoot(List<T> list, Comparator<? super T> comp, int last,
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
      rectifyRoots(list, comp, leftRoot, structure.trees >> 1, structure.offset + 1);
      // and then including the right child
      rectifyRoots(list, comp, rightRoot, structure.trees, structure.offset);
   }
   
   /**
    * Sorts the specified list in place using the smooth sort algorithm. The process first heapifies
    * the list and then pops the largest element from the heap until it's empty.
    *
    * @param list the list
    * @param comp the comparator for comparing two elements
    */
   private static <T> void sortInPlace(List<T> list, Comparator<? super T> comp) {
      HeapStructure structure = heapify(list, comp);
      for (int i = list.size() - 1; i > 0; i--) {
         removeSmallestRoot(list, comp, i, structure);
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
