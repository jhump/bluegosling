package com.apriori.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * A list implementation that attempts to achieve some of the benefits of a linked list without
 * sacrificing the overwhelming performance advantages and memory efficiency of arrays.
 * 
 * <p>
 * A memory backed linked list works by using arrays to store the data as well as supplemental
 * arrays that store references for "next" and "previous" elements:
 * 
 * <pre>
 * Object data[];  // stores the elements in the list
 * int next[];     // next[i] stores the index in data for the item "after" data[i]
 * int previous[]; // similar to next, but stores index of previous item
 * int head;       // index in data for the first item in the list
 * int tail;       // index in data for the last item in the list
 * 
 * // so...
 * data[head]         // this is the first item in the array
 * next[head]         // this is the index of the second item in the array ..
 *                    //  ... OR -1 if the list has only one item
 * previous[head]     // this will be -1 since there is no item prior to head
 * data[next[head]]   // the second item in the array
 * head == tail == -1 // true when the list is empty
 * </pre>
 * 
 * This allows us to optimize storage as arrays (fixed number of objects to allocate and then
 * garbage collect instead of one object per item in the list). For append-only lists, the internal
 * structure ({@code data}) will be identical to the internal array of {@code java.util.ArrayList}.
 * 
 * <p>
 * The implementation of adding an item to the list will generally be constant time but it can
 * degrade to linear time under some cases. If the list is populated and then a large number of
 * items are removed, adding new items back to the list will try to "fill the gaps" in the buffer
 * before growing it. Finding such a gap can involve <em>O(n)</em> traversal through the buffer.
 * 
 * <p>
 * <em><strong>Note:</strong> a possible enhancement to this class is to track a queue of
 * free nodes. This would involve minor overhead during insertion and removal operations
 * but keep insertions to always constant time. The bigger drawback is that it causes
 * this list structure to occupy more memory. To make sure that queue operations are
 * fast, it would best be implemented as an array (same length as internal buffer) that
 * is used as circular buffer, like in {@code java.util.ArrayDeque}.</em>
 * 
 * <p>
 * This class uses a similar strategy for automatically growing the internal buffer as
 * {@code java.util.ArrayList} and {@code java.util.HashMap}. To minimize the amount of heap space
 * required by the list, this class provides a method named {@code trimToSize()} that is similar to,
 * though not quite the same as, the method of the same name provided by {@code java.util.ArrayList}
 * . However, in cases where elements are arbitrarily inserted and removed to and from the list, the
 * internal structure can become fragmented so this method may not actually release very much
 * memory. So two other methods are also provided:
 * <ol>
 * <li>{@link #compact()} - This method will first defragment the internal buffer and then trim the
 * buffer to size. This method is just as effective as {@code java.util.ArrayList.trimToSize()} at
 * freeing heap space, but, depending on the extent of fragmentation, may take longer to execute. It
 * runs in <em>O(n)</em> time.</li>
 * <li>{@link #optimize()} - Instead of trying to defragment the list, this method creates a new
 * buffer and populates it with the elements of the list <em>in order</em>. The result is a buffer
 * where the first item in the array is the first item in the list, the second item in the array is
 * the second item in the list, and so on. This not only frees up heap space but also improves the
 * performance of iteration since it should reduce the chances of memory cache misses since the
 * items are stored contiguously vs. back and forth all over the array.</li>
 * </ol>
 * 
 * <p>
 * Terminology used in doc and comments for this class:
 * <dl>
 * <dt>List index</dt>
 * <dd>An index that describes the ordinal position of an element or node in the list.</dd>
 * <dt>Raw index</dt>
 * <dd>An index that describes the actual position in the array buffer of an element or node. This
 * is often different than an element's <em>list index</em>.</dd>
 * <dt>High water point</dt>
 * <dd>The index in the array buffer <em>after</em> the last raw index which contains an item. If
 * the array buffer is full or if, even if it is mostly empty but the last element in the array
 * buffer contains a list item, then this is equal to the size of the array buffer. So if you insert
 * 20 items into an empty list and then remove 10, it is possible (depending on which 10 items are
 * removed) that the high water point will still be 20 (until you {@code compact()} or
 * {@code optimize()} the list).</dd>
 * </dl>
 * 
 * <p>
 * This list is not thread-safe. It can be accessed for read-only operations safely but simultaneous
 * changes to the list from multiple threads (or even changes from one thread with reads from other
 * threads) may have undefined behavior. Most operations, including iteration, could have undefined
 * results if the underlying list is changed concurrently. So these operations implement "fail fast"
 * and make a best effort to raise {@code ConcurrentModificationException}s if it is detected that
 * changes have been made to the list. Note that these failures should not be relied on for correct
 * multiple access to the list but are instead present only for detecting bugs in multi-threaded
 * code.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @param <E> The type of element in the array
 */
public class ArrayBackedLinkedList<E> extends AbstractList<E>
      implements Deque<E>, Cloneable, Serializable {

   /**
    * Concrete implementation of {@code ListIterator}. This same class is used for both iterators
    * for the list and iterators for sub-lists returned from
    * {@link ArrayBackedLinkedList#subList(int, int)}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class IteratorImpl implements ListIterator<E> {

      /**
       * The iterator's current raw index into the buffer.
       */
      int pos;

      /**
       * The iterator's current list index.
       */
      int idx;

      /**
       * The raw index into the buffer for the item last retrieved by either {@code next()} or
       * {@code previous()}.
       */
      int lastFetched = -1;

      /**
       * The current modification state of the iterator.
       */
      private IteratorModifiedState lastFetchModified = IteratorModifiedState.NONE;

      /**
       * Snapshot of {@code modCount} for detecting concurrent modifications.
       */
      int myModCount = getModCount();

      /**
       * Creates an iterator for the list starting at the first element.
       */
      IteratorImpl() {
         this(-1, -1);
      }

      /**
       * Creates an iterator for the list starting at the specified list index.
       * 
       * @param idx the list index <em>before</em> the first element returned by {@code next()}
       */
      IteratorImpl(int idx) {
         this(find(idx), idx);
      }

      /**
       * Creates an iterator for the list starting at the specified list index. For efficiency, the
       * raw index is also specified so that a traversal through the list isn't necessary (useful
       * for places where the raw index for a given list index is already known).
       * 
       * @param pos the raw index of element <em>before</em> the first element returned by
       *           {@code next()}
       * @param idx the list index of the element <em>before</em> first element returned by
       *           {@code next()}
       */
      IteratorImpl(int pos, int idx) {
         this.pos = pos;
         this.idx = idx;
      }
      
      void resetModCount() {
         myModCount = getModCount();
      }

      @Override
      public void add(E e) {
         checkMod(myModCount);
         int addAt;
         if (pos == -1) {
            addAt = head;
         }
         else {
            addAt = next[pos];
         }
         if (addAt == -1) {
            ArrayBackedLinkedList.this.add(e); // append
            pos = tail;
         }
         else {
            pos = addInternal(addAt, e);
         }
         idx++;
         resetModCount();
         lastFetchModified = IteratorModifiedState.ADDED;
      }

      private void checkCanModifyElement(String op) {
         if (lastFetched == -1) {
            throw new IllegalStateException("No element to " + op + ": "
                  + "next() or previous() never called");
         }
         if (lastFetchModified != IteratorModifiedState.NONE) {
            throw new IllegalStateException(
                  "Cannot "
                        + op
                        + " item after call to "
                        + (lastFetchModified == IteratorModifiedState.REMOVED ? "remove()"
                              : "add()"));
         }
      }

      void dec() {
         checkMod(myModCount);
         if (idx < 0) {
            throw new NoSuchElementException("At beginning of list");
         }
         lastFetched = pos;
         lastFetchModified = IteratorModifiedState.NONE;
         idx--;
         pos = prev[pos];
      }

      @Override
      public boolean hasNext() {
         return idx < size - 1;
      }

      @Override
      public boolean hasPrevious() {
         return idx >= 0;
      }

      void inc() {
         checkMod(myModCount);
         if (idx >= size - 1) {
            throw new NoSuchElementException("At end of list");
         }
         idx++;
         if (pos == -1) {
            pos = head;
         }
         else {
            pos = next[pos];
         }
         lastFetched = pos;
         lastFetchModified = IteratorModifiedState.NONE;
      }

      @Override
      @SuppressWarnings("unchecked")
      public E next() {
         inc();
         return (E) data[lastFetched];
      }

      @Override
      public int nextIndex() {
         return idx + 1;
      }

      @Override
      @SuppressWarnings("unchecked")
      public E previous() {
         dec();
         return (E) data[lastFetched];
      }

      @Override
      public int previousIndex() {
         return idx;
      }

      @Override
      public void remove() {
         checkMod(myModCount);
         checkCanModifyElement("remove");
         if (lastFetched == pos) {
            // shift current pointer back so we're no longer
            // pointing at removed item
            pos = prev[lastFetched];
            idx--;
         }
         removeInternal(lastFetched);
         resetModCount();
         lastFetchModified = IteratorModifiedState.REMOVED;
      }

      @Override
      public void set(E e) {
         checkMod(myModCount);
         checkCanModifyElement("set");
         data[lastFetched] = e;
      }
   }

   /**
    * A view of the list where random access operations will run in <em>O(1)</em> constant time.
    * This is mostly a read-only view since changes made to the list can alter its internal storage,
    * making it no longer optimized and making random access operations revert to O(n) performance.
    * Mutative operations that are allowed are adding items to the very end of the list and removing
    * the very last item in the list. Other mutative operations will throw
    * {@code UnsupportedOperationException}s. This list implementation attempts to detect
    * concurrent modifications (since they may alter the internal structure) and fail fast by
    * throwing {@code ConcurrentModificationException}s.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class RandomAccessImpl extends AbstractList<E> implements RandomAccess {

      public RandomAccessImpl() {}

      @Override
      public boolean add(E e) {
         checkOptimized();
         return ArrayBackedLinkedList.this.add(e);
      }

      @Override
      public void add(int idx, E e) {
         if (idx != size) {
            // only support adding items to end of list, which keeps
            // internal buffer optimized
            throw new UnsupportedOperationException("asRandomAccess().add(int, Object)");
         }
         checkOptimized();
         ArrayBackedLinkedList.this.add(idx, e);
      }

      @Override
      public boolean addAll(Collection<? extends E> coll) {
         checkOptimized();
         return ArrayBackedLinkedList.this.addAll(coll);
      }

      @Override
      public boolean addAll(int idx, Collection<? extends E> coll) {
         if (idx != size) {
            // only support adding items to end of list, which keeps
            // internal buffer optimized
            throw new UnsupportedOperationException("asRandomAccess().addAll(int, Collection)");
         }
         checkOptimized();
         return ArrayBackedLinkedList.this.addAll(idx, coll);
      }

      private void checkOptimized() {
         if (!isOptimized) {
            throw new ConcurrentModificationException();
         }
      }

      @Override
      public void clear() {
         ArrayBackedLinkedList.this.clear();
      }

      @Override
      public boolean contains(Object o) {
         return ArrayBackedLinkedList.this.contains(o);
      }

      @Override
      public boolean containsAll(Collection<?> coll) {
         return ArrayBackedLinkedList.this.containsAll(coll);
      }

      @Override
      public E get(int idx) {
         checkOptimized();
         return ArrayBackedLinkedList.this.get(idx);
      }

      @Override
      public int indexOf(Object o) {
         return ArrayBackedLinkedList.this.indexOf(o);
      }

      @Override
      public boolean isEmpty() {
         return ArrayBackedLinkedList.this.isEmpty();
      }

      @Override
      public Iterator<E> iterator() {
         return ArrayBackedLinkedList.this.iterator();
      }

      @Override
      public int lastIndexOf(Object o) {
         return ArrayBackedLinkedList.this.lastIndexOf(o);
      }

      @Override
      public ListIterator<E> listIterator() {
         return ArrayBackedLinkedList.this.listIterator();
      }

      @Override
      public ListIterator<E> listIterator(int from) {
         return ArrayBackedLinkedList.this.listIterator(from);
      }

      @Override
      public E remove(int idx) {
         if (idx != size) {
            // only support removing items from end of list, which keeps
            // internal buffer optimized
            throw new UnsupportedOperationException("asRandomAccess().remove(int)");
         }
         checkOptimized();
         return ArrayBackedLinkedList.this.remove(idx);
      }

      @Override
      public boolean remove(Object o) {
         return ArrayBackedLinkedList.this.remove(o);
      }

      @Override
      public boolean removeAll(Collection<?> coll) {
         return ArrayBackedLinkedList.this.removeAll(coll);
      }

      @Override
      public boolean retainAll(Collection<?> coll) {
         return ArrayBackedLinkedList.this.retainAll(coll);
      }

      @Override
      public E set(int idx, E e) {
         checkOptimized();
         return ArrayBackedLinkedList.this.set(idx, e);
      }

      @Override
      public int size() {
         return ArrayBackedLinkedList.this.size();
      }

      @Override
      public List<E> subList(int from, int to) {
         checkOptimized();
         return ArrayBackedLinkedList.this.subList(from, to);
      }

      @Override
      public Object[] toArray() {
         checkOptimized();
         return ArrayBackedLinkedList.this.toArray();
      }

      @Override
      public <T> T[] toArray(T[] array) {
         checkOptimized();
         return ArrayBackedLinkedList.this.toArray(array);
      }
   }

   /**
    * A sub-list that provides the {@code List} interface over a subset of elements in this list.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubListImpl extends AbstractList<E> {

      int low;
      int high;
      int subHead;
      int subTail;

      SubListImpl(int low, int high) {
         this.low = low;
         this.high = high;
         this.subHead = find(low);
         this.subTail = find(high - 1);
         this.modCount = getModCount();
      }

      @Override
      public boolean add(E e) {
         checkMod(modCount);
         if (subTail == tail) {
            ArrayBackedLinkedList.this.add(e);
            subTail = tail;
         }
         else {
            if (subTail == -1) {
               subTail = head;
            }
            else {
               subTail = next[subTail];
            }
            int newTail = addInternal(subTail, e);
            if (subHead == -1) {
               subHead = newTail;
            }
            subTail = newTail;
         }
         if (subHead == -1) {
            subHead = head;
         }
         high++;
         modCount = getModCount();
         return true;
      }

      @Override
      public void add(int idx, E e) {
         if (idx == high - low) {
            add(e);
         }
         else {
            checkWide(idx);
            checkMod(modCount);
            ArrayBackedLinkedList.this.add(low + idx, e);
            high++;
            modCount = getModCount();
         }
      }

      @Override
      public boolean addAll(Collection<? extends E> coll) {
         return addAll(high - low, coll);
      }

      @Override
      public boolean addAll(int idx, Collection<? extends E> coll) {
         checkWide(idx);
         checkMod(modCount);
         if (coll.size() == 0)
            return false;
         for (E e : coll) {
            add(idx++, e);
         }
         modCount = getModCount();
         return true;
      }

      private void check(int index) {
         if (index >= high - low) {
            throw new IndexOutOfBoundsException("" + index + " >= " + (high - low));
         }
         else if (index < 0) {
            throw new IndexOutOfBoundsException("" + index + " < 0");
         }
      }

      private void checkWide(int index) {
         if (index > high - low) {
            throw new IndexOutOfBoundsException("" + index + " > " + (high - low));
         }
         else if (index < 0) {
            throw new IndexOutOfBoundsException("" + index + " < 0");
         }
      }

      @Override
      public void clear() {
         checkMod(modCount);
         super.clear();
         modCount = getModCount();
      }

      @Override
      public boolean contains(Object item) {
         checkMod(modCount);
         return super.contains(item);
      }

      @Override
      public boolean containsAll(Collection<?> coll) {
         checkMod(modCount);
         return super.containsAll(coll);
      }

      @SuppressWarnings("unchecked")
      @Override
      public E get(int idx) {
         check(idx);
         checkMod(modCount);
         return (E) data[find(idx + low)];
      }

      @Override
      public int indexOf(Object o) {
         checkMod(modCount);
         return super.indexOf(o);
      }

      @Override
      public boolean isEmpty() {
         checkMod(modCount);
         return high == low;
      }

      @Override
      public Iterator<E> iterator() {
         checkMod(modCount);
         return new SubListIteratorImpl(this);
      }

      @Override
      public int lastIndexOf(Object o) {
         checkMod(modCount);
         return super.lastIndexOf(o);
      }

      @Override
      public ListIterator<E> listIterator() {
         checkMod(modCount);
         return new SubListIteratorImpl(this);
      }

      @Override
      public ListIterator<E> listIterator(int idx) {
         checkMod(modCount);
         checkWide(idx);
         return new SubListIteratorImpl(this, low + idx - 1);
      }

      @Override
      public E remove(int idx) {
         checkMod(modCount);
         check(idx);
         high--;
         subTail = prev[subTail];
         E ret = ArrayBackedLinkedList.this.remove(low + idx);
         modCount = getModCount();
         return ret;
      }

      @Override
      public boolean remove(Object o) {
         checkMod(modCount);
         if (super.remove(o)) {
            modCount = getModCount();
            return true;
         }
         return false;
      }

      @Override
      public boolean removeAll(Collection<?> coll) {
         checkMod(modCount);
         if (super.removeAll(coll)) {
            modCount = getModCount();
            return true;
         }
         return false;
      }

      @Override
      public boolean retainAll(Collection<?> coll) {
         checkMod(modCount);
         if (super.retainAll(coll)) {
            modCount = getModCount();
            return true;
         }
         return false;
      }

      @Override
      public E set(int idx, E e) {
         checkMod(modCount);
         check(idx);
         int pos = find(idx + low);
         @SuppressWarnings("unchecked")
         E ret = (E) data[pos];
         data[pos] = e;
         return ret;
      }

      @Override
      public int size() {
         checkMod(modCount);
         return high - low;
      }

      @Override
      public List<E> subList(int from, int to) {
         checkMod(modCount);
         checkWide(from);
         checkWide(to);
         if (from > to) {
            throw new IllegalArgumentException("from > to");
         }
         return new SubListImpl(low + from, low + to);
      }

      @Override
      public Object[] toArray() {
         checkMod(modCount);
         int sz = high - low;
         Object ret[] = new Object[sz];
         if (isOptimized) {
            System.arraycopy(data, low, ret, 0, sz);
         }
         else {
            int i = 0;
            for (E e : this) {
               ret[i++] = e;
            }
         }
         return ret;
      }

      @SuppressWarnings("unchecked")
      @Override
      public <T> T[] toArray(T[] array) {
         checkMod(modCount);
         int sz = high - low;
         if (array.length < sz) {
            array = (T[]) Array.newInstance(array.getClass().getComponentType(), sz);
         }
         if (isOptimized) {
            System.arraycopy(data, low, array, 0, sz);
         }
         else {
            int i = 0;
            for (E e : this) {
               array[i++] = (T) e;
            }
         }
         if (array.length > sz) {
            array[sz] = null;
         }
         return array;
      }
      
      int getSubListModCount() {
         return modCount;
      }
      
      void resetSubListModCount() {
         this.modCount = getModCount();
      }
   }

   /**
    * An implementation of {@code ListIterator} for sub-lists. This class is used for iterators for
    * sub-lists returned from {@link ArrayBackedLinkedList#subList(int, int)}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class SubListIteratorImpl extends IteratorImpl {

      /**
       * A reference to the sub-list over whose elements we iterate.
       */
      private final SubListImpl sublist;

      /**
       * Creates an iterator for a subset of the list starting at the first element.
       * 
       * @param sublist the sub-list over whose items we iterate
       */
      SubListIteratorImpl(SubListImpl sublist) {
         super(sublist.low - 1);
         this.sublist = sublist;
         // at same rev as source sub-list
         myModCount = sublist.getSubListModCount();
      }

      /**
       * Creates an iterator for a subset of the list starting at the specified element.
       * 
       * @param sublist the sub-list over whose items we iterate
       * @param idx the list index (into the main list, not into the sub-list) for the element
       *           <em>before</em> the first element returned by {@code next()}
       */
      SubListIteratorImpl(SubListImpl sublist, int idx) {
         super(idx);
         this.sublist = sublist;
         // at same rev as source sub-list
         myModCount = sublist.getSubListModCount();
      }

      @Override
      void resetModCount() {
         myModCount = getModCount();
         // update sublist, too, so sublist doesn't throw concurrent modifications from add/remove
         // operations from its iterator
         sublist.resetSubListModCount();
      }
      
      @Override
      public void add(E e) {
         super.add(e);
         if (idx == sublist.high) {
            if (sublist.subTail == -1) {
               sublist.subTail = head;
            }
            else {
               sublist.subTail = next[sublist.subTail];
            }
         }
         if (idx == sublist.low) {
            if (sublist.subHead == -1) {
               sublist.subHead = head;
            }
            else {
               sublist.subHead = prev[sublist.subHead];
            }
         }
         sublist.high++;
      }

      @Override
      void dec() {
         if (idx < sublist.low) {
            throw new NoSuchElementException("At beginning of list");
         }
         super.dec();
      }

      @Override
      public boolean hasNext() {
         return idx != sublist.high - 1;
      }

      @Override
      public boolean hasPrevious() {
         return idx >= sublist.low;
      }

      @Override
      void inc() {
         if (idx == sublist.high - 1) {
            throw new NoSuchElementException("At end of list");
         }
         super.inc();
      }

      @Override
      public int nextIndex() {
         return idx - sublist.low + 1;
      }

      @Override
      public int previousIndex() {
         return idx - sublist.low;
      }

      @Override
      public void remove() {
         int newHead = sublist.subHead;
         if (lastFetched == newHead && lastFetched != -1)
            newHead = next[newHead];
         int newTail = sublist.subTail;
         if (lastFetched == newTail && lastFetched != -1)
            newTail = prev[newTail];
         super.remove();
         sublist.subTail = newTail;
         sublist.subHead = newHead;
         sublist.high--;
      }
   }

   /**
    * A simple iterator that walks the internal storage buffer and returns items in the order they
    * are organized, not necessarily ordered by their list index. This is to be used for cases where
    * order of visiting nodes does not matter and is used internally to implement
    * {@link #removeAll(Collection)}, {@link #retainAll(Collection)}, and
    * {@link #containsAll(Collection)}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class UnorderedIteratorImpl implements Iterator<E> {

      private IteratorModifiedState modState = IteratorModifiedState.NONE;
      private int lastFetched = -1;
      private int idx = -1;
      private int myModCount;

      UnorderedIteratorImpl() {
         myModCount = getModCount();
         advance();
      }

      private void advance() {
         while (++idx < highWater && next[idx] == -1 && prev[idx] == -1);
      }

      @Override
      public boolean hasNext() {
         checkMod(myModCount);
         return idx < highWater;
      }

      @Override
      public E next() {
         checkMod(myModCount);
         if (idx >= highWater) {
            throw new NoSuchElementException("At end of list");
         }
         lastFetched = idx;
         @SuppressWarnings("unchecked")
         E ret = (E) data[idx];
         advance();
         modState = IteratorModifiedState.NONE;
         return ret;
      }

      @Override
      public void remove() {
         if (lastFetched == -1) {
            throw new IllegalStateException(
                  "No element to remove: next() never called");
         }
         if (modState == IteratorModifiedState.REMOVED) {
            throw new IllegalStateException(
                  "Cannot remove item after call to remove()");
         }
         checkMod(myModCount);
         modState = IteratorModifiedState.REMOVED;
         removeInternal(lastFetched);
         myModCount = getModCount();
      }
   }

   private static final long serialVersionUID = -4604439572205925230L;
   
   private static final int DEFAULT_INITIAL_CAPACITY = 10;
   
   /**
    * The buffer in which list elements are stored.
    */
   transient Object[] data;

   /**
    * The array of indexes which point from a given index in the buffer to its next element in the
    * list.
    */
   transient int[] next;

   /**
    * The array of indexes which point from a given index in the buffer to its previous element in
    * the list.
    */
   transient int[] prev;

   /**
    * The number of items currently in the list.
    */
   int size;

   /**
    * The index in the buffer of the first element in the list.
    */
   transient int head;

   /**
    * The index in the buffer of the last element in the list.
    */
   transient int tail;

   /**
    * The high water point in the buffer.
    */
   transient int highWater;

   /**
    * The index in the buffer of the last element removed from the list. It may also be set to the
    * last gap found when an insertion operation tries to "fill in the gaps" caused from
    * fragmentation before choosing to grow the buffer.
    */
   private transient int lastRemove;

   /**
    * A flag indicating whether the current buffer is optimized or not. If it is optimized then all
    * items are stored in order, just like in the buffer of an {@code java.util.ArrayList}.
    */
   transient boolean isOptimized = true;

   /**
    * Constructs a new empty list.
    */
   public ArrayBackedLinkedList() {
      this(DEFAULT_INITIAL_CAPACITY);
   }

   /**
    * Constructs a new list with the specified contents. The order of items in the list will be the
    * same as the order they are returned from the specified collection's {@code Iterator}.
    * 
    * @param coll collection of items that will be included in the new list
    */
   public ArrayBackedLinkedList(Collection<? extends E> coll) {
      data = coll.toArray();
      size = data.length;
      if (!data.getClass().getComponentType().equals(Object.class)) {
         data = Arrays.copyOf(data, size, Object[].class);
      }
      next = new int[size];
      prev = new int[size];
      for (int i = 0; i < size; i++) {
         int nextIdx = i + 1;
         next[i] = nextIdx >= size ? -1 : nextIdx;
         prev[i] = i - 1;
      }
      tail = size - 1;
      highWater = size;
   }

   /**
    * Constructs a new empty list with buffers sized as specified.
    * 
    * @param initialCapacity the size of the internal buffers
    */
   public ArrayBackedLinkedList(int initialCapacity) {
      if (initialCapacity < 0) {
         throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
      }
      data = new Object[initialCapacity];
      next = new int[initialCapacity];
      prev = new int[initialCapacity];
      Arrays.fill(next, -1);
      Arrays.fill(prev, -1);
      head = tail = -1;
   }

   /** {@inheritDoc} */
   @Override
   public boolean add(E e) {
      modCount++;
      int pos = findEmpty();
      data[pos] = e;
      prev[pos] = tail;
      if (tail != -1) {
         next[tail] = pos;
      }
      else {
         head = pos;
      }
      tail = pos;
      size++;
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public void add(int index, E e) {
      if (index == size) {
         add(e);
      }
      else {
         check(index);
         addInternal(find(index), e);
      }
   }

   /** {@inheritDoc} */
   @Override
   public boolean addAll(Collection<? extends E> coll) {
      maybeGrowBy(coll.size());
      return super.addAll(coll);
   }

   /** {@inheritDoc} */
   @Override
   public boolean addAll(int index, Collection<? extends E> coll) {
      maybeGrowBy(coll.size());
      return super.addAll(index, coll);
   }

   /** {@inheritDoc} */
   @Override
   public void addFirst(E e) {
      add(0, e);
   }

   /**
    * Adds an item the specified position in the buffer.
    * 
    * @param index a raw index into the buffer where the new item should be added
    * @param e the item to add to the list
    * @return the raw index of the newly added item
    */
   int addInternal(int index, E e) {
      modCount++;
      isOptimized = false;
      int pos = findEmpty();
      data[pos] = e;
      next[pos] = index;
      prev[pos] = prev[index];
      if (head == index) {
         head = pos;
      }
      else {
         next[prev[index]] = pos;
      }
      prev[index] = pos;
      size++;
      return pos;
   }

   /** {@inheritDoc} */
   @Override
   public void addLast(E e) {
      add(e);
   }

   /**
    * Returns the current list as an instance of {@code RandomAccess}. The returned list will not be
    * a copy of this list or a new list but a view into the list.
    * 
    * <p>
    * Note that structural modifications, like inserting and removing items, should only be done
    * through this interface. Such changes made via the main {@code ArrayBackedLinkedList} instance
    * methods can invalidate the <em>O(1)</em> random access nature of the underlying buffer. This
    * list implements a "fail fast" strategy and will raise a
    * {@code ConcurrentModificationException} exception if such a case is detected.
    * 
    * <p>
    * Also note that the main use for the returned list should be for operations that do
    * <em>not</em> change the structure since such operations could invalidate the internal storage
    * characteristics that provide <em>O(1)</em> random access. These operations, adding an item
    * anywhere in the list other than the end and removing an item in the list (not from the end),
    * are thus not supported by the returned list and will result in
    * {@code UnsupportedOperationException}s being thrown.
    * 
    * @param optimize true if the internal storage of this list should be optimized if necessary
    *           (could be a costly operation)
    * @return a version of this list that implements {@code RandomAccess} or {@code null} if
    *         {@code optimize} is false and the list is not already optimized
    */
   public List<E> asRandomAccess(boolean optimize) {
      if (!optimize && !isOptimized) {
         return null;
      }
      if (!isOptimized) {
         optimize();
      }
      return new RandomAccessImpl();
   }

   /**
    * Checks that the specified list index is valid. The list index is invalid if it is less than
    * zero or greater than or equal to the list size. If it is invalid then an
    * {@code IndexOutOfBoundsException} is thrown.
    * 
    * @param index list index to check
    * @throws IndexOutOfBoundsException if the list index is invalid
    */
   private void check(int index) {
      if (index >= size) {
         throw new IndexOutOfBoundsException("" + index + " >= " + size);
      }
      else if (index < 0) {
         throw new IndexOutOfBoundsException("" + index + " < 0");
      }
   }

   /**
    * Checks the modification level of the list. If the current level does not match the specified
    * level then a {@code ConcurrentModificationException} is thrown.
    * 
    * @param aModCount modification level
    * @throws ConcurrentModificationException if the list's modification level is different than the
    *            specified level
    */
   void checkMod(int aModCount) {
      if (aModCount != modCount) {
         throw new ConcurrentModificationException();
      }
   }
   
   /**
    * Gets the list's modification count. This method would have to be synthesized if not provided
    * since enclosed classes that are not sub-classes do not otherwise have access to protected
    * members.
    * 
    * @return the list's modification count
    */
   int getModCount() {
      return modCount;
   }

   /**
    * Checks that the specified list index is valid. The method differs from {@code check} in that
    * it allows a list index that is equal to the size of the list.
    * 
    * @param index list index to check
    * @throws IndexOutOfBoundsException if the list index is invalid
    */
   private void checkWide(int index) {
      if (index > size) {
         throw new IndexOutOfBoundsException("" + index + " > " + size);
      }
      else if (index < 0) {
         throw new IndexOutOfBoundsException("" + index + " < 0");
      }
   }

   /** {@inheritDoc} */
   @Override
   public void clear() {
      modCount++;
      lastRemove = 0;
      highWater = 0;
      size = 0;
      head = -1;
      tail = -1;
      Arrays.fill(data, null);
      Arrays.fill(next, -1);
      Arrays.fill(prev, -1);
      isOptimized = true;
   }

   /** {@inheritDoc} */
   @Override
   public ArrayBackedLinkedList<E> clone() {
      if (this.getClass() == ArrayBackedLinkedList.class) {
         // don't bother cloning internal state - just create a new optimized list
         return new ArrayBackedLinkedList<E>(this);
      }
      try {
         @SuppressWarnings("unchecked")
         ArrayBackedLinkedList<E> copy = (ArrayBackedLinkedList<E>) super.clone();
         // deep copy the arrays
         copy.data = data.clone();
         copy.next = next.clone();
         copy.prev = prev.clone();
         // now sub-class can do whatever else with this...
         return copy;
      }
      catch (CloneNotSupportedException e) {
         // should never happen since we implement Cloneable -- but just in
         // case, wrap in a runtime exception that sort of makes sense...
         throw new ClassCastException(Cloneable.class.getName());
      }
   }

   /**
    * Compacts the internal buffers for the list. This is like {@code trimToSize()} except that it
    * first rearranges items to fill in any gaps in the buffers caused by fragmentation.
    */
   public void compact() {
      if (highWater != size) {
         modCount++;
         int pos = 0;
         while (highWater > size) {
            // find next empty slot
            while (next[pos] != -1 || prev[pos] != -1)
               pos++;
            // move item at end of the array to empty slot
            highWater--;
            int nextPos = next[highWater];
            int prevPos = prev[highWater];
            next[pos] = nextPos;
            prev[pos] = prevPos;
            data[pos] = data[highWater];
            next[highWater] = -1;
            prev[highWater] = -1;
            data[highWater] = null;
            if (prevPos != -1)
               next[prevPos] = pos;
            if (nextPos != -1)
               prev[nextPos] = pos;
            if (head == highWater)
               head = pos;
            if (tail == highWater)
               tail = pos;
         }
         lastRemove = 0;
      }
      trimToSize();
   }

   /** {@inheritDoc} */
   @Override
   public Iterator<E> descendingIterator() {
      return reverseIterator(size);
   }

   /** {@inheritDoc} */
   @Override
   public E element() {
      if (size > 0) {
         @SuppressWarnings("unchecked")
         E ret = (E) data[head];
         return ret;
      }
      else {
         throw new NoSuchElementException("List is empty");
      }
   }

   /**
    * Grows the internal buffers of the list, if necessary, to accommodate the specified number of
    * items.
    * 
    * @param capacity number of items that need to fit in the list
    */
   public void ensureCapacity(int capacity) {
      if (data.length < capacity) {
         growTo(capacity);
      }
   }

   /**
    * Determines the raw index into the buffer for the specified list index.
    * 
    * @param index list index
    * @return raw index or -1 if the specified list index is not valid
    */
   int find(int index) {
      if (index < 0) {
         return -1;
      }
      if (isOptimized) {
         return index;
      }
      int cur = head;
      while (cur != -1 && index > 0) {
         cur = next[cur];
         index--;
      }
      return cur;
   }

   /**
    * Finds an available spot in the buffer for adding a new item.
    * 
    * @return next raw index in the buffer that is empty
    */
   private int findEmpty() {
      maybeGrow();
      int pos;
      if (highWater == data.length) {
         // no room at end of array - find empty space
         while (next[lastRemove] != -1 || prev[lastRemove] != -1) {
            lastRemove++;
            // wrap back to beginning of array
            if (lastRemove == highWater) {
               lastRemove = 0;
            }
         }
         pos = lastRemove++;
      }
      else {
         pos = highWater++;
      }
      return pos;
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public E get(int index) {
      check(index);
      return (E) data[find(index)];
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public E getFirst() {
      if (size == 0) {
         throw new NoSuchElementException("List is empty");
      }
      return (E) data[head];
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public E getLast() {
      if (size == 0) {
         throw new NoSuchElementException("List is empty");
      }
      return (E) data[tail];
   }

   private void growTo(int newSize) {
      modCount++;
      Object oldData[] = data;
      int oldNext[] = next;
      int oldPrev[] = prev;
      data = new Object[newSize];
      next = new int[newSize];
      prev = new int[newSize];
      System.arraycopy(oldData, 0, data, 0, oldData.length);
      System.arraycopy(oldNext, 0, next, 0, oldNext.length);
      System.arraycopy(oldPrev, 0, prev, 0, oldPrev.length);
      Arrays.fill(next, oldNext.length, next.length, -1);
      Arrays.fill(prev, oldPrev.length, prev.length, -1);
   }

   /** {@inheritDoc} */
   @Override
   public Iterator<E> iterator() {
      return new IteratorImpl();
   }

   /** {@inheritDoc} */
   @Override
   public ListIterator<E> listIterator() {
      return new IteratorImpl();
   }

   /** {@inheritDoc} */
   @Override
   public ListIterator<E> listIterator(int idx) {
      checkWide(idx);
      // don't waste O(n) time getting the raw index if we
      // know it's the tail
      if (idx == size)
         return new IteratorImpl(tail, idx - 1);
      else
         return new IteratorImpl(idx - 1);
   }

   private void maybeGrow() {
      int prevLen = data.length;
      if (prevLen > size)
         return;
      int len = prevLen << 1;
      // avoid overflow
      if (len <= prevLen) {
         len = Integer.MAX_VALUE - 8;
      }
      growTo(len);
   }

   private void maybeGrowBy(int moreElements) {
      int prevLen = data.length;
      int newLen = prevLen + moreElements;
      if (prevLen >= newLen)
         return;
      int len = prevLen << 1;
      while (len < newLen && prevLen < len) {
         len <<= 1;
      }
      // avoid overflow
      if (len <= prevLen) {
         len = Integer.MAX_VALUE - 8;
      }
      growTo(len);
   }

   /** {@inheritDoc} */
   @Override
   public boolean offer(E e) {
      return add(e);
   }

   /** {@inheritDoc} */
   @Override
   public boolean offerFirst(E e) {
      add(0, e);
      return true;
   }

   /** {@inheritDoc} */
   @Override
   public boolean offerLast(E e) {
      return add(e);
   }

   /**
    * Rearranges storage in internal buffers to optimize traversal and minimize storage size (to
    * reduce memory pressure). This basically orders the items internally into iteration order and
    * updates corresponding {@code next} and {@code prev} pointers correspondingly. It also
    * defragments the internal buffer and trims it so there are no empty slots in the buffer.
    * 
    * <p>
    * This is generally an <em>O(n)</em> operation and should only need to be done if there are many
    * random access removals and/or insertion (which could cause the buffer to become unordered per
    * iteration order and cause fragmentation).
    */
   public void optimize() {
      if (isOptimized) {
         // no internal re-arranging needed so
         // just trim the storage
         trimToSize();
         return;
      }
      // similar to compact(), but rearranges contents so that array buffer
      // contains items in their list order for best performance iteration.
      // if list is heavily fragmented then this will perform better than
      // compact()
      modCount++;
      Object oldData[] = data;
      int oldNext[] = next;
      data = new Object[size];
      next = new int[size];
      prev = new int[size];
      highWater = size;
      for (int pos = head, idx = 0; pos != -1; pos = oldNext[pos], idx++) {
         data[idx] = oldData[pos];
         next[idx] = idx + 1;
         prev[idx] = idx - 1;
         idx++;
      }
      head = 0;
      tail = size - 1;
      lastRemove = 0;
      isOptimized = true;
   }

   /** {@inheritDoc} */
   @Override
   public E peek() {
      if (size > 0) {
         @SuppressWarnings("unchecked")
         E ret = (E) data[head];
         return ret;
      }
      else {
         return null;
      }
   }

   /** {@inheritDoc} */
   @Override
   public E peekFirst() {
      return peek();
   }

   /** {@inheritDoc} */
   @Override
   public E peekLast() {
      if (size > 0) {
         @SuppressWarnings("unchecked")
         E ret = (E) data[tail];
         return ret;
      }
      else {
         return null;
      }
   }

   /** {@inheritDoc} */
   @Override
   public E poll() {
      if (size > 0) {
         @SuppressWarnings("unchecked")
         E ret = (E) data[head];
         removeInternal(head);
         return ret;
      }
      else {
         return null;
      }
   }

   /** {@inheritDoc} */
   @Override
   public E pollFirst() {
      return poll();
   }

   /** {@inheritDoc} */
   @Override
   public E pollLast() {
      if (size > 0) {
         @SuppressWarnings("unchecked")
         E ret = (E) data[tail];
         removeInternal(tail);
         return ret;
      }
      else {
         return null;
      }
   }

   /** {@inheritDoc} */
   @Override
   public E pop() {
      return removeFirst();
   }

   @Override
   public void push(E e) {
      addFirst(e);
   }

   /**
    * Customizes de-serialization to read list of elements same way as written by
    * {@link #writeObject(ObjectOutputStream)}.
    * 
    * @param in the stream from which the list is read
    * @throws IOException if an exception is raised when reading from {@code in}
    * @throws ClassNotFoundException if de-serializing an element fails to locate the element's
    *            class
    */
   private void readObject(ObjectInputStream in) throws IOException,
         ClassNotFoundException {
      in.defaultReadObject();
      data = new Object[size];
      next = new int[size];
      prev = new int[size];
      for (int i = 0; i < size; i++) {
         data[i] = in.readObject();
         int nextIdx = i + 1;
         next[i] = nextIdx >= size ? -1 : nextIdx;
         prev[i] = i - 1;
      }
      tail = size - 1;
      highWater = size;
      isOptimized = true;
   }

   /** {@inheritDoc} */
   @Override
   public E remove() {
      if (size > 0) {
         @SuppressWarnings("unchecked")
         E ret = (E) data[head];
         removeInternal(head);
         return ret;
      }
      else {
         throw new NoSuchElementException("List is empty");
      }
   }

   /** {@inheritDoc} */
   @Override
   public E remove(int idx) {
      check(idx);
      int pos = find(idx);
      @SuppressWarnings("unchecked")
      E ret = (E) data[pos];
      removeInternal(pos);
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public E removeFirst() {
      return remove();
   }

   /** {@inheritDoc} */
   @Override
   public boolean removeFirstOccurrence(Object item) {
      return remove(item);
   }

   void removeInternal(int index) {
      modCount++;

      data[index] = null; // allow gc
      int nextIdx = next[index];
      int prevIdx = prev[index];
      if (prevIdx != -1) {
         next[prevIdx] = nextIdx;
      }
      if (nextIdx != -1) {
         prev[nextIdx] = prevIdx;
      }
      if (head == index) {
         head = nextIdx;
      }
      if (tail == index) {
         tail = prevIdx;
      }
      next[index] = -1;
      prev[index] = -1;

      if (index == highWater - 1) {
         // compute new highWater
         while (index >= 0 && prev[index] == -1 && next[index] == -1)
            index--;
         highWater = index + 1;
         lastRemove = 0;
      }
      else {
         lastRemove = index;
         isOptimized = false;
      }
      size--;
   }

   /** {@inheritDoc} */
   @Override
   public E removeLast() {
      return remove(size - 1);
   }

   /** {@inheritDoc} */
   @Override
   public boolean removeLastOccurrence(Object item) {
      if (size == 0) return false;
      return CollectionUtils.removeObject(item, reverseIterator(size), true);
   }

   private ListIterator<E> reverseIterator(int idx) {
      return CollectionUtils.reverseIterator(listIterator(idx));
   }

   /** {@inheritDoc} */
   @Override
   public E set(int idx, E e) {
      check(idx);
      int pos = find(idx);
      @SuppressWarnings("unchecked")
      E ret = (E) data[pos];
      data[pos] = e;
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public int size() {
      return size;
   }

   /**
    * Sorts the list. This is slightly more efficient than {@code Collections.sort(List)} and has
    * the side benefit of optimizing internal storage of the items for subsequent iteration.
    * 
    * <p>
    * The values must be mutually comparable or else a {@code RuntimeException} (such as
    * {@code ClassCastException}) may be thrown.
    */
   public void sort() {
      modCount++;
      compact();
      Arrays.sort(data);
      for (int i = 0; i < size; i++) {
         next[i] = i + 1;
         prev[i] = i - 1;
      }
      isOptimized = true;
   }

   /**
    * Sorts the list. This is slightly more efficient than {@code Collections.sort(List)} and has
    * the side benefit of optimizing internal storage of the items for subsequent iteration.
    * 
    * <p>
    * This uses the specified comparator to compare and order items.
    * 
    * @param c the comparator used to define the sort order
    */
   @SuppressWarnings("unchecked")
   public void sort(Comparator<? super E> c) {
      modCount++;
      compact();
      Arrays.sort((E[]) data, c);
      for (int i = 0; i < size; i++) {
         next[i] = i + 1;
         prev[i] = i - 1;
      }
      isOptimized = true;
   }

   /** {@inheritDoc} */
   @Override
   public List<E> subList(int from, int to) {
      check(from);
      checkWide(to);
      if (from > to) {
         throw new IllegalArgumentException("from > to");
      }
      return new SubListImpl(from, to);
   }

   /** {@inheritDoc} */
   @Override
   public Object[] toArray() {
      Object ret[] = new Object[size];
      if (isOptimized) {
         System.arraycopy(data, 0, ret, 0, size);
      }
      else {
         CollectionUtils.copyToArray(this, ret);
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public <T> T[] toArray(T[] array) {
      array = ArrayUtils.ensureCapacity(array, size);
      if (isOptimized) {
         System.arraycopy(data, 0, array, 0, size);
      }
      else {
         CollectionUtils.copyToArray(this, array);
      }
      if (array.length > size) {
         array[size] = null;
      }
      return array;
   }

   /**
    * Shrinks the internal buffer as much as possible without performing any other more aggressive
    * reorganization. This may not actually free much (or even any) space, even when the internal
    * buffer is sized much large than the actual number of items. This could be the case due to
    * fragmentation of the buffer. Use {@link #compact()} or {@link #optimize()} for more aggressive
    * management of internal buffers.
    */
   public void trimToSize() {
      if (data.length != highWater) {
         modCount++;
         data = Arrays.copyOf(data, highWater);
         next = Arrays.copyOf(next, highWater);
         prev = Arrays.copyOf(prev, highWater);
      }
   }

   /**
    * Creates an iterator that provides maximum performance iteration. The iteration, however, may
    * not actually return elements in the order of their list index. This is useful for providing a
    * slight performance increase to operations that will neeed to operate on every item in the
    * list, regardless of order.
    * 
    * @return an iterator whose order of items may not match the list order of items but whose
    *         traversal may perform more efficiently
    */
   public Iterator<E> unorderedIterator() {
      return new UnorderedIteratorImpl();
   }

   /**
    * Customizes serialization by just writing the list contents in order.
    * 
    * @param out the stream to which to serialize this list
    * @throws IOException if an exception is raised when writing to {@code out}
    */
   private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();
      for (E e : this) {
         out.writeObject(e);
      }
   }
}
