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
//TODO: tests
@ValueType
//For efficiency, we store the value in a single Object field and then must cast to type variable
//A or B (which is an unchecked cast). This is safe due to the invariant ensured by the factory
//methods that create instances: if index == 0 then value is an A, if index == 1 then it's a B, 
//and so on.
@SuppressWarnings("unchecked")
public final class Union5<A, B, C, D, E> implements Choice.OfFive<A, B, C, D, E>, Serializable {
   private static final long serialVersionUID = -1914510684839843360L;

   private static final Union5<?, ?, ?, ?, ?> EMPTY = withFirst(null);

   private final Object value;
   private final int index;
   
   private Union5(Object value, int index) {
      assert index >= 0 && index < 5;
      this.value = value;
      this.index = index;
   }
   
   public static <A, B, C, D, E> Union5<A, B, C, D, E> withFirst(A a) {
      return new Union5<>(a, 0);
   }
   
   public static <A, B, C, D, E> Union5<A, B, C, D, E> withSecond(B b) {
      return new Union5<>(b, 1);
   }

   public static <A, B, C, D, E> Union5<A, B, C, D, E> withThird(C c) {
      return new Union5<>(c, 2);
   }

   public static <A, B, C, D, E> Union5<A, B, C, D, E> withFourth(D d) {
      return new Union5<>(d, 3);
   }

   public static <A, B, C, D, E> Union5<A, B, C, D, E> withFifth(E e) {
      return new Union5<>(e, 4);
   }

   public static <A, B, C, D, E> Union5<A, B, C, D, E> of(A a, B b, C c, D d, E e) {
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
         throw new IllegalArgumentException("Exactly one argument should be non-null");
      }
      if (a != null) {
         return new Union5<>(a, 0);
      } else if (b != null) {
         return new Union5<>(b, 1);
      } else if (c != null) {
         return new Union5<>(c, 2);
      } else if (d != null) {
         return new Union5<>(d, 3);
      } else { // e != null
         return new Union5<>(e, 4);
      }
   }
   
   public static <A, B, C, D, E> Union5<A, B, C, D, E> firstOf(A a, B b, C c, D d, E e) {
      if (a != null) {
         return new Union5<>(a, 0);
      } else if (b != null) {
         return new Union5<>(b, 1);
      } else if (c != null) {
         return new Union5<>(c, 2);
      } else if (d != null) {
         return new Union5<>(d, 3);
      } else if (e != null) {
         return new Union5<>(e, 4);
      } else {
         return (Union5<A, B, C, D, E>) EMPTY;
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
   public <T> Union5<T, B, C, D, E> mapFirst(Function<? super A, ? extends T> function) {
      return index == 0 ? withFirst(function.apply((A) value)) : (Union5<T, B, C, D, E>) this;
   }
   
   @Override
   public <T> Union5<A, T, C, D, E> mapSecond(Function<? super B, ? extends T> function) {
      return index == 1 ? withSecond(function.apply((B) value)) : (Union5<A, T, C, D, E>) this;
   }

   @Override
   public <T> Union5<A, B, T, D, E> mapThird(Function<? super C, ? extends T> function) {
      return index == 2 ? withThird(function.apply((C) value)) : (Union5<A, B, T, D, E>) this;
   }

   @Override
   public <T> Union5<A, B, C, T, E> mapFourth(Function<? super D, ? extends T> function) {
      return index == 3 ? withFourth(function.apply((D) value)) : (Union5<A, B, C, T, E>) this;
   }

   @Override
   public <T> Union5<A, B, C, D, T> mapFifth(Function<? super E, ? extends T> function) {
      return index == 4 ? withFifth(function.apply((E) value)) : (Union5<A, B, C, D, T>) this;
   }

   public Union4<B, C, D, E> contractFirst(Function<? super A, Union4<B, C, D, E>> function) {
      if (index == 0) {
         return function.apply((A) value);
      } else if (index == 1) {
         return Union4.withFirst((B) value);
      } else if (index == 2) {
         return Union4.withSecond((C) value);
      } else if (index == 3) {
         return Union4.withThird((D) value);
      } else { // index == 4
         return Union4.withFourth((E) value);
      }
   }

   public Union4<A, C, D, E> contractSecond(Function<? super B, Union4<A, C, D, E>> function) {
      if (index == 0) {
         return Union4.withFirst((A) value);
      } else if (index == 1) {
         return function.apply((B) value);
      } else if (index == 2) {
         return Union4.withSecond((C) value);
      } else if (index == 3) {
         return Union4.withThird((D) value);
      } else { // index == 4
         return Union4.withFourth((E) value);
      }
   }

   public Union4<A, B, D, E> contractThird(Function<? super C, Union4<A, B, D, E>> function) {
      if (index == 0) {
         return Union4.withFirst((A) value);
      } else if (index == 1) {
         return Union4.withSecond((B) value);
      } else if (index == 2) {
         return function.apply((C) value);
      } else if (index == 3) {
         return Union4.withThird((D) value);
      } else { // index == 4
         return Union4.withFourth((E) value);
      }
   }

   public Union4<A, B, C, E> contractFourth(Function<? super D, Union4<A, B, C, E>> function) {
      if (index == 0) {
         return Union4.withFirst((A) value);
      } else if (index == 1) {
         return Union4.withSecond((B) value);
      } else if (index == 2) {
         return Union4.withThird((C) value);
      } else if (index == 3) {
         return function.apply((D) value);
      } else { // index == 4
         return Union4.withFourth((E) value);
      }
   }

   public Union4<A, B, C, D> contractFifth(Function<? super E, Union4<A, B, C, D>> function) {
      if (index == 0) {
         return Union4.withFirst((A) value);
      } else if (index == 1) {
         return Union4.withSecond((B) value);
      } else if (index == 2) {
         return Union4.withThird((C) value);
      } else if (index == 3) {
         return Union4.withFourth((D) value);
      } else { // index == 4
         return function.apply((E) value);
      }
   }

   public Union5<A, B, C, D, E> flatMapFirst(Function<? super A, Union5<A, B, C, D, E>> function) {
      return index == 0 ? requireNonNull(function.apply((A) value)) : this;
   }

   public Union5<A, B, C, D, E> flatMapSecond(Function<? super B, Union5<A, B, C, D, E>> function) {
      return index == 1 ? requireNonNull(function.apply((B) value)) : this;
   }

   public Union5<A, B, C, D, E> flatMapThird(Function<? super C, Union5<A, B, C, D, E>> function) {
      return index == 2 ? requireNonNull(function.apply((C) value)) : this;
   }

   public Union5<A, B, C, D, E> flatMapFourth(Function<? super D, Union5<A, B, C, D, E>> function) {
      return index == 3 ? requireNonNull(function.apply((D) value)) : this;
   }

   public Union5<A, B, C, D, E> flatMapFifth(Function<? super E, Union5<A, B, C, D, E>> function) {
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
      if (!(o instanceof Union5)) {
         return false;
      }
      Union5<?, ?, ?, ?, ?> other = (Union5<?, ?, ?, ?, ?>) o;
      return index == other.index && Objects.equals(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return index ^ Objects.hashCode(value); 
   }
   
   @Override
   public String toString() {
      if (index == 0) {
         return "Union5.first[" + value + "]";
      } else if (index == 1) {
         return "Union5.second[" + value + "]";
      } else if (index == 2) {
         return "Union5.third[" + value + "]";
      } else if (index == 3) {
         return "Union5.fourth[" + value + "]";
      } else { // index == 4
         return "Union5.fifth[" + value + "]";
      }
   }
}
