package com.apriori.collections;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;

public class SortedArraySet<E> implements NavigableSet<E> {

   private static final int DEFAULT_INITIAL_CAPACITY = 10;

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
         return (NavigableSet<E>) base.tailSet(toElement);
      }

      @Override
      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         return base.tailSet(toElement, inclusive);
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
         return (NavigableSet<E>) base.headSet(fromElement);
      }

      @Override
      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         return base.headSet(fromElement, inclusive);
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
      if (comp == null) {
         Arrays.sort(data);
      }
      else {
         @SuppressWarnings("unchecked")
         E toSort[] = (E[]) data;
         Arrays.sort(toSort, comp);
      }
   }

   void checkMod(int someModCount) {
      if (someModCount != modCount) {
         throw new ConcurrentModificationException();
      }
   }

   private void maybeGrowArray() {
      if (data.length > size) {
         return;
      }
      // TODO: grow it
      modCount++;
   }

   @SuppressWarnings("unchecked")
   private int findIndexWithoutComparator(Object o, int lo, int hi) {
      int mid = (hi + lo) >> 1;
      int c = ((Comparable<Object>) data[lo]).compareTo(o);
      if (c == 0) {
         return mid;
      }
      else if (c < 0) {
         if (hi == mid) {
            return -(hi + 2);
         }
         return findIndexWithoutComparator(o, mid + 1, hi);
      }
      else {
         if (lo == mid) {
            return -(lo + 1);
         }
         return findIndexWithoutComparator(o, lo, mid - 1);
      }
   }

   @SuppressWarnings("unchecked")
   private int findIndexWithComparator(Object o, int lo, int hi) {
      int mid = (hi + lo) >> 1;
      int c = comp.compare((E) data[mid], (E) o);
      if (c == 0) {
         return mid;
      }
      else if (c < 0) {
         if (hi == mid) {
            return -(hi + 2);
         }
         return findIndexWithComparator(o, mid + 1, hi);
      }
      else {
         if (lo == mid) {
            return -(lo + 1);
         }
         return findIndexWithComparator(o, lo, mid - 1);
      }
   }

   private int findIndex(Object element) {
      // binary search
      if (comp == null) {
         return findIndexWithoutComparator(element, 0, size - 1);
      }
      else {
         return findIndexWithComparator(element, 0, size - 1);
      }
   }

   private void insertItem(E element, int index) {
      maybeGrowArray();
      // TODO
      modCount++;
      size++;
   }

   void removeItem(int index) {
      // TODO
      modCount++;
      size--;
   }

   public void trimToSize() {
      // TODO
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
      boolean ret = false;
      for (E e : elements) {
         if (add(e)) {
            ret = true;
         }
      }
      return ret;
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
      boolean ret = false;
      for (Object o : elements) {
         if (remove(o)) {
            ret = true;
         }
      }
      return ret;
   }

   /** {@inheritDoc} */
   @Override
   public boolean retainAll(Collection<?> elements) {
      // TODO Auto-generated method stub
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
      // TODO Auto-generated method stub
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public <T> T[] toArray(T[] a) {
      // TODO Auto-generated method stub
      return null;
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
      // TODO Auto-generated method stub
      return null;
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
      // TODO Auto-generated method stub
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E fromElement) {
      // TODO Auto-generated method stub
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public E ceiling(E e) {
      // TODO Auto-generated method stub
      return null;
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
      // TODO Auto-generated method stub
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> headSet(E toElement, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public E higher(E e) {
      // TODO Auto-generated method stub
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public E lower(E e) {
      // TODO Auto-generated method stub
      return null;
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
      // TODO Auto-generated method stub
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
      // TODO Auto-generated method stub
      return null;
   }
}
