package com.bluegosling.tuples;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.bluegosling.util.ValueType;

/**
 * A tuple with five items.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the type of the first item
 * @param <B> the type of the second item
 * @param <C> the type of the third item
 * @param <D> the type of the fourth item
 * @param <E> the type of the fifth item
 */
@ValueType
public final class Quintuple<A, B, C, D, E> implements Tuple.Ops5<A, B, C, D, E>, Serializable {

   private static final long serialVersionUID = -6961697944717178646L;

   /**
    * Returns a list with a tighter upper bound than {@link Tuple#asList()}.
    * 
    * <p>Due to limitations in expressiveness of lower bounds and wildcards in Java's generic type
    * system, achieving the right bounds must be done as a static method.
    * 
    * @param quintet a quintet of items
    * @return a list view of the specified quintet
    */
   @SuppressWarnings("unchecked") // thanks to type bounds, we know the cast is safe
   public static <T> List<T> asTypedList(Quintuple<? extends T, ? extends T, ? extends T, ? extends T, ? extends T> quintet) {
      return (List<T>) quintet.asList();
   }
   
   /**
    * Separates the values in a collection of quintets to produce a quintet of collections. The
    * returned lists will have the same size as the specified collection and items will be in the
    * same order as returned by iteration over the specified collection (e.g. the first items in the
    * returned lists represent the items extracted from the first quintet in the collection).
    * 
    * @param quintets a collection of quintet
    * @return a quintet of lists whose values were extracted from the collection of quintets
    */
   public static <T, U, V, W, X> Quintuple<List<T>, List<U>, List<V>, List<W>, List<X>> separate(
         Collection<Quintuple<T, U, V, W, X>> quintets) {
      List<T> t = new ArrayList<T>(quintets.size());
      List<U> u = new ArrayList<U>(quintets.size());
      List<V> v = new ArrayList<V>(quintets.size());
      List<W> w = new ArrayList<W>(quintets.size());
      List<X> x = new ArrayList<X>(quintets.size());
      for (Quintuple<T, U, V, W, X> quintet : quintets) {
         t.add(quintet.getFirst());
         u.add(quintet.getSecond());
         v.add(quintet.getThird());
         w.add(quintet.getFourth());
         x.add(quintet.getFifth());
      }
      return of(Collections.unmodifiableList(t), Collections.unmodifiableList(u),
            Collections.unmodifiableList(v), Collections.unmodifiableList(w),
            Collections.unmodifiableList(x));
   }

   /**
    * Combines a quintet of collections into a collection of quintets. The returned list will have
    * the same size as the specified collections and items will be in the same order as returned by
    * iteration over the specified collections (e.g. the first quintet in the returned list is a
    * quintet with the first value from each collection).
    * 
    * @param quintet a quintet of collections
    * @return a list of quintets, each one representing corresponding values from the collections
    * @throws IllegalArgumentException if any collection has a different size than the others
    */   
   public static <T, U, V, W, X> List<Quintuple<T, U, V, W, X>> combine(
         Quintuple<? extends Collection<T>, ? extends Collection<U>, ? extends Collection<V>,
               ? extends Collection<W>, ? extends Collection<X>> quintet) {
      return combine(quintet.getFirst(), quintet.getSecond(), quintet.getThird(),
            quintet.getFourth(), quintet.getFifth());
   }
   
   /**
    * Combines five collections into a collection of quintets. The returned list will have the
    * same size as the specified collections and items will be in the same order as returned by
    * iteration over the specified collections (e.g. the first quintet in the returned list is a
    * quintet with the first value from each collection).
    * 
    * @param t a collection whose elements will constitute the first value of a quintet
    * @param u a collection whose elements will constitute the second value of a quintet
    * @param v a collection whose elements will constitute the third value of a quintet
    * @param w a collection whose elements will constitute the fourth value of a quintet
    * @param x a collection whose elements will constitute the fifth value of a quintet
    * @return a list of quintets, each one representing corresponding values from the collections
    * @throws IllegalArgumentException if any collection has a different size than the others
    */
   public static <T, U, V, W, X> List<Quintuple<T, U, V, W, X>> combine(Collection<T> t,
         Collection<U> u, Collection<V> v, Collection<W> w, Collection<X> x) {
      if (t.size() != u.size() || t.size() != v.size() || t.size() != w.size()
            || t.size() != x.size()) {
         throw new IllegalArgumentException();
      }
      List<Quintuple<T, U, V, W, X>> list = new ArrayList<Quintuple<T, U, V, W, X>>(t.size());
      Iterator<T> tIter = t.iterator();
      Iterator<U> uIter = u.iterator();
      Iterator<V> vIter = v.iterator();
      Iterator<W> wIter = w.iterator();
      Iterator<X> xIter = x.iterator();
      while (tIter.hasNext() && uIter.hasNext() && vIter.hasNext() && wIter.hasNext()
            && xIter.hasNext()) {
         list.add(of(tIter.next(), uIter.next(), vIter.next(), wIter.next(), xIter.next()));
      }
      if (tIter.hasNext() || uIter.hasNext() || vIter.hasNext() || wIter.hasNext()
            || xIter.hasNext()) {
         // size changed since check above such that collections differ
         throw new IllegalArgumentException();
      }
      return Collections.unmodifiableList(list);
   }

   private final A a;
   private final B b;
   private final C c;
   private final D d;
   private final E e;
   
   private Quintuple(A a, B b, C c, D d, E e) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
   }
   
   /**
    * Creates a new quintet of items.
    * 
    * @param a the first item in the quintet
    * @param b the second item in the quintet
    * @param c the third item in the quintet
    * @param d the fourth item in the quintet
    * @param e the fifth item in the quintet
    * @return a new quintet
    */
   public static <A, B, C, D, E> Quintuple<A, B, C, D, E> of(A a, B b, C c, D d, E e) {
      return new Quintuple<A, B, C, D, E>(a, b, c, d, e);
   }

   @Override
   public boolean contains(Object o) {
      return (a == null ? o == null : a.equals(o))
            || (b == null ? o == null : b.equals(o))
            || (c == null ? o == null : c.equals(o))
            || (d == null ? o == null : d.equals(o))
            || (e == null ? o == null : e.equals(o));
   }
   
   @Override
   public int size() {
      return 5;
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
   public E getFifth() {
      return e;
   }

   @Override
   public Object[] toArray() {
      return new Object[] { a, b, c, d, e };
   }

   @Override
   public <T> Quintuple<T, B, C, D, E> setFirst(T t) {
      return Quintuple.of(t, b, c, d, e);
   }

   @Override
   public <T> Quintuple<A, T, C, D, E> setSecond(T t) {
      return Quintuple.of(a, t, c, d, e);
   }

   @Override
   public <T> Quintuple<A, B, T, D, E> setThird(T t) {
      return Quintuple.of(a, b, t, d, e);
   }

   @Override
   public <T> Quintuple<A, B, C, T, E> setFourth(T t) {
      return Quintuple.of(a, b, c, t, e);
   }

   @Override
   public <T> Quintuple<A, B, C, D, T> setFifth(T t) {
      return Quintuple.of(a, b, c, d, t);
   }

   @Override
   public Quadruple<B, C, D, E> removeFirst() {
      return Quadruple.of(b, c, d, e);
   }

   @Override
   public Quadruple<A, C, D, E> removeSecond() {
      return Quadruple.of(a, c, d, e);
   }

   @Override
   public Quadruple<A, B, D, E> removeThird() {
      return Quadruple.of(a, b, d, e);
   }

   @Override
   public Quadruple<A, B, C, E> removeFourth() {
      return Quadruple.of(a, b, c, e);
   }

   @Override
   public Quadruple<A, B, C, D> removeFifth() {
      return Quadruple.of(a, b, c, d);
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
   public <T> Quintuple<T, T, T, T, T> transformAll(Function<Object, ? extends T> function) {
      return Quintuple.<T, T, T, T, T>of(function.apply(a), function.apply(b), function.apply(c),
            function.apply(d), function.apply(e));
   }

   @Override
   public <T> Quintuple<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
      return Quintuple.<T, B, C, D, E>of(function.apply(a), b, c, d, e);
   }

   @Override
   public <T> Quintuple<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
      return Quintuple.<A, T, C, D, E>of(a, function.apply(b), c, d, e);
   }

   @Override
   public <T> Quintuple<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
      return Quintuple.<A, B, T, D, E>of(a, b, function.apply(c), d, e);
   }

   @Override
   public <T> Quintuple<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
      return Quintuple.<A, B, C, T, E>of(a, b, c, function.apply(d), e);
   }

   @Override
   public <T> Quintuple<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
      return Quintuple.<A, B, C, D, T>of(a, b, c, d, function.apply(e));
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
