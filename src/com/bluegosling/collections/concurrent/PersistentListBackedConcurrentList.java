package com.bluegosling.collections.concurrent;

import com.bluegosling.collections.immutable.PersistentList;
import com.bluegosling.tuples.Pair;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

// TODO: javadoc
// TODO: tests
public abstract class PersistentListBackedConcurrentList<E> implements ConcurrentList<E> {
   
   public static <E> PersistentListBackedConcurrentList<E> withUnmodifiableSublist(
         PersistentList<E> underlying) {
      return new WithUnmodifiableSublist<E>(underlying);
   }

   public static <E> PersistentListBackedConcurrentList<E> withModifiableSublist(
         PersistentList<E> underlying) {
      return new WithModifiableSublist<E>(underlying);
   }
   
   PersistentListBackedConcurrentList() {
   }
   
   abstract PersistentList<E> get();

   @Override
   public int size() {
      return get().size();
   }

   @Override
   public boolean isEmpty() {
      return get().isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return get().contains(o);
   }

   @Override
   public Iterator<E> iterator() {
      return listIterator();
   }

   @Override
   public Object[] toArray() {
      return get().toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return get().toArray(a);
   }
   
   @Override
   public boolean containsAll(Collection<?> c) {
      return get().containsAll(c);
   }

   @Override
   public E get(int index) {
      return get().get(index);
   }
   
   @Override
   public int indexOf(Object o) {
      return get().indexOf(o);
   }

   @Override
   public int lastIndexOf(Object o) {
      return get().lastIndexOf(o);
   }

   @Override
   public ListIterator<E> listIterator() {
      return listIterator(0);
   } 
   
   @Override
   public ListIterator<E> listIterator(int index) {
      int sz = size();
      if (index < 0 || index > sz) {
         throw new IndexOutOfBoundsException();
      }
      return new Iter<>(this, get().iterator(), index);
   }
   
   private static class WithUnmodifiableSublist<E> extends PersistentListBackedConcurrentList<E> {
      final AtomicReference<PersistentList<E>> underlying;
      
      public WithUnmodifiableSublist(PersistentList<E> underlying) {
         this.underlying = new AtomicReference<>(underlying);
      }
      
      @Override
      final PersistentList<E> get() {
         return underlying.get();
      }

      @Override
      public E set(int index, E element) {
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.withReplacement(index, element);
            if (modified == original) {
               return element;
            }
            if (underlying.compareAndSet(original, modified)) {
               return original.get(index);
            }
         }
      }

      @Override
      public boolean replace(int index, E expectedValue, E newValue) {
         while (true) {
            PersistentList<E> original = get();
            if (!Objects.equals(original.get(index), expectedValue)) {
               return false;
            }
            PersistentList<E> modified = original.withReplacement(index, newValue);
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }
      
      @Override
      public boolean add(E e) {
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.with(e);
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }

      @Override
      public int addLast(E e) {
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.withTail(e);
            if (underlying.compareAndSet(original, modified)) {
               return original.size();
            }
         }
      }

      @Override
      public void add(int index, E element) {
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.with(index, element);
            if (underlying.compareAndSet(original, modified)) {
               return;
            }
         }
      }

      @Override
      public boolean addAll(Collection<? extends E> c) {
         if (c.isEmpty()) {
            return false;
         }
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.withAll(c);
            if (modified == original) {
               return false;
            }
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }

      @Override
      public boolean addAll(int index, Collection<? extends E> c) {
         if (c.isEmpty()) {
            return false;
         }
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.withAll(index, c);
            if (modified == original) {
               return false;
            }
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }
      
      @Override
      public boolean addAfter(int index, E expectedPriorValue, E addition) {
         while (true) {
            PersistentList<E> original = get();
            if (!Objects.equals(original.get(index - 1), expectedPriorValue)) {
               return false;
            }
            PersistentList<E> modified = original.with(index, addition);
            assert modified != original;
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }

      @Override
      public boolean addBefore(int index, E expectedNextValue, E addition) {
         while (true) {
            PersistentList<E> original = get();
            if (!Objects.equals(original.get(index), expectedNextValue)) {
               return false;
            }
            PersistentList<E> modified = original.with(index, addition);
            assert modified != original;
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }

      @Override
      public E remove(int index) {
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.without(index);
            assert modified != original;
            if (underlying.compareAndSet(original, modified)) {
               return original.get(index);
            }
         }
      }

      @Override
      public boolean remove(int index, E expectedValue) {
         while (true) {
            PersistentList<E> original = get();
            if (!Objects.equals(original.get(index), expectedValue)) {
               return false;
            }
            PersistentList<E> modified = original.without(index);
            assert modified != original;
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }

      @Override
      public boolean remove(Object o) {
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.without(o);
            if (modified == original) {
               return false;
            }
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         while (true) {
            PersistentList<E> original = get();
            PersistentList<E> modified = original.withoutAny(c);
            if (modified == original) {
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
            PersistentList<E> original = get();
            PersistentList<E> modified = original.withOnly(c);
            if (modified == original) {
               return false;
            }
            if (underlying.compareAndSet(original, modified)) {
               return true;
            }
         }
      }

      @Override
      public void clear() {
         underlying.set(get().removeAll());
      }

      @Override
      public ConcurrentList<E> subList(int fromIndex, int toIndex) {
         if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
         }
         // TODO
         //return new UnmodifiableSubList(fromIndex, toIndex);
         return null;
      }

      @Override
      public ConcurrentList<E> tailList(int fromIndex) {
         if (fromIndex < 0 || fromIndex > size()) {
            throw new IndexOutOfBoundsException();
         }
         // TODO
         //return new UnmodifiableSubList(fromIndex, -1);
         return null;
      }
      
      // TODO
      private abstract class UnmodifiableSubList implements ConcurrentList<E> {
         UnmodifiableSubList(int fromIndex, int toIndex) {
            // TODO
         }
      }
   }
   
   private static class WithModifiableSublist<E> extends PersistentListBackedConcurrentList<E> {
      final Object lock = new Object();
      volatile PersistentList<E> underlying;
      
      public WithModifiableSublist(PersistentList<E> underlying) {
         this.underlying = underlying;
      }
      
      @Override
      final PersistentList<E> get() {
         return underlying;
      }

      @Override
      public boolean add(E e) {
         synchronized (lock) {
            addLastLocked(e);
         }
         return true;
      }

      @Override
      public int addLast(E e) {
         synchronized (lock) {
            return addLastLocked(e);
         }
      }

      int addLastLocked(E e) {
         PersistentList<E> original = underlying;
         underlying = underlying.withTail(e);
         return original.size();
      }

      @Override
      public boolean remove(Object o) {
         synchronized (lock) {
            return removeLocked(o);
         }
      }

      boolean removeLocked(Object o) {
         PersistentList<E> modified = underlying.without(o);
         if (modified == underlying) {
            return false;
         }
         underlying = modified;
         return true;
      }

      @Override
      public boolean addAll(Collection<? extends E> c) {
         if (c.isEmpty()) {
            return false;
         }
         synchronized (lock) {
            return addAllLocked(c);
         }
      }
      
      boolean addAllLocked(Collection<? extends E> c) {
         PersistentList<E> modified = underlying.withAll(c);
         if (modified == underlying) {
            return false;
         }
         underlying = modified;
         return true;
      }

      @Override
      public boolean addAll(int index, Collection<? extends E> c) {
         if (c.isEmpty()) {
            return false;
         }
         synchronized (lock) {
            return addAllLocked(index, c);
         }
      }
      
      boolean addAllLocked(int index, Collection<? extends E> c) {
         PersistentList<E> modified = underlying.withAll(index, c);
         if (modified == underlying) {
            return false;
         }
         underlying = modified;
         return true;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
         if (underlying.isEmpty() || c.isEmpty()) {
            return false;
         }
         synchronized (lock) {
            return removeAllLocked(c);
         }
      }
      
      boolean removeAllLocked(Collection<?> c) {
         PersistentList<E> modified = underlying.withoutAny(c);
         if (modified == underlying) {
            return false;
         }
         underlying = modified;
         return true;
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         if (underlying.isEmpty()) {
            return false;
         }
         synchronized (lock) {
            return retainAllLocked(c);
         }
      }

      boolean retainAllLocked(Collection<?> c) {
         PersistentList<E> modified = underlying.withOnly(c);
         if (modified == underlying) {
            return false;
         }
         underlying = modified;
         return true;
      }

      @Override
      public void clear() {
         if (underlying.isEmpty()) {
            return;
         }
         synchronized (lock) {
            clearLocked();
         }
      }
      
      void clearLocked() {
         underlying = underlying.removeAll();
      }

      @Override
      public E set(int index, E element) {
         synchronized (lock) {
            PersistentList<E> modified = underlying.withReplacement(index, element);
            if (modified == underlying) {
               return element;
            }
            E ret = underlying.get(index);
            underlying = modified;
            return ret;
         }
      }

      @Override
      public boolean replace(int index, E existing, E replacement) {
         synchronized (lock) {
            if (!Objects.equals(existing, underlying.get(index))) {
               return false;
            }
            underlying = underlying.withReplacement(index, replacement);
            return true;
         }
      }

      @Override
      public void add(int index, E element) {
         synchronized (lock) {
            addLocked(index, element);
         }
      }

      void addLocked(int index, E e) {
         underlying = underlying.with(index, e);
      }


      @Override
      public boolean addAfter(int index, E expectedPriorValue, E addition) {
         if (index < 1) {
            throw new IllegalArgumentException();
         }
         synchronized (lock) {
            if (!Objects.equals(underlying.get(index - 1), expectedPriorValue)) {
               return false;
            }
            underlying = underlying.with(index, addition);
            return true;
         }
      }

      @Override
      public boolean addBefore(int index, E expectedNextValue, E addition) {
         synchronized (lock) {
            if (index >= underlying.size()) {
               throw new IllegalArgumentException();
            }
            if (!Objects.equals(underlying.get(index), expectedNextValue)) {
               return false;
            }
            underlying = underlying.with(index, addition);
            return true;
         }
      }

      @Override
      public E remove(int index) {
         synchronized (lock) {
            return removeLocked(index);
         }
      }

      E removeLocked(int index) {
         PersistentList<E> modified = underlying.without(index);
         E ret = underlying.get(index);
         underlying = modified;
         return ret;
      }
      
      @Override
      public boolean remove(int index, Object o) {
         synchronized (lock) {
            return removeLocked(index, o);
         }
      }
      
      private boolean removeLocked(int index, Object o) {
         if (!Objects.equals(o, underlying.get(index))) {
            return false;
         }
         underlying = underlying.without(index);
         return true;
      }

      @Override
      public ConcurrentList<E> subList(int fromIndex, int toIndex) {
         if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
         }
         // TODO
         //return new ModifiableSubList(this, fromIndex, toIndex);
         return null;
      }

      @Override
      public ConcurrentList<E> tailList(int fromIndex) {
         if (fromIndex < 0 || fromIndex > size()) {
            throw new IndexOutOfBoundsException();
         }
         // TODO
         //return new ModifiableSubList(this, fromIndex, -1);
         return null;
      }

      private abstract class ModifiableSubList implements ConcurrentList<E> {
         private final int from;
         private final int to;
         private final AtomicReference<Pair<List<E>, List<E>>> memoized;
         
         ModifiableSubList(int from, int to) {
            this.from = from;
            this.to = to;
            memoized = new AtomicReference<>();
         }
         
         private List<E> underlyingSublist() {
            // TODO
            /*while (true) {
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
            }*/
            return null;
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
            return new Iter<>(this, underlyingSublist().iterator(), index, from);
         }

         @Override
         public ConcurrentList<E> subList(int fromIndex, int toIndex) {
            List<E> list = underlyingSublist();
            if (fromIndex < 0 || toIndex > list.size() || fromIndex > toIndex) {
               throw new IndexOutOfBoundsException();
            }
            // TODO
            //return new NestedSubList(this, fromIndex, toIndex);
            return null;
         }

         @Override
         public ConcurrentList<E> tailList(int fromIndex) {
            List<E> list = underlyingSublist();
            if (fromIndex < 0 || fromIndex > list.size()) {
               throw new IndexOutOfBoundsException();
            }
            // TODO
            //return new NestedSubList(this, fromIndex, -1);
            return null;
         }
      }
   }

   /**
    * A node in a doubly-linked list, used to represent the snapshot view of the list for
    * iteration and support mutation operations. The iterator is a consistent snapshot of the
    * list at the time iteration began, and will reflect all mutations made through the iterator.
    * But it will not reflect any modifications made directly to the list, outside of the
    * iterator.
    * 
    * <p>This linked list is capped at the end with a special "end" node. This allows advancing
    * the iterator to a position "after the end of the list", with navigating back through the
    * list via predecessor nodes still being possible.
    *
    * @param <E> the type of element in the list
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class IterNode<E> {

      /**
       * Returns the first element from the given iterator as a node with no predecessor.
       *
       * @param iter an iterator
       * @return a node whose value is the first element from the given iterator and that has no
       *       predecessor
       */
      static <E> IterNode<E> head(Iterator<E> iter) {
         return iter.hasNext()
               ? endNode(null) : new IterNode<E>(iter.next(), null, nextFromIter(iter));
      }

      /**
       * Returns a function that computes the next node as the next element from the given iterator.
       *
       * @param iter an iterator
       * @return a function that uses the next element from the given iterator as the next node
       */
      private static <E> Function<IterNode<E>, IterNode<E>> nextFromIter(Iterator<E> iter) {
         return prev -> iter.hasNext()
               ? endNode(prev)
               : new IterNode<E>(iter.next(), prev, nextFromIter(iter));
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
   
   private static class Iter<E> implements ListIterator<E> {
      private ConcurrentList<E> list;
      private final int offset;
      private int currentIndex;
      private IterNode<E> current;
      private IterNode<E> lastFetched;
      
      Iter(ConcurrentList<E> list, Iterator<E> iter, int start) {
         this(list, iter, start, 0);
      }

      Iter(ConcurrentList<E> list, Iterator<E> iter, int start, int offset) {
         this.list = list;
         this.offset = offset;
         currentIndex = start;
         current = IterNode.head(iter);
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
         if (!list.remove(lastFetchedIndex() + offset, lastFetched.value())) {
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
         if (!list.replace(lastFetchedIndex() + offset, lastFetched.value(), e)) {
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
            list.add(offset, e);
         } else if (isPredecessor) {
            if (!list.addAfter(currentIndex + offset, adjacent.value(), e)) {
               throw new ConcurrentModificationException();
            }
         } else {
            if (!list.addBefore(currentIndex + offset, adjacent.value(), e)) {
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
