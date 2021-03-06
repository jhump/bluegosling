package com.bluegosling.tuples;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.bluegosling.util.ValueType;

/**
 * A tuple with a single item.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the element type
 */
@ValueType
public final class Single<A> implements Tuple.Ops1<A>, Serializable {

   private static final long serialVersionUID = -9201943154135089064L;
   
   /**
    * Extracts values from a collection of units into a collection of values. The returned list will
    * have the same size as the specified collection and items will be in the same order as returned
    * by iteration over the specified collection (e.g. the first item in the returned list is the
    * value extracted from the first unit in the collection).
    * 
    * @param units a collection of units
    * @return the list of values, extracted from the units
    */
   public static <T> List<T> extract(Collection<Single<T>> units) {
      List<T> extracted = new ArrayList<T>(units.size());
      for (Single<T> unit : units) {
         extracted.add(unit.getFirst());
      }
      return Collections.unmodifiableList(extracted);
   }

   /**
    * Encloses a collection of values into a collection of units. The returned list will have the
    * same size as the specified collection and items will be in the same order as returned by
    * iteration over the specified collection (e.g. the first item in the returned list is a unit
    * with the first value of the collection).
    * 
    * @param coll a collection of values
    * @return a list of units, each one representing a corresponding value in the collection
    */
   public static <T> List<Single<T>> enclose(Collection<? extends T> coll) {
      List<Single<T>> enclosed = new ArrayList<Single<T>>(coll.size());
      for (T t : coll) {
         enclosed.add(of(t));
      }
      return Collections.unmodifiableList(enclosed);
   }

   private final A a;
   
   private Single(A a) {
      this.a = a;
   }
   
   /**
    * Creates a new single element tuple.
    * 
    * @param t the sole element
    * @return a new tuple with the one specified item
    */
   public static <T> Single<T> of(T t) {
      return new Single<T>(t);
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>Since this tuple has exactly one element and we know its type, we can return a precise type
    * instead of returning {@code List<?>}.
    */
   @Override
   @SuppressWarnings("unchecked") // its only element is an A, so cast is safe
   public List<A> asList() {
      return (List<A>) TupleUtils.asList(this);
   }
   
   @Override
   public boolean contains(Object o) {
      return a == null ? o == null : a.equals(o);
   }
   
   @Override
   public int size() {
      return 1;
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
   public Object[] toArray() {
      return new Object[] { a };
   }

   @Override
   public <T> Single<T> setFirst(T t) {
      return new Single<T>(t);
   }

   @Override
   public Empty removeFirst() {
      return Empty.INSTANCE;
   }

   @Override
   public <T> Pair<A, T> add(T t) {
      return Pair.of(a, t);
   }

   @Override
   public <T> Pair<T, A> insertFirst(T t) {
      return Pair.of(t, a);
   }

   @Override
   public <T> Pair<A, T> insertSecond(T t) {
      return add(t);
   }

   @Override
   public <T> Single<T> transformAll(Function<Object, ? extends T> function) {
      return transformFirst(function);
   }

   @Override
   public <T> Single<T> transformFirst(Function<? super A, ? extends T> function) {
      return Single.<T>of(function.apply(a));
      
   }
   
   /**
    * Transforms the contained value and returns the results.
    * 
    * @param function the function
    * @return the result of applying the function to the contained value
    */
   public <B> B transform(Function<? super A, ? extends B> function) {
      return function.apply(a);
   }
   
   /**
    * Tests the contained value using the specified predicate.
    * 
    * @param predicate the predicate
    * @return true if the contained value matches the predicate; false otherwise
    */
   public boolean test(Predicate<? super A> predicate) {
      return predicate.test(a);
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
