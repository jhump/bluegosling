package com.apriori.collections;

import com.apriori.util.Predicates;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * A growable array that does not require doubling and copying an underlying array. Instead, it
 * uses multiple arrays of increasing size (data blocks), and still provides constant time random
 * access and constant time for growing or shrinking the array by one element. For very large
 * arrays, it can be much less wasteful than the typical array-doubling approach. The possible
 * wasted memory (unused portion of underlying structure) is bounded by O(&radic;n).
 * 
 * <p>The implementation is based on the "singly resizable array" that is described in
 * <a href="https://cs.uwaterloo.ca/research/tr/1999/09/CS-99-09.pdf">this paper</a>.
 *
 * @param <T> the type of element in the array
 * 
 * @see <a href="https://cs.uwaterloo.ca/research/tr/1999/09/CS-99-09.pdf">Resizable Arrays in Optimal Time and Space</a>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
// TODO: serialization, cloning
// TODO: more efficient iterator, spliterator
// TODO: concurrent modification checks in iterator and spliterator
public class NoCopyGrowableArray<T> implements GrowableArray<T>, Serializable, Cloneable {

   private static final long serialVersionUID = -5864366571938870266L;
   
   private Object indexBlock[][];
   private int numSuperBlocks;
   private int superBlockSize;
   private int lastSuperBlockOccupancy;
   private int numDataBlocks;
   private int dataBlockSize;
   private int lastDataBlockOccupancy;
   private int size;
   private int modCount;
   
   public NoCopyGrowableArray() {
      indexBlock = new Object[2][];
      superBlockSize = lastSuperBlockOccupancy = dataBlockSize = lastDataBlockOccupancy = 1;
   }
   
   public NoCopyGrowableArray(int initialSize) {
      this();
      growBy(initialSize);
   }
   
   public NoCopyGrowableArray(Iterable<? extends T> others) {
      this();
      pushAll(others);
   }
   
   private void rangeCheck(int i) {
      if (i < 0 || i >= size) {
         throw new IndexOutOfBoundsException();
      }
   }
   
   @Override
   public void growBy(int numNewElements) {
      if (numNewElements < 0) {
         throw new IllegalArgumentException();
      }
      internalGrowBy(numNewElements);
   }
   
   private void internalGrowBy(int numNewElements) {
      modCount++;
      int remaining = numNewElements;
      while (remaining > 0) {
         int blockCapacity = dataBlockSize - lastDataBlockOccupancy;
         if (remaining <= blockCapacity) {
            lastDataBlockOccupancy += remaining;
            break;
         } else {
            remaining -= blockCapacity;
            if (lastSuperBlockOccupancy == superBlockSize) {
               if (++numSuperBlocks > 1) {
                  if ((numSuperBlocks & 1) == 1) {
                     superBlockSize <<= 1;
                  } else {
                     dataBlockSize <<= 1;
                  }
               }
               lastSuperBlockOccupancy = 0;
            }
            int indexLimit = indexBlock.length;
            if (numDataBlocks == indexLimit) {
               Object oldIndex[][] = indexBlock;
               indexBlock = new Object[indexLimit << 1][];
               System.arraycopy(oldIndex, 0, indexBlock, 0, numDataBlocks);
               indexBlock[numDataBlocks] = new Object[dataBlockSize];
            } else if (indexBlock[numDataBlocks] == null) {
               indexBlock[numDataBlocks] = new Object[dataBlockSize];
            }
            numDataBlocks++;
            lastSuperBlockOccupancy++;
            lastDataBlockOccupancy = 0;
         }
      }
      size += numNewElements;
   }
   
   @Override
   public void shrinkBy(int numElementsToRemove) {
      if (numElementsToRemove < 0) {
         throw new IllegalArgumentException();
      }
      internalShrinkBy(numElementsToRemove);
   }
   
   private void internalShrinkBy(int numElementsToRemove) {
      modCount++;
      int remaining = numElementsToRemove;
      while (remaining > 0) {
         if (remaining < lastDataBlockOccupancy) {
            int end = lastDataBlockOccupancy;
            lastDataBlockOccupancy -= remaining;
            // null out any refs
            Arrays.fill(indexBlock[numDataBlocks - 1], lastDataBlockOccupancy, end, null);
            break;
         } else {
            remaining -= lastDataBlockOccupancy;
            if (numDataBlocks < indexBlock.length && indexBlock[numDataBlocks] == null) {
               // we never want more than one empty data block so "de-allocate" the last one
               indexBlock[numDataBlocks] = null;
            }
            int threshold = indexBlock.length >> 2;
            if (numDataBlocks <= threshold) {
               Object oldIndex[][] = indexBlock;
               indexBlock = new Object[threshold << 1][];
               System.arraycopy(oldIndex, 0, indexBlock, 0, numDataBlocks);
            }
            Arrays.fill(indexBlock[--numDataBlocks], 0, lastDataBlockOccupancy, null);
            if (--lastSuperBlockOccupancy == 0) {
               if (--numSuperBlocks > 0) {
                  if ((numSuperBlocks & 1) == 0) {
                     superBlockSize >>= 1;
                  } else {
                     dataBlockSize >>= 1;
                  }
               }
               lastSuperBlockOccupancy = superBlockSize;
            }
            lastDataBlockOccupancy = dataBlockSize;
         }
      }
      size -= numElementsToRemove;
   }
   
   @Override
   public T get(int index) {
      rangeCheck(index);
      int r, k, b, e;
      r = index + 1;
      k = Integer.SIZE - 1 - Integer.numberOfLeadingZeros(r);
      int eBits = (k + 1) >> 1;
      e = r & ~(-1 << eBits);
      r >>= eBits;
      int bBits = k >> 1;
      b = r & ~(-1 << bBits);
      int p = (1 << eBits) + (1 << bBits) - 2;
      @SuppressWarnings("unchecked")
      T ret = (T) indexBlock[p + b][e];
      return ret;
   }

   @Override
   public void set(int index, T item) {
      rangeCheck(index);
      int r, k, b, e;
      r = index + 1;
      k = Integer.SIZE - 1 - Integer.numberOfLeadingZeros(r);
      int eBits = (k + 1) >> 1;
      e = r & ~(-1 << eBits);
      r >>= eBits;
      int bBits = k >> 1;
      b = r & ~(-1 << bBits);
      int p = (1 << eBits) + (1 << bBits) - 2;
      indexBlock[p + b][e] = item;
   }
   
   @Override
   public T last() {
      if (size == 0) {
         throw new NoSuchElementException();
      }
      @SuppressWarnings("unchecked")
      T ret = (T) indexBlock[numDataBlocks - 1][lastDataBlockOccupancy - 1];
      return ret;
   }
   
   @Override
   public T first() {
      if (size == 0) {
         throw new NoSuchElementException();
      }
      @SuppressWarnings("unchecked")
      T ret = (T) indexBlock[0][0];
      return ret;
   }
   
   @Override
   public void push(T item) {
      internalGrowBy(1);
      indexBlock[numDataBlocks - 1][lastDataBlockOccupancy - 1] = item;
   }
   
   @Override
   public T pop() {
      T ret = last();
      internalShrinkBy(1);
      return ret;
   }
   
   @Override
   public void pushAll(Iterable<? extends T> values) {
      // mark the initial index, for the first item we'll append
      int i = size;
      int d = numDataBlocks - 1;
      int e = lastDataBlockOccupancy;
      int lim = d == -1 ? 0 : indexBlock[d].length;
      // try to pre-allocate the entire amount needed
      if (values instanceof Collection) {
         growBy(((Collection<?>) values).size());
      } else if (values instanceof ImmutableCollection) {
         growBy(((ImmutableCollection<?>) values).size());
      }
      for (T t : values) {
         if (i >= size) {
            internalGrowBy(1);
         }
         if (e == lim || d == -1) {
            d++;
            e = 0;
            lim = indexBlock[d].length;
         }
         indexBlock[d][e] = t;
         e++;
         i++;
      }
      // in case values collection was concurrently modified to have fewer
      // elements than we initially made room for
      if (i < size) {
         internalShrinkBy(size - i);
      }
   }
   
   @Override
   public int size() {
      return size;
   }

   @Override
   public void clear() {
      modCount++;
      if (indexBlock.length != 2) {
         indexBlock = new Object[2][];
      } else {
         indexBlock[0] = null;
         indexBlock[1] = null;
      }
      superBlockSize = lastSuperBlockOccupancy = dataBlockSize = lastDataBlockOccupancy = 1;
      size = numSuperBlocks = numDataBlocks = 0; 
   }
   
   public static void main(String args[]) {
      GrowableArray<String> l = new NoCopyGrowableArray<>();
      for (int i = 0; i < 26; i++) {
         char ch[] = new char[3];
         ch[0] = (char)('A' + i);
         for (int j = 0; j < 26; j++) {
            ch[1] = (char)('a' + j);
            for (int k = 0; k < 26; k++) {
               ch[2] = (char)('a' + k);
               l.push(String.copyValueOf(ch));
               l.growBy(1);
               l.set(l.size() - 1, String.copyValueOf(ch));
            }
         }
      }
      l = l.stream().filter(Predicates.every(26))
            .collect(GrowableArray.collector(NoCopyGrowableArray::new));
      for (String s : l) {
         System.out.println(s);
      }
      l = new NoCopyGrowableArray<>(l);
      while (!l.isEmpty()) {
         System.out.println(l.pop());
      }
      System.out.println(l.parallelStream().map(s -> s.charAt(0)).distinct().count());
      while (l.size() > 1) {
         l.shrinkBy(1);
      }
      for (String s : l) {
         System.out.println(s);
      }
   }
}
