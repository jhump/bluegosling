package com.apriori.choice;

import com.apriori.possible.Reference;

import java.io.Serializable;
import java.util.function.Function;

//TODO: javadoc
//TODO: tests
public abstract class Variant5<A, B, C, D, E> implements Choice.OfFive<A, B, C, D, E> {
   
   public static <A, B, C, D, E> Variant5<A, B, C, D, E> withFirst(A a) {
      return new First<A, B, C, D, E>(a);
   }
   
   public static <A, B, C, D, E> Variant5<A, B, C, D, E> withSecond(B b) {
      return new Second<A, B, C, D, E>(b);
   }

   public static <A, B, C, D, E> Variant5<A, B, C, D, E> withThird(C c) {
      return new Third<A, B, C, D, E>(c);
   }

   public static <A, B, C, D, E> Variant5<A, B, C, D, E> withFourth(D d) {
      return new Fourth<A, B, C, D, E>(d);
   }

   public static <A, B, C, D, E> Variant5<A, B, C, D, E> withFifth(E e) {
      return new Fifth<A, B, C, D, E>(e);
   }

   public static <A, B, C, D, E> Variant5<A, B, C, D, E> of(A a, B b, C c, D d, E e) {
      int count = 0;
      if (a != null) {
         count++;
      }
      if (b != null) {
         count++;
      }
      if (c != null) {
         count++;
      }
      if (d != null) {
         count++;
      }
      if (e != null) {
         count++;
      }
      if (count > 1) {
         throw new IllegalArgumentException("Only one argument can be non-null");
      }
      if (a != null || count == 0) {
         return new First<A, B, C, D, E>(a);
      } else if (b != null) {
         return new Second<A, B, C, D, E>(b);
      } else if (c != null) {
         return new Third<A, B, C, D, E>(c);
      } else if (d != null) {
         return new Fourth<A, B, C, D, E>(d);
      } else { // e != null
         return new Fifth<A, B, C, D, E>(e);
      }
   }
   
   public static <A, B, C, D, E> Variant5<A, B, C, D, E> firstOf(A a, B b, C c, D d, E e) {
      if (a != null) {
         return new First<A, B, C, D, E>(a);
      } else if (b != null) {
         return new Second<A, B, C, D, E>(b);
      } else if (c != null) {
         return new Third<A, B, C, D, E>(c);
      } else if (d != null) {
         return new Fourth<A, B, C, D, E>(d);
      } else {
         return new Fifth<A, B, C, D, E>(e);
      }
   }
   
   Variant5() {
   }
      
   @Override
   public abstract Reference<A> tryFirst();

   @Override
   public abstract Reference<B> trySecond();

   @Override
   public abstract Reference<C> tryThird();

   @Override
   public abstract Reference<D> tryFourth();

   @Override
   public abstract Reference<E> tryFifth();

   @Override
   public abstract <T> Variant5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function);
   
   @Override
   public abstract <T> Variant5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function);

   @Override
   public abstract <T> Variant5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function);

   @Override
   public abstract <T> Variant5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function);

   @Override
   public abstract <T> Variant5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function);

   public abstract Variant4<B, C, D, E> contractFirst(Function<? super A, Variant4<B, C, D, E>> function);

   public abstract Variant4<A, C, D, E> contractSecond(Function<? super B, Variant4<A, C, D, E>> function);

   public abstract Variant4<A, B, D, E> contractThird(Function<? super C, Variant4<A, B, D, E>> function);

   public abstract Variant4<A, B, C, E> contractFourth(Function<? super D, Variant4<A, B, C, E>> function);

   public abstract Variant4<A, B, C, D> contractFifth(Function<? super E, Variant4<A, B, C, D>> function);
   
   private static class First<A, B, C, D, E> extends Variant5<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = 6751001283511150055L;

      private final A a;
      
      First(A a) {
         this.a = a;
      }
      
      @Override
      public boolean hasFirst() {
         return true;
      }
   
      @Override
      public boolean hasSecond() {
         return false;
      }
   
      @Override
      public boolean hasThird() {
         return false;
      }
   
      @Override
      public boolean hasFourth() {
         return false;
      }
   
      @Override
      public boolean hasFifth() {
         return false;
      }
   
      @Override
      public Object get() {
         return a;
      }
      
      @Override
      public A getFirst() {
         return a;
      }
   
      @Override
      public B getSecond() {
         throw new IllegalStateException();
      }
   
      @Override
      public C getThird() {
         throw new IllegalStateException();
      }
   
      @Override
      public D getFourth() {
         throw new IllegalStateException();
      }
   
      @Override
      public E getFifth() {
         throw new IllegalStateException();
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.setTo(a);
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.unset();
      }
   
      @Override
      public Reference<E> tryFifth() {
         return Reference.unset();
      }
      
      @Override
      public <T> Variant5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         return Variant5.<T, B, C, D, E>withFirst(function.apply(a));
      }
   
      @Override
      public <T> Variant5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant5<A, T, C, D, E> ret = (Variant5<A, T, C, D, E>) this;
         return ret;
      }

      @Override
      public <T> Variant5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant5<A, B, T, D, E> ret = (Variant5<A, B, T, D, E>) this;
         return ret;
      }

      @Override
      public <T> Variant5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         Variant5<A, B, C, T, E> ret = (Variant5<A, B, C, T, E>) this;
         return ret;
      }

      @Override
      public <T> Variant5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fifth not present, can safely recast that variable
         Variant5<A, B, C, D, T> ret = (Variant5<A, B, C, D, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitFirst(a);
      }
      
      @Override
      public Variant4<B, C, D, E> contractFirst(Function<? super A, Variant4<B, C, D, E>> function) {
         Variant4<B, C, D, E> result = function.apply(a);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public Variant4<A, C, D, E> contractSecond(Function<? super B, Variant4<A, C, D, E>> function) {
         return Variant4.withFirst(a);
      }

      @Override
      public Variant4<A, B, D, E> contractThird(Function<? super C, Variant4<A, B, D, E>> function) {
         return Variant4.withFirst(a);
      }

      @Override
      public Variant4<A, B, C, E> contractFourth(Function<? super D, Variant4<A, B, C, E>> function) {
         return Variant4.withFirst(a);
      }

      @Override
      public Variant4<A, B, C, D> contractFifth(Function<? super E, Variant4<A, B, C, D>> function) {
         return Variant4.withFirst(a);
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof First && a.equals(((First<?, ?, ?, ?, ?>) o).a);
      }
      
      @Override
      public int hashCode() {
         return First.class.hashCode() ^ a.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant5.First: " + a;
      }
   }

   private static class Second<A, B, C, D, E> extends Variant5<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = -6643284935107794L;

      private final B b;
      
      Second(B b) {
         this.b = b;
      }
      
      @Override
      public boolean hasFirst() {
         return false;
      }
   
      @Override
      public boolean hasSecond() {
         return true;
      }

      @Override
      public boolean hasThird() {
         return false;
      }

      @Override
      public boolean hasFourth() {
         return false;
      }

      @Override
      public boolean hasFifth() {
         return false;
      }

      @Override
      public Object get() {
         return b;
      }
      
      @Override
      public A getFirst() {
         throw new IllegalStateException();
      }
   
      @Override
      public B getSecond() {
         return b;
      }
   
      @Override
      public C getThird() {
         throw new IllegalStateException();
      }
   
      @Override
      public D getFourth() {
         throw new IllegalStateException();
      }
   
      @Override
      public E getFifth() {
         throw new IllegalStateException();
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.setTo(b);
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.unset();
      }
   
      @Override
      public Reference<E> tryFifth() {
         return Reference.unset();
      }
      
      @Override
      public <T> Variant5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant5<T, B, C, D, E> ret = (Variant5<T, B, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         return Variant5.<A, T, C, D, E>withSecond(function.apply(b));
      }
   
      @Override
      public <T> Variant5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant5<A, B, T, D, E> ret = (Variant5<A, B, T, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         Variant5<A, B, C, T, E> ret = (Variant5<A, B, C, T, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fifth not present, can safely recast that variable
         Variant5<A, B, C, D, T> ret = (Variant5<A, B, C, D, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitSecond(b);
      }
      
      @Override
      public Variant4<B, C, D, E> contractFirst(Function<? super A, Variant4<B, C, D, E>> function) {
         return Variant4.withFirst(b);
      }

      @Override
      public Variant4<A, C, D, E> contractSecond(Function<? super B, Variant4<A, C, D, E>> function) {
         Variant4<A, C, D, E> result = function.apply(b);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public Variant4<A, B, D, E> contractThird(Function<? super C, Variant4<A, B, D, E>> function) {
         return Variant4.withSecond(b);
      }

      @Override
      public Variant4<A, B, C, E> contractFourth(Function<? super D, Variant4<A, B, C, E>> function) {
         return Variant4.withSecond(b);
      }

      @Override
      public Variant4<A, B, C, D> contractFifth(Function<? super E, Variant4<A, B, C, D>> function) {
         return Variant4.withSecond(b);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Second && b.equals(((Second<?, ?, ?, ?, ?>) o).b);
      }
      
      @Override
      public int hashCode() {
         return Second.class.hashCode() ^ b.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant5.Second: " + b;
      }
   }

   private static class Third<A, B, C, D, E> extends Variant5<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = 7217259636360590284L;
      
      private final C c;
      
      Third(C c) {
         this.c = c;
      }
      
      @Override
      public boolean hasFirst() {
         return false;
      }
   
      @Override
      public boolean hasSecond() {
         return false;
      }

      @Override
      public boolean hasThird() {
         return true;
      }

      @Override
      public boolean hasFourth() {
         return false;
      }

      @Override
      public boolean hasFifth() {
         return false;
      }

      @Override
      public Object get() {
         return c;
      }
      
      @Override
      public A getFirst() {
         throw new IllegalStateException();
      }
   
      @Override
      public B getSecond() {
         throw new IllegalStateException();
      }
   
      @Override
      public C getThird() {
         return c;
      }
   
      @Override
      public D getFourth() {
         throw new IllegalStateException();
      }
   
      @Override
      public E getFifth() {
         throw new IllegalStateException();
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.setTo(c);
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.unset();
      }
   
      @Override
      public Reference<E> tryFifth() {
         return Reference.unset();
      }
      
      @Override
      public <T> Variant5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant5<T, B, C, D, E> ret = (Variant5<T, B, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant5<A, T, C, D, E> ret = (Variant5<A, T, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         return Variant5.<A, B, T, D, E>withThird(function.apply(c));
      }
   
      @Override
      public <T> Variant5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         Variant5<A, B, C, T, E> ret = (Variant5<A, B, C, T, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fifth not present, can safely recast that variable
         Variant5<A, B, C, D, T> ret = (Variant5<A, B, C, D, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitThird(c);
      }
      
      @Override
      public Variant4<B, C, D, E> contractFirst(Function<? super A, Variant4<B, C, D, E>> function) {
         return Variant4.withSecond(c);
      }

      @Override
      public Variant4<A, C, D, E> contractSecond(Function<? super B, Variant4<A, C, D, E>> function) {
         return Variant4.withSecond(c);
      }

      @Override
      public Variant4<A, B, D, E> contractThird(Function<? super C, Variant4<A, B, D, E>> function) {
         Variant4<A, B, D, E> result = function.apply(c);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public Variant4<A, B, C, E> contractFourth(Function<? super D, Variant4<A, B, C, E>> function) {
         return Variant4.withThird(c);
      }

      @Override
      public Variant4<A, B, C, D> contractFifth(Function<? super E, Variant4<A, B, C, D>> function) {
         return Variant4.withThird(c);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Third && c.equals(((Third<?, ?, ?, ?, ?>) o).c);
      }
      
      @Override
      public int hashCode() {
         return Third.class.hashCode() ^ c.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant5.Third: " + c;
      }
   }

   private static class Fourth<A, B, C, D, E> extends Variant5<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = -4063989296432903718L;
      
      private final D d;
      
      Fourth(D d) {
         this.d = d;
      }
      
      @Override
      public boolean hasFirst() {
         return false;
      }
   
      @Override
      public boolean hasSecond() {
         return false;
      }

      @Override
      public boolean hasThird() {
         return false;
      }

      @Override
      public boolean hasFourth() {
         return true;
      }

      @Override
      public boolean hasFifth() {
         return false;
      }

      @Override
      public Object get() {
         return d;
      }
      
      @Override
      public A getFirst() {
         throw new IllegalStateException();
      }
   
      @Override
      public B getSecond() {
         throw new IllegalStateException();
      }
   
      @Override
      public C getThird() {
         throw new IllegalStateException();
      }
   
      @Override
      public D getFourth() {
         return d;
      }
   
      @Override
      public E getFifth() {
         throw new IllegalStateException();
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.setTo(d);
      }
   
      @Override
      public Reference<E> tryFifth() {
         return Reference.unset();
      }
      
      @Override
      public <T> Variant5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant5<T, B, C, D, E> ret = (Variant5<T, B, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant5<A, T, C, D, E> ret = (Variant5<A, T, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant5<A, B, T, D, E> ret = (Variant5<A, B, T, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         return Variant5.<A, B, C, T, E>withFourth(function.apply(d));
      }
   
      @Override
      public <T> Variant5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fifth not present, can safely recast that variable
         Variant5<A, B, C, D, T> ret = (Variant5<A, B, C, D, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitFourth(d);
      }

      @Override
      public Variant4<B, C, D, E> contractFirst(Function<? super A, Variant4<B, C, D, E>> function) {
         return Variant4.withThird(d);
      }

      @Override
      public Variant4<A, C, D, E> contractSecond(Function<? super B, Variant4<A, C, D, E>> function) {
         return Variant4.withThird(d);
      }

      @Override
      public Variant4<A, B, D, E> contractThird(Function<? super C, Variant4<A, B, D, E>> function) {
         return Variant4.withThird(d);
      }

      @Override
      public Variant4<A, B, C, E> contractFourth(Function<? super D, Variant4<A, B, C, E>> function) {
         Variant4<A, B, C, E> result = function.apply(d);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public Variant4<A, B, C, D> contractFifth(Function<? super E, Variant4<A, B, C, D>> function) {
         return Variant4.withFourth(d);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Fourth && d.equals(((Fourth<?, ?, ?, ?, ?>) o).d);
      }
      
      @Override
      public int hashCode() {
         return Fourth.class.hashCode() ^ d.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant5.Fourth: " + d;
      }
   }

   private static class Fifth<A, B, C, D, E> extends Variant5<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = -2996730923823015154L;
      
      private final E e;
      
      Fifth(E e) {
         this.e = e;
      }
      
      @Override
      public boolean hasFirst() {
         return false;
      }
   
      @Override
      public boolean hasSecond() {
         return false;
      }

      @Override
      public boolean hasThird() {
         return false;
      }

      @Override
      public boolean hasFourth() {
         return false;
      }

      @Override
      public boolean hasFifth() {
         return true;
      }

      @Override
      public Object get() {
         return e;
      }
      
      @Override
      public A getFirst() {
         throw new IllegalStateException();
      }
   
      @Override
      public B getSecond() {
         throw new IllegalStateException();
      }
   
      @Override
      public C getThird() {
         throw new IllegalStateException();
      }
   
      @Override
      public D getFourth() {
         throw new IllegalStateException();
      }
   
      @Override
      public E getFifth() {
         return e;
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.unset();
      }
   
      @Override
      public Reference<E> tryFifth() {
         return Reference.setTo(e);
      }
      
      @Override
      public <T> Variant5<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant5<T, B, C, D, E> ret = (Variant5<T, B, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant5<A, T, C, D, E> ret = (Variant5<A, T, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant5<A, B, T, D, E> ret = (Variant5<A, B, T, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         Variant5<A, B, C, T, E> ret = (Variant5<A, B, C, T, E>) this;
         return ret;
      }
   
      @Override
      public <T> Variant5<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         return Variant5.<A, B, C, D, T>withFifth(function.apply(e));
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitFifth(e);
      }

      @Override
      public Variant4<B, C, D, E> contractFirst(Function<? super A, Variant4<B, C, D, E>> function) {
         return Variant4.withFourth(e);
      }

      @Override
      public Variant4<A, C, D, E> contractSecond(Function<? super B, Variant4<A, C, D, E>> function) {
         return Variant4.withFourth(e);
      }

      @Override
      public Variant4<A, B, D, E> contractThird(Function<? super C, Variant4<A, B, D, E>> function) {
         return Variant4.withFourth(e);
      }

      @Override
      public Variant4<A, B, C, E> contractFourth(Function<? super D, Variant4<A, B, C, E>> function) {
         return Variant4.withFourth(e);
      }

      @Override
      public Variant4<A, B, C, D> contractFifth(Function<? super E, Variant4<A, B, C, D>> function) {
         Variant4<A, B, C, D> result = function.apply(e);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Fifth && e.equals(((Fifth<?, ?, ?, ?, ?>) o).e);
      }
      
      @Override
      public int hashCode() {
         return Fifth.class.hashCode() ^ e.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant5.Fifth: " + e;
      }
   }
}
