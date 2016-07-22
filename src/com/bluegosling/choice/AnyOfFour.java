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
//and so on.
@SuppressWarnings("unchecked")
public final class AnyOfFour<A, B, C, D> implements Choice.OfFour<A, B, C, D>, Serializable {
   private static final long serialVersionUID = 843813593675166274L;
   
   private final Object value;
   private final int index;
   
   private AnyOfFour(Object value, int index) {
      assert index >= 0 && index < 4;
      assert value != null;
      this.value = value;
      this.index = index;
   }
   
   public static <A, B, C, D> AnyOfFour<A, B, C, D> withFirst(A a) {
      return new AnyOfFour<>(requireNonNull(a), 0);
   }
   
   public static <A, B, C, D> AnyOfFour<A, B, C, D> withSecond(B b) {
      return new AnyOfFour<>(requireNonNull(b), 1);
   }

   public static <A, B, C, D> AnyOfFour<A, B, C, D> withThird(C c) {
      return new AnyOfFour<>(requireNonNull(c), 2);
   }

   public static <A, B, C, D> AnyOfFour<A, B, C, D> withFourth(D d) {
      return new AnyOfFour<>(requireNonNull(d), 3);
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
         return new AnyOfFour<>(a, 0);
      } else if (b != null) {
         return new AnyOfFour<>(b, 1);
      } else if (c != null) {
         return new AnyOfFour<>(c, 2);
      } else { // d != null
         return new AnyOfFour<>(d, 3);
      }
   }
   
   public static <A, B, C, D> AnyOfFour<A, B, C, D> firstOf(A a, B b, C c, D d) {
      if (a != null) {
         return new AnyOfFour<>(a, 0);
      } else if (b != null) {
         return new AnyOfFour<>(b, 1);
      } else if (c != null) {
         return new AnyOfFour<>(c, 2);
      } else if (d != null) {
         return new AnyOfFour<>(d, 3);
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
   public boolean hasFourth() {
      return index == 3;
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
   public D getFourth() {
      if (index != 3) {
         throw new NoSuchElementException();
      }
      return (D) value;
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
   public Possible<D> tryFourth() {
      return index == 3 ? Reference.setTo((D) value) : Reference.unset();
   }
      
   @Override
   public <T> AnyOfFour<T, B, C, D> mapFirst(Function<? super A, ? extends T> function) {
      return index == 0 ? withFirst(function.apply((A) value)) : (AnyOfFour<T, B, C, D>) this;
   }
   
   @Override
   public <T> AnyOfFour<A, T, C, D> mapSecond(Function<? super B, ? extends T> function) {
      return index == 1 ? withSecond(function.apply((B) value)) : (AnyOfFour<A, T, C, D>) this;
   }

   @Override
   public <T> AnyOfFour<A, B, T, D> mapThird(Function<? super C, ? extends T> function) {
      return index == 2 ? withThird(function.apply((C) value)) : (AnyOfFour<A, B, T, D>) this;
   }

   @Override
   public <T> AnyOfFour<A, B, C, T> mapFourth(Function<? super D, ? extends T> function) {
      return index == 3 ? withFourth(function.apply((D) value)) : (AnyOfFour<A, B, C, T>) this;
   }

   @Override
   public <E> AnyOfFive<E, A, B, C, D> expandFirst() {
      if (index == 0) {
         return AnyOfFive.withSecond((A) value);
      } else if (index == 1) {
         return AnyOfFive.withThird((B) value);
      } else if (index == 2) {
         return AnyOfFive.withFourth((C) value);
      } else { // index == 3
         return AnyOfFive.withFifth((D) value);
      }
   }

   @Override
   public <E> AnyOfFive<A, E, B, C, D> expandSecond() {
      if (index == 0) {
         return AnyOfFive.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFive.withThird((B) value);
      } else if (index == 2) {
         return AnyOfFive.withFourth((C) value);
      } else { // index == 3
         return AnyOfFive.withFifth((D) value);
      }
   }

   @Override
   public <E> AnyOfFive<A, B, E, C, D> expandThird() {
      if (index == 0) {
         return AnyOfFive.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFive.withSecond((B) value);
      } else if (index == 2) {
         return AnyOfFive.withFourth((C) value);
      } else { // index == 3
         return AnyOfFive.withFifth((D) value);
      }
   }

   @Override
   public <E> AnyOfFive<A, B, C, E, D> expandFourth() {
      if (index == 0) {
         return AnyOfFive.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFive.withSecond((B) value);
      } else if (index == 2) {
         return AnyOfFive.withThird((C) value);
      } else { // index == 3
         return AnyOfFive.withFifth((D) value);
      }
   }

   @Override
   public <E> AnyOfFive<A, B, C, D, E> expandFifth() {
      if (index == 0) {
         return AnyOfFive.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFive.withSecond((B) value);
      } else if (index == 2) {
         return AnyOfFive.withThird((C) value);
      } else { // index == 3
         return AnyOfFive.withFourth((D) value);
      }
   }
   
   public AnyOfThree<B, C, D> contractFirst(Function<? super A, AnyOfThree<B, C, D>> function) {
      if (index == 0) {
         return function.apply((A) value);
      } else if (index == 1) {
         return AnyOfThree.withFirst((B) value);
      } else if (index == 2) {
         return AnyOfThree.withSecond((C) value);
      } else { // index == 3
         return AnyOfThree.withThird((D) value);
      }
   }

   public AnyOfThree<A, C, D> contractSecond(Function<? super B, AnyOfThree<A, C, D>> function) {
      if (index == 0) {
         return AnyOfThree.withFirst((A) value);
      } else if (index == 1) {
         return function.apply((B) value);
      } else if (index == 2) {
         return AnyOfThree.withSecond((C) value);
      } else { // index == 3
         return AnyOfThree.withThird((D) value);
      }
   }

   public AnyOfThree<A, B, D> contractThird(Function<? super C, AnyOfThree<A, B, D>> function) {
      if (index == 0) {
         return AnyOfThree.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfThree.withSecond((B) value);
      } else if (index == 2) {
         return function.apply((C) value);
      } else { // index == 3
         return AnyOfThree.withThird((D) value);
      }
   }

   public AnyOfThree<A, B, C> contractFourth(Function<? super D, AnyOfThree<A, B, C>> function) {
      if (index == 0) {
         return AnyOfThree.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfThree.withSecond((B) value);
      } else if (index == 2) {
         return AnyOfThree.withThird((C) value);
      } else { // index == 3
         return function.apply((D) value);
      }
   }

   public AnyOfFour<A, B, C, D> flatMapFirst(Function<? super A, AnyOfFour<A, B, C, D>> function) {
      return index == 0 ? requireNonNull(function.apply((A) value)) : this;
   }
   
   public AnyOfFour<A, B, C, D> flatMapSecond(Function<? super B, AnyOfFour<A, B, C, D>> function) {
      return index == 1 ? requireNonNull(function.apply((B) value)) : this;
   }
   
   public AnyOfFour<A, B, C, D> flatMapThird(Function<? super C, AnyOfFour<A, B, C, D>> function) {
      return index == 2 ? requireNonNull(function.apply((C) value)) : this;
   }
   
   public AnyOfFour<A, B, C, D> flatMapFourth(Function<? super D, AnyOfFour<A, B, C, D>> function) {
      return index == 3 ? requireNonNull(function.apply((D) value)) : this;
   }
   
   @Override
   public <R> R visit(VisitorOfFour<? super A, ? super B, ? super C, ? super D, R> visitor) {
      if (index == 0) {
         return visitor.visitFirst((A) value);
      } else if (index == 1) {
         return visitor.visitSecond((B) value);
      } else if (index == 2) {
         return visitor.visitThird((C) value);
      } else { // index == 3
         return visitor.visitFourth((D) value);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof AnyOfFour)) {
         return false;
      }
      AnyOfFour<?, ?, ?, ?> other = (AnyOfFour<?, ?, ?, ?>) o;
      return index == other.index && Objects.equals(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return index ^ Objects.hashCode(value); 
   }
   
   @Override
   public String toString() {
      if (index == 0) {
         return "AnyOfFour.first[" + value + "]";
      } else if (index == 1) {
         return "AnyOfFour.second[" + value + "]";
      } else if (index == 2) {
         return "AnyOfFour.third[" + value + "]";
      } else { // index == 3
         return "AnyOfFour.fourth[" + value + "]";
      }
   }
}
