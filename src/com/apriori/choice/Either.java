package com.apriori.choice;

import com.apriori.choice.Choices.Choices2;
import com.apriori.choice.Choices.Visitor2;
import com.apriori.possible.Optional;
import com.apriori.util.Function;

import java.io.Serializable;

//TODO: javadoc
//TODO: tests
public abstract class Either<A, B> implements Choices2<A, B> {
   
   public static <A, B> Either<A, B> withFirst(A a) {
      if (a == null) {
         throw new NullPointerException();
      }
      return new First<A, B>(a);
   }
   
   public static <A, B> Either<A, B> withSecond(B b) {
      if (b == null) {
         throw new NullPointerException();
      }
      return new Second<A, B>(b);
   }

   public static <A, B> Either<A, B> of(A a, B b) {
      if ((a == null) == (b == null)) {
         // both are null or both are non-null
         throw new IllegalArgumentException("Exactly one argument must be non-null");
      }
      if (a != null) {
         return new First<A, B>(a);
      } else {
         return new Second<A, B>(b);
      }
   }

   public static <A, B> Either<A, B> firstOf(A a, B b) {
      if (a != null) {
         return new First<A, B>(a);
      } else if (b != null) {
         return new Second<A, B>(b);
      } else {
         throw new IllegalArgumentException("At least one argument must be non-null");
      }
   }
   
   Either() {
   }
      
   @Override
   public abstract Optional<A> tryFirst();

   @Override
   public abstract Optional<B> trySecond();

   @Override
   public abstract <T> Either<T, B> transformFirst(Function<? super A, ? extends T> function);
   
   @Override
   public abstract <T> Either<A, T> transformSecond(Function<? super B, ? extends T> function);
   
   @Override
   public abstract <C> AnyOfThree<A, B, C> expand();
   
   public abstract Either<B, A> swap();
   
   private static class First<A, B> extends Either<A, B> implements Serializable {
      private static final long serialVersionUID = 660180035695385048L;

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
      public Optional<A> tryFirst() {
         return Optional.of(a);
      }
   
      @Override
      public Optional<B> trySecond() {
         return Optional.none();
      }
   
      @Override
      public <T> Either<T, B> transformFirst(Function<? super A, ? extends T> function) {
         return Either.<T, B>withFirst(function.apply(a));
      }
   
      @Override
      public <T> Either<A, T> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Either<A, T> ret = (Either<A, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(Visitor2<? super A, ? super B, R> visitor) {
         return visitor.visitFirst(a);
      }

      @Override
      public <C> AnyOfThree<A, B, C> expand() {
         return AnyOfThree.withFirst(a);
      }
      
      @Override
      public Either<B, A> swap() {
         return new Second<B, A>(a);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof First && a.equals(((First<?, ?>) o).a);
      }
      
      @Override
      public int hashCode() {
         return First.class.hashCode() ^ a.hashCode();
      }
      
      @Override
      public String toString() {
         return "Either.First: " + a;
      }
   }

   private static class Second<A, B> extends Either<A, B> implements Serializable {
      private static final long serialVersionUID = -2546803703837174624L;

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
      public Optional<A> tryFirst() {
         return Optional.none();
      }
   
      @Override
      public Optional<B> trySecond() {
         return Optional.of(b);
      }
   
      @Override
      public <T> Either<T, B> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Either<T, B> ret = (Either<T, B>) this;
         return ret;
      }
   
      @Override
      public <T> Either<A, T> transformSecond(Function<? super B, ? extends T> function) {
         return Either.<A, T>withSecond(function.apply(b));
      }

      @Override
      public <R> R visit(Visitor2<? super A, ? super B, R> visitor) {
         return visitor.visitSecond(b);
      }

      @Override
      public <C> AnyOfThree<A, B, C> expand() {
         return AnyOfThree.withSecond(b);
      }
      
      @Override
      public Either<B, A> swap() {
         return new First<B, A>(b);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Second && b.equals(((Second<?, ?>) o).b);
      }
      
      @Override
      public int hashCode() {
         return Second.class.hashCode() ^ b.hashCode();
      }
      
      @Override
      public String toString() {
         return "Either.Second: " + b;
      }
   }
}
