package com.apriori.collections;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

// TODO: javadoc
// TODO: tests
public class PersistentListBackedConcurrentList<E> implements List<E> {
   
   private final AtomicReference<PersistentList<E>> underlying;
   
   public PersistentListBackedConcurrentList(PersistentList<E> underlying) {
      this.underlying = new AtomicReference<PersistentList<E>>(underlying);
   }

   @Override
   public int size() {
      return underlying.get().size();
   }

   @Override
   public boolean isEmpty() {
      return underlying.get().isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return underlying.get().contains(o);
   }

   @Override
   public Iterator<E> iterator() {
      return listIterator();
   }

   @Override
   public Object[] toArray() {
      return underlying.get().toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return underlying.get().toArray(a);
   }

   @Override
   public boolean add(E e) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.add(e);
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public boolean remove(Object o) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.remove(o);
         if (original == modified) {
            return false;
         }
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return underlying.get().containsAll(c);
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.addAll(c);
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.addAll(index, c);
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.removeAll(c);
         if (original == modified) {
            return false;
         }
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.retainAll(c);
         if (original == modified) {
            return false;
         }
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public void clear() {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original;
         while (!modified.isEmpty()) {
            modified = modified.rest();
         }
         if (original == modified || underlying.compareAndSet(original, modified)) {
            return;
         }
      }
   }

   @Override
   public E get(int index) {
      return underlying.get().get(index);
   }

   @Override
   public E set(int index, E element) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.set(index, element);
         if (original == modified) {
            return element;
         }
         if (underlying.compareAndSet(original, modified)) {
            return original.get(index);
         }
      }
   }

   public boolean replace(int index, E existing, E replacement) {
      while (true) {
         PersistentList<E> original = underlying.get();
         E e = original.get(index);
         if (existing == null ? e != null : !existing.equals(e)) {
            return false;
         }
         PersistentList<E> modified = original.set(index, replacement);
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public void add(int index, E element) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.add(index, element);
         if (underlying.compareAndSet(original, modified)) {
            return;
         }
      }
   }

   @Override
   public E remove(int index) {
      while (true) {
         PersistentList<E> original = underlying.get();
         PersistentList<E> modified = original.remove(index);
         if (underlying.compareAndSet(original, modified)) {
            return original.get(index);
         }
      }
   }

   public boolean remove(int index, Object o) {
      while (true) {
         PersistentList<E> original = underlying.get();
         E e = original.get(index);
         if (o == null ? e != null : !o.equals(e)) {
            return false;
         }
         PersistentList<E> modified = original.remove(index);
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public int indexOf(Object o) {
      return underlying.get().indexOf(o);
   }

   @Override
   public int lastIndexOf(Object o) {
      return underlying.get().lastIndexOf(o);
   }

   @Override
   public ListIterator<E> listIterator() {
      return listIterator(0);
   }

   @Override
   public ListIterator<E> listIterator(int index) {
      ImmutableList<E> list = underlying.get();
      if (index < 0 || index > list.size()) {
         throw new IndexOutOfBoundsException();
      }
      return new Iter(list, index);
   }

   @Override
   public List<E> subList(int fromIndex, int toIndex) {
      ImmutableList<E> list = underlying.get();
      if (fromIndex < 0 || toIndex > list.size() || fromIndex > toIndex) {
         throw new IndexOutOfBoundsException();
      }
      return new SubList(fromIndex, toIndex);
   }
   
   private class SubList implements List<E> {
      private final int from;
      private final int to;
      
      SubList(int from, int to) {
         this.from = from;
         this.to = to;
      }
      
      @SuppressWarnings("synthetic-access")
      private ImmutableList<E> underlying() {
         ImmutableList<E> list = underlying.get();
         if (from > size()) {
            throw new ConcurrentModificationException();
         }  
         return list.subList(from, Math.min(to, list.size()));
      }
      
      @Override
      public int size() {
         return underlying().size();
      }

      @Override
      public boolean isEmpty() {
         return underlying().isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return underlying().contains(o);
      }

      @Override
      public Iterator<E> iterator() {
         return listIterator();
      }

      @Override
      public Object[] toArray() {
         return underlying().toArray();
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return underlying().toArray(a);
      }

      @Override
      public boolean add(E e) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean remove(Object o) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return underlying().containsAll(c);
      }

      @Override
      public boolean addAll(Collection<? extends E> c) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean addAll(int index, Collection<? extends E> c) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         // TODO: implement me
         return false;
      }

      @Override
      public void clear() {
         // TODO: implement me
      }

      @Override
      public E get(int index) {
         return underlying().get(index);
      }

      @Override
      public E set(int index, E element) {
         // TODO: implement me
         return null;
      }

      @Override
      public void add(int index, E element) {
         // TODO: implement me
      }

      @Override
      public E remove(int index) {
         // TODO: implement me
         return null;
      }

      @Override
      public int indexOf(Object o) {
         return underlying().indexOf(o);
      }

      @Override
      public int lastIndexOf(Object o) {
         return underlying().lastIndexOf(o);
      }

      @Override
      public ListIterator<E> listIterator() {
         return listIterator(0);
      }

      @Override
      public ListIterator<E> listIterator(int index) {
         // TODO: implement me
         return null;
      }

      @Override
      public List<E> subList(int fromIndex, int toIndex) {
         ImmutableList<E> list = underlying();
         if (fromIndex < 0 || toIndex > list.size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
         }
         return new SubList(from + fromIndex, from + toIndex);
      }
      
   }
   
   private static class IterNode<E> {
      private IterNode<E> previous;
      private IterNode<E> next;
      private final ImmutableList<E> head;
      private E value;
      private boolean hasValue;
      final int index;
      
      IterNode(ImmutableList<E> list, int index, IterNode<E> previous) {
         this.head = list;
         this.index = index;
         this.previous = previous;
      }
      
      synchronized IterNode<E> next() {
         if (next == null) {
            next = new IterNode<E>(head.rest(), index + 1, this);
         }
         return next;
      }
      
      synchronized void setNext(IterNode<E> next) {
         this.next = next;
      }
      
      synchronized E value() {
         return hasValue ? value : head.first();
      }
      
      synchronized void setValue(E value) {
         this.value = value;
         hasValue = true;
      }
      
      IterNode<E> previous() {
         return previous;
      }
      
      synchronized void setPrevious(IterNode<E> previous) {
         this.previous = previous;
      }
   }
   
   private class Iter implements ListIterator<E> {
      private IterNode<E> current;
      private IterNode<E> lastFetched;
      private boolean lastFetchedPrevious;
      private final int from;
      private final int to;
      
      Iter(ImmutableList<E> list, int start) {
         this(list, start, 0, list.size());
      }

      Iter(ImmutableList<E> list, int start, int from, int to) {
         this.from = from;
         this.to = to;
         while (from > 0) {
            list = list.rest();
            from--;
         }
         IterNode<E> node = new IterNode<E>(list, 0, null);
         for (int i = 0; i < start; i++) {
            node = node.next();
         }
         current = node;
      }

      @Override
      public boolean hasNext() {
         return current.index + from < to;
      }

      @Override
      public synchronized E next() {
         if (!hasNext()) {
            throw new NoSuchElementException();
         }
         E ret = current.value();
         lastFetched = current;
         lastFetchedPrevious = false;
         current = current.next();
         return ret;
      }

      @Override
      public synchronized boolean hasPrevious() {
         return current.previous() != null;
      }

      @Override
      public synchronized E previous() {
         if (!hasNext()) {
            throw new NoSuchElementException();
         }
         current = current.previous();
         lastFetched = current;
         lastFetchedPrevious = true;
         return current.value();
      }

      @Override
      public synchronized int nextIndex() {
         return current.index;
      }

      @Override
      public synchronized int previousIndex() {
         return current.index - 1;
      }

      @Override
      public synchronized void remove() {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         if (!PersistentListBackedConcurrentList.this
               .remove(lastFetched.index + from, lastFetched.value())) {
            throw new ConcurrentModificationException();
         }
         // re-wire current iterator state to reflect removal
         if (lastFetchedPrevious) {
            IterNode<E> prev = current.previous();
            IterNode<E> next = current.next();
            if (prev != null) {
               prev.setNext(next);
            }
            next.setPrevious(prev);
            current = next;
         } else {
            IterNode<E> rem = current.previous();
            IterNode<E> prev = rem.previous();
            if (prev != null) {
               prev.setNext(current);
            }
            current.setPrevious(prev);
         }
         lastFetched = null;
      }

      @Override
      public synchronized void set(E e) {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         if (!PersistentListBackedConcurrentList.this
               .replace(lastFetched.index + from, lastFetched.value(), e)) {
            throw new ConcurrentModificationException();
         }
         // update current iterator state to show new element
         if (lastFetchedPrevious) {
            current.setValue(e);
         } else {
            current.previous().setValue(e);
         }
      }

      @Override
      public void add(E e) {
         throw new UnsupportedOperationException();
      }
      
   }
}
