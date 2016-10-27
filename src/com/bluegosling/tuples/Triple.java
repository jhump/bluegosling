package com.bluegosling.tuples;

import com.bluegosling.function.TriFunction;
import com.bluegosling.function.TriPredicate;
import com.bluegosling.util.ValueType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * A tuple with three items.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the type of the first item
 * @param <B> the type of the second item
 * @param <C> the type of the third item
 */
@ValueType
public final class Triple<A, B, C> implements Tuple.Ops3<A, B, C>, Serializable {

   private static final long serialVersionUID = -2245545958928314038L;

   /**
    * Returns a list with a tighter upper bound than {@link Tuple#asList()}.
    * 
    * <p>Due to limitations in expressiveness of lower bounds and wildcards in Java's generic type
    * system, achieving the right bounds must be done as a static method.
    * 
    * @param trio a trio of items
    * @return a list view of the specified trio
    */
   @SuppressWarnings("unchecked") // thanks to type bounds, we know the cast is safe
   public static <T> List<T> asTypedList(Triple<? extends T, ? extends T, ? extends T> trio) {
      return (List<T>) trio.asList();
   }
   
   /**
    * Separates the values in a collection of trios to produce a trio of collections. The returned
    * lists will have the same size as the specified collection and items will be in the same order
    * as returned by iteration over the specified collection (e.g. the first items in the returned
    * lists represent the items extracted from the first trio in the collection).
    * 
    * @param trios a collection of trios
    * @return a trio of lists whose values were extracted from the collection of trios
    */
   public static <T, U, V> Triple<List<T>, List<U>, List<V>> separate(
         Collection<Triple<T, U, V>> trios) {
      List<T> t = new ArrayList<T>(trios.size());
      List<U> u = new ArrayList<U>(trios.size());
      List<V> v = new ArrayList<V>(trios.size());
      for (Triple<T, U, V> trio : trios) {
         t.add(trio.getFirst());
         u.add(trio.getSecond());
         v.add(trio.getThird());
      }
      return of(Collections.unmodifiableList(t), Collections.unmodifiableList(u),
            Collections.unmodifiableList(v));
   }

   /**
    * Combines a trio of collections into a collection of trios. The returned list will have the
    * same size as the specified collections and items will be in the same order as returned by
    * iteration over the specified collections (e.g. the first trio in the returned list is a trio
    * with the first value from each collection).
    * 
    * @param trio a trio of collections
    * @return a list of trios, each one representing corresponding values from the collection
    * @throws IllegalArgumentException if any collection has a different size than the others
    */   
   public static <T, U, V> List<Triple<T, U, V>> combine(
         Triple<? extends Collection<T>, ? extends Collection<U>, ? extends Collection<V>> trio) {
      return combine(trio.getFirst(), trio.getSecond(), trio.getThird());
   }
   
   /**
    * Combines three collections into a collection of trios. The returned list will have the
    * same size as the specified collections and items will be in the same order as returned by
    * iteration over the specified collections (e.g. the first trio in the returned list is a trio
    * with the first value from each collection).
    * 
    * @param t a collection whose elements will constitute the first value of a trio
    * @param u a collection whose elements will constitute the second value of a trio
    * @param v a collection whose elements will constitute the third value of a trio
    * @return a list of trios, each one representing corresponding values from the collections
    * @throws IllegalArgumentException if any collection has a different size than the others
    */
   public static <T, U, V> List<Triple<T, U, V>> combine(Collection<T> t, Collection<U> u,
         Collection<V> v) {
      if (t.size() != u.size() || t.size() != v.size()) {
         throw new IllegalArgumentException();
      }
      List<Triple<T, U, V>> list = new ArrayList<Triple<T, U, V>>(t.size());
      Iterator<T> tIter = t.iterator();
      Iterator<U> uIter = u.iterator();
      Iterator<V> vIter = v.iterator();
      while (tIter.hasNext() && uIter.hasNext() && vIter.hasNext()) {
         list.add(of(tIter.next(), uIter.next(), vIter.next()));
      }
      if (tIter.hasNext() || uIter.hasNext() || vIter.hasNext()) {
         // size changed since check above such that collections differ
         throw new IllegalArgumentException();
      }
      return Collections.unmodifiableList(list);
   }
   
   private final A a;
   private final B b;
   private final C c;
   
   private Triple(A a, B b, C c) {
      this.a = a;
      this.b = b;
      this.c = c;
   }
   
   /**
    * Creates a new trio of items.
    * 
    * @param a the first item in the trio
    * @param b the second item in the trio
    * @param c the third item in the trio
    * @return a new trio
    */
   public static <A, B, C> Triple<A, B, C> of(A a, B b, C c) {
      return new Triple<A, B, C>(a, b, c);
   }

   @Override
   public boolean contains(Object o) {
      return (a == null ? o == null : a.equals(o))
            || (b == null ? o == null : b.equals(o))
            || (c == null ? o == null : c.equals(o));
   }
   
   @Override
   public int size() {
      return 3;
   }
   
   @Override
   public boolean isEmpty() {
      return false;
   }
   
   @Override
   public A getFirst() {
      return a;
   }

   @Override
   public B getSecond() {
      return b;
   }

   @Override
   public C getThird() {
      return c;
   }

   @Override
   public Object[] toArray() {
      return new Object[] { a, b, c };
   }

   @Override
   public <T> Triple<T, B, C> setFirst(T t) {
      return Triple.of(t, b, c);
   }

   @Override
   public <T> Triple<A, T, C> setSecond(T t) {
      return Triple.of(a, t, c);
   }

   @Override
   public <T> Triple<A, B, T> setThird(T t) {
      return Triple.of(a, b, t);
   }

   @Override
   public Pair<B, C> removeFirst() {
      return Pair.of(b, c);
   }

   @Override
   public Pair<A, C> removeSecond() {
      return Pair.of(a, c);
   }

   @Override
   public Pair<A, B> removeThird() {
      return Pair.of(a, b);
   }

   @Override
   public <T> Quadruple<A, B, C, T> add(T t) {
      return Quadruple.of(a, b, c, t);
   }

   @Override
   public <T> Quadruple<T, A, B, C> insertFirst(T t) {
      return Quadruple.of(t, a, b, c);
   }

   @Override
   public <T> Quadruple<A, T, B, C> insertSecond(T t) {
      return Quadruple.of(a, t, b, c);
   }

   @Override
   public <T> Quadruple<A, B, T, C> insertThird(T t) {
      return Quadruple.of(a, b, t, c);
   }

   @Override
   public <T> Quadruple<A, B, C, T> insertFourth(T t) {
      return add(t);
   }

   @Override
   public <T> Triple<T, T, T> transformAll(Function<Object, ? extends T> function) {
      return Triple.<T, T, T>of(function.apply(a), function.apply(b), function.apply(c));
   }

   @Override
   public <T> Triple<T, B, C> transformFirst(Function<? super A, ? extends T> function) {
      return Triple.<T, B, C>of(function.apply(a), b, c);
   }

   @Override
   public <T> Triple<A, T, C> transformSecond(Function<? super B, ? extends T> function) {
      return Triple.<A, T, C>of(a, function.apply(b), c);
   }

   @Override
   public <T> Triple<A, B, T> transformThird(Function<? super C, ? extends T> function) {
      return Triple.<A, B, T>of(a, b, function.apply(c));
   }
   
   /**
    * Combines the three values in this trio into a single result using the specified function.
    * 
    * @param function the function
    * @return the result of applying the function to the three elements of this trio
    */
   public <D> D combine(TriFunction<? super A, ? super B, ? super C, ? extends D> function) {
      return function.apply(a, b, c);
   }
   
   /**
    * Tests the trio of values using the specified predicate.
    * 
    * @param predicate the predicate
    * @return true if the contained values match the predicate; false otherwise
    */
   public boolean test(TriPredicate<? super A, ? super B, ? super C> predicate) {
      return predicate.test(a, b, c);
   }
   
   @Override
   public Iterator<Object> iterator() {
      return TupleUtils.iterator(this);
   }
   
   @Override
   public <T> T[] toArray(T[] a) {
      return TupleUtils.toArray(this, a);
   }
   
   @Override
   public List<?> asList() {
      return TupleUtils.asList(this);
   }
   
   @Override
   public boolean equals(Object o) {
      return TupleUtils.equals(this, o);
   }

   @Override
   public int hashCode() {
      return TupleUtils.hashCode(this);
   }

   @Override
   public String toString() {
      return TupleUtils.toString(this);
   }
}
