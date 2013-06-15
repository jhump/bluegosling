package com.apriori.tuples;

import com.apriori.util.Function;

import java.io.Serializable;

//TODO: javadoc
//TODO: serialization, cloning
//TODO: tests!
public class NTuple<A, B, C, D, E> extends AbstractTuple
      implements Tuple.Ops5<A, B, C, D, E>, Serializable, Cloneable {

   private static final long serialVersionUID = 787923089202872798L;
   
   private transient final A a;
   private transient final B b;
   private transient final C c;
   private transient final D d;
   private transient final E e;
   private final Object[] array;
   
   private NTuple(A a, B b, C c, D d, E e, Object o) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
      this.array = new Object[6];
      this.array[0] = a;
      this.array[1] = b;
      this.array[2] = c;
      this.array[3] = d;
      this.array[4] = e;
      this.array[5] = o;
   }
   
   private NTuple(A a, B b, C c, D d, E e, Object o1, Object o2, Object array[]) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
      this.array = new Object[array.length + 7];
      this.array[0] = a;
      this.array[1] = b;
      this.array[2] = c;
      this.array[3] = d;
      this.array[4] = e;
      this.array[5] = o1;
      this.array[6] = o2;
      if (array.length > 0) {
         System.arraycopy(array, 0, this.array, 7, array.length);
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
   
   public static <A, B, C, D, E> NTuple<A, B, C, D, E> create(A a, B b, C c, D d, E e, Object o) {
      return new NTuple<A, B, C, D, E>(a, b, c, d, e, o);
   }

   public static <A, B, C, D, E> NTuple<A, B, C, D, E> create(A a, B b, C c, D d, E e, Object o1, Object o2, Object... others) {
      return new NTuple<A, B, C, D, E>(a, b, c, d, e, o1, o2, others);
   }
   
   @Override
   public D getFourth() {
      return d;
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
   public C getThird() {
      return c;
   }

   @Override
   public B getSecond() {
      return b;
   }

   @Override
   public A getFirst() {
      return a;
   }

   @Override
   public Object[] toArray() {
      return array.clone();
   }

   @Override
   public E getFifth() {
      return e;
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
   
   @Override
   public Ops5<B, C, D, E, ?> removeFirst() {
      if (array.length == 6) {
         return Quintet.create(b, c, d, e, array[5]);
      } else {
         return new NTuple<B, C, D, E, Object>(removeItem(array, 0));
      }
   }

   @Override
   public Ops5<A, C, D, E, ?> removeSecond() {
      if (array.length == 6) {
         return Quintet.create(a, c, d, e, array[5]);
      } else {
         return new NTuple<A, C, D, E, Object>(removeItem(array, 1));
      }
   }

   @Override
   public Ops5<A, B, D, E, ?> removeThird() {
      if (array.length == 6) {
         return Quintet.create(a, b, d, e, array[5]);
      } else {
         return new NTuple<A, B, D, E, Object>(removeItem(array, 2));
      }
   }

   @Override
   public Ops5<A, B, C, E, ?> removeFourth() {
      if (array.length == 6) {
         return Quintet.create(a, b, c, e, array[5]);
      } else {
         return new NTuple<A, B, C, E, Object>(removeItem(array, 3));
      }
   }

   @Override
   public Ops5<A, B, C, D, ?> removeFifth() {
      if (array.length == 6) {
         return Quintet.create(a, b, c, d, array[5]);
      } else {
         return new NTuple<A, B, C, D, Object>(removeItem(array, 4));
      }
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
}
