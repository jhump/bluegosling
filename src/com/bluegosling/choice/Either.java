package com.bluegosling.choice;

import com.bluegosling.possible.Possible;
import com.bluegosling.possible.Reference;
import com.bluegosling.util.ValueType;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
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
@ValueType
//For efficiency, we store the value in a single Object field and then must cast to type variable
//A or B (which is an unchecked cast). This is safe due to the invariant ensured by the factory
//methods that create instances: if index == 0 then value is an A, otherwise it's a B.
@SuppressWarnings("unchecked")
public final class Either<A, B> implements Choice.OfTwo<A, B>, Serializable {
   private static final long serialVersionUID = -3463776758581248720L;
   
   private final Object value;
   private final int index;
   
   private Either(Object value, int index) {
      assert index >= 0 && index < 2;
      assert value != null;
      this.value = value;
      this.index = index;
   }
   
   /**
    * Constructs an object whose first choice is present.
    *
    * @param a the value of the first choice
    * @return an object whose first choice is present
    * @throws NullPointerException if the given value is null
    */
   public static <A, B> Either<A, B> withFirst(A a) {
      return new Either<>(requireNonNull(a), 0);
   }
   
   /**
    * Constructs an object whose second choice is present.
    *
    * @param b the value of the second choice
    * @return an object whose second choice is present
    * @throws NullPointerException if the given value is null
    */
   public static <A, B> Either<A, B> withSecond(B b) {
      return new Either<>(requireNonNull(b), 1);
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
      return a != null ? new Either<>(a, 0) : new Either<>(b, 1);
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
         return new Either<>(a, 0);
      } else if (b != null) {
         return new Either<>(b, 1);
      } else {
         throw new IllegalArgumentException("At least one argument must be non-null");
      }
   }

   @Override
   public boolean hasFirst() {
      return index == 0;
   }

   @Override
   public boolean hasSecond() {
      return index == 1;
   }

   @Override
   public Object get() {
      return value;
   }
   
   @Override
   public A getFirst() {
      if (index != 0) {
         throw new NoSuchElementException();
      }
      return (A) value;
   }

   @Override
   public B getSecond() {
      if (index != 1) {
         throw new NoSuchElementException();
      }
      return (B) value;
   }

   @Override
   public Possible<A> tryFirst() {
      return index == 0 ? Reference.setTo((A) value) : Reference.unset();
   }

   @Override
   public Possible<B> trySecond() {
      return index == 1 ? Reference.setTo((B) value) : Reference.unset();
   }
   
   /**
    * @throws NullPointerException if the function is invoked and returns null
    */
   @Override
   public <T> Either<T, B> mapFirst(Function<? super A, ? extends T> function) {
      return index == 0 ? withFirst(function.apply((A) value)) : (Either<T, B>) this;
   }
   
   /**
    * @throws NullPointerException if the function is invoked and returns null
    */
   @Override
   public <T> Either<A, T> mapSecond(Function<? super B, ? extends T> function) {
      return index == 1 ? withSecond(function.apply((B) value)) : (Either<A, T>) this;
   }
   
   @Override
   public <C> AnyOfThree<C, A, B> expandFirst() {
      return index == 0 ? AnyOfThree.withSecond((A) value) : AnyOfThree.withThird((B) value);
   }

   @Override
   public <C> AnyOfThree<A, C, B> expandSecond() {
      return index == 0 ? AnyOfThree.withFirst((A) value) : AnyOfThree.withThird((B) value);
   }

   @Override
   public <C> AnyOfThree<A, B, C> expandThird() {
      return index == 0 ? AnyOfThree.withFirst((A) value) : AnyOfThree.withSecond((B) value);
   }
   
   /**
    * Returns a new object with the same choice present as this object, but with positions swapped.
    * So if this object has the first choice present, the returned object has the second choice
    * present, and vice versa.
    *
    * @return a new object with the first and second choices in swapped positions
    */
   public Either<B, A> swap() {
      return new Either<>(value, 1 - index);
   }
   
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
    */
   public Either<A, B> exchangeFirst(Function<? super A, ? extends B> function) {
      return index == 0 ? withSecond(function.apply((A) value)) : this;
   }

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
    * @throws NullPointerException if the function is invoked and returns null
    */
   public Either<A, B> exchangeSecond(Function<? super B, ? extends A> function) {
      return index == 1 ? withFirst(function.apply((B) value)) : this;
   }
   
   /**
    * Contracts this choice from two options to one by removing the first option. If the first
    * option is present, the given function is used to compute a new value of the correct type.
    *
    * @param function a function that accepts the first option, if present, and computes a value
    *       with the same type as the second option
    * @return the value of the second option if present, otherwise the result of applying the given
    *       function to the value of the first option
    */
   public B contractFirst(Function<? super A, ? extends B> function) {
      return index == 0 ? function.apply((A) value) : (B) value;
   }
   
   /**
    * Contracts this choice from two options to one by removing the second option. If the second
    * option is present, the given function is used to compute a new value of the correct type.
    *
    * @param function a function that accepts the second option, if present, and computes a value
    *       with the same type as the first option
    * @return the value of the first option if present, otherwise the result of applying the given
    *       function to the value of the second option
    */
   public A contractSecond(Function<? super B, ? extends A> function) {
      return index == 1 ? function.apply((B) value) : (A) value;
   }

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
   public Either<A, B> flatMapFirst(Function<? super A, Either<A, B>> function) {
      return index == 0 ? requireNonNull(function.apply((A) value)) : this;
   }

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
   public Either<A, B> flatMapSecond(Function<? super B, Either<A, B>> function) {
      return index == 1 ? requireNonNull(function.apply((B) value)) : this;
   }
   
   @Override
   public <R> R visit(VisitorOfTwo<? super A, ? super B, R> visitor) {
      return index == 0 ? visitor.visitFirst((A) value) : visitor.visitSecond((B) value);
   }
   
   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Either)) {
         return false;
      }
      Either<?, ?> other = (Either<?, ?>) o;
      return index == other.index && Objects.equals(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return index ^ Objects.hashCode(value); 
   }
   
   @Override
   public String toString() {
      return index == 0
            ? "Either.first[" + value + "]"
            : "Either.second[" + value + "]";
   }
}
