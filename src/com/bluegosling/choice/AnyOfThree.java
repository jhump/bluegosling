package com.bluegosling.choice;

import com.bluegosling.possible.Possible;
import com.bluegosling.possible.Reference;
import com.bluegosling.util.ValueType;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

//TODO: javadoc
@ValueType
//For efficiency, we store the value in a single Object field and then must cast to type variable
//A or B (which is an unchecked cast). This is safe due to the invariant ensured by the factory
//methods that create instances: if index == 0 then value is an A, if index == 1 then it's a B, 
// otherwise it's a C.
@SuppressWarnings("unchecked")
public final class AnyOfThree<A, B, C> implements Choice.OfThree<A, B, C>, Serializable {
   private static final long serialVersionUID = 6701641679792447300L;

   private final Object value;
   private final int index;
   
   private AnyOfThree(Object value, int index) {
      assert index >= 0 && index < 3;
      assert value != null;
      this.value = value;
      this.index = index;
   }
   
   public static <A, B, C> AnyOfThree<A, B, C> withFirst(A a) {
      return new AnyOfThree<>(requireNonNull(a), 0);
   }
   
   public static <A, B, C> AnyOfThree<A, B, C> withSecond(B b) {
      return new AnyOfThree<>(requireNonNull(b), 1);
   }

   public static <A, B, C> AnyOfThree<A, B, C> withThird(C c) {
      return new AnyOfThree<>(requireNonNull(c), 2);
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
         return new AnyOfThree<>(a, 0);
      } else if (b != null) {
         return new AnyOfThree<>(b, 1);
      } else { // c != null
         return new AnyOfThree<>(c, 2);
      }
   }
   
   public static <A, B, C> AnyOfThree<A, B, C> firstOf(A a, B b, C c) {
      if (a != null) {
         return new AnyOfThree<>(a, 0);
      } else if (b != null) {
         return new AnyOfThree<>(b, 1);
      } else if (c != null) {
         return new AnyOfThree<>(c, 2);
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
   public boolean hasThird() {
      return index == 2;
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
   public C getThird() {
      if (index != 2) {
         throw new NoSuchElementException();
      }
      return (C) value;
   }

   @Override
   public Possible<A> tryFirst() {
      return index == 0 ? Reference.setTo((A) value) : Reference.unset();
   }

   @Override
   public Possible<B> trySecond() {
      return index == 1 ? Reference.setTo((B) value) : Reference.unset();
   }

   @Override
   public Possible<C> tryThird() {
      return index == 2 ? Reference.setTo((C) value) : Reference.unset();
   }

   @Override
   public <T> AnyOfThree<T, B, C> mapFirst(Function<? super A, ? extends T> function) {
      return index == 0 ? withFirst(function.apply((A) value)) : (AnyOfThree<T, B, C>) this;
   }
   
   @Override
   public <T> AnyOfThree<A, T, C> mapSecond(Function<? super B, ? extends T> function) {
      return index == 1 ? withSecond(function.apply((B) value)) : (AnyOfThree<A, T, C>) this;
   }

   @Override
   public <T> AnyOfThree<A, B, T> mapThird(Function<? super C, ? extends T> function) {
      return index == 2 ? withThird(function.apply((C) value)) : (AnyOfThree<A, B, T>) this;
   }

   @Override
   public <D> AnyOfFour<D, A, B, C> expandFirst() {
      if (index == 0) {
         return AnyOfFour.withSecond((A) value);
      } else if (index == 1) {
         return AnyOfFour.withThird((B) value);
      } else { // index == 2
         return AnyOfFour.withFourth((C) value);
      }
   }

   @Override
   public <D> AnyOfFour<A, D, B, C> expandSecond() {
      if (index == 0) {
         return AnyOfFour.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFour.withThird((B) value);
      } else { // index == 2
         return AnyOfFour.withFourth((C) value);
      }
   }

   @Override
   public <D> AnyOfFour<A, B, D, C> expandThird() {
      if (index == 0) {
         return AnyOfFour.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFour.withSecond((B) value);
      } else { // index == 2
         return AnyOfFour.withFourth((C) value);
      }
   }

   @Override
   public <D> AnyOfFour<A, B, C, D> expandFourth() {
      if (index == 0) {
         return AnyOfFour.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFour.withSecond((B) value);
      } else { // index == 2
         return AnyOfFour.withThird((C) value);
      }
   }
   
   public Either<B, C> contractFirst(Function<? super A, Either<B, C>> function) {
      if (index == 0) {
         return function.apply((A) value);
      } else if (index == 1) {
         return Either.withFirst((B) value);
      } else { // index == 2
         return Either.withSecond((C) value);
      }
   }

   public Either<A, C> contractSecond(Function<? super B, Either<A, C>> function) {
      if (index == 0) {
         return Either.withFirst((A) value);
      } else if (index == 1) {
         return function.apply((B) value);
      } else { // index == 2
         return Either.withSecond((C) value);
      }
   }

   public Either<A, B> contractThird(Function<? super C, Either<A, B>> function) {
      if (index == 0) {
         return Either.withFirst((A) value);
      } else if (index == 1) {
         return Either.withSecond((B) value);
      } else { // index == 2
         return function.apply((C) value);
      }
   }

   public AnyOfThree<A, B, C> flatMapFirst(Function<? super A, AnyOfThree<A, B, C>> function) {
      return index == 0 ? requireNonNull(function.apply((A) value)) : this;
   }
   
   public AnyOfThree<A, B, C> flatMapSecond(Function<? super B, AnyOfThree<A, B, C>> function) {
      return index == 1 ? requireNonNull(function.apply((B) value)) : this;
   }
   
   public AnyOfThree<A, B, C> flatMapThird(Function<? super C, AnyOfThree<A, B, C>> function) {
      return index == 2 ? requireNonNull(function.apply((C) value)) : this;
   }
   
   @Override
   public <R> R visit(VisitorOfThree<? super A, ? super B, ? super C, R> visitor) {
      if (index == 0) {
         return visitor.visitFirst((A) value);
      } else if (index == 1) {
         return visitor.visitSecond((B) value);
      } else { // index == 2
         return visitor.visitThird((C) value);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof AnyOfThree)) {
         return false;
      }
      AnyOfThree<?, ?, ?> other = (AnyOfThree<?, ?, ?>) o;
      return index == other.index && Objects.equals(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return index ^ Objects.hashCode(value); 
   }
   
   @Override
   public String toString() {
      if (index == 0) {
         return "AnyOfThree.first[" + value + "]";
      } else if (index == 1) {
         return "AnyOfThree.second[" + value + "]";
      } else { // index == 2
         return "AnyOfThree.third[" + value + "]";
      }
   }
}
