package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;
import java.util.List;

//TODO: javadoc
public class Pair<A, B> extends AbstractTuple implements Tuple.Ops2<A, B>, Serializable, Cloneable {
   
   private static final long serialVersionUID = 6469872298989893473L;

   @SuppressWarnings("unchecked") // thanks to type bounds, we know the cast is safe
   public static <T> List<T> asTypedList(Pair<? extends T, ? extends T> pair) {
      return (List<T>) pair.asList();
   }
   
   private final A a;
   private final B b;
   
   private Pair(A a, B b) {
      this.a = a;
      this.b = b;
   }
   
   public static <A, B> Pair<A, B> create(A a, B b) {
      return new Pair<A, B>(a, b);
   }

   @Override
   @SuppressWarnings("unchecked")
   public Pair<A, B> clone() {
      try {
         return (Pair<A, B>) super.clone();
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
   public <T> Pair<T, T> transformAll(Function<Object, T> function) {
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
}
