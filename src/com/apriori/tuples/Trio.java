package com.apriori.tuples;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A tuple with three items.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the type of the first item
 * @param <B> the type of the second item
 * @param <C> the type of the third item
 */
public class Trio<A, B, C> extends AbstractTuple implements Tuple.Ops3<A, B, C>, Serializable {

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
   public static <T> List<T> asTypedList(Trio<? extends T, ? extends T, ? extends T> trio) {
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
   public static <T, U, V> Trio<List<T>, List<U>, List<V>> separate(
         Collection<Trio<T, U, V>> trios) {
      List<T> t = new ArrayList<T>(trios.size());
      List<U> u = new ArrayList<U>(trios.size());
      List<V> v = new ArrayList<V>(trios.size());
      for (Trio<T, U, V> trio : trios) {
         t.add(trio.getFirst());
         u.add(trio.getSecond());
         v.add(trio.getThird());
      }
      return create(t, u, v);
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
   public static <T, U, V> List<Trio<T, U, V>> combine(
         Trio<? extends Collection<T>, ? extends Collection<U>, ? extends Collection<V>> trio) {
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
   public static <T, U, V> List<Trio<T, U, V>> combine(Collection<T> t, Collection<U> u,
         Collection<V> v) {
      if (t.size() != u.size() || t.size() != v.size()) {
         throw new IllegalArgumentException();
      }
      List<Trio<T, U, V>> list = new ArrayList<Trio<T, U, V>>(t.size());
      Iterator<T> tIter = t.iterator();
      Iterator<U> uIter = u.iterator();
      Iterator<V> vIter = v.iterator();
      while (tIter.hasNext() && uIter.hasNext() && vIter.hasNext()) {
         list.add(create(tIter.next(), uIter.next(), vIter.next()));
      }
      if (tIter.hasNext() || uIter.hasNext() || vIter.hasNext()) {
         // size changed since check above such that collections differ
         throw new IllegalArgumentException();
      }
      return list;
   }
   
   private final A a;
   private final B b;
   private final C c;
   
   private Trio(A a, B b, C c) {
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
   public static <A, B, C> Trio<A, B, C> create(A a, B b, C c) {
      return new Trio<A, B, C>(a, b, c);
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
   public <T> Trio<T, B, C> setFirst(T t) {
      return Trio.create(t, b, c);
   }

   @Override
   public <T> Trio<A, T, C> setSecond(T t) {
      return Trio.create(a, t, c);
   }

   @Override
   public <T> Trio<A, B, T> setThird(T t) {
      return Trio.create(a, b, t);
   }

   @Override
   public Pair<B, C> removeFirst() {
      return Pair.create(b, c);
   }

   @Override
   public Pair<A, C> removeSecond() {
      return Pair.create(a, c);
   }

   @Override
   public Pair<A, B> removeThird() {
      return Pair.create(a, b);
   }

   @Override
   public <T> Quartet<A, B, C, T> add(T t) {
      return Quartet.create(a, b, c, t);
   }

   @Override
   public <T> Quartet<T, A, B, C> insertFirst(T t) {
      return Quartet.create(t, a, b, c);
   }

   @Override
   public <T> Quartet<A, T, B, C> insertSecond(T t) {
      return Quartet.create(a, t, b, c);
   }

   @Override
   public <T> Quartet<A, B, T, C> insertThird(T t) {
      return Quartet.create(a, b, t, c);
   }

   @Override
   public <T> Quartet<A, B, C, T> insertFourth(T t) {
      return add(t);
   }

   @Override
   public <T> Trio<T, T, T> transformAll(Function<Object, T> function) {
      return Trio.<T, T, T>create(function.apply(a), function.apply(b), function.apply(c));
   }

   @Override
   public <T> Trio<T, B, C> transformFirst(Function<? super A, ? extends T> function) {
      return Trio.<T, B, C>create(function.apply(a), b, c);
   }

   @Override
   public <T> Trio<A, T, C> transformSecond(Function<? super B, ? extends T> function) {
      return Trio.<A, T, C>create(a, function.apply(b), c);
   }

   @Override
   public <T> Trio<A, B, T> transformThird(Function<? super C, ? extends T> function) {
      return Trio.<A, B, T>create(a, b, function.apply(c));
   }
   
   /**
    * Combines the three values in this trio into a single result using the specified function.
    * 
    * @param function the function
    * @return the result of applying the function to the three elements of this trio
    */
   public <D> D combine(
         Function.Trivariate<? super A, ? super B, ? super C, ? extends D> function) {
      return function.apply(a, b, c);
   }
   
   /**
    * Tests the trio of values using the specified predicate.
    * 
    * @param predicate the predicate
    * @return true if the contained values match the predicate; false otherwise
    */
   public boolean test(Predicate.Trivariate<? super A, ? super B, ? super C> predicate) {
      return predicate.test(a, b, c);
   }
}
