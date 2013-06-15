package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;

// TODO: javadoc
public class Unit<A> extends AbstractTuple implements Tuple.Ops1<A>, Serializable, Cloneable {

   private static final long serialVersionUID = -9201943154135089064L;
   
   private final A a;
   
   private Unit(A a) {
      this.a = a;
   }
   
   public static <T> Unit<T> create(T t) {
      return new Unit<T>(t);
   }
   
   @Override
   @SuppressWarnings("unchecked")
   public Unit<A> clone() {
      try {
         return (Unit<A>) super.clone();
      } catch (CloneNotSupportedException e) {
         // this can't happen
         throw new AssertionError();
      }
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
