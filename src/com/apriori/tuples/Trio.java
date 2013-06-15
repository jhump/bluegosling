package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;
import java.util.List;

//TODO: javadoc
public class Trio<A, B, C> extends AbstractTuple
      implements Tuple.Ops3<A, B, C>, Serializable, Cloneable {

   private static final long serialVersionUID = -2245545958928314038L;

   @SuppressWarnings("unchecked") // thanks to type bounds, we know the cast is safe
   public static <T> List<T> asTypedList(Trio<? extends T, ? extends T, ? extends T> trio) {
      return (List<T>) trio.asList();
   }
   
   private final A a;
   private final B b;
   private final C c;
   
   private Trio(A a, B b, C c) {
      this.a = a;
      this.b = b;
      this.c = c;
   }
   
   public static <A, B, C> Trio<A, B, C> create(A a, B b, C c) {
      return new Trio<A, B, C>(a, b, c);
   }

   @Override
   @SuppressWarnings("unchecked")
   public Trio<A, B, C> clone() {
      try {
         return (Trio<A, B, C>) super.clone();
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
}
