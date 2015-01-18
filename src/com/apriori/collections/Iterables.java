package com.apriori.collections;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Utility methods for working with instances of {@link Iterable} and {@link Iterator}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public final class Iterables {
   private Iterables() {
   }
   
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
   
   /**
    * Adds the elements from the given iterator to the given collection. On return, the iterator
    * will have been exhausted and {@link Iterator#hasNext()} will return false.
    *
    * @param <E> the type of elements retrieved from the given iterator
    * @param iter an iterator
    * @param coll a collection
    * @return true if the collection was modified; false if the iterator yielded no elements or if
    *       the collection was not modified by the additions (e.g. {@link Collection#add(Object)}
    *       returns false for all elements)
    * @see #addTo(Iterable, Collection)
    */
   public static <E> boolean addTo(Iterator<E> iter, Collection<? super E> coll) {
      boolean ret = false;
      while (iter.hasNext()) {
         if (coll.add(iter.next())) {
            ret = true;
         }
      }
      return ret;
   }
   
   /**
    * Adds the elements from the given iterable to the given collection.
    *
    * @param <E> the type of elements in the given iterable
    * @param iter an iterable
    * @param coll a collection
    * @return true if the collection was modified; false if the iterable contained no elements or if
    *       the collection was not modified by the additions (e.g. {@link Collection#add(Object)}
    *       returns false for all elements)
    * @see #addTo(Iterator, Collection)
    */
   public static <E> boolean addTo(Iterable<E> iter, Collection<? super E> coll) {
      if (iter instanceof Collection) {
         return coll.addAll((Collection<E>) iter);
      } else if (iter instanceof SizedIterable) {
         return coll.addAll(SizedIterable.toCollection((SizedIterable<E>) iter));
      } else {
         return addTo(iter.iterator(), coll);
      }
   }

   /**
    * Returns true if the given iterable contains the given object.
    *
    * @param iter an iterable
    * @param o an object
    * @return true if the given object was found in the given iterable
    * 
    * @see Collection#contains(Object)
    * @see #contains(Iterator, Object)
    */
   public static boolean contains(Iterable<?> iter, Object o) {
      if (iter instanceof Collection) {
         return ((Collection<?>) iter).contains(o);
      } else if (iter instanceof ImmutableCollection) {
         return ((ImmutableCollection<?>) iter).contains(o);
      } else if (iter instanceof ImmutableMap) {
         return ((ImmutableMap<?, ?>) iter).entrySet().contains(o);
      } else {
         return contains(iter.iterator(), o);
      }
   }

   /**
    * Returns true if the given iterator contains the given object. On return, the iterator will
    * have been exhausted and {@link Iterator#hasNext()} will return false.
    *
    * @param iter an iterator
    * @param o an object
    * @return true if the given object was found in the given iterator
    * 
    * @see Collection#contains(Object)
    * @see #contains(Iterable, Object)
    */
   public static boolean contains(Iterator<?> iter, Object o) {
      while (iter.hasNext()) {
         Object obj= iter.next();
         if (Objects.equals(obj, o)) {
            return true;
         }
      }
      return false;
   }
   
   /**
    * Returns an iterator that yields the given single element and then no more. An attempt to
    * remove the element using the iterator's {@link Iterator#remove() remove} method will throw
    * {@link UnsupportedOperationException}.
    *
    * @param <E> the type of the element
    * @param element an element
    * @return an iterator that yields only the one element
    */
   public static <E> Iterator<E> singletonIterator(E element) {
      return new SingletonIterator<E>(element, null);
   }

   /**
    * Returns an iterator that yields the given single element and then no more. An attempt to
    * remove the element using the iterator's {@link Iterator#remove() remove} method will invoke
    * the given removal handler.
    *
    * @param <E> the type of the element
    * @param element an element
    * @param onRemove a removal handler, invoked when the iterator's
    *       {@link Iterator#remove() remove} method is called
    * @return an iterator that yields only the one element
    */
   public static <E> Iterator<E> singletonIterator(E element, Runnable onRemove) {
      return new SingletonIterator<E>(element, onRemove);
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
   
   private static IntSupplier sizer(Iterable<?> iterable) {
      if (iterable instanceof Collection) {
         Collection<?> coll = (Collection<?>) iterable; 
         return coll::size;
      } else if (iterable instanceof SizedIterable) {
         SizedIterable<?> szd = (SizedIterable<?>) iterable; 
         return szd::size;
      } else {
         return null;
      }
   }
   
   /**
    * Queries for the size of the given iterable, if known. If the given iterable implements
    * {@link Collection} or {@link SizedIterable} then the size is returned. Otherwise, an absent
    * value is returned, indicating that the size is not known.
    *
    * @param iterable an iterable
    * @return the size of the given iterable if known
    * @see #size(Iterable)
    */
   public static OptionalInt trySize(Iterable<?> iterable) {
      if (iterable instanceof Collection) {
         return OptionalInt.of(((Collection<?>) iterable).size());
      } else if (iterable instanceof SizedIterable) {
         return OptionalInt.of(((SizedIterable<?>) iterable).size());
      } else {
         return OptionalInt.empty();
      }
   }
   
   /**
    * Queries for the size of the given iterable, iterating and counting if necessary. If the given
    * iterable implements {@link Collection} or {@link SizedIterable} then its size method is
    * interrogated. Otherwise, an iteration is made and the elements are counted.
    *
    * @param iterable an iterable
    * @return the size of the given iterable
    * @see #trySize(Iterable)
    * @see #size(Iterator)
    */
   public static int size(Iterable<?> iterable) {
      if (iterable instanceof Collection) {
         return ((Collection<?>) iterable).size();
      } else if (iterable instanceof SizedIterable) {
         return ((SizedIterable<?>) iterable).size();
      } else {
         return size(iterable.iterator());
      }
   }

   /**
    * Counts the number of elements in the given iterator. On return, the iterator will have been
    * exhausted and {@link Iterator#hasNext()} will return false.
    *
    * @param iterator an iterator
    * @return the size of the given iterator
    * @see #size(Iterable)
    */
   public static int size(Iterator<?> iterator) {
      int sz = 0;
      while (iterator.hasNext()) {
         iterator.next();
         sz++;
      }
      return sz;
   }

   /**
    * Creates a snapshot of the given iterable. This involves iterating over the elements and
    * creating an unmodifiable collection with all of its contents.
    * 
    * <p>The snapshot is created with as little overhead as possible using a strategy like used by
    * {@link ArrayList}, except using linked nodes of arrays (each larger than the previous) instead
    * of resizing a single array (so no copies need be made when the initial array gets full). The
    * initial array is sized based on the given iterable's {@linkplain #trySize(Iterable)} size or
    * 16 if the size is unknown.
    *
    * @param iterable an iterable
    * @return a snapshot of the iterable's contents in an unmodifiable collection
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
    * <p>Iterables that do not provide such a method must resort to a custom mechanism. A
    * {@linkplain #snapshot(Iterable) snapshot} of the iterable is captured, and then a new array is
    * created from the contents of that snapshot. So there is <em>O(n)</em> memory overhead, in
    * addition to the <em>O(n)</em> memory needed by the returned array.
    *
    * @param iterable an iterable
    * @return a snapshot of the iterable's contents in an array
    * @see Collection#toArray()
    * @see #toArray(Iterator)
    * @see #toArray(Iterable, Object[])
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
    * @see Collection#toArray(Object[])
    * @see #toArray(Iterator, Object[])
    * @see #toArray(Iterable)
    */
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
         return toArrayInternal(iter, array, sizer(iterable));
      }
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
    * @see Collection#toArray()
    * @see #toArray(Iterator, Object[])
    * @see #toArray(Iterable)
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
    * @see Collection#toArray(Object[])
    * @see #toArray(Iterator)
    * @see #toArray(Iterable, Object[])
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
    * @see Iterables#toChunks(Iterator, IntSupplier)
    * @see Iterables#toChunks(Iterator, Object[], IntSupplier)
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
    * Casts the given sized iterable to one whose elements are a super-type.
    *
    * @param iterable an sized iterable
    * @return the given iterable, but with a generic type that is a super-type of the one given
    */
   @SuppressWarnings("unchecked")
   public static <E> SizedIterable<E> cast(SizedIterable<? extends E> iterable) {
      return (SizedIterable<E>) iterable;
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
      return concat(pushed.iterator(), iterator);
   }

   /**
    * Concatenates all of the given iterators into one. Iteration order will yield all elements in
    * the first given iterator, and then the next and so on.
    *
    * <p>Attempts to remove the elements using {@link Iterator#remove()} will remove them from the
    * underlying iterator that emitted them.
    * 
    * @param iterators an array of iterators
    * @return a view of the array of iterators as a single iterators with all elements concatenated
    *       into a new sequence
    * @see #concat(Iterable)
    */
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
            return new FlatMapIterator<>(Arrays.asList(iterators).iterator(), Function.identity());
      }
   }

   /**
    * Concatenates all of the given iterators into one. Iteration order will yield all elements in
    * the first given iterator, and then the next and so on.
    *
    * <p>Attempts to remove the elements using {@link Iterator#remove()} will remove them from the
    * underlying iterator that emitted them.
    * 
    * @param iterators a sequence of iterators
    * @return a view of the array of iterators as a single iterators with all elements concatenated
    *       into a new sequence
    * @see #concat(Iterator...)
    */
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
      return new FlatMapIterator<>(push(iter, first, second), Function.identity());
   }
   
   // TODO: doc
   
   @SafeVarargs
   public static <E> Iterable<E> concatIterables(Iterable<? extends E>... iterables) {
      return concatIterables(Arrays.asList(iterables));
   }

   public static <E> Iterable<E> concatIterables(Iterable<Iterable<? extends E>> iterables) {
      return () -> new FlatMapIterator<Iterable<? extends E>, E>(iterables.iterator(),
            Iterable::iterator);
   }

   /**
    * Returns a view of the first <em>n</em> elements of the given iterable. This iterable will
    * yield elements from the given iterable until either the underlying iterable is exhausted
    * or it has yielded the given number of elements.
    *
    * @param iterable an iterable
    * @param n the maximum number of elements to yield
    * @return an iterable that contains up to the first <em>n</em> elements of the given iterable
    * @see #upToN(Iterator, int)
    */
   public static <E> Iterable<E> upToN(Iterable<E> iterable, int n) {
      return () -> upToN(iterable.iterator(), n);
   }
   
   /**
    * Returns a view of the first <em>n</em> elements of the given iterator. This iterator will
    * yield elements from the given iterator until either the underlying iterator is exhausted
    * or it has yielded the given number of elements.
    *
    * @param iterator an iterator
    * @param n the maximum number of elements to yield
    * @return an iterator that yields up to the first <em>n</em> elements of the given iterator
    * @see #upToN(Iterable, int)
    */
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
      return new FlatMapIterator<>(iterator, fn);
   }

   /**
    * An iterator that emits exactly one element.
    *
    * @param <E> the type of the value emitted
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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

   /**
    * An iterator that concatenates the elements from exactly two other iterators.
    *
    * @param <E> the type of the value emitted
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
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
   
   /**
    * An iterator that concatenates multiple iterators. A function is applied to elements of a given
    * iterator to expand them into constituent iterators. Iteration yields elements from these
    * constituent iterators.
    *
    * @param <T> the type of the given iterator
    * @param <U> the type of the values emitted
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class FlatMapIterator<T, U> implements Iterator<U> {
      private final Iterator<? extends T> iterator;
      private final Function<? super T, ? extends Iterator<? extends U>> fn;
      Iterator<? extends U> current;
      Iterator<? extends U> lastFetched;
      
      FlatMapIterator(Iterator<? extends T> iterator,
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
