package com.apriori.collections;

import com.apriori.tuples.Pair;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

// TODO: javadoc
// TODO: tests
public class PersistentListBackedConcurrentList<E> implements ConcurrentList<E> {
   
   final AtomicReference<PersistentList<E>> underlying;
   
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

   @Override
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
   public boolean addAfter(int index, E expectedPriorValue, E addition) {
      if (index < 1) {
         throw new IllegalArgumentException();
      }
      while (true) {
         PersistentList<E> original = underlying.get();
         E priorValue = original.get(index - 1);
         if (!Objects.equals(priorValue, expectedPriorValue)) {
            return false;
         }
         PersistentList<E> modified = original.add(index, addition);
         if (underlying.compareAndSet(original, modified)) {
            return true;
         }
      }
   }

   @Override
   public boolean addBefore(int index, E expectedNextValue, E addition) {
      while (true) {
         PersistentList<E> original = underlying.get();
         if (index >= original.size()) {
            throw new IllegalArgumentException();
         }
         E nextValue = original.get(index);
         if (!Objects.equals(nextValue, expectedNextValue)) {
            return false;
         }
         PersistentList<E> modified = original.add(index, addition);
         if (underlying.compareAndSet(original, modified)) {
            return true;
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

   @Override
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
   public ConcurrentList<E> subList(int fromIndex, int toIndex) {
      ImmutableList<E> list = underlying.get();
      if (fromIndex < 0 || toIndex > list.size() || fromIndex > toIndex) {
         throw new IndexOutOfBoundsException();
      }
      return new SubList(fromIndex, toIndex);
   }
   
   private class SubList implements ConcurrentList<E> {
      private final int from;
      private final int to;
      private final AtomicReference<Pair<ImmutableList<E>, ImmutableList<E>>> memoized;
      
      SubList(int from, int to) {
         this.from = from;
         this.to = to;
         memoized = new AtomicReference<>();
      }
      
      private ImmutableList<E> underlyingSublist() {
         while (true) {
            Pair<ImmutableList<E>, ImmutableList<E>> pair = memoized.get();
            ImmutableList<E> l = underlying.get();
            if (pair != null && l == pair.getFirst()) {
               return pair.getSecond();
            }
            int start = from > l.size() ? l.size() : from;
            int end = to > l.size() ? l.size() : to;
            ImmutableList<E> subList = l.subList(start, end);
            if (memoized.compareAndSet(pair, Pair.create(l, subList))) {
               return subList;
            }
         }
      }
      
      @Override
      public int size() {
         return underlyingSublist().size();
      }

      @Override
      public boolean isEmpty() {
         return underlyingSublist().isEmpty();
      }

      @Override
      public boolean contains(Object o) {
         return underlyingSublist().contains(o);
      }

      @Override
      public Iterator<E> iterator() {
         return listIterator();
      }

      @Override
      public Object[] toArray() {
         return underlyingSublist().toArray();
      }

      @Override
      public <T> T[] toArray(T[] a) {
         return underlyingSublist().toArray(a);
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
         return underlyingSublist().containsAll(c);
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
         return underlyingSublist().get(index);
      }

      @Override
      public E set(int index, E element) {
         // TODO: implement me
         return null;
      }

      @Override
      public boolean replace(int index, E expectedValue, E newValue) {
         // TODO: implement me
         return false;
      }

      @Override
      public void add(int index, E element) {
         // TODO: implement me
      }

      @Override
      public boolean addAfter(int index, E expectedPriorValue, E addition) {
         // TODO: implement me
         return false;
      }

      @Override
      public boolean addBefore(int index, E expectedNextValue, E addition) {
         // TODO: implement me
         return false;
      }

      @Override
      public E remove(int index) {
         // TODO: implement me
         return null;
      }

      @Override
      public boolean remove(int index, E expectedValue) {
         // TODO: implement me
         return false;
      }

      @Override
      public int indexOf(Object o) {
         return underlyingSublist().indexOf(o);
      }

      @Override
      public int lastIndexOf(Object o) {
         return underlyingSublist().lastIndexOf(o);
      }

      @Override
      public ListIterator<E> listIterator() {
         return listIterator(0);
      }

      @Override
      public ListIterator<E> listIterator(int index) {
         return new Iter(underlyingSublist(), index, from);
      }

      @Override
      public ConcurrentList<E> subList(int fromIndex, int toIndex) {
         ImmutableList<E> list = underlyingSublist();
         if (fromIndex < 0 || toIndex > list.size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
         }
         return new SubList(from + fromIndex, from + toIndex);
      }
   }
   
   /**
    * A node in a doubly-linked list, used to represent the snapshot view of the list for iteration
    * and support mutation operations. The iterator is a consistent snapshot of the list at the
    * time iteration began, and will reflect all mutations made through the iterator. But it will
    * not reflect any modifications made directly to the list, outside of the iterator.
    * 
    * <p>This linked list is capped at the end with a special "end" node. This allows advancing the
    * iterator to a position "after the end of the list", with navigating back through the list via
    * predecessor nodes still being possible.
    *
    * @param <E> the type of element in the list
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class IterNode<E> {

      /**
       * Returns the head of the given list as a node with no predecessor.
       *
       * @param list a list
       * @return a node whose value is the head of the given list and that has no predecessor
       */
      static <E> IterNode<E> listHead(ImmutableList<E> list) {
         return list.isEmpty()
               ? endNode(null) : new IterNode<E>(list.first(), null, nextFromList(list));
      }

      /**
       * Returns a function that computes the next node as the head of the given list.
       *
       * @param list the list
       * @return a function that uses the head of the given list as the next node
       */
      private static <E> Function<IterNode<E>, IterNode<E>> nextFromList(ImmutableList<E> list) {
         return prev -> list.isEmpty()
               ? endNode(prev)
               : new IterNode<E>(list.first(), prev, nextFromList(list.rest()));
      }
      
      /**
       * Creates a new node with the given value and adjacent nodes.
       *
       * @param value the node's value
       * @param prev the node's predecessor
       * @param next the node's successor
       * @return the new node
       */
      static <E> IterNode<E> create(E value, IterNode<E> prev, IterNode<E> next) {
         return new IterNode<>(value, prev, next);
      }
      
      /**
       * Creates an "end" node with the given predecessor. The predecessor is the tail of the list.
       *
       * @param tail the predecessor
       * @return an end node
       */
      static <E> IterNode<E> endNode(IterNode<E> tail) {
         return new IterNode<>(tail);
      }

      private final boolean isEnd;
      private IterNode<E> previous;
      private IterNode<E> next;
      private Function<IterNode<E>, IterNode<E>> nextFn;
      private E value;
      
      private IterNode(IterNode<E> previous) {
         isEnd = true;
         this.value = null;
         this.previous = previous;
         this.next = null;
      }
      
      private IterNode(E value, IterNode<E> previous, Function<IterNode<E>, IterNode<E>> nextFn) {
         isEnd = false;
         this.value = value;
         this.previous = previous;
         assert nextFn != null;
         this.nextFn = nextFn;
      }

      private IterNode(E value, IterNode<E> previous, IterNode<E> next) {
         isEnd = false;
         this.value = value;
         this.previous = previous;
         assert next != null;
         this.next = next;
      }
      
      boolean isEnd() {
         return isEnd;
      }

      IterNode<E> next() {
         assert !isEnd;
         if (next == null && nextFn != null) {
            next = nextFn.apply(this);
            assert next != null;
            nextFn = null;
         }
         return next;
      }
      
      void setNext(IterNode<E> next) {
         assert !isEnd;
         this.next = next;
      }
      
      E value() {
         assert !isEnd;
         return value;
      }
      
      void setValue(E value) {
         assert !isEnd;
         this.value = value;
      }
      
      IterNode<E> previous() {
         return previous;
      }
      
      void setPrevious(IterNode<E> previous) {
         this.previous = previous;
      }
   }
   
   private class Iter implements ListIterator<E> {
      private final int offset;
      private int currentIndex;
      private IterNode<E> current;
      private IterNode<E> lastFetched;
      
      Iter(ImmutableList<E> list, int start) {
         this(list, start, 0);
      }

      Iter(ImmutableList<E> list, int start, int offset) {
         this.offset = offset;
         currentIndex = start;
         current = IterNode.listHead(list);
         for (int i = 0; i < start; i++) {
            current = current.next();
         }
      }
      
      @Override
      public boolean hasNext() {
         return !current.isEnd();
      }

      @Override
      public E next() {
         if (!hasNext()) {
            throw new NoSuchElementException();
         }
         E ret = current.value();
         lastFetched = current;
         current = current.next();
         currentIndex++;
         return ret;
      }

      @Override
      public boolean hasPrevious() {
         return current.previous() != null;
      }

      @Override
      public E previous() {
         if (!hasPrevious()) {
            throw new NoSuchElementException();
         }
         current = current.previous();
         lastFetched = current;
         currentIndex--;
         return current.value();
      }

      @Override
      public int nextIndex() {
         return currentIndex;
      }

      @Override
      public int previousIndex() {
         return currentIndex - 1;
      }
      
      private int lastFetchedIndex() {
         return lastFetched == current ? currentIndex : currentIndex - 1;
      }

      @Override
      public void remove() {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         if (!PersistentListBackedConcurrentList.this
               .remove(lastFetchedIndex() + offset, lastFetched.value())) {
            throw new ConcurrentModificationException();
         }
         // re-wire current iterator state to reflect removal
         if (lastFetched == current) {
            IterNode<E> prev = current.previous();
            IterNode<E> next = current.next();
            if (prev != null) {
               prev.setNext(next);
            }
            next.setPrevious(prev);
            current = next;
         } else {
            assert lastFetched == current.previous();
            IterNode<E> prev = lastFetched.previous();
            if (prev != null) {
               prev.setNext(current);
            }
            current.setPrevious(prev);
            currentIndex--;
         }
         lastFetched = null;
      }

      @Override
      public void set(E e) {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         if (!PersistentListBackedConcurrentList.this
               .replace(lastFetchedIndex() + offset, lastFetched.value(), e)) {
            throw new ConcurrentModificationException();
         }
         // update current iterator state to show new element
         lastFetched.setValue(e);
      }

      @Override
      public void add(E e) {
         boolean isPredecessor;
         IterNode<E> adjacent;
         if (lastFetched == null) {
            if (hasNext()) {
               // grab successor
               adjacent = current;
               isPredecessor = false;
            } else if (hasPrevious()) {
               // grab predecessor
               adjacent = current.previous();
               isPredecessor = true;
            } else {
               adjacent = null;
               isPredecessor = false; // doesn't matter...
            }
         } else {
            adjacent = lastFetched;
            isPredecessor = lastFetched != current;
         }
         
         if (adjacent == null) {
            PersistentListBackedConcurrentList.this.add(offset, e);
         } else if (isPredecessor) {
            if (!PersistentListBackedConcurrentList.this
                  .addAfter(currentIndex + offset, adjacent.value(), e)) {
               throw new ConcurrentModificationException();
            }
         } else {
            if (!PersistentListBackedConcurrentList.this
                  .addBefore(currentIndex + offset, adjacent.value(), e)) {
               throw new ConcurrentModificationException();
            }
         }
         IterNode<E> prev = current.previous();
         IterNode<E> newNode = IterNode.create(e, prev, current.next());
         if (prev != null) {
            prev.setNext(newNode);
         }
         current.setPrevious(newNode);
         currentIndex++;
      }
   }
}
