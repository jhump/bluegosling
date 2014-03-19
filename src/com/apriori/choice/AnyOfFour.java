package com.apriori.choice;

import com.apriori.possible.Optional;
import com.apriori.util.Function;

import java.io.Serializable;

//TODO: javadoc
//TODO: tests
public abstract class AnyOfFour<A, B, C, D> implements Choice.OfFour<A, B, C, D> {
   
   public static <A, B, C, D> AnyOfFour<A, B, C, D> withFirst(A a) {
      if (a == null) {
         throw new NullPointerException();
      }
      return new First<A, B, C, D>(a);
   }
   
   public static <A, B, C, D> AnyOfFour<A, B, C, D> withSecond(B b) {
      if (b == null) {
         throw new NullPointerException();
      }
      return new Second<A, B, C, D>(b);
   }

   public static <A, B, C, D> AnyOfFour<A, B, C, D> withThird(C c) {
      if (c == null) {
         throw new NullPointerException();
      }
      return new Third<A, B, C, D>(c);
   }

   public static <A, B, C, D> AnyOfFour<A, B, C, D> withFourth(D d) {
      if (d == null) {
         throw new NullPointerException();
      }
      return new Fourth<A, B, C, D>(d);
   }

   public static <A, B, C, D> AnyOfFour<A, B, C, D> of(A a, B b, C c, D d) {
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
      if (count != 1) {
         throw new IllegalArgumentException("Exactly one argument must be non-null");
      }
      if (a != null) {
         return new First<A, B, C, D>(a);
      } else if (b != null) {
         return new Second<A, B, C, D>(b);
      } else if (c != null) {
         return new Third<A, B, C, D>(c);
      } else { // d != null
         return new Fourth<A, B, C, D>(d);
      }
   }
   
   public static <A, B, C, D> AnyOfFour<A, B, C, D> firstOf(A a, B b, C c, D d) {
      if (a != null) {
         return new First<A, B, C, D>(a);
      } else if (b != null) {
         return new Second<A, B, C, D>(b);
      } else if (c != null) {
         return new Third<A, B, C, D>(c);
      } else if (d != null) {
         return new Fourth<A, B, C, D>(d);
      } else {
         throw new IllegalArgumentException("At least one argument must be non-null");
      }
   }
   
   AnyOfFour() {
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
   public abstract <T> AnyOfFour<T, B, C, D> transformFirst(Function<? super A, ? extends T> function);
   
   @Override
   public abstract <T> AnyOfFour<A, T, C, D> transformSecond(Function<? super B, ? extends T> function);

   @Override
   public abstract <T> AnyOfFour<A, B, T, D> transformThird(Function<? super C, ? extends T> function);

   @Override
   public abstract <T> AnyOfFour<A, B, C, T> transformFourth(Function<? super D, ? extends T> function);

   @Override
   public abstract <E> AnyOfFive<E, A, B, C, D> expandFirst();

   @Override
   public abstract <E> AnyOfFive<A, E, B, C, D> expandSecond();

   @Override
   public abstract <E> AnyOfFive<A, B, E, C, D> expandThird();

   @Override
   public abstract <E> AnyOfFive<A, B, C, E, D> expandFourth();

   @Override
   public abstract <E> AnyOfFive<A, B, C, D, E> expandFifth();
   
   public abstract AnyOfThree<B, C, D> contractFirst(Function<? super A, AnyOfThree<B, C, D>> function);

   public abstract AnyOfThree<A, C, D> contractSecond(Function<? super B, AnyOfThree<A, C, D>> function);

   public abstract AnyOfThree<A, B, D> contractThird(Function<? super C, AnyOfThree<A, B, D>> function);

   public abstract AnyOfThree<A, B, C> contractFourth(Function<? super D, AnyOfThree<A, B, C>> function);
   
   private static class First<A, B, C, D> extends AnyOfFour<A, B, C, D> implements Serializable {
      private static final long serialVersionUID = -5776364918376280846L;

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
      public <T> AnyOfFour<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
         return AnyOfFour.<T, B, C, D>withFirst(function.apply(a));
      }
   
      @Override
      public <T> AnyOfFour<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfFour<A, T, C, D> ret = (AnyOfFour<A, T, C, D>) this;
         return ret;
      }

      @Override
      public <T> AnyOfFour<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfFour<A, B, T, D> ret = (AnyOfFour<A, B, T, D>) this;
         return ret;
      }

      @Override
      public <T> AnyOfFour<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         AnyOfFour<A, B, C, T> ret = (AnyOfFour<A, B, C, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFour<? super A, ? super B, ? super C, ? super D, R> visitor) {
         return visitor.visitFirst(a);
      }

      @Override
      public <E> AnyOfFive<E, A, B, C, D> expandFirst() {
         return AnyOfFive.withSecond(a);
      }
      
      @Override
      public <E> AnyOfFive<A, E, B, C, D> expandSecond() {
         return AnyOfFive.withFirst(a);
      }
      
      @Override
      public <E> AnyOfFive<A, B, E, C, D> expandThird() {
         return AnyOfFive.withFirst(a);
      }
      
      @Override
      public <E> AnyOfFive<A, B, C, E, D> expandFourth() {
         return AnyOfFive.withFirst(a);
      }
      
      @Override
      public <E> AnyOfFive<A, B, C, D, E> expandFifth() {
         return AnyOfFive.withFirst(a);
      }
      
      @Override
      public AnyOfThree<B, C, D> contractFirst(Function<? super A, AnyOfThree<B, C, D>> function) {
        AnyOfThree<B, C, D> result = function.apply(a);
        if (result == null) {
           throw new NullPointerException();
        }
        return result;
      }

      @Override
      public AnyOfThree<A, C, D> contractSecond(Function<? super B, AnyOfThree<A, C, D>> function) {
         return AnyOfThree.withFirst(a);
      }

      @Override
      public AnyOfThree<A, B, D> contractThird(Function<? super C, AnyOfThree<A, B, D>> function) {
         return AnyOfThree.withFirst(a);
      }

      @Override
      public AnyOfThree<A, B, C> contractFourth(Function<? super D, AnyOfThree<A, B, C>> function) {
         return AnyOfThree.withFirst(a);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof First && a.equals(((First<?, ?, ?, ?>) o).a);
      }
      
      @Override
      public int hashCode() {
         return First.class.hashCode() ^ a.hashCode();
      }
      
      @Override
      public String toString() {
         return "AnyOfFour.First: " + a;
      }
   }

   private static class Second<A, B, C, D> extends AnyOfFour<A, B, C, D> implements Serializable {
      private static final long serialVersionUID = 377538971205933269L;
      
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
      public <T> AnyOfFour<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfFour<T, B, C, D> ret = (AnyOfFour<T, B, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFour<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
         return AnyOfFour.<A, T, C, D>withSecond(function.apply(b));
      }
   
      @Override
      public <T> AnyOfFour<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfFour<A, B, T, D> ret = (AnyOfFour<A, B, T, D>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFour<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         AnyOfFour<A, B, C, T> ret = (AnyOfFour<A, B, C, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFour<? super A, ? super B, ? super C, ? super D, R> visitor) {
         return visitor.visitSecond(b);
      }

      @Override
      public <E> AnyOfFive<E, A, B, C, D> expandFirst() {
         return AnyOfFive.withThird(b);
      }
      
      @Override
      public <E> AnyOfFive<A, E, B, C, D> expandSecond() {
         return AnyOfFive.withThird(b);
      }
      
      @Override
      public <E> AnyOfFive<A, B, E, C, D> expandThird() {
         return AnyOfFive.withSecond(b);
      }
      
      @Override
      public <E> AnyOfFive<A, B, C, E, D> expandFourth() {
         return AnyOfFive.withSecond(b);
      }
      
      @Override
      public <E> AnyOfFive<A, B, C, D, E> expandFifth() {
         return AnyOfFive.withSecond(b);
      }
      
      @Override
      public AnyOfThree<B, C, D> contractFirst(Function<? super A, AnyOfThree<B, C, D>> function) {
         return AnyOfThree.withFirst(b);
      }

      @Override
      public AnyOfThree<A, C, D> contractSecond(Function<? super B, AnyOfThree<A, C, D>> function) {
         AnyOfThree<A, C, D> result = function.apply(b);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public AnyOfThree<A, B, D> contractThird(Function<? super C, AnyOfThree<A, B, D>> function) {
         return AnyOfThree.withSecond(b);
      }

      @Override
      public AnyOfThree<A, B, C> contractFourth(Function<? super D, AnyOfThree<A, B, C>> function) {
         return AnyOfThree.withSecond(b);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Second && b.equals(((Second<?, ?, ?, ?>) o).b);
      }
      
      @Override
      public int hashCode() {
         return Second.class.hashCode() ^ b.hashCode();
      }
      
      @Override
      public String toString() {
         return "AnyOfFour.Second: " + b;
      }
   }

   private static class Third<A, B, C, D> extends AnyOfFour<A, B, C, D> implements Serializable {
      private static final long serialVersionUID = 1607338218124641896L;
      
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
      public <T> AnyOfFour<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfFour<T, B, C, D> ret = (AnyOfFour<T, B, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFour<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfFour<A, T, C, D> ret = (AnyOfFour<A, T, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFour<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
         return AnyOfFour.<A, B, T, D>withThird(function.apply(c));
      }
   
      @Override
      public <T> AnyOfFour<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         AnyOfFour<A, B, C, T> ret = (AnyOfFour<A, B, C, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfFour<? super A, ? super B, ? super C, ? super D, R> visitor) {
         return visitor.visitThird(c);
      }

      @Override
      public <E> AnyOfFive<E, A, B, C, D> expandFirst() {
         return AnyOfFive.withFourth(c);
      }
      
      @Override
      public <E> AnyOfFive<A, E, B, C, D> expandSecond() {
         return AnyOfFive.withFourth(c);
      }
      
      @Override
      public <E> AnyOfFive<A, B, E, C, D> expandThird() {
         return AnyOfFive.withFourth(c);
      }
      
      @Override
      public <E> AnyOfFive<A, B, C, E, D> expandFourth() {
         return AnyOfFive.withThird(c);
      }
      
      @Override
      public <E> AnyOfFive<A, B, C, D, E> expandFifth() {
         return AnyOfFive.withThird(c);
      }
      
      @Override
      public AnyOfThree<B, C, D> contractFirst(Function<? super A, AnyOfThree<B, C, D>> function) {
         return AnyOfThree.withSecond(c);
      }

      @Override
      public AnyOfThree<A, C, D> contractSecond(Function<? super B, AnyOfThree<A, C, D>> function) {
         return AnyOfThree.withSecond(c);
      }

      @Override
      public AnyOfThree<A, B, D> contractThird(Function<? super C, AnyOfThree<A, B, D>> function) {
         AnyOfThree<A, B, D> result = function.apply(c);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public AnyOfThree<A, B, C> contractFourth(Function<? super D, AnyOfThree<A, B, C>> function) {
         return AnyOfThree.withThird(c);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Third && c.equals(((Third<?, ?, ?, ?>) o).c);
      }
      
      @Override
      public int hashCode() {
         return Third.class.hashCode() ^ c.hashCode();
      }
      
      @Override
      public String toString() {
         return "AnyOfFour.Third: " + c;
      }
   }

   private static class Fourth<A, B, C, D> extends AnyOfFour<A, B, C, D> implements Serializable {
      private static final long serialVersionUID = 4866675968939538818L;
      
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
      public <T> AnyOfFour<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfFour<T, B, C, D> ret = (AnyOfFour<T, B, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFour<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfFour<A, T, C, D> ret = (AnyOfFour<A, T, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFour<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfFour<A, B, T, D> ret = (AnyOfFour<A, B, T, D>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfFour<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
         return AnyOfFour.<A, B, C, T>withFourth(function.apply(d));
      }
      
      @Override
      public <R> R visit(VisitorOfFour<? super A, ? super B, ? super C, ? super D, R> visitor) {
         return visitor.visitFourth(d);
      }

      @Override
      public <E> AnyOfFive<E, A, B, C, D> expandFirst() {
         return AnyOfFive.withFifth(d);
      }
      
      @Override
      public <E> AnyOfFive<A, E, B, C, D> expandSecond() {
         return AnyOfFive.withFifth(d);
      }
      
      @Override
      public <E> AnyOfFive<A, B, E, C, D> expandThird() {
         return AnyOfFive.withFifth(d);
      }
      
      @Override
      public <E> AnyOfFive<A, B, C, E, D> expandFourth() {
         return AnyOfFive.withFifth(d);
      }
      
      @Override
      public <E> AnyOfFive<A, B, C, D, E> expandFifth() {
         return AnyOfFive.withFourth(d);
      }
      
      @Override
      public AnyOfThree<B, C, D> contractFirst(Function<? super A, AnyOfThree<B, C, D>> function) {
         return AnyOfThree.withThird(d);
      }

      @Override
      public AnyOfThree<A, C, D> contractSecond(Function<? super B, AnyOfThree<A, C, D>> function) {
         return AnyOfThree.withThird(d);
      }

      @Override
      public AnyOfThree<A, B, D> contractThird(Function<? super C, AnyOfThree<A, B, D>> function) {
         return AnyOfThree.withThird(d);
      }

      @Override
      public AnyOfThree<A, B, C> contractFourth(Function<? super D, AnyOfThree<A, B, C>> function) {
         AnyOfThree<A, B, C> result = function.apply(d);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Fourth && d.equals(((Fourth<?, ?, ?, ?>) o).d);
      }
      
      @Override
      public int hashCode() {
         return Fourth.class.hashCode() ^ d.hashCode();
      }
      
      @Override
      public String toString() {
         return "AnyOfFour.Fourth: " + d;
      }
   }
}
