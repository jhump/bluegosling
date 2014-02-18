package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A tuple with four items.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the type of the first item
 * @param <B> the type of the second item
 * @param <C> the type of the third item
 * @param <D> the type of the fourth item
 */
public class Quartet<A, B, C, D> extends AbstractTuple
      implements Tuple.Ops4<A, B, C, D>, Serializable {

   private static final long serialVersionUID = -4005223210115823097L;

   /**
    * Returns a list with a tighter upper bound than {@link Tuple#asList()}.
    * 
    * <p>Due to limitations in expressiveness of lower bounds and wildcards in Java's generic type
    * system, achieving the right bounds must be done as a static method.
    * 
    * @param quartet a quartet of items
    * @return a list view of the specified quartet
    */
   @SuppressWarnings("unchecked") // thanks to type bounds, we know the cast is safe
   public static <T> List<T> asTypedList(Quartet<? extends T, ? extends T, ? extends T, ? extends T> quartet) {
      return (List<T>) quartet.asList();
   }

   /**
    * Separates the values in a collection of quartets to produce a quartet of collections. The
    * returned lists will have the same size as the specified collection and items will be in the
    * same order as returned by iteration over the specified collection (e.g. the first items in the
    * returned lists represent the items extracted from the first quartet in the collection).
    * 
    * @param quartets a collection of quartet
    * @return a quartet of lists whose values were extracted from the collection of quartets
    */
   public static <T, U, V, W> Quartet<List<T>, List<U>, List<V>, List<W>> separate(
         Collection<Quartet<T, U, V, W>> quartets) {
      List<T> t = new ArrayList<T>(quartets.size());
      List<U> u = new ArrayList<U>(quartets.size());
      List<V> v = new ArrayList<V>(quartets.size());
      List<W> w = new ArrayList<W>(quartets.size());
      for (Quartet<T, U, V, W> quartet : quartets) {
         t.add(quartet.getFirst());
         u.add(quartet.getSecond());
         v.add(quartet.getThird());
         w.add(quartet.getFourth());
      }
      return create(Collections.unmodifiableList(t), Collections.unmodifiableList(u),
            Collections.unmodifiableList(v), Collections.unmodifiableList(w));
   }

   /**
    * Combines a quartet of collections into a collection of quartets. The returned list will have
    * the same size as the specified collections and items will be in the same order as returned by
    * iteration over the specified collections (e.g. the first quartet in the returned list is a
    * quartet with the first value from each collection).
    * 
    * @param quartet a quartet of collections
    * @return a list of quartets, each one representing corresponding values from the collections
    * @throws IllegalArgumentException if any collection has a different size than the others
    */   
   public static <T, U, V, W> List<Quartet<T, U, V, W>> combine(
         Quartet<? extends Collection<T>, ? extends Collection<U>, ? extends Collection<V>,
               ? extends Collection<W>> quartet) {
      return combine(quartet.getFirst(), quartet.getSecond(), quartet.getThird(),
            quartet.getFourth());
   }
   
   /**
    * Combines four collections into a collection of quartets. The returned list will have the
    * same size as the specified collections and items will be in the same order as returned by
    * iteration over the specified collections (e.g. the first quartet in the returned list is a
    * quartet with the first value from each collection).
    * 
    * @param t a collection whose elements will constitute the first value of a quartet
    * @param u a collection whose elements will constitute the second value of a quartet
    * @param v a collection whose elements will constitute the third value of a quartet
    * @param w a collection whose elements will constitute the fourth value of a quartet
    * @return a list of quartets, each one representing corresponding values from the collections
    * @throws IllegalArgumentException if any collection has a different size than the others
    */
   public static <T, U, V, W> List<Quartet<T, U, V, W>> combine(Collection<T> t, Collection<U> u,
         Collection<V> v, Collection<W> w) {
      if (t.size() != u.size() || t.size() != v.size() || t.size() != w.size()) {
         throw new IllegalArgumentException();
      }
      List<Quartet<T, U, V, W>> list = new ArrayList<Quartet<T, U, V, W>>(t.size());
      Iterator<T> tIter = t.iterator();
      Iterator<U> uIter = u.iterator();
      Iterator<V> vIter = v.iterator();
      Iterator<W> wIter = w.iterator();
      while (tIter.hasNext() && uIter.hasNext() && vIter.hasNext() && wIter.hasNext()) {
         list.add(create(tIter.next(), uIter.next(), vIter.next(), wIter.next()));
      }
      if (tIter.hasNext() || uIter.hasNext() || vIter.hasNext() || wIter.hasNext()) {
         // size changed since check above such that collections differ
         throw new IllegalArgumentException();
      }
      return Collections.unmodifiableList(list);
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
   
   /**
    * Creates a new quartet of items.
    * 
    * @param a the first item in the quartet
    * @param b the second item in the quartet
    * @param c the third item in the quartet
    * @param d the fourth item in the quartet
    * @return a new quartet
    */
   public static <A, B, C, D> Quartet<A, B, C, D> create(A a, B b, C c, D d) {
      return new Quartet<A, B, C, D>(a, b, c, d);
   }

   @Override
   public boolean contains(Object o) {
      return (a == null ? o == null : a.equals(o))
            || (b == null ? o == null : b.equals(o))
            || (c == null ? o == null : c.equals(o))
            || (d == null ? o == null : d.equals(o));
   }
   
   @Override
   public int size() {
      return 4;
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
   public <T> Quartet<T, T, T, T> transformAll(Function<Object, ? extends T> function) {
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
