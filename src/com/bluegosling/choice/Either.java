package com.bluegosling.choice;

import com.bluegosling.possible.Possible;
import com.bluegosling.possible.Reference;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A choice between one of two options, neither of which can be null. Either the first or second
 * option will be present -- never both and never neither. The option that is present will never be
 * null.
 *
 * @param <A> the type of the first choice
 * @param <B> the type of the second choice
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests
public abstract class Either<A, B> implements Choice.OfTwo<A, B> {
   
   /**
    * Constructs an object whose first choice is present.
    *
    * @param a the value of the first choice
    * @return an object whose first choice is present
    * @throws NullPointerException if the given value is null
    */
   public static <A, B> Either<A, B> withFirst(A a) {
      if (a == null) {
         throw new NullPointerException();
      }
      return new First<A, B>(a);
   }
   
   /**
    * Constructs an object whose second choice is present.
    *
    * @param b the value of the second choice
    * @return an object whose second choice is present
    * @throws NullPointerException if the given value is null
    */
   public static <A, B> Either<A, B> withSecond(B b) {
      if (b == null) {
         throw new NullPointerException();
      }
      return new Second<A, B>(b);
   }

   /**
    * Constructs an object with the given values. Exactly one of the given values must be non-null.
    * If the first value is non-null, an object whose first choice is present is returned.
    * Otherwise, an object whose second choice is present is returned.
    *
    * @param a the value of the first choice
    * @param b the value of the second choice
    * @return either the first or second value, depending on which is non-null
    * @throws IllegalArgumentException if both arguments are null or if neither is
    */
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

   /**
    * Constructs an object using the first of the given values that is non-null. At least one of the
    * given values must be non-null. If the first value is non-null, an object whose first choice is
    * present is returned. Otherwise, an object whose second choice is present is returned.
    *
    * @param a the value of the first choice
    * @param b the value of the second choice
    * @return either the first value if it is non-null or the second value otherwise
    * @throws IllegalArgumentException if both arguments are null
    */
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
      
   /**
    * @throws NullPointerException if the function is invoked and returns null
    */
   @Override
   public abstract <T> Either<T, B> mapFirst(Function<? super A, ? extends T> function);
   
   /**
    * @throws NullPointerException if the function is invoked and returns null
    */
   @Override
   public abstract <T> Either<A, T> mapSecond(Function<? super B, ? extends T> function);
   
   @Override
   public abstract <C> AnyOfThree<C, A, B> expandFirst();

   @Override
   public abstract <C> AnyOfThree<A, C, B> expandSecond();

   @Override
   public abstract <C> AnyOfThree<A, B, C> expandThird();
   
   /**
    * Returns a new object with the same choice present as this object, but with positions swapped.
    * So if this object has the first choice present, the returned object has the second choice
    * present, and vice versa.
    *
    * @return a new object with the first and second choices in swapped positions
    */
   public abstract Either<B, A> swap();
   
   /**
    * Exchanges an object with its first option present for one with its second option present. If
    * this option has the second option present, it is returned unchanged. The given function is
    * used to "exchange" the first option, if present, for a value whose type is that of the second
    * option.
    *
    * @param function a function that exchanges the first option, if present, for a value of the
    *       same type as the second option
    * @return a new object with its second option present, after exchanging the first option if
    *       necessary
    * @throws NullPointerException if the function is invoked and returns null
    * @see #exchangeSecond(Function)
    */
   public abstract Either<A, B> exchangeFirst(Function<? super A, ? extends B> function);

   /**
    * Exchanges an object with its second option present for one with its first option present. If
    * this option has the first option present, it is returned unchanged. The given function is
    * used to "exchange" the second option, if present, for a value whose type is that of the first
    * option.
    *
    * @param function a function that exchanges the second option, if present, for a value of the
    *       same type as the first option
    * @return a new object with its first option present, after exchanging the second option if
    *       necessary
    */
   public abstract Either<A, B> exchangeSecond(Function<? super B, ? extends A> function);
   
   /**
    * Contracts this choice from two options to one by removing the first option. If the first
    * option is present, the given function is used to compute a new value of the correct type.
    *
    * @param function a function that accepts the first option, if present, and computes a value
    *       with the same type as the second option
    * @return the value of the second option if present, otherwise the result of applying the given
    *       function to the value of the first option
    */
   public abstract B contractFirst(Function<? super A, ? extends B> function);
   
   /**
    * Contracts this choice from two options to one by removing the second option. If the second
    * option is present, the given function is used to compute a new value of the correct type.
    *
    * @param function a function that accepts the second option, if present, and computes a value
    *       with the same type as the first option
    * @return the value of the first option if present, otherwise the result of applying the given
    *       function to the value of the second option
    */
   public abstract A contractSecond(Function<? super B, ? extends A> function);

   /**
    * Maps the value of the first option, if present, to a new choice. If this option has its second
    * option present, it is returned unchanged. Otherwise, the given function is applied to the
    * value of the first option to produce a new choice.
    *
    * @param function a function that accepts the first option, if present, and computes a new
    *       choice
    * @return the current choice if the second option is present, or the result of applying the
    *       given function to the value of the first option
    */
   public abstract Either<A, B> flatMapFirst(Function<? super A, Either<A, B>> function);

   /**
    * Maps the value of the second option, if present, to a new choice. If this option has its first
    * option present, it is returned unchanged. Otherwise, the given function is applied to the
    * value of the second option to produce a new choice.
    *
    * @param function a function that accepts the second option, if present, and computes a new
    *       choice
    * @return the current choice if the first option is present, or the result of applying the
    *       given function to the value of the second option
    */
   public abstract Either<A, B> flatMapSecond(Function<? super B, Either<A, B>> function);

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
      public Possible<A> tryFirst() {
         return Reference.setTo(a);
      }
   
      @Override
      public Possible<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public <T> Either<T, B> mapFirst(Function<? super A, ? extends T> function) {
         return Either.<T, B>withFirst(function.apply(a));
      }
   
      @Override
      public <T> Either<A, T> mapSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Either<A, T> ret = (Either<A, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(VisitorOfTwo<? super A, ? super B, R> visitor) {
         return visitor.visitFirst(a);
      }

      @Override
      public <C> AnyOfThree<C, A, B> expandFirst() {
         return AnyOfThree.withSecond(a);
      }
      
      @Override
      public <C> AnyOfThree<A, C, B> expandSecond() {
         return AnyOfThree.withFirst(a);
      }
      
      @Override
      public <C> AnyOfThree<A, B, C> expandThird() {
         return AnyOfThree.withFirst(a);
      }
      
      @Override
      public Either<B, A> swap() {
         return new Second<B, A>(a);
      }
      
      @Override
      public Either<A, B> exchangeFirst(Function<? super A, ? extends B> function) {
         return Either.<A, B>withSecond(function.apply(a));
      }

      @Override
      public Either<A, B> exchangeSecond(Function<? super B, ? extends A> function) {
         return this;
      }
      
      @Override
      public B contractFirst(Function<? super A, ? extends B> function) {
         return function.apply(a);
      }
      
      @Override
      public A contractSecond(Function<? super B, ? extends A> function) {
         return a;
      }

      @Override
      public Either<A, B> flatMapFirst(Function<? super A, Either<A, B>> function) {
         return function.apply(a);
      }

      @Override
      public Either<A, B> flatMapSecond(Function<? super B, Either<A, B>> function) {
         return this;
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
      public Possible<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Possible<B> trySecond() {
         return Reference.setTo(b);
      }
   
      @Override
      public <T> Either<T, B> mapFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Either<T, B> ret = (Either<T, B>) this;
         return ret;
      }
   
      @Override
      public <T> Either<A, T> mapSecond(Function<? super B, ? extends T> function) {
         return Either.<A, T>withSecond(function.apply(b));
      }

      @Override
      public <R> R visit(VisitorOfTwo<? super A, ? super B, R> visitor) {
         return visitor.visitSecond(b);
      }

      @Override
      public <C> AnyOfThree<C, A, B> expandFirst() {
         return AnyOfThree.withThird(b);
      }
      
      @Override
      public <C> AnyOfThree<A, C, B> expandSecond() {
         return AnyOfThree.withThird(b);
      }
      
      @Override
      public <C> AnyOfThree<A, B, C> expandThird() {
         return AnyOfThree.withSecond(b);
      }
      
      @Override
      public Either<B, A> swap() {
         return new First<B, A>(b);
      }
      
      @Override
      public Either<A, B> exchangeFirst(Function<? super A, ? extends B> function) {
         return this;
      }

      @Override
      public Either<A, B> exchangeSecond(Function<? super B, ? extends A> function) {
         return Either.<A, B>withFirst(function.apply(b));
      }
      
      @Override
      public B contractFirst(Function<? super A, ? extends B> function) {
         return b;
      }
      
      @Override
      public A contractSecond(Function<? super B, ? extends A> function) {
         return function.apply(b);
      }

      @Override
      public Either<A, B> flatMapFirst(Function<? super A, Either<A, B>> function) {
         return this;
      }

      @Override
      public Either<A, B> flatMapSecond(Function<? super B, Either<A, B>> function) {
         return function.apply(b);
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
