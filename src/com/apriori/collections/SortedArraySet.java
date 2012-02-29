package com.apriori.collections;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;

public class SortedArraySet<E> implements NavigableSet<E> {

   private static final int DEFAULT_INITIAL_CAPACITY = 10;
   
   private static final int THRESHOLD_FOR_BULK_OP = 100;

   private class IteratorImpl implements Iterator<E> {

      private int myModCount = modCount;
      private int start;
      private int limit;
      private int idx;
      private boolean removed = false;

      IteratorImpl() {
         this(0, size);
      }

      IteratorImpl(int start, int limit) {
         this.start = start;
         this.limit = limit;
         idx = start;
      }

      @Override
      public boolean hasNext() {
         checkMod(myModCount);
         return idx < limit;
      }

      @Override
      public E next() {
         // check/throw
         checkMod(myModCount);
         removed = false;
         @SuppressWarnings("unchecked")
         E ret = (E) data[idx++];
         return ret;
      }

      @Override
      public void remove() {
         if (removed) {
            // TODO throw
         }
         else if (idx == start) {
            // TODO throw
         }
         checkMod(myModCount);
         removeItem(--idx);
         myModCount = modCount;
         removed = true;
      }
   }

   private class DescendingIteratorImpl implements Iterator<E> {

      private int myModCount = modCount;
      private int start;
      private int limit;
      private int idx = size - 1;
      private boolean removed = false;

      DescendingIteratorImpl() {
         this(size, 0);
      }

      DescendingIteratorImpl(int start, int limit) {
         this.start = start;
         this.limit = limit;
         idx = start;
      }

      @Override
      public boolean hasNext() {
         checkMod(myModCount);
         return idx >= limit;
      }

      @Override
      public E next() {
         // check/throw
         checkMod(myModCount);
         removed = false;
         @SuppressWarnings("unchecked")
         E ret = (E) data[idx--];
         return ret;
      }

      @Override
      public void remove() {
         if (removed) {
            // TODO throw
         }
         else if (idx == start - 1) {
            // TODO throw
         }
         checkMod(myModCount);
         removeItem(idx + 1);
         myModCount = modCount;
         removed = true;
      }
   }

   private static class DescendingSetImpl<E> implements NavigableSet<E> {

      private NavigableSet<E> base;

      public DescendingSetImpl(NavigableSet<E> base) {
         this.base = base;
      }

      @Override
      public Comparator<? super E> comparator() {
         final Comparator<? super E> comp = base.comparator();
         if (comp == null) {
            return null;
         }
         return new Comparator<E>() {
            @Override
            public int compare(E o1, E o2) {
               // reverse it
               return comp.compare(o2, o1);
            }
         };
      }

      @Override
      public E first() {
         return base.last();
      }

      @Override
      public E last() {
         return base.first();
      }

      @Override
      public boolean add(E e) {
         return base.add(e);
      }

      @Override
      public boolean addAll(Collection<? extends E> c) {
         return base.addAll(c);
      }

      @Override
      public void clear() {
         base.clear();
      }

      @Override
      public boolean contains(Object o) {
         return base.contains(o);
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return base.containsAll(c);
      }

      @Override
      public boolean isEmpty() {
         return base.isEmpty();
      }

      @Override
      public boolean remove(Object o) {
         return base.remove(o);
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         return base.removeAll(c);
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return base.retainAll(c);
      }

      @Override
      public int size() {
         return base.size();
      }

      private void reverseArray(Object[] a) {
         for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            Object tmp = a[j];
            a[j] = a[i];
            a[i] = tmp;
         }
      }

      @Override
      public Object[] toArray() {
         Object ret[] = base.toArray();
         reverseArray(ret);
         return ret;
      }

      @Override
      public <T> T[] toArray(T[] a) {
         T ret[] = base.toArray(a);
         reverseArray(ret);
         return ret;
      }

      @Override
      public E ceiling(E e) {
         return base.floor(e);
      }

      @Override
      public Iterator<E> descendingIterator() {
         return base.iterator();
      }

      @Override
      public NavigableSet<E> descendingSet() {
         return base;
      }

      @Override
      public E floor(E e) {
         return base.ceiling(e);
      }

      @Override
      public NavigableSet<E> headSet(E toElement) {
         return new DescendingSetImpl<E>((NavigableSet<E>) base.tailSet(toElement));
      }

      @Override
      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         return new DescendingSetImpl<E>(base.tailSet(toElement, inclusive));
      }

      @Override
      public E higher(E e) {
         return base.lower(e);
      }

      @Override
      public Iterator<E> iterator() {
         return base.descendingIterator();
      }

      @Override
      public E lower(E e) {
         return base.higher(e);
      }

      @Override
      public E pollFirst() {
         return base.pollLast();
      }

      @Override
      public E pollLast() {
         return base.pollFirst();
      }

      @Override
      public NavigableSet<E> subSet(E fromElement, E toElement) {
         return new DescendingSetImpl<E>((NavigableSet<E>) base.subSet(toElement, fromElement));
      }

      @Override
      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
            boolean toInclusive) {
         return new DescendingSetImpl<E>(base.subSet(toElement, toInclusive, fromElement,
               fromInclusive));
      }

      @Override
      public NavigableSet<E> tailSet(E fromElement) {
         return new DescendingSetImpl<E>((NavigableSet<E>) base.headSet(fromElement));
      }

      @Override
      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         return new DescendingSetImpl<E>(base.headSet(fromElement, inclusive));
      }
   }

   private class SubSetImpl implements NavigableSet<E> {
      
      public SubSetImpl(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
         // TODO
      }

      /** {@inheritDoc} */
      @Override
      public Comparator<? super E> comparator() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E first() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E last() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public boolean add(E arg0) {
         // TODO implement me
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public boolean addAll(Collection<? extends E> arg0) {
         // TODO implement me
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public void clear() {
         // TODO implement me
         
      }

      /** {@inheritDoc} */
      @Override
      public boolean contains(Object arg0) {
         // TODO implement me
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public boolean containsAll(Collection<?> arg0) {
         // TODO implement me
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public boolean isEmpty() {
         // TODO implement me
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public boolean remove(Object arg0) {
         // TODO implement me
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public boolean removeAll(Collection<?> arg0) {
         // TODO implement me
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public boolean retainAll(Collection<?> arg0) {
         // TODO implement me
         return false;
      }

      /** {@inheritDoc} */
      @Override
      public int size() {
         // TODO implement me
         return 0;
      }

      /** {@inheritDoc} */
      @Override
      public Object[] toArray() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public <T> T[] toArray(T[] arg0) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E ceiling(E e) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public Iterator<E> descendingIterator() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> descendingSet() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E floor(E e) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> headSet(E toElement) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E higher(E e) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public Iterator<E> iterator() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E lower(E e) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E pollFirst() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public E pollLast() {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> subSet(E fromElement, E toElement) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
            boolean toInclusive) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public SortedSet<E> tailSet(E fromElement) {
         // TODO implement me
         return null;
      }

      /** {@inheritDoc} */
      @Override
      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         // TODO implement me
         return null;
      }
      
   }
   
   Object data[];
   int size;
   private Comparator<? super E> comp;
   int modCount;

   public SortedArraySet() {
      this(DEFAULT_INITIAL_CAPACITY, null);
   }

   public SortedArraySet(Comparator<? super E> comp) {
      this(DEFAULT_INITIAL_CAPACITY, comp);
   }

   public SortedArraySet(int initialCapacity) {
      this(initialCapacity, null);
   }

   public SortedArraySet(int initialCapacity, Comparator<? super E> comp) {
      if (initialCapacity < 0) {
         throw new IllegalArgumentException("initialCapacity should not be negative");
      }
      data = new Object[initialCapacity];
      size = 0;
      this.comp = comp;
   }

   public SortedArraySet(Collection<? extends E> source) {
      this(source, null);
   }

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
   
   @SuppressWarnings("unchecked")
   private void removeDups() {
      int numUnique = 0;
      for (int i = 1; i < size; i++) {
         int c;
         if (comp == null) {
            c = ((Comparable<Object>) data[i]).compareTo(data[numUnique]);
         } else {
            c = comp.compare((E) data[i], (E) data[numUnique]);
         }
         if (c != 0) {
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

   @SuppressWarnings("unchecked")
   private int findIndex(Object o) {
      // binary search
      int lo = 0;
      int hi = size - 1;
      while (true) {
         int mid = (hi + lo) >> 1;
         int c;
         if (comp == null) {
            c = ((Comparable<Object>) data[mid]).compareTo(o);
         } else {
            c = comp.compare((E) data[mid], (E) o);
         }
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

   private void insertItem(E element, int index) {
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
      return new DescendingSetImpl<E>(this);
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
}
