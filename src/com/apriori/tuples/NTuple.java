package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A tuple that has more than five elements. It is so named since it can have {@code n} number of
 * elements (where {@code n > 5}).
 * 
 * <p>To prevent the number of generic type arguments from getting out of hand and becoming
 * unwieldy, this provides type safety (and rich API) only for the first 5 elements. Access to other
 * elements in the tuple can be had using the {@linkplain #asList() list} or {@linkplain #toArray()
 * array} forms.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <A> the type of the first item
 * @param <B> the type of the second item
 * @param <C> the type of the third item
 * @param <D> the type of the fourth item
 * @param <E> the type of the fifth item
 */
//TODO: tests!
public class NTuple<A, B, C, D, E> extends AbstractTuple
      implements Tuple.Ops5<A, B, C, D, E>, Serializable {

   private static final long serialVersionUID = 787923089202872798L;
   
   // ideally, these would all be final. but they can't be to support serialization
   
   private transient A a;
   private transient B b;
   private transient C c;
   private transient D d;
   private transient E e;
   private final Object[] array;
   
   private NTuple(A a, B b, C c, D d, E e, Object o, Object array[]) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
      this.array = new Object[array.length + 6];
      this.array[0] = a;
      this.array[1] = b;
      this.array[2] = c;
      this.array[3] = d;
      this.array[4] = e;
      this.array[5] = o;
      if (array.length > 0) {
         System.arraycopy(array, 0, this.array, 6, array.length);
      }
   }
   
   @SuppressWarnings("unchecked") // calling code passes correct types in the array
   private NTuple(Object array[]) {
      this.array = array;
      this.a = (A) array[0];
      this.b = (B) array[1];
      this.c = (C) array[2];
      this.d = (D) array[3];
      this.e = (E) array[4];
   }
   
   /**
    * Creates a new tuple. The tuple must have at least 6 items.
    * 
    * @param a the first item
    * @param b the second item
    * @param c the third item
    * @param d the fourth item
    * @param e the fifth item
    * @param f the sixth item
    * @param others any items beyond the sixth
    * @return a tuple with the specified items
    */
   public static <A, B, C, D, E> NTuple<A, B, C, D, E> create(A a, B b, C c, D d, E e, Object f,
         Object... others) {
      return new NTuple<A, B, C, D, E>(a, b, c, d, e, f, others);
   }

   @Override
   public boolean contains(Object o) {
      for (Object item : array) {
         if (item == null ? o == null : item.equals(o)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public int size() {
      return array.length;
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
      return array.clone();
   }

   private static Object[] setItem(Object array[], int pos, Object item) {
      Object ret[] = array.clone();
      ret[pos] = item;
      return ret;
   }

   @Override
   public <T> NTuple<T, B, C, D, E> setFirst(T t) {
      return new NTuple<T, B, C, D, E>(setItem(array, 0, t));
   }

   @Override
   public <T> NTuple<A, T, C, D, E> setSecond(T t) {
      return new NTuple<A, T, C, D, E>(setItem(array, 1, t));
   }

   @Override
   public <T> NTuple<A, B, T, D, E> setThird(T t) {
      return new NTuple<A, B, T, D, E>(setItem(array, 2, t));
   }

   @Override
   public <T> NTuple<A, B, C, T, E> setFourth(T t) {
      return new NTuple<A, B, C, T, E>(setItem(array, 3, t));
   }

   @Override
   public <T> NTuple<A, B, C, D, T> setFifth(T t) {
      return new NTuple<A, B, C, D, T>(setItem(array, 4, t));
   }

   private static Object[] removeItem(Object array[], int pos) {
      int len = array.length;
      Object ret[] = new Object[len - 1];
      if (pos > 0) {
         System.arraycopy(array, 0, ret, 0, pos);
      }
      if (pos < len - 1) {
         System.arraycopy(array, pos + 1, ret, pos, len - pos - 1);
      }
      return ret;
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>If this tuple has exactly six elements, then the returned tuple will have five elements and
    * thus be an instance of {@link Quintet}. If this tuple has more than six elements, then the
    * returned tuple will also be an {@link NTuple}.
    */
   @Override
   public Ops5<B, C, D, E, ?> removeFirst() {
      if (array.length == 6) {
         return Quintet.create(b, c, d, e, array[5]);
      } else {
         return new NTuple<B, C, D, E, Object>(removeItem(array, 0));
      }
   }

   /**
    * {@inheritDoc}
    * 
    * <p>If this tuple has exactly six elements, then the returned tuple will have five elements and
    * thus be an instance of {@link Quintet}. If this tuple has more than six elements, then the
    * returned tuple will also be an {@link NTuple}.
    */
   @Override
   public Ops5<A, C, D, E, ?> removeSecond() {
      if (array.length == 6) {
         return Quintet.create(a, c, d, e, array[5]);
      } else {
         return new NTuple<A, C, D, E, Object>(removeItem(array, 1));
      }
   }

   /**
    * {@inheritDoc}
    * 
    * <p>If this tuple has exactly six elements, then the returned tuple will have five elements and
    * thus be an instance of {@link Quintet}. If this tuple has more than six elements, then the
    * returned tuple will also be an {@link NTuple}.
    */
   @Override
   public Ops5<A, B, D, E, ?> removeThird() {
      if (array.length == 6) {
         return Quintet.create(a, b, d, e, array[5]);
      } else {
         return new NTuple<A, B, D, E, Object>(removeItem(array, 2));
      }
   }

   /**
    * {@inheritDoc}
    * 
    * <p>If this tuple has exactly six elements, then the returned tuple will have five elements and
    * thus be an instance of {@link Quintet}. If this tuple has more than six elements, then the
    * returned tuple will also be an {@link NTuple}.
    */
   @Override
   public Ops5<A, B, C, E, ?> removeFourth() {
      if (array.length == 6) {
         return Quintet.create(a, b, c, e, array[5]);
      } else {
         return new NTuple<A, B, C, E, Object>(removeItem(array, 3));
      }
   }

   /**
    * {@inheritDoc}
    * 
    * <p>If this tuple has exactly six elements, then the returned tuple will have five elements and
    * thus be an instance of {@link Quintet}. If this tuple has more than six elements, then the
    * returned tuple will also be an {@link NTuple}.
    */
   @Override
   public Ops5<A, B, C, D, ?> removeFifth() {
      if (array.length == 6) {
         return Quintet.create(a, b, c, d, array[5]);
      } else {
         return new NTuple<A, B, C, D, Object>(removeItem(array, 4));
      }
   }

   private static Object[] addItem(Object array[], int pos, Object item) {
      int len = array.length;
      Object ret[] = new Object[len + 1];
      if (pos > 0) {
         System.arraycopy(array, 0, ret, 0, pos);
      }
      ret[pos] = item;
      if (pos < len) {
         System.arraycopy(array, pos, ret, pos + 1, len - pos);
      }
      return ret;
   }
   
   @Override
   public <T> NTuple<A, B, C, D, E> add(T t) {
      return new NTuple<A, B, C, D, E>(addItem(array, array.length, t));
   }

   @Override
   public <T> NTuple<T, A, B, C, D> insertFirst(T t) {
      return new NTuple<T, A, B, C, D>(addItem(array, 0, t));
   }

   @Override
   public <T> NTuple<A, T, B, C, D> insertSecond(T t) {
      return new NTuple<A, T, B, C, D>(addItem(array, 1, t));
   }

   @Override
   public <T> NTuple<A, B, T, C, D> insertThird(T t) {
      return new NTuple<A, B, T, C, D>(addItem(array, 2, t));
   }

   @Override
   public <T> NTuple<A, B, C, T, D> insertFourth(T t) {
      return new NTuple<A, B, C, T, D>(addItem(array, 3, t));
   }

   @Override
   public <T> NTuple<A, B, C, D, T> insertFifth(T t) {
      return new NTuple<A, B, C, D, T>(addItem(array, 4, t));
   }

   @Override
   public <T> NTuple<T, T, T, T, T> transformAll(Function<Object, T> function) {
      Object ret[] = new Object[array.length];
      for (int i = 0, len = ret.length; i < len; i++) {
         ret[i] = function.apply(array[i]);
      }
      return new NTuple<T, T, T, T, T>(ret);
   }

   @Override
   public <T> NTuple<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
      return this.<T>setFirst(function.apply(a));
   }

   @Override
   public <T> NTuple<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
      return this.<T>setSecond(function.apply(b));
   }

   @Override
   public <T> NTuple<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
      return this.<T>setThird(function.apply(c));
   }

   @Override
   public <T> NTuple<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
      return this.<T>setFourth(function.apply(d));
   }

   @Override
   public <T> NTuple<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
      return this.<T>setFifth(function.apply(e));
   }
   
   /**
    * Customizes de-serialization to populate the typesafe fields representing the first five
    * members of the tuple.
    * 
    * @param in the stream from which the list is read
    * @throws IOException if an exception is raised when reading from {@code in}
    * @throws ClassNotFoundException if de-serializing an element fails to locate the element's
    *            class
    */
   @SuppressWarnings("unchecked") // deserializing is inherently unsafe, but this will be safe if
                                  // an item is serialized and then deserialized back into the same
                                  // types since construction of object enforces proper element
                                  // types in the array
   private void readObject(ObjectInputStream in) throws IOException,
         ClassNotFoundException {
      in.defaultReadObject();
      // populate the redundant (but typesafe) members
      a = (A) array[0];
      b = (B) array[1];
      c = (C) array[2];
      d = (D) array[3];
      e = (E) array[4];
   }
}
