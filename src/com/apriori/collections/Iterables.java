package com.apriori.collections;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.RandomAccess;
import java.util.function.Function;
import java.util.function.IntSupplier;

// TODO: javadoc
// TODO: tests
public final class Iterables {
   private Iterables() {
   }
   
   public static <E> Comparator<Iterable<E>> comparator(Comparator<? super E> comparator) {
      return (i1, i2) -> {
         Iterator<E> iter1 = i1.iterator();
         Iterator<E> iter2 = i2.iterator();
         while (iter1.hasNext() && iter2.hasNext()) {
            E e1 = iter1.next();
            E e2 = iter2.next();
            int c = comparator.compare(e1, e2);
            if (c != 0) {
               return c;
            }
         }
         if (iter1.hasNext()) {
            return 1;
         } else if (iter2.hasNext()) {
            return -1;
         }
         return 0;
      };
   }
   
   public static <E> boolean addTo(Iterator<E> iter, Collection<? super E> coll) {
      boolean ret = false;
      while (iter.hasNext()) {
         if (coll.add(iter.next())) {
            ret = true;
         }
      }
      return ret;
   }
   
   public static <E> boolean addTo(Iterable<E> iter, Collection<? super E> coll) {
      if (iter instanceof Collection) {
         return coll.addAll((Collection<E>) iter);
      } else if (iter instanceof SizedIterable) {
         return coll.addAll(SizedIterable.toCollection((SizedIterable<E>) iter));
      } else {
         return addTo(iter.iterator(), coll);
      }
   }
   
   public static <E> Iterator<E> singletonIterator(E element) {
      return new SingletonIterator<E>(element, null);
   }

   public static <E> Iterator<E> singletonIterator(E element, Runnable onRemove) {
      return new SingletonIterator<E>(element, onRemove);
   }
   

   public static <E> Iterator<E> reversed(Iterable<E> iterable) {
      // check for a few special cases where we can reverse the iterable very efficiently
      if (iterable instanceof List) {
         List<E> list = (List<E>) iterable;
         return CollectionUtils.reverseIterator(list.listIterator(list.size()));
      } else if (iterable instanceof ImmutableList && iterable instanceof RandomAccess) {
         final ImmutableList<E> list = (ImmutableList<E>) iterable;
         return new Iterator<E>() {
            int nextIndex = list.size() - 1;
            
            @Override public boolean hasNext() {
               return nextIndex >= 0;
            }

            @Override public E next() {
               if (nextIndex < 0) {
                  throw new NoSuchElementException();
               }
               return list.get(nextIndex--);
            }
         };
      } else if (iterable instanceof Deque) {
         return ((Deque<E>) iterable).descendingIterator();
      } else if (iterable instanceof NavigableSet) {
         return ((NavigableSet<E>) iterable).descendingIterator();
      } else if (iterable instanceof ImmutableSortedSet) {
         return Immutables.descendingIterator((ImmutableSortedSet<E>) iterable);
      } else {
         // No way that we know of to efficiently reverse the given iterable. So we push all of its
         // contents into a stack, so we can then trivially iterate the stack to reverse order
         Iterator<E> iter = iterable.iterator();
         if (!iter.hasNext()) {
            // empty iterator -- no sense doing anything else
            return iter;
         }
         ArrayDeque<E> deque;
         // try to determine best initial size
         OptionalInt size = trySize(iterable);
         if (size.isPresent()) {
            deque = new ArrayDeque<>(size.getAsInt());
         } else {
            deque = new ArrayDeque<>();
         }
         while (iter.hasNext()) {
            deque.push(iter.next());
         }
         return deque.iterator();
      }
   }
   
   public static OptionalInt trySize(Iterable<?> iterable) {
      if (iterable instanceof Collection) {
         return OptionalInt.of(((Collection<?>) iterable).size());
      } else if (iterable instanceof SizedIterable) {
         return OptionalInt.of(((SizedIterable<?>) iterable).size());
      } else {
         return OptionalInt.empty();
      }
   }
   
   public static int size(Iterable<?> iterable) {
      if (iterable instanceof Collection) {
         return ((Collection<?>) iterable).size();
      } else if (iterable instanceof SizedIterable) {
         return ((SizedIterable<?>) iterable).size();
      } else {
         return size(iterable.iterator());
      }
   }

   public static int size(Iterator<?> iterator) {
      int sz = 0;
      while (iterator.hasNext()) {
         iterator.next();
         sz++;
      }
      return sz;
   }

   /**
    * Creates a snapshot of the given iterable. This involves exhausting the iterable and returning
    * an unmodifiable collection with all of its contents.
    *
    * @param iterable an iterable
    * @return a snapshot of the iterable's contents in an unmodifiable collection
    */
   @SuppressWarnings("unchecked")
   public static <E> List<E> snapshot(Iterable<? extends E> iterable) {
     return Collections.unmodifiableList((List<E>) Arrays.asList(toArray(iterable))); 
   }
   
   private static class Chunk {
      final Object contents[];
      Chunk next;
      
      Chunk(Object array[]) {
         contents = array;
      }
      
      Chunk(int limit) {
         this(new Object[limit]);
      }
      
      Chunk(int limit, Class<?> elementType) {
         this(elementType != Object.class
               ? (Object[]) Array.newInstance(elementType, limit)
               : new Object[limit]);
      }
   }
   
   /**
    * Creates a snapshot of the given iterable as an array. Implementations that already have a
    * {@code toArray} method, like {@link Collection} and {@link ImmutableCollection}, will just use
    * that existing implementation.
    * 
    * <p>Iterables that do not provide such a method must resort to a custom mechanism. Instead of
    * using a growable array, where we have to copy contents on each resize, we use a structure of
    * linked nodes, where each node has an array whose length is the same as all nodes before it (so
    * adding a new node effectively doubles the capacity, just like in standard growable array
    * approaches). The last step is to consolidate the nodes into a single array once the iterable's
    * contents have been exhausted.
    *
    * @param iterable an iterable
    * @return a snapshot of the iterable's contents in an array
    */
   public static Object[] toArray(Iterable<?> iterable) {
      Iterator<?> iter = iterable.iterator();
      if (!iter.hasNext()) {
         return Immutables.EMPTY;
      } else if (iterable instanceof Collection) {
         return ((Collection<?>) iterable).toArray();
      } else if (iterable instanceof ImmutableCollection) {
         return ((ImmutableCollection<?>) iterable).toArray();
      } else if (iterable instanceof ImmutableMap) {
         return ((ImmutableMap<?, ?>) iterable).entrySet().toArray();
      } else if (iterable instanceof PriorityQueue) {
         return ((PriorityQueue<?, ?>) iterable).toArray();
      } else {
         return toArrayInternal(iter,
               iterable instanceof SizedIterable ? ((SizedIterable<?>) iterable)::size : null);
      }
   }
      
   public static <T> T[] toArray(Iterable<?> iterable, T[] array) {
      Iterator<?> iter = iterable.iterator();
      if (!iter.hasNext()) {
         if (array.length > 0) {
            array[0] = null;
         }
         return array;
      } else if (iterable instanceof Collection) {
         return ((Collection<?>) iterable).toArray(array);
      } else if (iterable instanceof ImmutableCollection) {
         return ((ImmutableCollection<?>) iterable).toArray(array);
      } else if (iterable instanceof ImmutableMap) {
         return ((ImmutableMap<?, ?>) iterable).entrySet().toArray(array);
      } else {
         return toArrayInternal(iter, array,
               iterable instanceof SizedIterable ? ((SizedIterable<?>) iterable)::size : null);
      }
   }
   
   public static Object[] toArray(Iterator<?> iter) {
      return toArrayInternal(iter, null);
   }
   
   private static Object[] toArrayInternal(Iterator<?> iter, IntSupplier sized) {
      int size = 0;
      int chunkLimit = sized != null ? sized.getAsInt() : 16;
      Chunk head = new Chunk(chunkLimit);
      Chunk current = head;
      int currentIndex = 0;
      while (iter.hasNext()) {
         if (currentIndex == chunkLimit) {
            size += currentIndex;
            int newSize;
            if (sized != null && (newSize = sized.getAsInt()) > size) {
               chunkLimit = newSize - size + 1;
            } else {
               // don't know by how much to grow? double capacity
               chunkLimit = size;
            }
            current.next = new Chunk(chunkLimit);
            current = current.next;
            currentIndex = 0;
         }
         Object o = iter.next();
         current.contents[currentIndex++] = o;
      }
      size += currentIndex;
      if (head.next == null && head.contents.length == size) {
         // we captured it perfectly in first chunk, so just return the chunk's contents
         return head.contents;
      }
      int lastChunkSize = currentIndex;
      Object elements[] = new Object[size];
      currentIndex = 0;
      for (current = head; current != null; current = current.next) {
         if (current.next == null) {
            // last chunk
            if (lastChunkSize > 0) {
               System.arraycopy(current.contents, 0, elements, currentIndex, lastChunkSize);
            }
         } else {
            int chunkLength = current.contents.length;
            System.arraycopy(current.contents, 0, elements, currentIndex, chunkLength);
            currentIndex += chunkLength;
         }
      }
      return elements;
   }
   
   public static <T> T[] toArray(Iterator<?> iter, T[] array) {
      return toArrayInternal(iter, array, null);
   }

   private static <T> T[] toArrayInternal(Iterator<?> iter, T[] array, IntSupplier sized) {
      int size = 0;
      int chunkLimit = sized != null ? sized.getAsInt() : 16;
      Class<?> elementType = array.getClass().getComponentType();
      Chunk head = array.length > 0 ? new Chunk(array) : new Chunk(chunkLimit, elementType);
      Chunk current = head;
      int currentIndex = 0;
      while (iter.hasNext()) {
         if (currentIndex == chunkLimit) {
            size += currentIndex;
            int newSize;
            if (sized != null && (newSize = sized.getAsInt()) > size) {
               chunkLimit = newSize - size + 1;
            } else {
               // don't know by how much to grow? double capacity
               chunkLimit = size;
            }
            current.next = new Chunk(chunkLimit, elementType);
            current = current.next;
            currentIndex = 0;
         }
         Object o = iter.next();
         current.contents[currentIndex++] = o;
      }
      size += currentIndex;
      if (head.next == null) {
         if (head.contents.length == size) {
            // we captured it perfectly in first chunk, so just return the chunk's contents
            @SuppressWarnings("unchecked") // we made sure each chunk has right component type
            T ret[] = (T[]) head.contents;
            return ret;
         } else if (head.contents == array) {
            // never exceeded original array, so just null-terminate and done
            array[size] = null;
            return array;
         }
      }
      int lastChunkSize = currentIndex;
      @SuppressWarnings("unchecked")
      T elements[] = (T[]) Array.newInstance(elementType, size);
      currentIndex = 0;
      for (current = head; current != null; current = current.next) {
         if (current.next == null) {
            // last chunk
            if (lastChunkSize > 0) {
               System.arraycopy(current.contents, 0, elements, currentIndex, lastChunkSize);
            }
         } else {
            int chunkLength = current.contents.length;
            System.arraycopy(current.contents, 0, elements, currentIndex, chunkLength);
            currentIndex += chunkLength;
         }
      }
      return elements;
   }

   @SuppressWarnings("unchecked")
   public static <E> Iterator<E> cast(Iterator<? extends E> iterator) {
      return (Iterator<E>) iterator;
   }
   
   @SuppressWarnings("unchecked")
   public static <E> Iterable<E> cast(Iterable<? extends E> iterable) {
      return (Iterable<E>) iterable;
   }

   @SuppressWarnings("unchecked")
   public static <E> SizedIterable<E> cast(SizedIterable<? extends E> iterable) {
      return (SizedIterable<E>) iterable;
   }

   public static <E> Iterator<E> push(Iterator<? extends E> iterator, E pushed) {
      return new ConsIterator<E>(pushed, iterator, null);
   }

   public static <E> Iterator<E> push(Iterator<? extends E> iterator, E pushed, Runnable onRemove) {
      return new ConsIterator<E>(pushed, iterator, onRemove);
   }

   @SafeVarargs
   public static <E> Iterator<E> push(Iterator<? extends E> iterator, E... pushed) {
      return push(iterator, Arrays.asList(pushed));
   }

   public static <E> Iterator<E> push(Iterator<? extends E> iterator, Iterable<E> pushed) {
      return concat(pushed.iterator(), iterator);
   }

   @SafeVarargs
   public static <E> Iterable<E> concat(Iterable<? extends E>... iterables) {
      List<Iterable<? extends E>> iterableList = Arrays.asList(iterables);
      return () -> concat(
            new TransformingIterable<Iterable<? extends E>, Iterator<? extends E>>(iterableList,
                  iterable -> iterable.iterator()));
   }

   @SafeVarargs
   public static <E> Iterator<E> concat(Iterator<? extends E>... iterators) {
      switch (iterators.length) {
         case 0:
            return Collections.emptyIterator();
         case 1:
            return cast(iterators[0]);
         case 2:
            if (iterators[0].hasNext()) {
               if (iterators[1].hasNext()) {
                  return new TwoIterators<>(iterators[0], iterators[1]);
               } else {
                  return cast(iterators[0]);
               }
            } else {
               return cast(iterators[1]);
            }
         default:
            return new ConcatIterators<>(Arrays.asList(iterators).iterator());
      }
   }

   public static <E> Iterator<E> concat(Iterable<Iterator<? extends E>> iterators) {
      Iterator<Iterator<? extends E>> iter = iterators.iterator();
      if (!iter.hasNext()) {
         return Collections.emptyIterator();
      }
      Iterator<? extends E> first = iter.next();
      if (!iter.hasNext()) {
         return cast(first);
      }
      Iterator<? extends E> second = iter.next();
      if (!iter.hasNext()) {
         if (first.hasNext()) {
            if (second.hasNext()) {
               return new TwoIterators<>(first, second);
            } else {
               return cast(first);
            }
         } else {
            return cast(second);
         }
      }
      return new ConcatIterators<>(push(iter, first, second)); 
   }
   
   public static <E> Iterable<E> upToN(Iterable<E> iterable, int n) {
      return () -> upToN(iterable.iterator(), n);
   }
   
   public static <E> Iterator<E> upToN(Iterator<E> iterator, int n) {
      return new Iterator<E>() {
         int remaining = n;

         @Override
         public boolean hasNext() {
            return iterator.hasNext() && remaining > 0;
         }

         @Override
         public E next() {
            if (remaining <= 0) {
               throw new NoSuchElementException();
            }
            remaining--;
            return iterator.next();
         }
         
         @Override
         public void remove() {
            iterator.remove();
         }
      };
   }
   
   public static <T, U> Iterator<U> flatMap(Iterator<T> iterator,
         Function<? super T, ? extends Iterator<? extends U>> fn) {
      return new FlatMapIterator<>(iterator, fn);
   }

   private static class SingletonIterator<E> implements Iterator<E> {
      private final E e;
      private final Runnable onRemove;
      private int state; // 0 zilch, 1 fetched, 2 removed
      
      SingletonIterator(E e, Runnable onRemove) {
         this.e = e;
         this.onRemove = onRemove;
      }
      
      @Override
      public boolean hasNext() {
         return state == 0;
      }

      @Override
      public E next() {
         if (state > 0) {
            throw new NoSuchElementException();
         }
         state = 1;
         return e;
      }

      @Override
      public void remove() {
         if (onRemove == null) {
            throw new UnsupportedOperationException();
         }
         if (state != 1) {
            throw new IllegalStateException();
         }
         onRemove.run();
         state = 2;
      }
   }
   
   private static class TwoIterators<E> implements Iterator<E> {
      final Iterator<? extends E> iter1;
      final Iterator<? extends E> iter2;
      boolean first;
      int lastFetched = 0;
      
      TwoIterators(Iterator<? extends E> iter1, Iterator<? extends E> iter2) {
         this.iter1 = iter1;
         this.iter2 = iter2;
         first = iter1.hasNext();
      }
      
      @Override
      public boolean hasNext() {
         return first ? iter1.hasNext() : iter2.hasNext();
      }

      @Override
      public E next() {
         if (first) {
            lastFetched = 1;
            E ret = iter1.next();
            if (!iter1.hasNext()) {
               first = false;
            }
            return ret;
         }
         lastFetched = 2;
         return iter2.next();
      }
      
      @Override
      public void remove() {
         switch (lastFetched) {
            case 0:
               throw new IllegalStateException();
            case 1:
               iter1.remove();
               break;
            case 2:
               iter2.remove();
               break;
            default:
               throw new AssertionError();
         }
      }
   }
   
   private static class ConsIterator<E> implements Iterator<E> {
      final Iterator<? extends E> iter;
      E initial;
      Runnable onRemoveInitial;
      int fetched;
      
      ConsIterator(E initial, Iterator<? extends E> iter, Runnable onRemoveInitial) {
         this.iter = iter;
         this.initial = initial;
         this.onRemoveInitial = onRemoveInitial;
      }

      @Override
      public boolean hasNext() {
         return fetched == 0 || iter.hasNext();
      }

      @Override
      public E next() {
         fetched++;
         if (fetched == 1) {
            // first element
            E ret = initial;
            initial = null; // let GC do its thang
            return ret;
         } else if (fetched == 2) {
            onRemoveInitial = null; // won't need this anymore
         }
         return iter.next();
      }
      
      @Override
      public void remove() {
         if (fetched == 0) {
            throw new IllegalStateException();
         }
         if (fetched == 1) {
            if (onRemoveInitial == null) {
               throw new UnsupportedOperationException();
            } else {
               onRemoveInitial.run();
            }
         } else {
            iter.remove();
         }
      }
   }
   
   private static class ConcatIterators<E> implements Iterator<E> {
      final Iterator<Iterator<? extends E>> iterators;
      Iterator<? extends E> current;
      Iterator<? extends E> lastFetched;
      
      ConcatIterators(Iterator<Iterator<? extends E>> iterators) {
         this.iterators = iterators;
         findNext();
      }
      
      private void findNext() {
         while (iterators.hasNext()) {
            Iterator<? extends E> iter = iterators.next();
            if (iter.hasNext()) {
               current = iter;
               return;
            }
         }
         current = null;
      }

      @Override
      public boolean hasNext() {
         return current != null && current.hasNext();
      }

      @Override
      public E next() {
         if (current == null) {
            throw new NoSuchElementException();
         }
         lastFetched = current;
         E ret = current.next();
         if (!current.hasNext()) {
            findNext();
         }
         return ret;
      }
      
      @Override
      public void remove() {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         lastFetched.remove();
      }
   }
   
   private static class FlatMapIterator<T, U> implements Iterator<U> {
      private final Iterator<T> iterator;
      private final Function<? super T, ? extends Iterator<? extends U>> fn;
      Iterator<? extends U> current;
      Iterator<? extends U> lastFetched;
      
      FlatMapIterator(Iterator<T> iterator,
            Function<? super T, ? extends Iterator<? extends U>> fn) {
         this.iterator = iterator;
         this.fn = fn;
         findNext();
      }

      private void findNext() {
         while (iterator.hasNext()) {
            Iterator<? extends U> iter = fn.apply(iterator.next());
            if (iter.hasNext()) {
               current = iter;
               return;
            }
         }
         current = null;
      }

      @Override
      public boolean hasNext() {
         return current != null && current.hasNext();
      }

      @Override
      public U next() {
         if (current == null) {
            throw new NoSuchElementException();
         }
         lastFetched = current;
         U ret = current.next();
         if (!current.hasNext()) {
            findNext();
         }
         return ret;
      }

      @Override
      public void remove() {
         if (lastFetched == null) {
            throw new IllegalStateException();
         }
         lastFetched.remove();
      }
   }
}