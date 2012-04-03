package com.apriori.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * An implementation of {@link NavigableSet} that is optimized for memory efficiency at the cost of
 * slower mutations. This is ideal for representing very large sets that do not change. Smaller sets
 * that change infrequently is another good use case.
 * 
 * <p>This set uses an array internally to store the items in the set. Lookups are
 * <em>O(log<sub>2</sub>n)</em>. Due to the way the elements are stored in the array, mutations
 * (insertions and removals can involve <em>O(n)</em> operations to shuffle elements around.
 * 
 * <p>This set does not support {@code null} values.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * @param <E> the type of element in the set
 */
public class SortedArraySet<E> implements NavigableSet<E>, Cloneable, Serializable {

   /**
    * An iterator over elements of the set.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class IteratorImpl implements Iterator<E> {

      int myModCount = modCount;
      private int start;
      private int limit;
      private int idx;
      private boolean removed = false;

      IteratorImpl() {
         this(0, size, modCount);
      }

      IteratorImpl(int start, int limit, int modCount) {
         this.start = start;
         this.limit = limit;
         myModCount = modCount;
         idx = start;
      }
      
      void resetModCount() {
         myModCount = modCount;
      }

      @Override
      public boolean hasNext() {
         checkMod(myModCount);
         return idx < limit;
      }

      @Override
      public E next() {
         if (idx >= limit) {
            throw new NoSuchElementException();
         }
         checkMod(myModCount);
         removed = false;
         @SuppressWarnings("unchecked")
         E ret = (E) data[idx++];
         return ret;
      }

      @Override
      public void remove() {
         if (removed) {
            // already removed
            throw new IllegalStateException("remove() already called");
         }
         else if (idx == start) {
            // no element yet to remove
            throw new IllegalStateException("next() never called");
         }
         checkMod(myModCount);
         removeItem(--idx);
         limit--;
         resetModCount();
         removed = true;
      }
   }

   /**
    * An iterator that visits elements in the set in the opposite order as a normal iterator: from
    * largest to smallest.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class DescendingIteratorImpl implements Iterator<E> {

      int myModCount;
      private int start;
      private int limit;
      private int idx;
      private boolean removed = false;

      DescendingIteratorImpl() {
         this(size, 0, modCount);
      }

      DescendingIteratorImpl(int start, int limit, int modCount) {
         this.start = start;
         this.limit = limit;
         myModCount = modCount;
         idx = start;
      }

      void resetModCount() {
         myModCount = modCount;
      }
      
      @Override
      public boolean hasNext() {
         checkMod(myModCount);
         return idx > limit;
      }

      @Override
      public E next() {
         if (idx <= limit) {
            throw new NoSuchElementException();
         }
         checkMod(myModCount);
         removed = false;
         @SuppressWarnings("unchecked")
         E ret = (E) data[--idx];
         return ret;
      }

      @Override
      public void remove() {
         if (removed) {
            // already removed
            throw new IllegalStateException("remove() already called");
         }
         else if (idx == start) {
            // no element yet to remove
            throw new IllegalStateException("next() never called");
         }
         checkMod(myModCount);
         removeItem(idx);
         resetModCount();
         removed = true;
      }
   }

   /**
    * A subset of a {@link SortedArraySet}. This is used to implement head-, tail-, and sub-sets.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubSetImpl implements NavigableSet<E> {
      private final E fromElement;
      private final boolean fromInclusive;
      private int fromIndex;
      
      private final E toElement;
      private final boolean toInclusive;
      int toIndex;
      
      int myModCount;
      
      public SubSetImpl(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
         this(fromElement, fromInclusive, -1, toElement, toInclusive, -1, modCount);
      }

      private SubSetImpl(E fromElement, boolean fromInclusive, int fromIndex,
            E toElement, boolean toInclusive, int toIndex, int modCount) {
         
         if (fromElement != null && toElement != null) {
            int check = compare(fromElement, toElement);
            if (check > 0 || (check == 0 && !fromInclusive && !toInclusive)) {
               throw new IllegalArgumentException("Invalid subset bounds");
            }
         } else if (fromElement == null && toElement == null) {
            throw new NullPointerException();
         }
         
         this.fromElement = fromElement;
         this.fromInclusive = fromInclusive;
         if (fromIndex == -1) {
            determineFromIndex();
         } else {
            this.fromIndex = fromIndex;
         }
         this.toElement = toElement;
         this.toInclusive = toInclusive;
         if (toIndex == -1) {
            determineToIndex();
         } else {
            this.toIndex = toIndex;
         }
         this.myModCount = modCount;
      }
      
      private void determineFromIndex() {
         if (fromElement == null) {
            fromIndex = 0;
         } else {
            fromIndex = findIndex(fromElement);
            if (fromIndex < 0) {
               fromIndex = -fromIndex - 1;
            } else if (!fromInclusive) {
               fromIndex++;
            }
         }
      }
      
      private void determineToIndex() {
         if (toElement == null) {
            toIndex = size - 1;
         } else {
            toIndex = findIndex(toElement);
            if (toIndex < 0) {
               toIndex = -toIndex - 2;
            } else if (!toInclusive) {
               toIndex--;
            }
         }
      }
      
      private void checkRangeLow(Object o, boolean inclusive) {
         if (!isInRangeLow(o, inclusive)) {
            throw new IllegalArgumentException("Object outside of subset range");
         }
      }
      
      private boolean isInRangeLow(Object o, boolean inclusive) {
         if (fromElement != null) {
            int c = compare(o, fromElement);
            if (c < 0 || (c == 0 && inclusive && !fromInclusive)) {
               return false;
            }
         }
         return true;
      }
      
      private void checkRangeHigh(Object o, boolean inclusive) {
         if (!isInRangeHigh(o, inclusive)) {
            throw new IllegalArgumentException("Object outside of subset range");
         }
      }
      
      private boolean isInRangeHigh(Object o, boolean inclusive) {
         if (toElement != null) {
            int c = compare(o, toElement);
            if (c > 0 || (c == 0 && inclusive && !toInclusive)) {
               return false;
            }
         }
         return true;
      }
      
      private void checkRange(Object o) {
         checkRangeLow(o, true);
         checkRangeHigh(o, true);
      }
      
      private boolean isInRange(Object o) {
         return isInRangeLow(o, true) && isInRangeHigh(o, true);
      }
      
      /** {@inheritDoc} */
      @Override
      public Comparator<? super E> comparator() {
         return comp;
      }

      /** {@inheritDoc} */
      @Override
      public E first() {
         checkMod(myModCount);
         if (fromIndex > toIndex) {
            throw new NoSuchElementException();
         }
         @SuppressWarnings("unchecked")
         E ret = (E) data[fromIndex];
         return ret;
      }

      /** {@inheritDoc} */
      @Override
      public E last() {
         checkMod(myModCount);
         if (fromIndex > toIndex) {
            throw new NoSuchElementException();
         }
         @SuppressWarnings("unchecked")
         E ret = (E) data[toIndex];
         return ret;
      }

      /** {@inheritDoc} */
      @Override
      public boolean add(E e) {
         checkMod(myModCount);
         checkRange(e);
         boolean ret = SortedArraySet.this.add(e);
         if (ret) {
            toIndex++;
            myModCount = modCount;
         }
         return ret;
      }

      /** {@inheritDoc} */
      @Override
      public boolean addAll(Collection<? extends E> coll) {
         checkMod(myModCount);
         for (Object o : coll) {
            checkRange(o);
         }
         boolean ret = SortedArraySet.this.addAll(coll);
         if (ret) {
            // not sure how many items were added, so re-calculate
            determineToIndex();
            myModCount = modCount;
         }
         return ret;
      }

      /** {@inheritDoc} */
      @Override
      public void clear() {
         checkMod(myModCount);
         if (fromIndex <= toIndex) {
            int removed = toIndex - fromIndex + 1;
            int tailKeep = size - toIndex - 1;
            // compact the before and after blocks
            if (tailKeep > 0) {
               System.arraycopy(data, toIndex + 1, data, fromIndex, tailKeep);
            }
            // and set extraneous references to null
            for (int i = fromIndex + tailKeep, len = removed - tailKeep; i < len; i++) {
               data[i] = null;
            }
            size -= removed;
            toIndex -= removed;
            myModCount = ++modCount;
         }
      }

      /** {@inheritDoc} */
      @Override
      public boolean contains(Object o) {
         checkMod(myModCount);
         return isInRange(o) ? SortedArraySet.this.contains(o) : false;
      }

      /** {@inheritDoc} */
      @Override
      public boolean containsAll(Collection<?> coll) {
         checkMod(myModCount);
         for (Object o : coll) {
            if (!contains(o)) {
               return false;
            }
         }
         return true;
      }

      /** {@inheritDoc} */
      @Override
      public boolean isEmpty() {
         checkMod(myModCount);
         return toIndex < fromIndex;
      }

      /** {@inheritDoc} */
      @Override
      public boolean remove(Object o) {
         checkMod(myModCount);
         if (isInRange(o)) {
            boolean ret = SortedArraySet.this.remove(o);
            if (ret) {
               toIndex--;
               myModCount = modCount;
            }
            return ret;
         } else {
            return false;
         }
      }

      /** {@inheritDoc} */
      @Override
      public boolean removeAll(Collection<?> coll) {
         checkMod(myModCount);
         HashSet<Object> toRemove = new HashSet<Object>(coll);
         for (Iterator<Object> iter = toRemove.iterator(); iter.hasNext(); ) {
            if (!isInRange(iter.next())) {
               iter.remove();
            }
         }
         if (!toRemove.isEmpty()) {
            boolean ret = SortedArraySet.this.removeAll(coll);
            if (ret) {
               // not sure how many items were removed so re-calc
               determineToIndex();
               myModCount = modCount;
            }
            return ret;
         } else {
            return false;
         }
      }

      /** {@inheritDoc} */
      @Override
      public boolean retainAll(Collection<?> coll) {
         checkMod(myModCount);
         ArrayList<Object> toRemove = new ArrayList<Object>();
         for (Object o : this) {
            if (!coll.contains(o)) {
               toRemove.add(o);
            }
         }
         if (toRemove.isEmpty()) {
            return false;
         } else {
            return removeAll(toRemove);
         }
      }

      /** {@inheritDoc} */
      @Override
      public int size() {
         checkMod(myModCount);
         return toIndex - fromIndex + 1;
      }

      /** {@inheritDoc} */
      @Override
      public Object[] toArray() {
         checkMod(myModCount);
         int len = size();
         Object a[] = new Object[len];
         if (len > 0) {
            System.arraycopy(data, fromIndex, a, 0, len);
         }
         return a;
      }

      /** {@inheritDoc} */
      @Override
      @SuppressWarnings("unchecked")
      public <T> T[] toArray(T[] a) {
         checkMod(myModCount);
         int len = size();
         if (a.length < len) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), len);
         }
         System.arraycopy(data, fromIndex, a, 0, len);
         if (a.length > len) {
            a[len] = null;
         }
         return a;
      }

      /** {@inheritDoc} */
      @Override
      public E ceiling(E e) {
         checkMod(myModCount);
         if (toIndex >= fromIndex) {
            if (isInRangeHigh(e, true)) {
               if (isInRangeLow(e, true)) {
                  E ret = SortedArraySet.this.ceiling(e);
                  if (isInRange(ret)) {
                     return ret;
                  }
               } else {
                  @SuppressWarnings("unchecked")
                  E ret = (E) data[fromIndex];
                  return ret;
               }
            }
         }
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public Iterator<E> descendingIterator() {
         checkMod(myModCount);
         return new DescendingIteratorImpl(toIndex + 1, fromIndex, myModCount) {
            @Override
            void resetModCount() {
               // update enclosing subset, too
               this.myModCount = modCount;
               SubSetImpl.this.myModCount = modCount;
            }
            @Override
            public void remove() {
               super.remove();
               toIndex--;
            }
         };
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> descendingSet() {
         checkMod(myModCount);
         return new DescendingSet<E>(this);
      }

      /** {@inheritDoc} */
      @Override
      public E floor(E e) {
         checkMod(myModCount);
         if (toIndex >= fromIndex) {
            if (isInRangeLow(e, true)) {
               if (isInRangeHigh(e, true)) {
                  E ret = SortedArraySet.this.floor(e);
                  if (isInRange(ret)) {
                     return ret;
                  }
               } else {
                  @SuppressWarnings("unchecked")
                  E ret = (E) data[toIndex];
                  return ret;
               }
            }
         }
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> headSet(E to) {
         return headSet(to, false);
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> headSet(E to, boolean inclusive) {
         checkMod(myModCount);
         checkRangeHigh(to, inclusive);
         return new SubSetImpl(this.fromElement, this.fromInclusive, this.fromIndex,
               to, inclusive, -1, myModCount);
      }

      /** {@inheritDoc} */
      @Override
      public E higher(E e) {
         checkMod(myModCount);
         if (toIndex >= fromIndex) {
            if (isInRangeHigh(e, true)) {
               if (isInRangeLow(e, true)) {
                  E ret = SortedArraySet.this.higher(e);
                  if (isInRange(ret)) {
                     return ret;
                  }
               } else {
                  @SuppressWarnings("unchecked")
                  E ret = (E) data[fromIndex];
                  return ret;
               }
            }
         }
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public Iterator<E> iterator() {
         checkMod(myModCount);
         return new IteratorImpl(fromIndex, toIndex + 1, myModCount) {
            @Override
            void resetModCount() {
               // update enclosing subset, too
               this.myModCount = modCount;
               SubSetImpl.this.myModCount = modCount;
            }
            @Override
            public void remove() {
               super.remove();
               toIndex--;
            }
         };
      }

      /** {@inheritDoc} */
      @Override
      public E lower(E e) {
         checkMod(myModCount);
         if (toIndex >= fromIndex) {
            if (isInRangeLow(e, true)) {
               if (isInRangeHigh(e, true)) {
                  E ret = SortedArraySet.this.lower(e);
                  if (isInRange(ret)) {
                     return ret;
                  }
               } else {
                  @SuppressWarnings("unchecked")
                  E ret = (E) data[toIndex];
                  return ret;
               }
            }
         }
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E pollFirst() {
         checkMod(myModCount);
         if (fromIndex > toIndex) {
            return null;
         }
         @SuppressWarnings("unchecked")
         E ret = (E) data[fromIndex];
         return ret;
      }

      /** {@inheritDoc} */
      @Override
      public E pollLast() {
         checkMod(myModCount);
         if (fromIndex > toIndex) {
            return null;
         }
         @SuppressWarnings("unchecked")
         E ret = (E) data[toIndex];
         return ret;
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> subSet(E from, E to) {
         return subSet(from, true, to, false);
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> subSet(E from, boolean fromInc, E to, boolean toInc) {
         checkMod(myModCount);
         checkRangeLow(from, fromInc);
         checkRangeHigh(to, toInc);
         return new SubSetImpl(from, fromInc, -1, to, toInc, -1, myModCount);
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> tailSet(E from) {
         return tailSet(from, true);
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> tailSet(E from, boolean inclusive) {
         checkMod(myModCount);
         checkRangeLow(from, inclusive);
         return new SubSetImpl(from, inclusive, -1,
               this.toElement, this.toInclusive, this.toIndex, myModCount);
      }
      
      @Override
      public boolean equals(Object o) {
         return Utils.equals(this, o);
      }
      
      @Override
      public int hashCode() {
         return Utils.hashCode(this);
      }
      
      @Override
      public String toString() {
         return Utils.toString(this);
      }
   }
   
   private static final long serialVersionUID = 576094990615323839L;

   private static final int DEFAULT_INITIAL_CAPACITY = 10;
   
   private static final int THRESHOLD_FOR_BULK_OP = 100;

   /**
    * The internal array that stores the elements in the set.
    */
   transient Object data[];
   
   /**
    * The size of the set. The internal array is automatically grown as needed and may be larger
    * than the actual size of the set.
    */
   transient int size;
  
   /**
    * The comparator used to sort elements in the set or {@code null} to indicate that items are
    * sorted by their natural order.
    */
   transient Comparator<? super E> comp;
   
   /**
    * The set's current revision level. Every modification to the set causes this count to be
    * incremented. It is used to implement the "fail-fast" behavior for detecting concurrent
    * modifications to the set.
    */
   protected transient int modCount;

   /**
    * Constructs a new empty set. All elements must be mutually comparable. They will be sorted per
    * their natural ordering.
    */
   public SortedArraySet() {
      this(DEFAULT_INITIAL_CAPACITY, null);
   }

   /**
    * Constructs a new empty set using the specified comparator to sort items within the set.
    * 
    * @param comp the comparator used to sort items in the set
    */
   public SortedArraySet(Comparator<? super E> comp) {
      this(DEFAULT_INITIAL_CAPACITY, comp);
   }

   /**
    * Constructs a new empty set with an internal array sized as specified. All elements must be
    * mutually comparable. They will be sorted per their natural ordering.
    *
    * @param initialCapacity the size of the internal array
    */
   public SortedArraySet(int initialCapacity) {
      this(initialCapacity, null);
   }

   /**
    * Constructs a new empty set. The internal array will be sized as specified, and items will be
    * sorted using the specified comparator.
    *
    * @param initialCapacity the size of the internal array
    * @param comp the comparator used to sort items in the set
    */
   public SortedArraySet(int initialCapacity, Comparator<? super E> comp) {
      if (initialCapacity < 0) {
         throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
      }
      data = new Object[initialCapacity];
      size = 0;
      this.comp = comp;
   }

   /**
    * Constructs a new set populated with the items in the given collection. All elements must be
    * mutually comparable. They will be sorted per their natural ordering.
    *
    * @param source the collection whose contents will be the initial contents of the set
    */
   public SortedArraySet(Collection<? extends E> source) {
      this(source, null);
   }

   /**
    * Constructs a new set populated with the items in the given collection.
    *
    * @param source the collection whose contents will be the initial contents of the set
    * @param comp the comparator used to sort items in the set
    */
   public SortedArraySet(Collection<? extends E> source, Comparator<? super E> comp) {
      this(source.size(), comp);
      size = source.size();
      int i = 0;
      for (E item : source) {
         data[i++] = item;
      }
      sort();
      removeDups();
   }

   /**
    * Sorts the internal array. Items are usually sorted as they are added, but this method can be
    * used to re-sort the array after bulk insertion operations.
    */
   private void sort() {
      if (comp == null) {
         Arrays.sort(data);
      }
      else {
         @SuppressWarnings("unchecked")
         E toSort[] = (E[]) data;
         Arrays.sort(toSort, comp);
      }
   }
   
   /**
    * Compares two elements for purposes of sorting in this set. If the set's comparator is
    * {@code null} then the items must implement {@code Comparable}.
    * 
    * @param o1 an element
    * @param o2 another element
    * @return negative one, zero, or positive one as the first element is less than, equal to, or
    *       greater than the second element, respectively
    */
   @SuppressWarnings("unchecked")
   int compare(Object o1, Object o2) {
      if (comp == null) {
         return ((Comparable<Object>) o1).compareTo(o2);
      } else {
         return comp.compare((E) o1, (E) o2);
      }
   }

   /**
    * Removes duplicate entries from the internal array. Generally, duplicates are not inserted into
    * the set. But this can be used to remove duplicates to make bulk insertion of items more
    * efficient.
    * 
    * <p>The internal array must already be {@link #sort() sorted} before this method is invoked.
    */
   private void removeDups() {
      int numUnique = 0;
      for (int i = 1; i < size; i++) {
         if (compare(data[i], data[numUnique]) != 0) {
            // not a dup!
            numUnique++;
            // move if necessary to keep unique items consolidated at the beginning of array
            if (numUnique != i) {
               data[numUnique] = data[i];
            }
         }
      }
      for (int i = numUnique + 1; i < size; i++) {
         // clear out the rest of the array, effectively null'ing out any dup references
         data[i] = null;
      }
      size = numUnique;
   }

   void checkMod(int someModCount) {
      if (someModCount != modCount) {
         throw new ConcurrentModificationException();
      }
   }

   private void maybeGrowArray() {
      int prevLen = data.length;
      if (prevLen > size) {
         return;
      }
      int len = prevLen << 1;
      // avoid overflow
      if (len <= prevLen) {
         len = Integer.MAX_VALUE - 8;
      }
      Object oldData[] = data;
      data = new Object[len];
      System.arraycopy(oldData, 0, data, 0, oldData.length);
      modCount++;
   }

   /**
    * Binary searches a range in the set to find the specified item. If the item is not found, an
    * encoded negative value is returned that indicates where the item <em>would</em> be. So if the
    * return value is negative then the following expression indicates the index into the array
    * where the item would go:
    * <pre>
    * -findIndex(o, lo, hi) - 1
    * </pre>
    *
    * @param o the item to find
    * @param lo the minimum index in the internal array that will be searched
    * @param hi the maximum index in the internal array that will be searched
    * @return the index in the list where the specified item was found or, if not found, a negative
    *       value that can be decoded to determine the index where the item would go
    */
   int findIndex(Object o, int lo, int hi) {
      if (hi < lo) {
         return -1;
      }
      while (true) {
         int mid = (hi + lo) >> 1;
         int c = compare(data[mid], o);
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

   int findIndex(Object o) {
      return findIndex(o, 0, size - 1);
   }

   void insertItem(E element, int index) {
      maybeGrowArray();
      if (index < size) {
         System.arraycopy(data, index, data, index + 1, size - index);
      }
      data[index] = element;
      modCount++;
      size++;
   }

   void removeItem(int index) {
      if (index < size - 1) {
         System.arraycopy(data, index + 1, data, index, size - 1 - index);
      }
      data[size - 1] = null; // clear last reference
      modCount++;
      size--;
   }

   /**
    * Shrinks the internal array so that it is exactly the necessary size. Attempts to add an item
    * to the set after this is called will cause it to grow the internal array to fit the new item.
    */
   public void trimToSize() {
      if (data.length != size) {
         data = Arrays.copyOf(data, size);
         modCount++;
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean add(E element) {
      int idx = findIndex(element);
      if (idx >= 0) {
         return false;
      }
      insertItem(element, -(idx + 1));
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean addAll(Collection<? extends E> elements) {
      int otherSize = elements.size();
      boolean ret = false;
      
      // Since adding an item is O(n) due to need to shift items around in internal array, the
      // simple approach degenerates into quadratic performance: O(m *n), where m is size of
      // specified collection and n is size of this set. So we instead defer the O(n) operation to
      // the end and do it only once. This is done by just appending elements into an array
      // (constant time for adding a single item) and then sort and remove duplicates in one final
      // pass at the end. That makes the whole algorithm O(m * log n).
      
      if (size < THRESHOLD_FOR_BULK_OP && otherSize < THRESHOLD_FOR_BULK_OP) {
         // simple approach is fine for small collections since linear ops use very fast
         // System.arraycopy which offsets algorithmic inefficiencies
         for (E e : elements) {
            if (add(e)) {
               ret = true;
            }
         }
         return ret;
      }
      
      Object newData[] = new Object[size + otherSize];
      newData = elements.toArray(newData);
      System.arraycopy(data, 0, newData, otherSize, size);
      int oldSize = size;
      sort();
      removeDups();
      return size > oldSize;
   }

   /** {@inheritDoc} */
   @Override
   public void clear() {
      for (int i = 0, len = data.length; i < len; i++) {
         data[i] = null;
      }
      size = 0;
      modCount++;
   }

   /** {@inheritDoc} */
   @Override
   public boolean contains(Object element) {
      return findIndex(element) >= 0;
   }

   /** {@inheritDoc} */
   @Override
   public boolean containsAll(Collection<?> elements) {
      for (Object o : elements) {
         if (!contains(o)) {
            return false;
         }
      }
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean isEmpty() {
      return size == 0;
   }

   /** {@inheritDoc} */
   @Override
   public Iterator<E> iterator() {
      return new IteratorImpl();
   }

   /** {@inheritDoc} */
   @Override
   public boolean remove(Object element) {
      int idx = findIndex(element);
      if (idx < 0) {
         return false;
      }
      removeItem(idx);
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean removeAll(Collection<?> elements) {
      if (size == 0) {
         return false;
      }
      
      // Since removing an item is O(n) due to need to shift items around in internal array, the
      // simple approach degenerates into quadratic performance: O(m *n), where m is size of
      // specified collection and n is size of this set. So we instead defer the O(n) operation to
      // the end and do it only once. This is done by overwriting removed elements with a duplicate
      // value and then doing a single O(n) de-dup pass at the end. That makes the whole algorithm
      // O(m * log n).
      
      boolean ret = false;
      if (elements.size() < THRESHOLD_FOR_BULK_OP && size < THRESHOLD_FOR_BULK_OP) {
         // simple approach is fine for small collections since linear ops use very fast
         // System.arraycopy which offsets algorithmic inefficiencies
         for (Object o : elements) {
            if (remove(o)) {
               ret = true;
            }
         }
         return ret;
      }
      
      for (Object o : elements) {
         int idx = findIndex(o);
         if (idx >= 0) {
            if (size == 1) {
               size = 0;
               data[0] = null;
               // nothing else that can be removed so skip out
               return true;
            }
            ret = true;
            if (idx == 0) {
               data[0] = data[1];
            }
            else {
               data[idx] = data[idx-1];
            }
         }
      }
      if (ret) {
         removeDups();
         modCount++;
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public boolean retainAll(Collection<?> elements) {
      boolean ret = false;

      // Similar performance notes as removeAll(). We use a bulk strategy for larger collections
      // to get O(m log n) performance instead of O(m * n).
      
      if (elements.size() < THRESHOLD_FOR_BULK_OP && size < THRESHOLD_FOR_BULK_OP) {
         // simple approach is acceptable for small collections
         for (Iterator<E> iter = iterator(); iter.hasNext(); ) {
            if (!elements.contains(iter.next())) {
               iter.remove();
               ret = true;
            }
         }
         return ret;
      }
      
      Object newData[] = new Object[elements.size()];
      int newSize = 0;
      for (Object o : elements) {
         int i = findIndex(o);
         if (i >= 0) {
            newData[newSize++] = data[i];
         }
      }
      data = newData;
      size = newSize;
      removeDups();
      modCount++;
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public int size() {
      return size;
   }

   /** {@inheritDoc} */
   @Override
   public Object[] toArray() {
      Object ret[] = new Object[size];
      System.arraycopy(data, 0, ret, 0, size);
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public <T> T[] toArray(T[] a) {
      if (a.length < size) {
         a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
      }
      System.arraycopy(data, 0, a, 0, size);
      if (a.length > size) {
         a[size] = null;
      }
      return a;
   }

   /** {@inheritDoc} */
   @Override
   public Comparator<? super E> comparator() {
      return comp;
   }

   /** {@inheritDoc} */
   @Override
   public E first() {
      if (size == 0) {
         throw new NoSuchElementException("set is empty");
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[0];
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> headSet(E toElement) {
      return headSet(toElement, false);
   }

   /** {@inheritDoc} */
   @Override
   public E last() {
      if (size == 0) {
         throw new NoSuchElementException("set is empty");
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[size - 1];
      return ret;
   }
   
   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> subSet(E fromElement, E toElement) {
      return subSet(fromElement, true, toElement, false);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E fromElement) {
      return tailSet(fromElement, true);
   }

   /** {@inheritDoc} */
   @Override
   public E ceiling(E e) {
      int idx = findIndex(e);
      if (idx < 0) {
         idx = -idx - 1;
         if (idx >= size) {
            return null;
         }
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[idx];
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public Iterator<E> descendingIterator() {
      return new DescendingIteratorImpl();
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> descendingSet() {
      return new DescendingSet<E>(this);
   }

   /** {@inheritDoc} */
   @Override
   public E floor(E e) {
      int idx = findIndex(e);
      if (idx < 0) {
         idx = -idx - 2;
         if (idx < 0) {
            return null;
         }
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[idx];
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      if (toElement == null) {
         throw new NullPointerException();
      }
      return new SubSetImpl(null, false, toElement, inclusive);
   }

   /** {@inheritDoc} */
   @Override
   public E higher(E e) {
      int idx = findIndex(e);
      if (idx >= 0) {
         return null;
      }
      else {
         idx = -idx - 1;
         if (idx >= size) {
            return null;
         }
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[idx];
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public E lower(E e) {
      int idx = findIndex(e);
      if (idx >= 0) {
         return null;
      }
      else {
         idx = -idx - 2;
         if (idx < 0) {
            return null;
         }
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[idx];
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public E pollFirst() {
      if (size == 0) {
         return null;
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[0];
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public E pollLast() {
      if (size == 0) {
         return null;
      }
      @SuppressWarnings("unchecked")
      E ret = (E) data[size - 1];
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
         boolean toInclusive) {
      if (fromElement == null || toElement == null) {
         throw new NullPointerException();
      }
      return new SubSetImpl(fromElement, fromInclusive, toElement, toInclusive);
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      if (fromElement == null) {
         throw new NullPointerException();
      }
      return new SubSetImpl(fromElement, inclusive, null, false);
   }
   
   /** {@inheritDoc} */
   @Override
   public boolean equals(Object o) {
      return Utils.equals(this, o);
   }
   
   /** {@inheritDoc} */
   @Override
   public int hashCode() {
      return Utils.hashCode(this);
   }
   
   /** {@inheritDoc} */
   @Override
   public String toString() {
      return Utils.toString(this);
   }
   
   @Override
   public SortedArraySet<E> clone() {
      if (this.getClass() == SortedArraySet.class) {
         // don't bother cloning internal state - just create a new optimized list
         return new SortedArraySet<E>(this);
      }
      else {
         try {
            @SuppressWarnings("unchecked")
            SortedArraySet<E> copy = (SortedArraySet<E>) super.clone();
            // deep copy the array
            copy.data = data.clone();
            // now sub-class can do whatever else with this...
            return copy;
         }
         catch (CloneNotSupportedException e) {
            // should never happen since we implement Cloneable -- but just in
            // case, wrap in a runtime exception that sort of makes sense...
            throw new ClassCastException("java.lang.Cloneable");
         }
      }
   }
   
   /**
    * Customizes de-serialization to read set of elements same way as written by
    * {@link #writeObject(ObjectOutputStream)}.
    * 
    * @param in the stream from which the set is read
    * @throws IOException if an exception is raised when reading from {@code in}
    * @throws ClassNotFoundException if de-serializing an element fails to locate the element's
    *            class
    */
   @SuppressWarnings("unchecked")
   private void readObject(ObjectInputStream in) throws IOException,
         ClassNotFoundException {
      in.defaultReadObject();
      comp = (Comparator<? super E>) in.readObject();
      size = in.readInt();
      data = new Object[size];
      for (int i = 0; i < size; i++) {
         data[i] = in.readObject();
      }
   }
   
   /**
    * Customizes serialization by just writing the set contents in order.
    * 
    * @param out the stream to which to serialize this set
    * @throws IOException if an exception is raised when writing to {@code out}
    */
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      out.writeObject(comp);
      out.writeInt(size);
      for (E e : this) {
         out.writeObject(e);
      }
   }
}
