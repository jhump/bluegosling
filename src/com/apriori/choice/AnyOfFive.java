package com.apriori.choice;

import com.apriori.possible.Optional;

import java.io.Serializable;
import java.util.function.Function;

//TODO: javadoc
//TODO: tests
public abstract class AnyOfFive<A, B, C, D, E> implements Choice.OfFive<A, B, C, D, E> {
   
   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withFirst(A a) {
      if (a == null) {
         throw new NullPointerException();
      }
      return new First<A, B, C, D, E>(a);
   }
   
   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withSecond(B b) {
      if (b == null) {
         throw new NullPointerException();
      }
      return new Second<A, B, C, D, E>(b);
   }

   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withThird(C c) {
      if (c == null) {
         throw new NullPointerException();
      }
      return new Third<A, B, C, D, E>(c);
   }

   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withFourth(D d) {
      if (d == null) {
         throw new NullPointerException();
      }
      return new Fourth<A, B, C, D, E>(d);
   }

   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withFifth(E e) {
      if (e == null) {
         throw new NullPointerException();
      }
      return new Fifth<A, B, C, D, E>(e);
   }

   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> of(A a, B b, C c, D d, E e) {
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
      if (count != 1) {
         throw new IllegalArgumentException("Exactly one argument must be non-null");
      }
      if (a != null) {
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
   
   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> firstOf(A a, B b, C c, D d, E e) {
      if (a != null) {
         return new First<A, B, C, D, E>(a);
      } else if (b != null) {
         return new Second<A, B, C, D, E>(b);
      } else if (c != null) {
         return new Third<A, B, C, D, E>(c);
      } else if (d != null) {
         return new Fourth<A, B, C, D, E>(d);
      } else if (e != null) {
         return new Fifth<A, B, C, D, E>(e);
      } else {
         throw new IllegalArgumentException("At least one argument must be non-null");
      }
   }
   
   AnyOfFive() {
   }
      
   @Override
   public abstract Optional<A> tryFirst();

   @Override
   public abstract Optional<B> trySecond();

   @Override
   public abstract Optional<C> tryThird();

   @Override
   public abstract Optional<D> tryFourth();

   @Override
   public abstract Optional<E> tryFifth();

   @Override
   public abstract <T> AnyOfFive<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function);
   
   @Override
   public abstract <T> AnyOfFive<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function);

   @Override
   public abstract <T> AnyOfFive<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function);

   @Override
   public abstract <T> AnyOfFive<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function);

   @Override
   public abstract <T> AnyOfFive<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function);

   public abstract AnyOfFour<B, C, D, E> contractFirst(Function<? super A, AnyOfFour<B, C, D, E>> function);

   public abstract AnyOfFour<A, C, D, E> contractSecond(Function<? super B, AnyOfFour<A, C, D, E>> function);

   public abstract AnyOfFour<A, B, D, E> contractThird(Function<? super C, AnyOfFour<A, B, D, E>> function);

   public abstract AnyOfFour<A, B, C, E> contractFourth(Function<? super D, AnyOfFour<A, B, C, E>> function);

   public abstract AnyOfFour<A, B, C, D> contractFifth(Function<? super E, AnyOfFour<A, B, C, D>> function);

   private static class First<A, B, C, D, E> extends AnyOfFive<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = -5452388106285417314L;
      
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
      public Optional<A> tryFirst() {
         return Optional.of(a);
      }
   
      @Override
      public Optional<B> trySecond() {
         return Optional.none();
      }
   
      @Override
      public Optional<C> tryThird() {
         return Optional.none();
      }
   
      @Override
      public Optional<D> tryFourth() {
         return Optional.none();
      }
   
      @Override
      public Optional<E> tryFifth() {
         return Optional.none();
      }
      
      @Override
      public <T> AnyOfFive<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         return AnyOfFive.<T, B, C, D, E>withFirst(function.apply(a));
      }
   
      @Override
      public <T> AnyOfFive<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfFive<A, T, C, D, E> ret = (AnyOfFive<A, T, C, D, E>) this;
         return ret;
      }

      @Override
      public <T> AnyOfFive<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfFive<A, B, T, D, E> ret = (AnyOfFive<A, B, T, D, E>) this;
         return ret;
      }

      @Override
      public <T> AnyOfFive<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         AnyOfFive<A, B, C, T, E> ret = (AnyOfFive<A, B, C, T, E>) this;
         return ret;
      }

      @Override
      public <T> AnyOfFive<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fifth not present, can safely recast that variable
         AnyOfFive<A, B, C, D, T> ret = (AnyOfFive<A, B, C, D, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitFirst(a);
      }
      
      @Override
      public AnyOfFour<B, C, D, E> contractFirst(Function<? super A, AnyOfFour<B, C, D, E>> function) {
         AnyOfFour<B, C, D, E> result = function.apply(a);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public AnyOfFour<A, C, D, E> contractSecond(Function<? super B, AnyOfFour<A, C, D, E>> function) {
         return AnyOfFour.withFirst(a);
      }

      @Override
      public AnyOfFour<A, B, D, E> contractThird(Function<? super C, AnyOfFour<A, B, D, E>> function) {
         return AnyOfFour.withFirst(a);
      }

      @Override
      public AnyOfFour<A, B, C, E> contractFourth(Function<? super D, AnyOfFour<A, B, C, E>> function) {
         return AnyOfFour.withFirst(a);
      }

      @Override
      public AnyOfFour<A, B, C, D> contractFifth(Function<? super E, AnyOfFour<A, B, C, D>> function) {
         return AnyOfFour.withFirst(a);
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
         return "AnyOfFive.First: " + a;
      }
   }

   private static class Second<A, B, C, D, E> extends AnyOfFive<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = -7985740643326792664L;
      
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
      public Optional<A> tryFirst() {
         return Optional.none();
      }
   
      @Override
      public Optional<B> trySecond() {
         return Optional.of(b);
      }
   
      @Override
      public Optional<C> tryThird() {
         return Optional.none();
      }
   
      @Override
      public Optional<D> tryFourth() {
         return Optional.none();
      }
   
      @Override
      public Optional<E> tryFifth() {
         return Optional.none();
      }
      
      @Override
      public <T> AnyOfFive<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfFive<T, B, C, D, E> ret = (AnyOfFive<T, B, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         return AnyOfFive.<A, T, C, D, E>withSecond(function.apply(b));
      }
   
      @Override
      public <T> AnyOfFive<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfFive<A, B, T, D, E> ret = (AnyOfFive<A, B, T, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         AnyOfFive<A, B, C, T, E> ret = (AnyOfFive<A, B, C, T, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fifth not present, can safely recast that variable
         AnyOfFive<A, B, C, D, T> ret = (AnyOfFive<A, B, C, D, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitSecond(b);
      }
      
      @Override
      public AnyOfFour<B, C, D, E> contractFirst(Function<? super A, AnyOfFour<B, C, D, E>> function) {
         return AnyOfFour.withFirst(b);
      }

      @Override
      public AnyOfFour<A, C, D, E> contractSecond(Function<? super B, AnyOfFour<A, C, D, E>> function) {
         AnyOfFour<A, C, D, E> result = function.apply(b);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public AnyOfFour<A, B, D, E> contractThird(Function<? super C, AnyOfFour<A, B, D, E>> function) {
         return AnyOfFour.withSecond(b);
      }

      @Override
      public AnyOfFour<A, B, C, E> contractFourth(Function<? super D, AnyOfFour<A, B, C, E>> function) {
         return AnyOfFour.withSecond(b);
      }

      @Override
      public AnyOfFour<A, B, C, D> contractFifth(Function<? super E, AnyOfFour<A, B, C, D>> function) {
         return AnyOfFour.withSecond(b);
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
         return "AnyOfFive.Second: " + b;
      }
   }

   private static class Third<A, B, C, D, E> extends AnyOfFive<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = -6373342884114224369L;
      
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
      public Optional<A> tryFirst() {
         return Optional.none();
      }
   
      @Override
      public Optional<B> trySecond() {
         return Optional.none();
      }
   
      @Override
      public Optional<C> tryThird() {
         return Optional.of(c);
      }
   
      @Override
      public Optional<D> tryFourth() {
         return Optional.none();
      }
   
      @Override
      public Optional<E> tryFifth() {
         return Optional.none();
      }
      
      @Override
      public <T> AnyOfFive<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfFive<T, B, C, D, E> ret = (AnyOfFive<T, B, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfFive<A, T, C, D, E> ret = (AnyOfFive<A, T, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         return AnyOfFive.<A, B, T, D, E>withThird(function.apply(c));
      }
   
      @Override
      public <T> AnyOfFive<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         AnyOfFive<A, B, C, T, E> ret = (AnyOfFive<A, B, C, T, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fifth not present, can safely recast that variable
         AnyOfFive<A, B, C, D, T> ret = (AnyOfFive<A, B, C, D, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitThird(c);
      }
      
      @Override
      public AnyOfFour<B, C, D, E> contractFirst(Function<? super A, AnyOfFour<B, C, D, E>> function) {
         return AnyOfFour.withSecond(c);
      }

      @Override
      public AnyOfFour<A, C, D, E> contractSecond(Function<? super B, AnyOfFour<A, C, D, E>> function) {
         return AnyOfFour.withSecond(c);
      }

      @Override
      public AnyOfFour<A, B, D, E> contractThird(Function<? super C, AnyOfFour<A, B, D, E>> function) {
         AnyOfFour<A, B, D, E> result = function.apply(c);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public AnyOfFour<A, B, C, E> contractFourth(Function<? super D, AnyOfFour<A, B, C, E>> function) {
         return AnyOfFour.withThird(c);
      }

      @Override
      public AnyOfFour<A, B, C, D> contractFifth(Function<? super E, AnyOfFour<A, B, C, D>> function) {
         return AnyOfFour.withThird(c);
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
         return "AnyOfFive.Third: " + c;
      }
   }

   private static class Fourth<A, B, C, D, E> extends AnyOfFive<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = 4271858188250623598L;
      
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
      public Optional<A> tryFirst() {
         return Optional.none();
      }
   
      @Override
      public Optional<B> trySecond() {
         return Optional.none();
      }
   
      @Override
      public Optional<C> tryThird() {
         return Optional.none();
      }
   
      @Override
      public Optional<D> tryFourth() {
         return Optional.of(d);
      }
   
      @Override
      public Optional<E> tryFifth() {
         return Optional.none();
      }
      
      @Override
      public <T> AnyOfFive<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfFive<T, B, C, D, E> ret = (AnyOfFive<T, B, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfFive<A, T, C, D, E> ret = (AnyOfFive<A, T, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfFive<A, B, T, D, E> ret = (AnyOfFive<A, B, T, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         return AnyOfFive.<A, B, C, T, E>withFourth(function.apply(d));
      }
   
      @Override
      public <T> AnyOfFive<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fifth not present, can safely recast that variable
         AnyOfFive<A, B, C, D, T> ret = (AnyOfFive<A, B, C, D, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitFourth(d);
      }

      @Override
      public AnyOfFour<B, C, D, E> contractFirst(Function<? super A, AnyOfFour<B, C, D, E>> function) {
         return AnyOfFour.withThird(d);
      }

      @Override
      public AnyOfFour<A, C, D, E> contractSecond(Function<? super B, AnyOfFour<A, C, D, E>> function) {
         return AnyOfFour.withThird(d);
      }

      @Override
      public AnyOfFour<A, B, D, E> contractThird(Function<? super C, AnyOfFour<A, B, D, E>> function) {
         return AnyOfFour.withThird(d);
      }

      @Override
      public AnyOfFour<A, B, C, E> contractFourth(Function<? super D, AnyOfFour<A, B, C, E>> function) {
         AnyOfFour<A, B, C, E> result = function.apply(d);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public AnyOfFour<A, B, C, D> contractFifth(Function<? super E, AnyOfFour<A, B, C, D>> function) {
         return AnyOfFour.withFourth(d);
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
         return "AnyOfFive.Fourth: " + d;
      }
   }

   private static class Fifth<A, B, C, D, E> extends AnyOfFive<A, B, C, D, E> implements Serializable {
      private static final long serialVersionUID = -8942536580371429918L;
      
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
      public Optional<A> tryFirst() {
         return Optional.none();
      }
   
      @Override
      public Optional<B> trySecond() {
         return Optional.none();
      }
   
      @Override
      public Optional<C> tryThird() {
         return Optional.none();
      }
   
      @Override
      public Optional<D> tryFourth() {
         return Optional.none();
      }
   
      @Override
      public Optional<E> tryFifth() {
         return Optional.of(e);
      }
      
      @Override
      public <T> AnyOfFive<T, B, C, D, E> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfFive<T, B, C, D, E> ret = (AnyOfFive<T, B, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, T, C, D, E> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfFive<A, T, C, D, E> ret = (AnyOfFive<A, T, C, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, T, D, E> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfFive<A, B, T, D, E> ret = (AnyOfFive<A, B, T, D, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, C, T, E> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         AnyOfFive<A, B, C, T, E> ret = (AnyOfFive<A, B, C, T, E>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFive<A, B, C, D, T> transformFifth(Function<? super E, ? extends T> function) {
         return AnyOfFive.<A, B, C, D, T>withFifth(function.apply(e));
      }
      
      @Override
      public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
         return visitor.visitFifth(e);
      }

      @Override
      public AnyOfFour<B, C, D, E> contractFirst(Function<? super A, AnyOfFour<B, C, D, E>> function) {
         return AnyOfFour.withFourth(e);
      }

      @Override
      public AnyOfFour<A, C, D, E> contractSecond(Function<? super B, AnyOfFour<A, C, D, E>> function) {
         return AnyOfFour.withFourth(e);
      }

      @Override
      public AnyOfFour<A, B, D, E> contractThird(Function<? super C, AnyOfFour<A, B, D, E>> function) {
         return AnyOfFour.withFourth(e);
      }

      @Override
      public AnyOfFour<A, B, C, E> contractFourth(Function<? super D, AnyOfFour<A, B, C, E>> function) {
         return AnyOfFour.withFourth(e);
      }

      @Override
      public AnyOfFour<A, B, C, D> contractFifth(Function<? super E, AnyOfFour<A, B, C, D>> function) {
         AnyOfFour<A, B, C, D> result = function.apply(e);
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
         return "AnyOfFive.Fifth: " + e;
      }
   }
}
