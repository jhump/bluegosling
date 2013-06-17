package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;
import java.util.List;

/**
 * A tuple with a single item.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the element type
 */
public class Unit<A> extends AbstractTuple implements Tuple.Ops1<A>, Serializable {

   private static final long serialVersionUID = -9201943154135089064L;
   
   private final A a;
   
   private Unit(A a) {
      this.a = a;
   }
   
   /**
    * Creates a new single element tuple.
    * 
    * @param t the sole element
    * @return a new tuple with the one specified item
    */
   public static <T> Unit<T> create(T t) {
      return new Unit<T>(t);
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
      return (List<A>) super.asList();
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
   public <T> Unit<T> setFirst(T t) {
      return new Unit<T>(t);
   }

   @Override
   public Empty removeFirst() {
      return Empty.INSTANCE;
   }

   @Override
   public <T> Pair<A, T> add(T t) {
      return Pair.create(a, t);
   }

   @Override
   public <T> Pair<T, A> insertFirst(T t) {
      return Pair.create(t, a);
   }

   @Override
   public <T> Pair<A, T> insertSecond(T t) {
      return add(t);
   }

   @Override
   public <T> Unit<T> transformAll(Function<Object, T> function) {
      return transformFirst(function);
   }

   @Override
   public <T> Unit<T> transformFirst(Function<? super A, ? extends T> function) {
      return Unit.<T>create(function.apply(a));
   }
}
