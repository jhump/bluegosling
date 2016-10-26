package com.bluegosling.collections;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntSupplier;

import com.bluegosling.function.TriFunction;
import com.google.common.collect.Iterators;

/**
 * Utility methods for working with and creating instanceos of {@link Iterator}. These methods
 * complement those in Guava's {@link Iterators} class.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class MoreIterators {

   /**
    * Returns a view of the given list iterator that does not support the mutator methods
    * {@code add}, {@code set}, and {@code remove}). If these methods are invoked, an
    * {@link UnsupportedOperationException} is thrown. This is just like the Guava method of
    * the same name in {@link Iterators#unmodifiableIterator(Iterator) Iterators} except that it
    * supports the full {@link ListIterator} interface.
    * 
    * @param iter a list iterator
    * @return a view of the given iterator that does not support modifications to the underlying
    *       collection
    * @see Iterators#unmodifiableIterator(Iterator)
    */
   public static <E> ListIterator<E> unmodifiableListIterator(ListIterator<? extends E> iter) {
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
   @SuppressWarnings("varargs") // for javac
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
    * A "chunk" in a growing array of elements. This is used to efficiently create a snapshot of
    * an iterable. The chunk is a linked structure, so the result of creating a snapshot closely
    * resembles an unrolled linked list, but the array in each node can vary in size from one node
    * to the next (generally getting larger and larger as the chunks are traversed).
    * 
    * @see MoreIterators#toChunks(Iterator, IntSupplier)
    * @see MoreIterators#toChunks(Iterator, Object[], IntSupplier)
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class Chunk {
      final Object contents[];
      Chunk next;
      
      Chunk(Object array[]) {
         contents = array;
      }
      
      Chunk(int limit) {
         this(new Object[limit]);
      }
      
      Chunk(int limit, Class<?> elementType) {
         this((Object[]) Array.newInstance(elementType, limit));
      }
   }

   /**
    * The "head" chunk, which is the first in a linked list of chunks. It includes extra
    * book-keeping fields that subsequent chunks do not need.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   static class HeadChunk extends Chunk {
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
   static final class ChunksAsCollection<E> extends AbstractCollection<E> {
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

   static <E> Iterator<E> unique(Iterator<E> iter, OptionalInt size) {
      Set<E> items = size.isPresent() ? new HashSet<>(size.getAsInt() * 4 / 3) : new HashSet<>();
      return new FilteringIterator<>(iter, e -> items.add(e));
   }

   /**
    * Creates a snapshot of the given iterator. This involves exhausting the elements and creating
    * an unmodifiable collection with all of its contents. On return, the iterator will have been
    * exhausted and {@link Iterator#hasNext()} will return false.
    * 
    * <p>The snapshot is created with as little overhead as possible using a strategy like used by
    * {@link ArrayList}, except using linked nodes of arrays (each larger than the previous) instead
    * of resizing a single array (so no copies need be made when the initial array gets full). The
    * initial array is sized based on the given iterable's {@linkplain MoreIterables#trySize(Iterable)} size or
    * 16 if the size is unknown.
    *
    * @param iterator an iterator
    * @return a snapshot of the iterator's contents in an unmodifiable collection
    * @see MoreIterables#snapshot(Iterable)
    */
   public static <E> Collection<E> snapshot(Iterator<? extends E> iterator) {
      HeadChunk head = toChunks(iterator, null);
      return new ChunksAsCollection<E>(head);
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
    * @see MoreIterables#toArray(Iterable)
    * @see Iterators#toArray(Iterator, Class)
    */
   public static Object[] toArray(Iterator<?> iter) {
      return toArrayInternal(iter, null);
   }

   static Object[] toArrayInternal(Iterator<?> iter, IntSupplier sized) {
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
    * @see MoreIterables#toArray(Iterable, Object[])
    * @see Iterators#toArray(Iterator, Class)
    */
   public static <T> T[] toArray(Iterator<?> iter, T[] array) {
      return toArrayInternal(iter, array, null);
   }

   static <T> T[] toArrayInternal(Iterator<?> iter, T[] array, IntSupplier sized) {
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

   static HeadChunk toChunks(Iterator<?> iter, IntSupplier sized) {
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

   // TODO: doc ...
   
   public static <T, U, V> Iterator<V> zip(Iterator<? extends T> iter1,
         Iterator<? extends U> iter2, BiFunction<? super T, ? super U, ? extends V> fn) {
      return new Iterator<V>() {
         @Override
         public boolean hasNext() {
            return iter1.hasNext() && iter2.hasNext();
         }
   
         @Override
         public V next() {
            T t = iter1.next();
            U u = iter2.next();
            return fn.apply(t, u);
         }
      };
   }

   public static <T, U, V, W> Iterator<W> zip(Iterator<? extends T> iter1,
         Iterator<? extends U> iter2, Iterator<? extends V> iter3,
         TriFunction<? super T, ? super U, ? super V, ? extends W> fn) {
      return new Iterator<W>() {
         @Override
         public boolean hasNext() {
            return iter1.hasNext() && iter2.hasNext() && iter3.hasNext();
         }
   
         @Override
         public W next() {
            T t = iter1.next();
            U u = iter2.next();
            V v = iter3.next();
            return fn.apply(t, u, v);
         }
      };
   }

   @SafeVarargs
   @SuppressWarnings("varargs") // for javac
   public static <T> Iterator<List<T>> zip(Iterator<? extends T>... iterables) {
      return zip(Arrays.asList(iterables).iterator(), iterables.length);
   }

   public static <T> Iterator<List<T>> zip(Iterable<? extends Iterator<? extends T>> i) {
      return zip(i.iterator(), MoreIterables.trySize(i).orElse(8));
   }

   public static <T> Iterator<List<T>> zip(Iterator<? extends Iterator<? extends T>> i) {
      return zip(i, 8);
   }

   static <T> Iterator<List<T>> zip(Iterator<? extends Iterator<? extends T>> i,
         int countHint) {
      if (!i.hasNext()) {
         return Collections.emptyIterator();
      }
      ArrayList<Iterator<? extends T>> iters = new ArrayList<>(countHint);
      Iterators.addAll(iters, i);
      iters.trimToSize();
      return new Iterator<List<T>>() {
         @Override
         public boolean hasNext() {
            for (Iterator<? extends T> i : iters) {
               if (!i.hasNext()) {
                  return false;
               }
            }
            return true;
         }
   
         @Override
         public List<T> next() {
            List<T> l = new ArrayList<>(iters.size());
            for (Iterator<? extends T> i : iters) {
               l.add(i.next());
            }
            return Collections.unmodifiableList(l);
         }
      };
   }

   /**
    * Returns an iterator that traverses elements in the opposite order of the specified iterator.
    * In other words, {@link ListIterator#next()} return the <em>previous</em> element and vice
    * versa.
    * 
    * <p>The returned iterator will support all operations that the underlying iterator supports,
    * including {@code add} and {@code remove}. Adding multiple elements in a row from the reversed
    * iterator effectively adds them in reverse order.
    * 
    * @param iter an iterator
    * @return a reversed iterator
    */
   public static <E> ListIterator<E> reverseListIterator(ListIterator<E> iter) {
      // wrap the list iterator with a simple version that
      // just iterates backwards
      return new ListIterator<E>() {
         private boolean added;
         
         @Override
         public void add(E e) {
            iter.add(e);
            // Add places item before the result returned by subsequent call to next() which means
            // newly added element is returned by call to previous(). To reverse (and make sure
            // multiple additions effectively inserts items in the right place in reverse order),
            // we have to adjust the cursor:
            iter.previous();
            // Underlying iterator would now allow a remove() or set() operation and act on the item
            // we just added, but proper behavior is to disallow the operation. We have to manage
            // that ourselves.
            added = true;
         }
   
         @Override
         public boolean hasNext() {
            return iter.hasPrevious();
         }
   
         @Override
         public boolean hasPrevious() {
            return iter.hasNext();
         }
   
         @Override
         public E next() {
            added = false; // reset
            return iter.previous();
         }
   
         @Override
         public int nextIndex() {
            return iter.previousIndex();
         }
   
         @Override
         public E previous() {
            added = false; // reset
            return iter.next();
         }
   
         @Override
         public int previousIndex() {
            return iter.nextIndex();
         }
   
         @Override
         public void remove() {
            if (added) {
               throw new IllegalStateException("Cannot remove item after call to add()");
            }
            iter.remove();
         }
   
         @Override
         public void set(E e) {
            if (added) {
               throw new IllegalStateException("Cannot set item after call to add()");
            }
            iter.set(e);
         }
      };
   }
}
