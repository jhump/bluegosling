package com.bluegosling.collections.views;

import com.bluegosling.collections.CollectionUtils;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

/**
 * A list whose elements are the results of applying a function to another list. This list is simply
 * a wrapper. Changes to the underlying list are visible in the transformed list. Accessing elements
 * incurs calls to the transforming function.
 * 
 * <p>Functions that perform the transformations should be deterministic so that a stable,
 * unchanging iterable does not appear to be mutating when accessed through this transforming
 * wrapper.
 * 
 * <p>Since transformations can only be done in one direction, some operations are not supported.
 * Namely, {@link #add(Object)}, {@link #add(int, Object)}, {@link #addAll(Collection)},
 * {@link #addAll(int, Collection)}, and {@link #set(int, Object)} throw
 * {@link UnsupportedOperationException}. Similarly, {@link ListIterator} instances returned by
 * this list will throw exceptions if {@link ListIterator#add(Object)} or
 * {@link ListIterator#set(Object)} is invoked.
 * 
 * <p>Also due to transformations only working in one direction, some methods are implemented in
 * terms of the collection's {@linkplain #iterator() transforming iterator} and thus may have worse
 * performance than the underlying collection's implementation. These methods include
 * {@link #contains(Object)}, {@link #containsAll(Collection)}, {@link #remove(Object)},
 * {@link #removeAll(Collection)}, and {@link #retainAll(Collection)}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <I> the "input" type; the type of the wrapped list
 * @param <O> the "output" type; the type of elements in this list
 * 
 * @see TransformingList.ReadOnly
 * @see TransformingList.Bidi
 * @see Lists#transform(List, com.google.common.base.Function)
 */
//TODO: tests
public class TransformingList<I, O> extends TransformingCollection<I, O> implements List<O> {

   /**
    * Constructs a new transforming list.
    * 
    * @param list the wrapped list
    * @param function the function used to transform elements
    */
   public TransformingList(List<I> list, Function<? super I, ? extends O> function) {
      super(list, function);
   }

   /**
    * Gets the wrapped list.
    * 
    * @return the wrapped list
    */
   @Override
   protected List<I> internal() {
      return (List<I>) super.internal();
   }
   
   @Override
   public boolean addAll(int index, Collection<? extends O> c) {
      throw new UnsupportedOperationException("addAll");
   }

   @Override
   public O get(int index) {
      return apply(internal().get(index));
   }

   @Override
   public O set(int index, O element) {
      throw new UnsupportedOperationException("set");
   }

   @Override
   public void add(int index, O element) {
      throw new UnsupportedOperationException("add");
   }

   @Override
   public O remove(int index) {
      return apply(internal().remove(index));
   }

   @Override
   public int indexOf(Object o) {
      int i = 0;
      for (O element : this) {
         if (o == null ? element == null : o.equals(element)) {
            return i;
         }
         i++;
      }
      return -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      int i = size() - 1;
      for (ListIterator<O> iterator = listIterator(size()); iterator.hasPrevious(); ) {
         O element = iterator.previous();
         if (o == null ? element == null : o.equals(element)) {
            return i;
         }
         i--;
      }
      return -1;
   }

   @Override
   public ListIterator<O> listIterator() {
      return new TransformingListIterator<I, O>(internal().listIterator(), function());
   }

   @Override
   public ListIterator<O> listIterator(int index) {
      return new TransformingListIterator<I, O>(internal().listIterator(index), function());
   }

   @Override
   public List<O> subList(int fromIndex, int toIndex) {
      return new TransformingList<I, O>(internal().subList(fromIndex, toIndex), function());
   }
   
   @Override
   public boolean equals(Object o) {
      return CollectionUtils.equals(this,  o);
   }
   
   @Override
   public int hashCode() {
      return CollectionUtils.hashCode(this);
   }
   
   /**
    * A transforming list that supports random access to its elements. Any {@link TransformingList}
    * that is wrapping a random access list supports efficient random access. But a standard
    * {@link TransformingList} is missing the {@link java.util.RandomAccess} marker interface that
    * is an important hint for many APIs (such as
    * {@link java.util.Collections#binarySearch(List, Object)}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped list
    * @param <O> the "output" type; the type of elements in this list
    */
   public static class RandomAccess<I, O> extends TransformingList<I, O>
         implements java.util.RandomAccess {
      /**
       * Constructs a new random access transforming list.
       * 
       * @param list the wrapped list (must implement {@link java.util.RandomAccess})
       * @param function the function used to transform elements
       */
      public <L extends List<I> & java.util.RandomAccess> RandomAccess(L list,
            Function<? super I, ? extends O> function) {
         super(list, function);
      }
   }

   /**
    * An unmodifiable transforming list. All mutation operations throw
    * {@link UnsupportedOperationException}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped list
    * @param <O> the "output" type; the type of elements in this list
    */
   public static class ReadOnly<I, O> extends TransformingList<I, O> {

      /**
       * Constructs a new read-only transforming list.
       * 
       * @param list the wrapped list
       * @param function the function used to transform elements
       */
      public ReadOnly(List<I> list, Function<? super I, ? extends O> function) {
         super(list, function);
      }
      
      @Override
      public Iterator<O> iterator() {
         return new TransformingIterator.ReadOnly<I, O>(internal().iterator(), function());
      }
      
      @Override
      public O remove(int index) {
         throw new UnsupportedOperationException("remove");
      }
      
      @Override
      public ListIterator<O> listIterator() {
         return new TransformingListIterator.ReadOnly<I, O>(internal().listIterator(), function());
      }

      @Override
      public ListIterator<O> listIterator(int index) {
         return new TransformingListIterator.ReadOnly<I, O>(internal().listIterator(index), function());
      }

      @Override
      public List<O> subList(int fromIndex, int toIndex) {
         return new TransformingList.ReadOnly<I, O>(internal().subList(fromIndex, toIndex), function());
      }
      
      /**
       * A read-only transforming list that also has the {@link java.util.RandomAccess} marker
       * interface.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       *
       * @param <I> the "input" type; the type of the wrapped list
       * @param <O> the "output" type; the type of elements in this list
       * 
       * @see TransformingList.RandomAccess
       */
      public static class RandomAccess<I, O> extends TransformingList.ReadOnly<I, O> {
         public <L extends List<I> & java.util.RandomAccess> RandomAccess(L list,
               Function<? super I, ? extends O> function) {
            super(list, function);
         }
      }
   }
   
   /**
    * A transforming list that can transform values in both directions. Since this list
    * can transform values in the other direction (output type -&gt; input type), all operations are
    * supported, including {@link #add(Object)} and {@link #addAll(Collection)}.
    * 
    * <p>Several methods are overridden to use the reverse function before delegating to the
    * underlying list. Since some of these interface methods actually accept any type of object
    * (instead of requiring the list's element type), this implementation could result in
    * {@link ClassCastException}s if objects with incorrect types are passed to them. These
    * methods are {@link #contains(Object)}, {@link #containsAll(Collection)},
    * {@link #remove(Object)}, {@link #removeAll(Collection)}, and {@link #retainAll(Collection)}.
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <I> the "input" type; the type of the wrapped list
    * @param <O> the "output" type; the type of elements in this list
    */
   public static class Bidi<I, O> extends TransformingList<I, O> {

      private final Function<? super O, ? extends I> function;
      
      /**
       * Constructs a new bidirectional transforming list. The two provided functions must be
       * inverses of one another. In other words, for any object {@code i} in the input domain,
       * {@code i} must <em>equal</em> {@code function2.apply(function1.apply(i))}. Similarly, for
       * any object {@code o} in the output domain, {@code o} must <em>equal</em>
       * {@code function1.apply(function2.apply(o))}.
       * 
       * @param list the wrapped list
       * @param function1 transforms elements; "input" -&gt; "output"
       * @param function2 the inverse of {@code function1}; "output" -&gt; "input"
       */
      public Bidi(List<I> list, Function<? super I, ? extends O> function1,
            Function<? super O, ? extends I> function2) {
         super(list, function1);
         this.function = function2;
      }
      
      /**
       * Transforms "output" elements into "input" elements. This is the opposite direction as
       * {@link #apply(Object)}.
       * 
       * @param input the object to transform, which should be of type {@code O}
       * @return the transformed result
       */
      @SuppressWarnings("unchecked")
      protected I unapply(Object input) {
         return function.apply((O) input);
      }
      
      /**
       * Transforms a collection of "output" elements into "input" elements.
       * 
       * @param c the collection to transform, whose elements should be of type {@code O}
       * @return the transformed results
       */
      @SuppressWarnings("unchecked")
      protected Collection<I> unapplyAll(Collection<?> c) {
         @SuppressWarnings("rawtypes")
         Collection rawCollection = c;
         return new TransformingCollection<O, I>(rawCollection, function);
      }
      
      @Override
      public boolean add(O e) {
         return internal().add(unapply(e));
      }

      @Override
      public boolean addAll(Collection<? extends O> c) {
         return internal().addAll(unapplyAll(c));
      }
      
      @Override
      public O set(int index, O element) {
         return apply(internal().set(index, unapply(element)));
      }

      @Override
      public void add(int index, O element) {
         internal().add(index, unapply(element));
      }
      
      @Override
      public boolean addAll(int index, Collection<? extends O> c) {
         return internal().addAll(index, unapplyAll(c));
      }

      @Override
      public boolean remove(Object o) {
         return internal().remove(unapply(o));
      }
      
      @Override
      public boolean contains(Object o) {
         return internal().contains(unapply(o));
      }

      @Override
      public boolean containsAll(Collection<?> c) {
         return internal().containsAll(unapplyAll(c));
      }
      
      @Override
      public boolean removeAll(Collection<?> c) {
         return internal().removeAll(unapplyAll(c));
      }

      @Override
      public boolean retainAll(Collection<?> c) {
         return internal().retainAll(unapplyAll(c));
      }

      @Override
      public ListIterator<O> listIterator() {
         return new TransformingListIterator.Bidi<I, O>(internal().listIterator(), function(), function);
      }

      @Override
      public ListIterator<O> listIterator(int index) {
         return new TransformingListIterator.Bidi<I, O>(internal().listIterator(index), function(), function);
      }
      
      @Override
      public List<O> subList(int fromIndex, int toIndex) {
         return new TransformingList.Bidi<I, O>(internal().subList(fromIndex, toIndex), function(), function);
      }
      
      /**
       * A bidirectional transforming list that also has the {@link java.util.RandomAccess} marker
       * interface.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       *
       * @param <I> the "input" type; the type of the wrapped list
       * @param <O> the "output" type; the type of elements in this list
       * 
       * @see TransformingList.RandomAccess
       */
      public static class RandomAccess<I, O> extends TransformingList.Bidi<I, O> {
         public <L extends List<I> & java.util.RandomAccess> RandomAccess(L list,
               Function<? super I, ? extends O> function1,
               Function<? super O, ? extends I> function2) {
            super(list, function1, function2);
         }
      }
   }
}
