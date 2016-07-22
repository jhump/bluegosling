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
public final class AnyOfFive<A, B, C, D, E> implements Choice.OfFive<A, B, C, D, E>, Serializable {
   private static final long serialVersionUID = -1273837947264890128L;
   
   private final Object value;
   private final int index;
   
   private AnyOfFive(Object value, int index) {
      assert index >= 0 && index < 5;
      assert value != null;
      this.value = value;
      this.index = index;
   }

   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withFirst(A a) {
      return new AnyOfFive<>(requireNonNull(a), 0);
   }
   
   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withSecond(B b) {
      return new AnyOfFive<>(requireNonNull(b), 1);
   }

   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withThird(C c) {
      return new AnyOfFive<>(requireNonNull(c), 2);
   }

   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withFourth(D d) {
      return new AnyOfFive<>(requireNonNull(d), 3);
   }

   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> withFifth(E e) {
      return new AnyOfFive<>(requireNonNull(e), 4);
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
         return new AnyOfFive<>(a, 0);
      } else if (b != null) {
         return new AnyOfFive<>(b, 1);
      } else if (c != null) {
         return new AnyOfFive<>(c, 2);
      } else if (d != null) {
         return new AnyOfFive<>(d, 3);
      } else { // e != null
         return new AnyOfFive<>(e, 4);
      }
   }
   
   public static <A, B, C, D, E> AnyOfFive<A, B, C, D, E> firstOf(A a, B b, C c, D d, E e) {
      if (a != null) {
         return new AnyOfFive<>(a, 0);
      } else if (b != null) {
         return new AnyOfFive<>(b, 1);
      } else if (c != null) {
         return new AnyOfFive<>(c, 2);
      } else if (d != null) {
         return new AnyOfFive<>(d, 3);
      } else if (e != null) {
         return new AnyOfFive<>(e, 4);
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
   public boolean hasFifth() {
      return index == 4;
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
   public E getFifth() {
      if (index != 4) {
         throw new NoSuchElementException();
      }
      return (E) value;
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
   public Possible<E> tryFifth() {
      return index == 4 ? Reference.setTo((E) value) : Reference.unset();
   }
   
   @Override
   public <T> AnyOfFive<T, B, C, D, E> mapFirst(Function<? super A, ? extends T> function) {
      return index == 0 ? withFirst(function.apply((A) value)) : (AnyOfFive<T, B, C, D, E>) this;
   }
   
   @Override
   public <T> AnyOfFive<A, T, C, D, E> mapSecond(Function<? super B, ? extends T> function) {
      return index == 1 ? withSecond(function.apply((B) value)) : (AnyOfFive<A, T, C, D, E>) this;
   }

   @Override
   public <T> AnyOfFive<A, B, T, D, E> mapThird(Function<? super C, ? extends T> function) {
      return index == 2 ? withThird(function.apply((C) value)) : (AnyOfFive<A, B, T, D, E>) this;
   }

   @Override
   public <T> AnyOfFive<A, B, C, T, E> mapFourth(Function<? super D, ? extends T> function) {
      return index == 3 ? withFourth(function.apply((D) value)) : (AnyOfFive<A, B, C, T, E>) this;
   }

   @Override
   public <T> AnyOfFive<A, B, C, D, T> mapFifth(Function<? super E, ? extends T> function) {
      return index == 4 ? withFifth(function.apply((E) value)) : (AnyOfFive<A, B, C, D, T>) this;
   }

   public AnyOfFour<B, C, D, E> contractFirst(Function<? super A, AnyOfFour<B, C, D, E>> function) {
      if (index == 0) {
         return function.apply((A) value);
      } else if (index == 1) {
         return AnyOfFour.withFirst((B) value);
      } else if (index == 2) {
         return AnyOfFour.withSecond((C) value);
      } else if (index == 3) {
         return AnyOfFour.withThird((D) value);
      } else { // index == 4
         return AnyOfFour.withFourth((E) value);
      }
   }

   public AnyOfFour<A, C, D, E> contractSecond(Function<? super B, AnyOfFour<A, C, D, E>> function) {
      if (index == 0) {
         return AnyOfFour.withFirst((A) value);
      } else if (index == 1) {
         return function.apply((B) value);
      } else if (index == 2) {
         return AnyOfFour.withSecond((C) value);
      } else if (index == 3) {
         return AnyOfFour.withThird((D) value);
      } else { // index == 4
         return AnyOfFour.withFourth((E) value);
      }
   }

   public AnyOfFour<A, B, D, E> contractThird(Function<? super C, AnyOfFour<A, B, D, E>> function) {
      if (index == 0) {
         return AnyOfFour.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFour.withSecond((B) value);
      } else if (index == 2) {
         return function.apply((C) value);
      } else if (index == 3) {
         return AnyOfFour.withThird((D) value);
      } else { // index == 4
         return AnyOfFour.withFourth((E) value);
      }
   }

   public AnyOfFour<A, B, C, E> contractFourth(Function<? super D, AnyOfFour<A, B, C, E>> function) {
      if (index == 0) {
         return AnyOfFour.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFour.withSecond((B) value);
      } else if (index == 2) {
         return AnyOfFour.withThird((C) value);
      } else if (index == 3) {
         return function.apply((D) value);
      } else { // index == 4
         return AnyOfFour.withFourth((E) value);
      }
   }

   public AnyOfFour<A, B, C, D> contractFifth(Function<? super E, AnyOfFour<A, B, C, D>> function) {
      if (index == 0) {
         return AnyOfFour.withFirst((A) value);
      } else if (index == 1) {
         return AnyOfFour.withSecond((B) value);
      } else if (index == 2) {
         return AnyOfFour.withThird((C) value);
      } else if (index == 3) {
         return AnyOfFour.withFourth((D) value);
      } else { // index == 4
         return function.apply((E) value);
      }
   }

   public AnyOfFive<A, B, C, D, E> flatMapFirst(Function<? super A, AnyOfFive<A, B, C, D, E>> function) {
      return index == 0 ? requireNonNull(function.apply((A) value)) : this;
   }

   public AnyOfFive<A, B, C, D, E> flatMapSecond(Function<? super B, AnyOfFive<A, B, C, D, E>> function) {
      return index == 1 ? requireNonNull(function.apply((B) value)) : this;
   }

   public AnyOfFive<A, B, C, D, E> flatMapThird(Function<? super C, AnyOfFive<A, B, C, D, E>> function) {
      return index == 2 ? requireNonNull(function.apply((C) value)) : this;
   }

   public AnyOfFive<A, B, C, D, E> flatMapFourth(Function<? super D, AnyOfFive<A, B, C, D, E>> function) {
      return index == 3 ? requireNonNull(function.apply((D) value)) : this;
   }

   public AnyOfFive<A, B, C, D, E> flatMapFifth(Function<? super E, AnyOfFive<A, B, C, D, E>> function) {
      return index == 4 ? requireNonNull(function.apply((E) value)) : this;
   }
   
   @Override
   public <R> R visit(VisitorOfFive<? super A, ? super B, ? super C, ? super D, ? super E, R> visitor) {
      if (index == 0) {
         return visitor.visitFirst((A) value);
      } else if (index == 1) {
         return visitor.visitSecond((B) value);
      } else if (index == 2) {
         return visitor.visitThird((C) value);
      } else if (index == 3) {
         return visitor.visitFourth((D) value);
      } else { // index == 4
         return visitor.visitFifth((E) value);
      }
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof AnyOfFive)) {
         return false;
      }
      AnyOfFive<?, ?, ?, ?, ?> other = (AnyOfFive<?, ?, ?, ?, ?>) o;
      return index == other.index && Objects.equals(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return index ^ Objects.hashCode(value); 
   }
   
   @Override
   public String toString() {
      if (index == 0) {
         return "AnyOfFive.first[" + value + "]";
      } else if (index == 1) {
         return "AnyOfFive.second[" + value + "]";
      } else if (index == 2) {
         return "AnyOfFive.third[" + value + "]";
      } else if (index == 3) {
         return "AnyOfFive.fourth[" + value + "]";
      } else { // index == 4
         return "AnyOfFive.fifth[" + value + "]";
      }
   }
}
