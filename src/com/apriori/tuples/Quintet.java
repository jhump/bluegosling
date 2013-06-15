package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;
import java.util.List;

//TODO: javadoc
public class Quintet<A, B, C, D, E> extends AbstractTuple
      implements Tuple.Ops5<A, B, C, D, E>, Serializable, Cloneable {

   private static final long serialVersionUID = -6961697944717178646L;

   @SuppressWarnings("unchecked") // thanks to type bounds, we know the cast is safe
   public static <T> List<T> asTypedList(Quintet<? extends T, ? extends T, ? extends T, ? extends T, ? extends T> quintet) {
      return (List<T>) quintet.asList();
   }
   
   private final A a;
   private final B b;
   private final C c;
   private final D d;
   private final E e;
   
   private Quintet(A a, B b, C c, D d, E e) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
   }
   
   public static <A, B, C, D, E> Quintet<A, B, C, D, E> create(A a, B b, C c, D d, E e) {
      return new Quintet<A, B, C, D, E>(a, b, c, d, e);
   }

   @Override
   @SuppressWarnings("unchecked")
   public Quintet<A, B, C, D, E> clone() {
      try {
         return (Quintet<A, B, C, D, E>) super.clone();
      } catch (CloneNotSupportedException ex) {
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
   public D getFourth() {
      return d;
   }

   @Override
   public E getFifth() {
      return e;
   }

   @Override
   public Object[] toArray() {
      return new Object[] { a, b, c, d, e };
   }

   @Override
   public <T> Quintet<T, B, C, D, E> setFirst(T t) {
      return Quintet.create(t, b, c, d, e);
   }

   @Override
   public <T> Quintet<A, T, C, D, E> setSecond(T t) {
      return Quintet.create(a, t, c, d, e);
   }

   @Override
   public <T> Quintet<A, B, T, D, E> setThird(T t) {
      return Quintet.create(a, b, t, d, e);
   }

   @Override
   public <T> Quintet<A, B, C, T, E> setFourth(T t) {
      return Quintet.create(a, b, c, t, e);
   }

   @Override
   public <T> Quintet<A, B, C, D, T> setFifth(T t) {
      return Quintet.create(a, b, c, d, t);
   }

   @Override
   public Quartet<B, C, D, E> removeFirst() {
      return Quartet.create(b, c, d, e);
   }

   @Override
   public Quartet<A, C, D, E> removeSecond() {
      return Quartet.create(a, c, d, e);
   }

   @Override
   public Quartet<A, B, D, E> removeThird() {
      return Quartet.create(a, b, d, e);
   }

   @Override
   public Quartet<A, B, C, E> removeFourth() {
      return Quartet.create(a, b, c, e);
   }

   @Override
   public Quartet<A, B, C, D> removeFifth() {
      return Quartet.create(a, b, c, d);
   }

   @Override
   public <T> NTuple<A, B, C, D, E> add(T t) {
      return NTuple.create(a, b, c, d, e, t);
   }

   @Override
   public <T> NTuple<T, A, B, C, D> insertFirst(T t) {
      return NTuple.create(t, a, b, c, d, e);
   }

   @Override
   public <T> NTuple<A, T, B, C, D> insertSecond(T t) {
      return NTuple.create(a, t, b, c, d, e);
   }

   @Override
   public <T> NTuple<A, B, T, C, D> insertThird(T t) {
      return NTuple.create(a, b, t, c, d, e);
   }

   @Override
   public <T> NTuple<A, B, C, T, D> insertFourth(T t) {
      return NTuple.create(a, b, c, t, d, e);
   }

   @Override
   public <T> NTuple<A, B, C, D, T> insertFifth(T t) {
      return NTuple.create(a, b, c, d, t, e);
   }

   @Override
   public <T> Quintet<T, T, T, T, T> transformAll(Function<Object, T> function) {
      return Quintet.<T, T, T, T, T>create(function.apply(a), function.apply(b), function.apply(c),
            function.apply(d), function.apply(e));
   }

   @Override
   public <T> Quintet<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
      return Quintet.<T, B, C, D, E>create(function.apply(a), b, c, d, e);
   }

   @Override
   public <T> Quintet<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
      return Quintet.<A, T, C, D, E>create(a, function.apply(b), c, d, e);
   }

   @Override
   public <T> Quintet<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
      return Quintet.<A, B, T, D, E>create(a, b, function.apply(c), d, e);
   }

   @Override
   public <T> Quintet<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
      return Quintet.<A, B, C, T, E>create(a, b, c, function.apply(d), e);
   }

   @Override
   public <T> Quintet<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
      return Quintet.<A, B, C, D, T>create(a, b, c, d, function.apply(e));
   }
}
