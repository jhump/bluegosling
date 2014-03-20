package com.apriori.tuples;

import java.util.List;
import java.util.function.Function;

/**
 * Base interface implemented by all type-safe tuples. Tuples are immutable and provide a rich
 * type-safe interface for typical operations. This interface includes methods provided by every
 * tuple, regardless of the number of items.
 * 
 * <p>The enclosed sub-interfaces offer type-safe operations for instances with up to five items.
 * Tuples with more than five items have less rich operations for the sixth and later items and also
 * do not have the same level of type safety. If you need tuples with more than five items, consider
 * using a {@link List} instead.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 * 
 * @see Tuple.Ops1
 * @see Tuple.Ops2
 * @see Tuple.Ops3
 * @see Tuple.Ops4
 * @see Tuple.Ops5
 */
public interface Tuple extends Iterable<Object> {
   /**
    * Returns the number of items in the tuple.
    * 
    * @return the number of items in the tuple
    */
   int size();
   
   /**
    * Determines whether the tuple has any items.
    * 
    * @return true if the item no items; false if it has at least one item
    */
   boolean isEmpty();

   /**
    * Determines if this tuple contains the specified object. The tuple contains the object if any
    * of the tuple's items is equal to the object. (An item {@code e1} is <em>equal</em> to an
    * object {@code e2} if {@code (e1==null ? e2==null : e1.equals(e2))}.)
    * 
    * @param o the object
    * @return true if any of the items in this tuple are equal to the specified object
    */
   boolean contains(Object o);
   
   /**
    * Returns a view of this tuple as a list.
    * 
    * @return a list view of this tuple
    */
   List<?> asList();
   
   /**
    * Returns an array of the items in this tuple. The first item in the array is the first item in
    * the tuple, and so on.
    * 
    * @return an array of the items in this tuple
    */
   Object[] toArray();
   
   /**
    * Returns an array of the items in this tuple. The runtime type of the returned array is that of
    * the specified array. If the tuple's elements fits in the specified array, they are returned
    * therein. Otherwise, a new array is allocated with the runtime type of the specified array and
    * the size of this collection.
    * 
    * <p>If this tuple fits in the specified array with room to spare (i.e., the array has more
    * elements than this tuple), the element in the array immediately following the last element of
    * the tuple is set to {@code null}. (This is useful in determining the length of this tuple
    * <i>only</i> if the caller knows that this tuple does not contain any {@code null} elements.)
    * 
    * @param a an array with the desired runtime type
    * @return an array of the items in this tuple
    * @throws ArrayStoreException if the runtime type of the specified array is not a supertype of
    *       the runtime type of every element in this collection
    * @throws NullPointerException if the specified array is {@code null}
    * 
    * @see java.util.Collection#toArray(Object[])
    */
   <T> T[] toArray(T a[]);
   
   /**
    * Adds a new item to this tuple. The item will be appended and thus ordered last in the new
    * tuple. Since tuples are immutable, a new tuple is returned with the same items as this tuple
    * but with the specified item added to the end.
    * 
    * @param t the item to add
    * @return a new tuple that is the specified item appended to this tuple
    */
   <T> Ops1<?> add(T t);
   
   /**
    * Adds an item into the first position of this tuple. Existing items are shifted over so the
    * first item in this tuple is the second item in the returned tuple. Since tuples are immutable,
    * a new tuple is returned with the same items as this tuple but with the specified item inserted
    * as the first item.
    * 
    * @param t the item to insert
    * @return a new tuple that is the specified item followed by the items in this tuple
    */   
   <T> Ops1<T> insertFirst(T t);
   
   /**
    * Transforms every item in this tuple by applying the specified function to each one. Since
    * tuples are immutable, a new tuple is returned. The returned tuple will have the same number of
    * items as this tuple.
    * 
    * @param function the function to apply
    * @return a new tuple that is the result of applying the specified function to all elements of
    *       this tuple
    */
   <T> Tuple transformAll(Function<Object, ? extends T> function);
   
   // TODO: shift and reverse methods?
   
   /**
    * Compares this tuple with the specified object. If the specified object is also a {@link Tuple}
    * and has the same items as this tuple in the same order, then this tuple is equal to the
    * specified object. (Two elements {@code e1} and {@code e2} are <em>equal</em> if
    * {@code (e1==null ? e2==null : e1.equals(e2))}.)
    * 
    * @param o the object to which this tuple is compared
    * @return true if the specified object is a tuple and has the same items
    */
   @Override boolean equals(Object o);
   
   /**
    * Returns the hash code value for this tuple. The hash code should be computed like so:
    * {@code Arrays.hashCode(tuple.toArray())}. This is effectively the same as
    * {@link List#hashCode()} for a list with the same items and in the same order as this tuple.
    * 
    * @return the hash code for this tuple
    */
   @Override int hashCode();

   /**
    * Operations supported by tuples that have at least one element.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Ops1<A> extends Tuple {
      /**
       * Retrieves the first item in the tuple.
       * 
       * @return the first item in the tuple
       */
      A getFirst();
      
      /**
       * Sets the first item in the tuple. Since tuples are immutable, this returns a new tuple
       * with the first item changed.
       * 
       * @param t the new item
       * @return a new tuple with the first item set to the specified new item
       */
      <T> Ops1<T> setFirst(T t);
      
      /**
       * Removes the first item from the tuple. Since tuples are immutable, this returns a new
       * tuple with one less item.
       * 
       * @return a new tuple with the same items as this tuple except that the first item is removed
       */
      Tuple removeFirst();
      
      @Override <T> Ops2<A, ?> add(T t);
      
      @Override <T> Ops2<T, A> insertFirst(T t);
      
      /**
       * Inserts an item into the second position of this tuple. Since tuples are immutable, this
       * returns a new tuple with one more item.
       * 
       * <p>If this tuple has only one item then this is the same as calling {@link #add(Object)}.
       * Otherwise, this will insert an item after the first item but before the second.
       * 
       * @param t the new item
       * @return a new tuple with the same items as this tuple except the specified new item is
       *       inserted as the second item in the tuple
       */
      <T> Ops2<A, T> insertSecond(T t);
      
      @Override <T> Ops1<T> transformAll(Function<Object, ? extends T> function);
      
      /**
       * Transforms the first item in the tuple using the specified function. Other items in the
       * tuple are unchanged. Since tuples are immutable, this returns a new tuple.
       * 
       * @param function the function to apply
       * @return a new tuple with the first item set to the result of applying the specified
       *    function to the first item in this tuple
       */
      <T> Ops1<T> transformFirst(Function<? super A, ? extends T> function);
   }

   /**
    * Operations supported by tuples that have at least two elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Ops2<A, B> extends Ops1<A> {
      /**
       * Retrieves the second item in the tuple.
       * 
       * @return the second item in the tuple
       */
      B getSecond();
      
      @Override <T> Ops2<T, B> setFirst(T t);
      
      /**
       * Sets the second item in the tuple. Since tuples are immutable, this returns a new tuple
       * with the second item changed.
       * 
       * @param t the new item
       * @return a new tuple with the second item set to the specified new item
       */
      <T> Ops2<A, T> setSecond(T t);
      
      @Override Ops1<B> removeFirst();
      
      /**
       * Removes the second item from the tuple. Since tuples are immutable, this returns a new
       * tuple with one less item.
       * 
       * @return a new tuple with the same items as this tuple except that the second item is
       *       removed
       */
      Ops1<A> removeSecond();
      
      @Override <T> Ops3<A, B, ?> add(T t);
      
      @Override <T> Ops3<T, A, B> insertFirst(T t);
      
      @Override <T> Ops3<A, T, B> insertSecond(T t);
      
      /**
       * Inserts an item into the third position of this tuple. Since tuples are immutable, this
       * returns a new tuple with one more item.
       * 
       * <p>If this tuple has only two items then this is the same as calling {@link #add(Object)}.
       * Otherwise, this will insert an item after the second item but before the third.
       * 
       * @param t the new item
       * @return a new tuple with the same items as this tuple except the specified new item is
       *       inserted as the third item in the tuple
       */
      <T> Ops3<A, B, T> insertThird(T t);
      
      @Override <T> Ops2<T, T> transformAll(Function<Object, ? extends T> function);
      
      @Override <T> Ops2<T, B> transformFirst(Function<? super A, ? extends T> function);
      
      /**
       * Transforms the second item in the tuple using the specified function. Other items in the
       * tuple are unchanged. Since tuples are immutable, this returns a new tuple.
       * 
       * @param function the function to apply
       * @return a new tuple with the second item set to the result of applying the specified
       *    function to the second item in this tuple
       */
      <T> Ops2<A, T> transformSecond(Function<? super B, ? extends T> function);
   }

   /**
    * Operations supported by tuples that have at least three elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Ops3<A, B, C> extends Ops2<A, B> {
      /**
       * Retrieves the third item in the tuple.
       * 
       * @return the third item in the tuple
       */
      C getThird();
      
      @Override <T> Ops3<T, B, C> setFirst(T t);
      
      @Override <T> Ops3<A, T, C> setSecond(T t);
      
      /**
       * Sets the third item in the tuple. Since tuples are immutable, this returns a new tuple
       * with the third item changed.
       * 
       * @param t the new item
       * @return a new tuple with the third item set to the specified new item
       */
      <T> Ops3<A, B, T> setThird(T t);
      
      @Override Ops2<B, C> removeFirst();
      
      @Override Ops2<A, C> removeSecond();
      
      /**
       * Removes the third item from the tuple. Since tuples are immutable, this returns a new
       * tuple with one less item.
       * 
       * @return a new tuple with the same items as this tuple except that the third item is removed
       */
      Ops2<A, B> removeThird();
      
      @Override <T> Ops4<A, B, C, ?> add(T t);
      
      @Override <T> Ops4<T, A, B, C> insertFirst(T t);
      
      @Override <T> Ops4<A, T, B, C> insertSecond(T t);
      
      @Override <T> Ops4<A, B, T, C> insertThird(T t);
      
      /**
       * Inserts an item into the fourth position of this tuple. Since tuples are immutable, this
       * returns a new tuple with one more item.
       * 
       * <p>If this tuple has only three items then this is the same as calling {@link #add(Object)}.
       * Otherwise, this will insert an item after the third item but before the fourth.
       * 
       * @param t the new item
       * @return a new tuple with the same items as this tuple except the specified new item is
       *       inserted as the fourth item in the tuple
       */
      <T> Ops4<A, B, C, T> insertFourth(T t);
      
      @Override <T> Ops3<T, T, T> transformAll(Function<Object, ? extends T> function);
      
      @Override <T> Ops3<T, B, C> transformFirst(Function<? super A, ? extends T> function);
      
      @Override <T> Ops3<A, T, C> transformSecond(Function<? super B, ? extends T> function);
      
      /**
       * Transforms the third item in the tuple using the specified function. Other items in the
       * tuple are unchanged. Since tuples are immutable, this returns a new tuple.
       * 
       * @param function the function to apply
       * @return a new tuple with the third item set to the result of applying the specified
       *    function to the third item in this tuple
       */
      <T> Ops3<A, B, T> transformThird(Function<? super C, ? extends T> function);
   }

   /**
    * Operations supported by tuples that have at least four elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Ops4<A, B, C, D> extends Ops3<A, B, C> {
      /**
       * Retrieves the fourth item in the tuple.
       * 
       * @return the fourth item in the tuple
       */
      D getFourth();
      
      @Override <T> Ops4<T, B, C, D> setFirst(T t);
      
      @Override <T> Ops4<A, T, C, D> setSecond(T t);
      
      @Override <T> Ops4<A, B, T, D> setThird(T t);
      
      /**
       * Sets the fourth item in the tuple. Since tuples are immutable, this returns a new tuple
       * with the fourth item changed.
       * 
       * @param t the new item
       * @return a new tuple with the fourth item set to the specified new item
       */
      <T> Ops4<A, B, C, T> setFourth(T t);
      
      @Override Ops3<B, C, D> removeFirst();
      
      @Override Ops3<A, C, D> removeSecond();
      
      @Override Ops3<A, B, D> removeThird();
      
      /**
       * Removes the fourth item from the tuple. Since tuples are immutable, this returns a new
       * tuple with one less item.
       * 
       * @return a new tuple with the same items as this tuple except that the fourth item is 
       *       removed
       */
      Ops3<A, B, C> removeFourth();
      
      @Override <T> Ops5<A, B, C, D, ?> add(T t);
      
      @Override <T> Ops5<T, A, B, C, D> insertFirst(T t);
      
      @Override <T> Ops5<A, T, B, C, D> insertSecond(T t);
      
      @Override <T> Ops5<A, B, T, C, D> insertThird(T t);
      
      @Override <T> Ops5<A, B, C, T, D> insertFourth(T t);
      
      /**
       * Inserts an item into the fifth position of this tuple. Since tuples are immutable, this
       * returns a new tuple with one more item.
       * 
       * <p>If this tuple has only four items then this is the same as calling {@link #add(Object)}.
       * Otherwise, this will insert an item after the fourth item but before the fifth.
       * 
       * @param t the new item
       * @return a new tuple with the same items as this tuple except the specified new item is
       *       inserted as the fifth item in the tuple
       */
      <T> Ops5<A, B, C, D, T> insertFifth(T t);
      
      @Override <T> Ops4<T, T, T, T> transformAll(Function<Object, ? extends T> function);
      
      @Override <T> Ops4<T, B, C, D> transformFirst(Function<? super A, ? extends T> function);
      
      @Override <T> Ops4<A, T, C, D> transformSecond(Function<? super B, ? extends T> function);
      
      @Override <T> Ops4<A, B, T, D> transformThird(Function<? super C, ? extends T> function);
      
      /**
       * Transforms the fourth item in the tuple using the specified function. Other items in the
       * tuple are unchanged. Since tuples are immutable, this returns a new tuple.
       * 
       * @param function the function to apply
       * @return a new tuple with the fourth item set to the result of applying the specified
       *    function to the fourth item in this tuple
       */
      <T> Ops4<A, B, C, T> transformFourth(Function<? super D, ? extends T> function);
   }

   /**
    * Operations supported by tuples that have at least five elements.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   interface Ops5<A, B, C, D, E> extends Ops4<A, B, C, D> {
      /**
       * Retrieves the fifth item in the tuple.
       * 
       * @return the fifth item in the tuple
       */
      E getFifth();
      
      @Override <T> Ops5<T, B, C, D, E> setFirst(T t);
      
      @Override <T> Ops5<A, T, C, D, E> setSecond(T t);
      
      @Override <T> Ops5<A, B, T, D, E> setThird(T t);
      
      @Override <T> Ops5<A, B, C, T, E> setFourth(T t);
      
      /**
       * Sets the fifth item in the tuple. Since tuples are immutable, this returns a new tuple
       * with the fifth item changed.
       * 
       * @param t the new item
       * @return a new tuple with the fifth item set to the specified new item
       */
      <T> Ops5<A, B, C, D, T> setFifth(T t);
      
      @Override Ops4<B, C, D, E> removeFirst();
      
      @Override Ops4<A, C, D, E> removeSecond();
      
      @Override Ops4<A, B, D, E> removeThird();
      
      @Override Ops4<A, B, C, E> removeFourth();
      
      /**
       * Removes the fifth item from the tuple. Since tuples are immutable, this returns a new
       * tuple with one less item.
       * 
       * @return a new tuple with the same items as this tuple except that the fifth item is removed
       */
      Ops4<A, B, C, D> removeFifth();
      
      // no additional add/insert functions -- five's the limit
      
      @Override <T> Ops5<T, T, T, T, T> transformAll(Function<Object, ? extends T> function);
      
      @Override <T> Ops5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function);
      
      @Override <T> Ops5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function);
      
      @Override <T> Ops5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function);
      
      @Override <T> Ops5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function);
      
      /**
       * Transforms the fifth item in the tuple using the specified function. Other items in the
       * tuple are unchanged. Since tuples are immutable, this returns a new tuple.
       * 
       * @param function the function to apply
       * @return a new tuple with the fifth item set to the result of applying the specified
       *    function to the fifth item in this tuple
       */
      <T> Ops5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function);
   }
}
