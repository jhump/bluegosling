package com.bluegosling.collections;

import com.bluegosling.collections.views.FilteringIterator;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Utility methods for working with instances of {@link Iterable} and {@link Iterator}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public final class MoreIterables {
   private MoreIterables() {
   }
   
   static final Object EMPTY[] = new Object[0];
   
   /**
    * Returns a view of the given iterable as a collection. If the given iterable already implements
    * {@link Collection} then it is returned. Otherwise, an unmodifiable collection view is
    * returned. The return view may require linear time to compute the size (by actually performing
    * an iteration and counting elements).
    *
    * @param <E> the type of elements in the collection
    * @param iter an iterable
    * @return a view of the given iterable as a collection
    */
   public static <E> Collection<E> asCollection(Iterable<E> iter) {
      if (iter instanceof Collection) {
         return ((Collection<E>) iter);
      }
      return new AbstractCollection<E>() {
         @Override
         public Iterator<E> iterator() {
            return iter.iterator();
         }

         @Override
         public int size() {
            return Iterables.size(iter);
         }
      };
   }
   
   /**
    * Returns a comparator of iterables based on the given comparator of elements. The returned
    * comparator will order iterables in pseudo-lexical order, as if each element were a character
    * in a string. So it first compares the first element from two iterables. If those are equal
    * then it must compare the next elements and so on. If the end of one iterable is encountered
    * first, then the shorter one (which is a strict prefix of the longer one) is considered less
    * than the longer one.
    *
    * @param <E> the type of element being compared
    * @param comparator a comparator of elements
    * @return a corresponding comparator of iterables
    */
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
   
   public static <E> ListIterator<E> unmodifiableIterator(ListIterator<? extends E> iter) {
      return new ListIterator<E>() {
         @Override
         public boolean hasNext() {
            return iter.hasNext();
         }

         @Override
         public E next() {
            return iter.next();
         }

         @Override
         public boolean hasPrevious() {
            return iter.hasPrevious();
         }

         @Override
         public E previous() {
            return iter.previous();
         }

         @Override
         public int nextIndex() {
            return iter.nextIndex();
         }

         @Override
         public int previousIndex() {
            return iter.previousIndex();
         }

         @Override
         public void set(E e) {
            throw new UnsupportedOperationException();
         }

         @Override
         public void add(E e) {
            throw new UnsupportedOperationException();
         }

         @Override
         public void remove() {
            throw new UnsupportedOperationException();
         }
      };
   }
   
   /**
    * Returns an iterator that omits duplicates from the given iterator. Note that the tracking of
    * duplicates requires state that can use <em>O(n)</em> amount of memory.
    *
    * @param <E> the type of elements retrieved from the iterator
    * @param iter an iterator
    * @return an iterator that returns the items of the given iterator but omits duplicates
    */
   public static <E> Iterator<E> unique(Iterator<E> iter) {
      return unique(iter, OptionalInt.empty());
   }
   
   /**
    * Returns an iterable that omits duplicates from the given iterable. Note that the tracking of
    * duplicates requires state that can use <em>O(n)</em> amount of memory for each iteration.
    *
    * @param <E> the type of elements in the iterable
    * @param iter an iterable
    * @return an iterable that returns the items of the given iterable but omits duplicates
    */
   public static <E> Iterable<E> unique(Iterable<E> iter) {
      return () -> unique(iter.iterator(), trySize(iter));
   }

   private static <E> Iterator<E> unique(Iterator<E> iter, OptionalInt size) {
      Set<E> items = size.isPresent() ? new HashSet<>(size.getAsInt() * 4 / 3) : new HashSet<>();
      return new FilteringIterator<>(iter, e -> items.add(e));
   }

   /**
    * Returns an iterator that navigates over the given iterable in reverse order. Most iterables do
    * not provide an efficient way to do this, in which case a stack is used (and thus will require
    * <em>O(n)</em> amount of memory). But for those that <strong>do</strong> provide efficient
    * implementations (like lists, deques, and navigable sets) they will be used so there is no
    * space overhead.
    *
    * @param <E> the type of elements in the iterable
    * @param iterable an iterable
    * @return an iterator that visits the elements of the iterable in reverse order
    */
   public static <E> Iterator<E> reversed(Iterable<E> iterable) {
      // check for a few special cases where we can reverse the iterable very efficiently
      if (iterable instanceof List) {
         List<E> list = (List<E>) iterable;
         return CollectionUtils.reverseIterator(list.listIterator(list.size()));
      } else if (iterable instanceof Deque) {
         return ((Deque<E>) iterable).descendingIterator();
      } else if (iterable instanceof NavigableSet) {
         return ((NavigableSet<E>) iterable).descendingIterator();
      } else {
         // No way that we know of to efficiently reverse the given iterable. So we push all of its
         // contents into a stack, so we can then trivially iterate the stack to reverse order
         Iterator<E> iter = iterable.iterator();
         if (!iter.hasNext()) {
            // empty iterator -- no sense doing anything else
            return iter;
         }
         ArrayList<E> list;
         // try to determine best initial size
         OptionalInt size = trySize(iterable);
         if (size.isPresent()) {
            list = new ArrayList<>(size.getAsInt());
         } else {
            list = new ArrayList<>();
         }
         Iterators.addAll(list, iter);
         return CollectionUtils.reverseIterator(list.listIterator(list.size()));
      }
   }
   
   private static IntSupplier sizer(Iterable<?> iterable) {
      if (iterable instanceof Collection) {
         Collection<?> coll = (Collection<?>) iterable; 
         return coll::size;
      } else {
         return null;
      }
   }
   
   /**
    * Queries for the size of the given iterable, if known. If the given iterable implements
    * {@link Collection} then the size is returned. Otherwise, an absent value is returned,
    * indicating that the size is not known.
    *
    * @param iterable an iterable
    * @return the size of the given iterable if known
    * 
    * @see Iterables#size(Iterable)
    * @see Iterators#size(Iterator)
    */
   public static OptionalInt trySize(Iterable<?> iterable) {
      if (iterable instanceof Collection) {
         return OptionalInt.of(((Collection<?>) iterable).size());
      } else {
         return OptionalInt.empty();
      }
   }

   /**
    * Creates a snapshot of the given iterable. This involves iterating over the elements and
    * creating an unmodifiable collection with all of its contents.
    * 
    * <p>The snapshot is created with as little overhead as possible using a strategy like used by
    * {@link ArrayList}, except using linked nodes of arrays (each larger than the previous) instead
    * of resizing a single array (so no copies need be made when the initial array gets full). The
    * initial array is sized based on the given iterable's {@linkplain #trySize(Iterable) size} or
    * 16 if the size is unknown.
    * 
    * <p>The returned snapshot is just a collection. If you need a snapshot that implements the
    * {@link List} or {@link Set} interface, use {@link ImmutableList#copyOf(Iterable)} or
    * {@link ImmutableSet#copyOf(Iterable)}.
    *
    * @param iterable an iterable
    * @return a snapshot of the iterable's contents in an unmodifiable collection
    * 
    * @see #snapshot(Iterator)
    */
   public static <E> Collection<E> snapshot(Iterable<? extends E> iterable) {
      HeadChunk head = toChunks(iterable.iterator(), sizer(iterable));
      return new ChunksAsCollection<E>(head);
   }

   /**
    * Creates a snapshot of the given iterator. This involves exhausting the elements and creating
    * an unmodifiable collection with all of its contents. On return, the iterator will have been
    * exhausted and {@link Iterator#hasNext()} will return false.
    * 
    * <p>The snapshot is created with as little overhead as possible using a strategy like used by
    * {@link ArrayList}, except using linked nodes of arrays (each larger than the previous) instead
    * of resizing a single array (so no copies need be made when the initial array gets full). The
    * initial array is sized based on the given iterable's {@linkplain #trySize(Iterable)} size or
    * 16 if the size is unknown.
    *
    * @param iterator an iterator
    * @return a snapshot of the iterator's contents in an unmodifiable collection
    * @see #snapshot(Iterable)
    */
   public static <E> Collection<E> snapshot(Iterator<? extends E> iterator) {
      HeadChunk head = toChunks(iterator, null);
      return new ChunksAsCollection<E>(head);
   }

   /**
    * Creates a snapshot of the given iterable as an array. Implementations that already have a
    * {@code toArray} method, like {@link Collection} and {@link ImmutableCollection}, will just use
    * that existing implementation.
    * 
    * <p>MoreIterables that do not provide such a method must resort to a custom mechanism. A
    * {@linkplain #snapshot(Iterable) snapshot} of the iterable is captured, and then a new array is
    * created from the contents of that snapshot. So there is <em>O(n)</em> memory overhead, in
    * addition to the <em>O(n)</em> memory needed by the returned array.
    *
    * @param iterable an iterable
    * @return a snapshot of the iterable's contents in an array
    * 
    * @see Collection#toArray()
    * @see #toArray(Iterator)
    * @see #toArray(Iterable, Object[])
    * @see Iterables#toArray(Iterable, Class)
    */
   public static Object[] toArray(Iterable<?> iterable) {
      Iterator<?> iter = iterable.iterator();
      if (!iter.hasNext()) {
         return EMPTY;
      } else if (iterable instanceof Collection) {
         return ((Collection<?>) iterable).toArray();
      } else {
         return toArrayInternal(iter, sizer(iterable));
      }
   }
      
   /**
    * Creates a snapshot of the given iterable as an array of the given type. The given array is
    * used if it is large enough. If it is too large, a null terminator will be written to the
    * location after the end of the given iterable's contents. If it is too small, a new array with
    * the same component type will be allocated and returned.
    *
    * @param iterable an iterable
    * @param array an array
    * @return a snapshot of the iterable's contents in an array
    * 
    * @see Collection#toArray(Object[])
    * @see #toArray(Iterator, Object[])
    * @see #toArray(Iterable)
    * @see Iterables#toArray(Iterable, Class)
    */
   public static <T> T[] toArray(Iterable<?> iterable, T[] array) {
      if (iterable instanceof Collection) {
         return ((Collection<?>) iterable).toArray(array);
      }
      
      Iterator<?> iter = iterable.iterator();
      if (!iter.hasNext()) {
         if (array.length > 0) {
            array[0] = null;
         }
         return array;
      }
      return toArrayInternal(iter, array, sizer(iterable));
   }
   
   /**
    * Creates a snapshot of the given iterator as an array.
    * 
    * <p>A {@linkplain #snapshot(Iterator) snapshot} of the iterator is captured, and then a new
    * array is created from the contents of that snapshot. So there is <em>O(n)</em> memory
    * overhead, in addition to the <em>O(n)</em> memory needed by the returned array.
    *
    * @param iter an iterator
    * @return a snapshot of the iterator's contents in an array
    * 
    * @see Collection#toArray()
    * @see #toArray(Iterator, Object[])
    * @see #toArray(Iterable)
    * @see Iterators#toArray(Iterator, Class)
    */
   public static Object[] toArray(Iterator<?> iter) {
      return toArrayInternal(iter, null);
   }
   
   private static Object[] toArrayInternal(Iterator<?> iter, IntSupplier sized) {
      HeadChunk head = toChunks(iter, sized);
      int size = head.size;
      if (head.next == null && head.contents.length == size) {
         // we captured it perfectly in first chunk, so just return the chunk's contents
         return head.contents;
      }
      Object elements[] = new Object[size];
      int currentIndex = 0;
      for (Chunk current = head; current != null; current = current.next) {
         if (current.next == null) {
            // last chunk
            int lastChunkSize = head.lastChunkSize;
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
   
   /**
    * Creates a snapshot of the given iterator as an array of the given type. The given array is
    * used if it is large enough. If it is too large, a null terminator will be written to the
    * location after the end of the given iterable's contents. If it is too small, a new array with
    * the same component type will be allocated and returned.
    * 
    * @param iter an iterator
    * @param array an array
    * @return a snapshot of the iterable's contents in an array
    * 
    * @see Collection#toArray(Object[])
    * @see #toArray(Iterator)
    * @see #toArray(Iterable, Object[])
    * @see Iterators#toArray(Iterator, Class)
    */
   public static <T> T[] toArray(Iterator<?> iter, T[] array) {
      return toArrayInternal(iter, array, null);
   }

   private static <T> T[] toArrayInternal(Iterator<?> iter, T[] array, IntSupplier sized) {
      Class<?> elementType = array.getClass().getComponentType();
      HeadChunk head = toChunks(iter, array, elementType, sized);
      int size = head.size;
      if (head.next == null) {
         if (head.contents.length == size) {
            // we captured it perfectly in first chunk, so just return the chunk's contents
            @SuppressWarnings("unchecked") // we made sure each chunk has right component type
            T ret[] = (T[]) head.contents;
            return ret;
         } else if (head.contents == array) {
            // it fit wholly in the given array, so just record a null terminator
            array[size] = null;
            return array;
         }
      }
      @SuppressWarnings("unchecked")
      T elements[] = (T[]) Array.newInstance(elementType, size);
      int currentIndex = 0;
      for (Chunk current = head; current != null; current = current.next) {
         if (current.next == null) {
            // last chunk
            int lastChunkSize = head.lastChunkSize;
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
   
   private static HeadChunk toChunks(Iterator<?> iter, IntSupplier sized) {
      int chunkLimit = sized != null ? sized.getAsInt() : 16;
      HeadChunk head = new HeadChunk(chunkLimit);
      Chunk current = head;
      int currentIndex = 0;
      while (iter.hasNext()) {
         if (currentIndex == chunkLimit) {
            head.size += currentIndex;
            if (head.size < 0) {
               // overflow
               throw new IllegalStateException(
                     "Too many elements. Max supported is " + Integer.MAX_VALUE);
            }
            int newSize;
            if (sized != null && (newSize = sized.getAsInt()) > head.size) {
               chunkLimit = newSize - head.size + 1;
            } else {
               // don't know by how much to grow? double capacity
               chunkLimit = head.size;
            }
            current.next = new Chunk(chunkLimit);
            current = current.next;
            currentIndex = 0;
         }
         Object o = iter.next();
         current.contents[currentIndex++] = o;
      }
      head.size += currentIndex;
      head.lastChunkSize = currentIndex;
      return head;
   }
   
   private static HeadChunk toChunks(Iterator<?> iter, Object array[], Class<?> elementType,
         IntSupplier sized) {
      int chunkLimit = sized != null ? sized.getAsInt() : 16;
      HeadChunk head =
            array.length > 0 ? new HeadChunk(array) : new HeadChunk(chunkLimit, elementType);
      Chunk current = head;
      int currentIndex = 0;
      while (iter.hasNext()) {
         if (currentIndex == chunkLimit) {
            head.size += currentIndex;
            int newSize;
            if (sized != null && (newSize = sized.getAsInt()) > head.size) {
               chunkLimit = newSize - head.size + 1;
            } else {
               // don't know by how much to grow? double capacity
               chunkLimit = head.size;
            }
            current.next = new Chunk(chunkLimit, elementType);
            current = current.next;
            currentIndex = 0;
         }
         Object o = iter.next();
         current.contents[currentIndex++] = o;
      }
      head.size += currentIndex;
      head.lastChunkSize = currentIndex;
      return head;
   }

   /**
    * A "chunk" in a growing array of elements. This is used to efficiently create a snapshot of
    * an iterable. The chunk is a linked structure, so the result of creating a snapshot closely
    * resembles an unrolled linked list, but the array in each node can vary in size from one node
    * to the next (generally getting larger and larger as the chunks are traversed).
    * 
    * @see MoreIterables#toChunks(Iterator, IntSupplier)
    * @see MoreIterables#toChunks(Iterator, Object[], IntSupplier)
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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
    * The "head" chunk, which is the first in a linked list of chunks. It includes extra
    * book-keeping fields that subsequent chunks do not need.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class HeadChunk extends Chunk {
      int size;
      int lastChunkSize;
      
      HeadChunk(Object array[]) {
         super(array);
      }
      
      HeadChunk(int limit) {
         super(limit);
      }
      
      HeadChunk(int limit, Class<?> elementType) {
         super(limit, elementType);
      }
   }

   /**
    * A collection view of a linked list of {@link Chunk}s.
    *
    * @param <E> the type of elements in the collection
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static final class ChunksAsCollection<E> extends AbstractCollection<E> {
      final HeadChunk head;
      
      ChunksAsCollection(HeadChunk head) {
         this.head = head;
      }
      
      @Override
      public Iterator<E> iterator() {
         return new Iterator<E>() {
            final int lastChunkSize = head.lastChunkSize;
            Chunk current = head;
            int idx = 0;
            
            @Override
            public boolean hasNext() {
               return current.next != null || idx < lastChunkSize;
            }

            @Override
            public E next() {
               if (current.next == null && idx >= lastChunkSize) {
                  throw new NoSuchElementException();
               }
               @SuppressWarnings("unchecked")
               E ret = (E) current.contents[idx++];
               if (idx >= current.contents.length && current.next != null) {
                  current = current.next;
                  idx = 0;
               }
               return ret;
            }
         };
      }

      @Override
      public int size() {
         return head.size;
      }
   }
   
   /**
    * Casts the given iterator to one whose elements are a super-type.
    *
    * @param iterator an iterator
    * @return the given iterator, but with a generic type that is a super-type of the one given
    */
   @SuppressWarnings("unchecked")
   public static <E> Iterator<E> cast(Iterator<? extends E> iterator) {
      return (Iterator<E>) iterator;
   }
   
   /**
    * Casts the given iterable to one whose elements are a super-type.
    *
    * @param iterable an iterable
    * @return the given iterable, but with a generic type that is a super-type of the one given
    */
   @SuppressWarnings("unchecked")
   public static <E> Iterable<E> cast(Iterable<? extends E> iterable) {
      return (Iterable<E>) iterable;
   }

   /**
    * Pushes the given element onto the head of the given iterator. This returns a new iterator that
    * first yields the pushed element and then yields the elements in the given iterator.
    * 
    * <p>If an attempt is made to remove the first element (the newly pushed element) via the
    * iterator's {@link Iterator#remove() remove} method then an
    * {@link UnsupportedOperationException} is thrown. Removal of other elements will delegate to
    * the underlying iterator.
    *
    * @param iterator an iterator
    * @param pushed the element to push to the head of the iterator
    * @return a new iterator, with the given element pushed in front
    */
   public static <E> Iterator<E> push(Iterator<? extends E> iterator, E pushed) {
      return new ConsIterator<E>(pushed, iterator, null);
   }

   /**
    * Pushes the given element onto the head of the given iterator. This returns a new iterator that
    * first yields the pushed element and then yields the elements in the given iterator.
    * 
    * 
    * <p>If an attempt is made to remove the first element (the newly pushed element) via the
    * iterator's {@link Iterator#remove() remove} method then the given removal handler is invoked.
    *
    * @param iterator an iterator
    * @param pushed the element to push to the head of the iterator
    * @param onRemove a removal handler
    * @return a new iterator, with the given element pushed in front
    */
   public static <E> Iterator<E> push(Iterator<? extends E> iterator, E pushed, Runnable onRemove) {
      return new ConsIterator<E>(pushed, iterator, onRemove);
   }

   /**
    * Pushes the given elements onto the head of the given iterator. This returns a new iterator
    * that first yields the pushed elements, in the given order, and then yields the elements in the
    * given iterator.
    * 
    * <p>Attempts to remove the pushed elements, using the returned iterator's
    * {@link Iterator#remove() remove} method, will throw {@link UnsupportedOperationException}s.
    *
    * @param iterator an iterator
    * @param pushed the elements to push to the head of the iterator; the first element is emitted
    *       by the returned iterator first, and then the next and so on
    * @return a new iterator, with the given elements pushed in front
    * @see #push(Iterator, Iterable)
    */
   @SafeVarargs
   public static <E> Iterator<E> push(Iterator<? extends E> iterator, E... pushed) {
      return push(iterator, Arrays.asList(pushed));
   }

   /**
    * Pushes the given elements onto the head of the given iterator. This returns a new iterator
    * that first yields the pushed elements, in the order they are emitted during iteration, and
    * then yields the elements in the given iterator.
    * 
    * <p>Attempts to remove the pushed elements, using the returned iterator's
    * {@link Iterator#remove() remove} method, will remove them from the given iterable if possible
    * by delegating to an iterator over the given pushed elements.
    *
    * @param iterator an iterator
    * @param pushed the elements to push to the head of the iterator
    * @return a new iterator, with the given elements pushed in front
    * @see #push(Iterator, Object...)
    */
   public static <E> Iterator<E> push(Iterator<? extends E> iterator, Iterable<E> pushed) {
      return Iterators.concat(pushed.iterator(), iterator);
   }
   
   /**
    * Expands the given sequence using the given function. Each element in the given iterator is
    * transformed into another sequence via the function. The resulting iterator concatenates all
    * such resulting sequences.
    *
    * @param iterator an iterator
    * @param fn a function that expands each element into another sequence
    * @return an iterator that the results of expanding each element
    */
   public static <T, U> Iterator<U> flatMap(Iterator<T> iterator,
         Function<? super T, ? extends Iterator<? extends U>> fn) {
      return Iterators.concat(Iterators.transform(iterator, fn::apply));
   }
   
   /**
    * An iterator constructed from a given head element and given tail iterator.
    *
    * @param <E> the type of the value emitted
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ConsIterator<E> implements Iterator<E> {
      E head;
      final Iterator<? extends E> tail;
      Runnable onRemoveHead;
      int fetched;
      
      ConsIterator(E head, Iterator<? extends E> tail, Runnable onRemoveHead) {
         this.tail = tail;
         this.head = head;
         this.onRemoveHead = onRemoveHead;
      }

      @Override
      public boolean hasNext() {
         return fetched == 0 || tail.hasNext();
      }

      @Override
      public E next() {
         fetched++;
         if (fetched == 1) {
            // first element
            E ret = head;
            head = null; // let GC do its thang
            return ret;
         } else if (fetched == 2) {
            onRemoveHead = null; // won't need this anymore
         }
         return tail.next();
      }
      
      @Override
      public void remove() {
         if (fetched == 0) {
            throw new IllegalStateException();
         }
         if (fetched == 1) {
            if (onRemoveHead == null) {
               throw new UnsupportedOperationException();
            } else {
               onRemoveHead.run();
            }
         } else {
            tail.remove();
         }
      }
   }
}
