package com.bluegosling.tuples;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import com.bluegosling.util.ValueType;

/**
 * A tuple with two items.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the type of the first item
 * @param <B> the type of the second item
 */
@ValueType
public final class Pair<A, B> implements Tuple.Ops2<A, B>, Serializable {
   
   private static final long serialVersionUID = 6469872298989893473L;

   /**
    * Returns a list with a tighter upper bound than {@link Tuple#asList()}.
    * 
    * <p>Due to limitations in expressiveness of lower bounds and wildcards in Java's generic type
    * system, achieving the right bounds must be done as a static method.
    * 
    * @param pair a pair of items
    * @return a list view of the specified pair
    */
   @SuppressWarnings("unchecked") // thanks to type bounds, we know the cast is safe
   public static <T> List<T> asTypedList(Pair<? extends T, ? extends T> pair) {
      return (List<T>) pair.asList();
   }
   
   /**
    * Separates the values in a collection of pairs to produce a pair of collections. The returned
    * lists will have the same size as the specified collection and items will be in the same order
    * as returned by iteration over the specified collection (e.g. the first items in the returned
    * lists represent the items extracted from the first pair in the collection).
    * 
    * @param pairs a collection of pairs
    * @return a pair of lists whose values were extracted from the collection of pairs
    */
   public static <T, U> Pair<List<T>, List<U>> separate(Collection<Pair<T, U>> pairs) {
      List<T> t = new ArrayList<T>(pairs.size());
      List<U> u = new ArrayList<U>(pairs.size());
      for (Pair<T, U> pair : pairs) {
         t.add(pair.getFirst());
         u.add(pair.getSecond());
      }
      return create(Collections.unmodifiableList(t), Collections.unmodifiableList(u));
   }

   /**
    * Combines a pair of collections into a collection of pairs. The returned list will have the
    * same size as the specified collections and items will be in the same order as returned by
    * iteration over the specified collections (e.g. the first pair in the returned list is a pair
    * with the first value from each collection).
    * 
    * @param pair a pair of collections
    * @return a list of pairs, each one representing corresponding values from the collections
    * @throws IllegalArgumentException if the two collections have different sizes
    */
   public static <T, U> List<Pair<T, U>> combine(
         Pair<? extends Collection<T>, ? extends Collection<U>> pair) {
      return combine(pair.getFirst(), pair.getSecond());
   }
   
   /**
    * Combines two collections into a collection of pairs. The returned list will have the
    * same size as the specified collections and items will be in the same order as returned by
    * iteration over the specified collections (e.g. the first pair in the returned list is a pair
    * with the first value from each collection).
    * 
    * @param t a collection whose elements will constitute the first value of a pair
    * @param u a collection whose elements will constitute the second value of a pair
    * @return a list of pairs, each one representing corresponding values from the collections
    * @throws IllegalArgumentException if the two collections have different sizes
    */
   public static <T, U> List<Pair<T, U>> combine(Collection<T> t, Collection<U> u) {
      if (t.size() != u.size()) {
         throw new IllegalArgumentException();
      }
      List<Pair<T, U>> list = new ArrayList<Pair<T, U>>(t.size());
      Iterator<T> tIter = t.iterator();
      Iterator<U> uIter = u.iterator();
      while (tIter.hasNext() && uIter.hasNext()) {
         list.add(create(tIter.next(), uIter.next()));
      }
      if (tIter.hasNext() || uIter.hasNext()) {
         // size changed since check above such that two collections differ
         throw new IllegalArgumentException();
      }
      return Collections.unmodifiableList(list);
   }
   
   private final A a;
   private final B b;
   
   private Pair(A a, B b) {
      this.a = a;
      this.b = b;
   }
   
   /**
    * Creates a new pair of items.
    * 
    * @param a the first item in the pair
    * @param b the second item in the pair
    * @return a new pair
    */
   public static <A, B> Pair<A, B> create(A a, B b) {
      return new Pair<A, B>(a, b);
   }

   @Override
   public boolean contains(Object o) {
      return (a == null ? o == null : a.equals(o))
            || (b == null ? o == null : b.equals(o));
   }
   
   @Override
   public int size() {
      return 2;
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
   public Object[] toArray() {
      return new Object[] { a, b };
   }

   @Override
   public <T> Pair<T, B> setFirst(T t) {
      return Pair.create(t, b);
   }

   @Override
   public <T> Pair<A, T> setSecond(T t) {
      return Pair.create(a, t);
   }

   @Override
   public Unit<B> removeFirst() {
      return Unit.create(b);
   }

   @Override
   public Unit<A> removeSecond() {
      return Unit.create(a);
   }

   @Override
   public <T> Trio<A, B, T> add(T t) {
      return Trio.create(a, b, t);
   }

   @Override
   public <T> Trio<T, A, B> insertFirst(T t) {
      return Trio.create(t, a, b);
   }

   @Override
   public <T> Trio<A, T, B> insertSecond(T t) {
      return Trio.create(a, t, b);
   }

   @Override
   public <T> Trio<A, B, T> insertThird(T t) {
      return add(t);
   }

   @Override
   public <T> Pair<T, T> transformAll(Function<Object, ? extends T> function) {
      return Pair.<T, T>create(function.apply(a), function.apply(b));
   }

   @Override
   public <T> Pair<T, B> transformFirst(Function<? super A, ? extends T> function) {
      return Pair.<T, B>create(function.apply(a), b);
   }

   @Override
   public <T> Pair<A, T> transformSecond(Function<? super B, ? extends T> function) {
      return Pair.<A, T>create(a, function.apply(b));
   }
   
   /**
    * Combines the two values in this pair into a single result using the specified function.
    * 
    * @param function the function
    * @return the result of applying the function to the two elements of this pair
    */
   public <C> C combine(BiFunction<? super A, ? super B, ? extends C> function) {
      return function.apply(a, b);
   }
   
   /**
    * Tests the pair of values using the specified predicate.
    * 
    * @param predicate the predicate
    * @return true if the contained values match the predicate; false otherwise
    */
   public boolean test(BiPredicate<? super A, ? super B> predicate) {
      return predicate.test(a, b);
   }
   
   // TODO: doc, test
   public Pair<B, A> swap() {
      return Pair.create(b, a);
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
