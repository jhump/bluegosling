package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;
import java.util.List;

/**
 * A tuple with two items.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the type of the first item
 * @param <B> the type of the second item
 */
public class Pair<A, B> extends AbstractTuple implements Tuple.Ops2<A, B>, Serializable {
   
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
   
   public <C> C combine(Function.Bivariate<? super A, ? super B, ? extends C> function) {
      return function.apply(a, b);
   }
}
