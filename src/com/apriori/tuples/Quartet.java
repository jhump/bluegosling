package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;
import java.util.List;

//TODO: javadoc
public class Quartet<A, B, C, D> extends AbstractTuple 
      implements Tuple.Ops4<A, B, C, D>, Serializable, Cloneable {

   private static final long serialVersionUID = -4005223210115823097L;

   @SuppressWarnings("unchecked") // thanks to type bounds, we know the cast is safe
   public static <T> List<T> asTypedList(Quartet<? extends T, ? extends T, ? extends T, ? extends T> quartet) {
      return (List<T>) quartet.asList();
   }
   
   private final A a;
   private final B b;
   private final C c;
   private final D d;
   
   private Quartet(A a, B b, C c, D d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
   }
   
   public static <A, B, C, D> Quartet<A, B, C, D> create(A a, B b, C c, D d) {
      return new Quartet<A, B, C, D>(a, b, c, d);
   }

   @Override
   @SuppressWarnings("unchecked")
   public Quartet<A, B, C, D> clone() {
      try {
         return (Quartet<A, B, C, D>) super.clone();
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
   public D getFourth() {
      return d;
   }

   @Override
   public Object[] toArray() {
      return new Object[] { a, b, c, d };
   }

   @Override
   public <T> Quartet<T, B, C, D> setFirst(T t) {
      return Quartet.create(t, b, c, d);
   }

   @Override
   public <T> Quartet<A, T, C, D> setSecond(T t) {
      return Quartet.create(a, t, c, d);
   }

   @Override
   public <T> Quartet<A, B, T, D> setThird(T t) {
      return Quartet.create(a, b, t, d);
   }

   @Override
   public <T> Quartet<A, B, C, T> setFourth(T t) {
      return Quartet.create(a, b, c, t);
   }

   @Override
   public Trio<B, C, D> removeFirst() {
      return Trio.create(b, c, d);
   }

   @Override
   public Trio<A, C, D> removeSecond() {
      return Trio.create(a, c, d);
   }

   @Override
   public Trio<A, B, D> removeThird() {
      return Trio.create(a, b, d);
   }

   @Override
   public Trio<A, B, C> removeFourth() {
      return Trio.create(a, b, c);
   }

   @Override
   public <T> Quintet<A, B, C, D, T> add(T t) {
      return Quintet.create(a, b, c, d, t);
   }

   @Override
   public <T> Quintet<T, A, B, C, D> insertFirst(T t) {
      return Quintet.create(t, a, b, c, d);
   }

   @Override
   public <T> Quintet<A, T, B, C, D> insertSecond(T t) {
      return Quintet.create(a, t, b, c, d);
   }

   @Override
   public <T> Quintet<A, B, T, C, D> insertThird(T t) {
      return Quintet.create(a, b, t, c, d);
   }

   @Override
   public <T> Quintet<A, B, C, T, D> insertFourth(T t) {
      return Quintet.create(a, b, c, t, d);
   }

   @Override
   public <T> Quintet<A, B, C, D, T> insertFifth(T t) {
      return add(t);
   }

   @Override
   public <T> Quartet<T, T, T, T> transformAll(Function<Object, T> function) {
      return Quartet.<T, T, T, T>create(function.apply(a), function.apply(b), function.apply(c),
            function.apply(d));
   }

   @Override
   public <T> Quartet<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
      return Quartet.<T, B, C, D>create(function.apply(a), b, c, d);
   }

   @Override
   public <T> Quartet<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
      return Quartet.<A, T, C, D>create(a, function.apply(b), c, d);
   }

   @Override
   public <T> Quartet<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
      return Quartet.<A, B, T, D>create(a, b, function.apply(c), d);
   }

   @Override
   public <T> Quartet<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
      return Quartet.<A, B, C, T>create(a, b, c, function.apply(d));
   }
}
