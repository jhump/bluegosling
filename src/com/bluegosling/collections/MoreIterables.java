package com.bluegosling.collections;

import com.bluegosling.collections.views.TransformingIterator;
import com.bluegosling.function.TriFunction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntSupplier;

/**
 * Utility methods for working with and creating instances of {@link Iterable}. These methods
 * complement those in Guava's {@link Iterables} class.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
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
   
   /**
    * Returns an iterable that omits duplicates from the given iterable. Note that the tracking of
    * duplicates requires state that can use <em>O(n)</em> amount of memory for each iteration.
    *
    * @param <E> the type of elements in the iterable
    * @param iter an iterable
    * @return an iterable that returns the items of the given iterable but omits duplicates
    */
   public static <E> Iterable<E> unique(Iterable<E> iter) {
      return () -> MoreIterators.unique(iter.iterator(), trySize(iter));
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
         return MoreIterators.reverseListIterator(list.listIterator(list.size()));
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
         return MoreIterators.reverseListIterator(list.listIterator(list.size()));
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
    * @see MoreIterators#snapshot(Iterator)
    */
   public static <E> Collection<E> snapshot(Iterable<? extends E> iterable) {
      MoreIterators.HeadChunk head = MoreIterators.toChunks(iterable.iterator(), sizer(iterable));
      return new MoreIterators.ChunksAsCollection<E>(head);
   }

   /**
    * Creates a snapshot of the given iterable as an array. Implementations that already have a
    * {@code toArray} method, like {@link Collection}, will just use that existing implementation.
    * 
    * <p>Iterables that do not provide such a method must resort to a custom mechanism. A
    * {@linkplain #snapshot(Iterable) snapshot} of the iterable is captured, and then a new array is
    * created from the contents of that snapshot. So there is <em>O(n)</em> memory overhead, in
    * addition to the <em>O(n)</em> memory needed by the returned array.
    *
    * @param iterable an iterable
    * @return a snapshot of the iterable's contents in an array
    * 
    * @see Collection#toArray()
    * @see MoreIterators#toArray(Iterator)
    * @see #toArray(Iterable, Object[])
    * @see Iterables#toArray(Iterable, Class)
    */
   public static Object[] toArray(Iterable<?> iterable) {
      if (iterable instanceof Collection) {
         return ((Collection<?>) iterable).toArray();
      }
      Iterator<?> iter = iterable.iterator();
      return iter.hasNext() ? MoreIterators.toArrayInternal(iter, sizer(iterable)) : EMPTY;
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
    * @see MoreIterators#toArray(Iterator, Object[])
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
      return MoreIterators.toArrayInternal(iter, array, sizer(iterable));
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
    * Expands the given sequence using the given function. Each element in the given iterator is
    * transformed into another sequence via the function. The resulting iterator concatenates all
    * such resulting sequences.
    *
    * @param iterator an iterator
    * @param fn a function that expands each element into another sequence
    * @return an iterator that the results of expanding each element
    */
   public static <T, U> Iterable<U> flatMap(Iterable<T> iterable,
         Function<? super T, ? extends Iterator<? extends U>> fn) {
      return () -> MoreIterators.flatMap(iterable.iterator(), fn);
   }

   // TODO: doc
   
   public static <T, U, V> Iterable<V> zip(Iterable<? extends T> c1, Iterable<? extends U> c2,
         BiFunction<? super T, ? super U, ? extends V> fn) {
      return new Iterable<V>() {
         @Override
         public Iterator<V> iterator() {
            return MoreIterators.zip(c1.iterator(), c2.iterator(), fn);
         }
      };
   }

   public static <T, U, V, W> Iterable<W> zip(Iterable<? extends T> c1, Iterable<? extends U> c2,
         Iterable<? extends V> c3, TriFunction<? super T, ? super U, ? super V, ? extends W> fn) {
      return new Iterable<W>() {
         @Override
         public Iterator<W> iterator() {
            return MoreIterators.zip(c1.iterator(), c2.iterator(), c3.iterator(), fn);
         }
      };
   }

   @SafeVarargs
   @SuppressWarnings("varargs") // for javac
   public static <T> Iterable<List<T>> zip(Iterable<? extends T>... colls) {
      return zip(Arrays.asList(colls));
   }

   public static <T> Iterable<List<T>> zip(Iterable<? extends Iterable<? extends T>> colls) {
      return new Iterable<List<T>>() {
         @SuppressWarnings("varargs") // for javac
         @Override
         public Iterator<List<T>> iterator() {
            return MoreIterators.zip(new TransformingIterator<>(colls.iterator(),
                  // javac warns of rawtype use with method reference
                  i -> i.iterator()),
                  trySize(colls).orElse(8));
         }
      };
   }
}
