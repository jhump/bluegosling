package com.apriori.choice;

import com.apriori.possible.Possible;
import com.apriori.possible.Reference;

import java.io.Serializable;
import java.util.function.Function;

//TODO: javadoc
//TODO: tests
public abstract class AnyOfThree<A, B, C> implements Choice.OfThree<A, B, C> {
   
   public static <A, B, C> AnyOfThree<A, B, C> withFirst(A a) {
      if (a == null) {
         throw new NullPointerException();
      }
      return new First<A, B, C>(a);
   }
   
   public static <A, B, C> AnyOfThree<A, B, C> withSecond(B b) {
      if (b == null) {
         throw new NullPointerException();
      }
      return new Second<A, B, C>(b);
   }

   public static <A, B, C> AnyOfThree<A, B, C> withThird(C c) {
      if (c == null) {
         throw new NullPointerException();
      }
      return new Third<A, B, C>(c);
   }

   public static <A, B, C> AnyOfThree<A, B, C> of(A a, B b, C c) {
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
      if (count != 1) {
         throw new IllegalArgumentException("Exactly one argument must be non-null");
      }
      if (a != null) {
         return new First<A, B, C>(a);
      } else if (b != null) {
         return new Second<A, B, C>(b);
      } else { // c != null
         return new Third<A, B, C>(c);
      }
   }
   
   public static <A, B, C> AnyOfThree<A, B, C> firstOf(A a, B b, C c) {
      if (a != null) {
         return new First<A, B, C>(a);
      } else if (b != null) {
         return new Second<A, B, C>(b);
      } else if (c != null) {
         return new Third<A, B, C>(c);
      } else {
         throw new IllegalArgumentException("At least one argument must be non-null");
      }
   }
   
   AnyOfThree() {
   }
      
   @Override
   public abstract <T> AnyOfThree<T, B, C> mapFirst(Function<? super A, ? extends T> function);
   
   @Override
   public abstract <T> AnyOfThree<A, T, C> mapSecond(Function<? super B, ? extends T> function);

   @Override
   public abstract <T> AnyOfThree<A, B, T> mapThird(Function<? super C, ? extends T> function);

   @Override
   public abstract <D> AnyOfFour<D, A, B, C> expandFirst();

   @Override
   public abstract <D> AnyOfFour<A, D, B, C> expandSecond();

   @Override
   public abstract <D> AnyOfFour<A, B, D, C> expandThird();

   @Override
   public abstract <D> AnyOfFour<A, B, C, D> expandFourth();
   
   public abstract Either<B, C> contractFirst(Function<? super A, Either<B, C>> function);

   public abstract Either<A, C> contractSecond(Function<? super B, Either<A, C>> function);

   public abstract Either<A, B> contractThird(Function<? super C, Either<A, B>> function);

   public abstract AnyOfThree<A, B, C> flatMapFirst(Function<? super A, AnyOfThree<A, B, C>> function);
   
   public abstract AnyOfThree<A, B, C> flatMapSecond(Function<? super B, AnyOfThree<A, B, C>> function);
   
   public abstract AnyOfThree<A, B, C> flatMapThird(Function<? super C, AnyOfThree<A, B, C>> function);
   
   private static class First<A, B, C> extends AnyOfThree<A, B, C> implements Serializable {
      private static final long serialVersionUID = 5913498780719060275L;
      
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
      public Possible<A> tryFirst() {
         return Reference.setTo(a);
      }
   
      @Override
      public Possible<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Possible<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public <T> AnyOfThree<T, B, C> mapFirst(Function<? super A, ? extends T> function) {
         return AnyOfThree.<T, B, C>withFirst(function.apply(a));
      }
   
      @Override
      public <T> AnyOfThree<A, T, C> mapSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfThree<A, T, C> ret = (AnyOfThree<A, T, C>) this;
         return ret;
      }

      @Override
      public <T> AnyOfThree<A, B, T> mapThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfThree<A, B, T> ret = (AnyOfThree<A, B, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfThree<? super A, ? super B, ? super C, R> visitor) {
         return visitor.visitFirst(a);
      }

      @Override
      public <D> AnyOfFour<D, A, B, C> expandFirst() {
         return AnyOfFour.withSecond(a);
      }
      
      @Override
      public <D> AnyOfFour<A, D, B, C> expandSecond() {
         return AnyOfFour.withFirst(a);
      }
      
      @Override
      public <D> AnyOfFour<A, B, D, C> expandThird() {
         return AnyOfFour.withFirst(a);
      }
      
      @Override
      public <D> AnyOfFour<A, B, C, D> expandFourth() {
         return AnyOfFour.withFirst(a);
      }
      
      @Override
      public Either<B, C> contractFirst(Function<? super A, Either<B, C>> function) {
         Either<B, C> result = function.apply(a);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public Either<A, C> contractSecond(Function<? super B, Either<A, C>> function) {
         return Either.withFirst(a);
      }

      @Override
      public Either<A, B> contractThird(Function<? super C, Either<A, B>> function) {
         return Either.withFirst(a);
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapFirst(Function<? super A, AnyOfThree<A, B, C>> function) {
         return function.apply(a);
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapSecond(Function<? super B, AnyOfThree<A, B, C>> function) {
         return this;
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapThird(Function<? super C, AnyOfThree<A, B, C>> function) {
         return this;
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof First && a.equals(((First<?, ?, ?>) o).a);
      }
      
      @Override
      public int hashCode() {
         return First.class.hashCode() ^ a.hashCode();
      }
      
      @Override
      public String toString() {
         return "AnyOfThree.First: " + a;
      }
   }

   private static class Second<A, B, C> extends AnyOfThree<A, B, C> implements Serializable {
      private static final long serialVersionUID = 4491297210217980296L;
      
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
      public Possible<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Possible<B> trySecond() {
         return Reference.setTo(b);
      }
   
      @Override
      public Possible<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public <T> AnyOfThree<T, B, C> mapFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfThree<T, B, C> ret = (AnyOfThree<T, B, C>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfThree<A, T, C> mapSecond(Function<? super B, ? extends T> function) {
         return AnyOfThree.<A, T, C>withSecond(function.apply(b));
      }
   
      @Override
      public <T> AnyOfThree<A, B, T> mapThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         AnyOfThree<A, B, T> ret = (AnyOfThree<A, B, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfThree<? super A, ? super B, ? super C, R> visitor) {
         return visitor.visitSecond(b);
      }

      @Override
      public <D> AnyOfFour<D, A, B, C> expandFirst() {
         return AnyOfFour.withThird(b);
      }
      
      @Override
      public <D> AnyOfFour<A, D, B, C> expandSecond() {
         return AnyOfFour.withThird(b);
      }
      
      @Override
      public <D> AnyOfFour<A, B, D, C> expandThird() {
         return AnyOfFour.withSecond(b);
      }
      
      @Override
      public <D> AnyOfFour<A, B, C, D> expandFourth() {
         return AnyOfFour.withSecond(b);
      }
      
      @Override
      public Either<B, C> contractFirst(Function<? super A, Either<B, C>> function) {
         return Either.withFirst(b);
      }

      @Override
      public Either<A, C> contractSecond(Function<? super B, Either<A, C>> function) {
         Either<A, C> result = function.apply(b);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public Either<A, B> contractThird(Function<? super C, Either<A, B>> function) {
         return Either.withSecond(b);
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapFirst(Function<? super A, AnyOfThree<A, B, C>> function) {
         return this;
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapSecond(Function<? super B, AnyOfThree<A, B, C>> function) {
         return function.apply(b);
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapThird(Function<? super C, AnyOfThree<A, B, C>> function) {
         return this;
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Second && b.equals(((Second<?, ?, ?>) o).b);
      }
      
      @Override
      public int hashCode() {
         return Second.class.hashCode() ^ b.hashCode();
      }
      
      @Override
      public String toString() {
         return "AnyOfThree.Second: " + b;
      }
   }

   private static class Third<A, B, C> extends AnyOfThree<A, B, C> implements Serializable {
      private static final long serialVersionUID = -4582067423968099933L;
      
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
      public Possible<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Possible<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Possible<C> tryThird() {
         return Reference.setTo(c);
      }
   
      @Override
      public <T> AnyOfThree<T, B, C> mapFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         AnyOfThree<T, B, C> ret = (AnyOfThree<T, B, C>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfThree<A, T, C> mapSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         AnyOfThree<A, T, C> ret = (AnyOfThree<A, T, C>) this;
         return ret;
      }
   
      @Override
      public <T> AnyOfThree<A, B, T> mapThird(Function<? super C, ? extends T> function) {
         return AnyOfThree.<A, B, T>withThird(function.apply(c));
      }
      
      @Override
      public <R> R visit(VisitorOfThree<? super A, ? super B, ? super C, R> visitor) {
         return visitor.visitThird(c);
      }

      @Override
      public <D> AnyOfFour<D, A, B, C> expandFirst() {
         return AnyOfFour.withFourth(c);
      }
      
      @Override
      public <D> AnyOfFour<A, D, B, C> expandSecond() {
         return AnyOfFour.withFourth(c);
      }
      
      @Override
      public <D> AnyOfFour<A, B, D, C> expandThird() {
         return AnyOfFour.withFourth(c);
      }
      
      @Override
      public <D> AnyOfFour<A, B, C, D> expandFourth() {
         return AnyOfFour.withThird(c);
      }
      
      @Override
      public Either<B, C> contractFirst(Function<? super A, Either<B, C>> function) {
         return Either.withSecond(c);
      }

      @Override
      public Either<A, C> contractSecond(Function<? super B, Either<A, C>> function) {
         return Either.withSecond(c);
      }

      @Override
      public Either<A, B> contractThird(Function<? super C, Either<A, B>> function) {
         Either<A, B> result = function.apply(c);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapFirst(Function<? super A, AnyOfThree<A, B, C>> function) {
         return this;
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapSecond(Function<? super B, AnyOfThree<A, B, C>> function) {
         return this;
      }
      
      @Override
      public AnyOfThree<A, B, C> flatMapThird(Function<? super C, AnyOfThree<A, B, C>> function) {
         return function.apply(c);
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof Third && c.equals(((Third<?, ?, ?>) o).c);
      }
      
      @Override
      public int hashCode() {
         return Third.class.hashCode() ^ c.hashCode();
      }
      
      @Override
      public String toString() {
         return "AnyOfThree.Third: " + c;
      }
   }
}
