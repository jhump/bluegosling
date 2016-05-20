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
public final class Union4<A, B, C, D> implements Choice.OfFour<A, B, C, D>, Serializable {
   private static final long serialVersionUID = -8557684884722232629L;

   private final Object value;
   private final int index;
   
   private Union4(Object value, int index) {
      assert index >= 0 && index < 4;
      this.value = value;
      this.index = index;
   }

   public static <A, B, C, D> Union4<A, B, C, D> withFirst(A a) {
      return new Union4<>(a, 0);
   }
   
   public static <A, B, C, D> Union4<A, B, C, D> withSecond(B b) {
      return new Union4<>(b, 1);
   }

   public static <A, B, C, D> Union4<A, B, C, D> withThird(C c) {
      return new Union4<>(c, 2);
   }

   public static <A, B, C, D> Union4<A, B, C, D> withFourth(D d) {
      return new Union4<>(d, 3);
   }

   public static <A, B, C, D> Union4<A, B, C, D> of(A a, B b, C c, D d) {
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
      if (count > 1) {
         throw new IllegalArgumentException("Only one argument can be non-null");
      }
      if (a != null || count == 0) {
         return new Union4<>(a, 0);
      } else if (b != null) {
         return new Union4<>(b, 1);
      } else if (c != null) {
         return new Union4<>(c, 2);
      } else { // d != null
         return new Union4<>(d, 3);
      }
   }
   
   public static <A, B, C, D> Union4<A, B, C, D> firstOf(A a, B b, C c, D d) {
      if (a != null) {
         return new Union4<>(a, 0);
      } else if (b != null) {
         return new Union4<>(b, 1);
      } else if (c != null) {
         return new Union4<>(c, 2);
      } else {
         return new Union4<>(d, 3);
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
   public <T> Union4<T, B, C, D> mapFirst(Function<? super A, ? extends T> function) {
      return index == 0 ? withFirst(function.apply((A) value)) : (Union4<T, B, C, D>) this;
   }
   
   @Override
   public <T> Union4<A, T, C, D> mapSecond(Function<? super B, ? extends T> function) {
      return index == 1 ? withSecond(function.apply((B) value)) : (Union4<A, T, C, D>) this;
   }

   @Override
   public <T> Union4<A, B, T, D> mapThird(Function<? super C, ? extends T> function) {
      return index == 2 ? withThird(function.apply((C) value)) : (Union4<A, B, T, D>) this;
   }

   @Override
   public <T> Union4<A, B, C, T> mapFourth(Function<? super D, ? extends T> function) {
      return index == 3 ? withFourth(function.apply((D) value)) : (Union4<A, B, C, T>) this;
   }
   
   @Override
   public <E> Union5<E, A, B, C, D> expandFirst() {
      if (index == 0) {
         return Union5.withSecond((A) value);
      } else if (index == 1) {
         return Union5.withThird((B) value);
      } else if (index == 2) {
         return Union5.withFourth((C) value);
      } else { // index == 3
         return Union5.withFifth((D) value);
      }
   }
   
   @Override
   public <E> Union5<A, E, B, C, D> expandSecond() {
      if (index == 0) {
         return Union5.withFirst((A) value);
      } else if (index == 1) {
         return Union5.withThird((B) value);
      } else if (index == 2) {
         return Union5.withFourth((C) value);
      } else { // index == 3
         return Union5.withFifth((D) value);
      }
   }
   
   @Override
   public <E> Union5<A, B, E, C, D> expandThird() {
      if (index == 0) {
         return Union5.withFirst((A) value);
      } else if (index == 1) {
         return Union5.withSecond((B) value);
      } else if (index == 2) {
         return Union5.withFourth((C) value);
      } else { // index == 3
         return Union5.withFifth((D) value);
      }
   }
   
   @Override
   public <E> Union5<A, B, C, E, D> expandFourth() {
      if (index == 0) {
         return Union5.withFirst((A) value);
      } else if (index == 1) {
         return Union5.withSecond((B) value);
      } else if (index == 2) {
         return Union5.withThird((C) value);
      } else { // index == 3
         return Union5.withFifth((D) value);
      }
   }
   
   @Override
   public <E> Union5<A, B, C, D, E> expandFifth() {
      if (index == 0) {
         return Union5.withFirst((A) value);
      } else if (index == 1) {
         return Union5.withSecond((B) value);
      } else if (index == 2) {
         return Union5.withThird((C) value);
      } else { // index == 3
         return Union5.withFourth((D) value);
      }
   }
   
   public Union3<B, C, D> contractFirst(Function<? super A, Union3<B, C, D>> function) {
      if (index == 0) {
         return function.apply((A) value);
      } else if (index == 1) {
         return Union3.withFirst((B) value);
      } else if (index == 2) {
         return Union3.withSecond((C) value);
      } else { // index == 3
         return Union3.withThird((D) value);
      }
   }

   public Union3<A, C, D> contractSecond(Function<? super B, Union3<A, C, D>> function) {
      if (index == 0) {
         return Union3.withFirst((A) value);
      } else if (index == 1) {
         return function.apply((B) value);
      } else if (index == 2) {
         return Union3.withSecond((C) value);
      } else { // index == 3
         return Union3.withThird((D) value);
      }
   }

   public Union3<A, B, D> contractThird(Function<? super C, Union3<A, B, D>> function) {
      if (index == 0) {
         return Union3.withFirst((A) value);
      } else if (index == 1) {
         return Union3.withSecond((B) value);
      } else if (index == 2) {
         return function.apply((C) value);
      } else { // index == 3
         return Union3.withThird((D) value);
      }
   }

   public Union3<A, B, C> contractFourth(Function<? super D, Union3<A, B, C>> function) {
      if (index == 0) {
         return Union3.withFirst((A) value);
      } else if (index == 1) {
         return Union3.withSecond((B) value);
      } else if (index == 2) {
         return Union3.withThird((C) value);
      } else { // index == 3
         return function.apply((D) value);
      }
   }
   
   public Union4<A, B, C, D> flatMapFirst(Function<? super A, Union4<A, B, C, D>> function) {
      return index == 0 ? requireNonNull(function.apply((A) value)) : this;
   }
   
   public Union4<A, B, C, D> flatMapSecond(Function<? super B, Union4<A, B, C, D>> function) {
      return index == 1 ? requireNonNull(function.apply((B) value)) : this;
   }
   
   public Union4<A, B, C, D> flatMapThird(Function<? super C, Union4<A, B, C, D>> function) {
      return index == 2 ? requireNonNull(function.apply((C) value)) : this;
   }
   
   public Union4<A, B, C, D> flatMapFourth(Function<? super D, Union4<A, B, C, D>> function) {
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
      if (!(o instanceof Union4)) {
         return false;
      }
      Union4<?, ?, ?, ?> other = (Union4<?, ?, ?, ?>) o;
      return index == other.index && Objects.equals(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return index ^ Objects.hashCode(value); 
   }
   
   @Override
   public String toString() {
      if (index == 0) {
         return "Union4.first[" + value + "]";
      } else if (index == 1) {
         return "Union4.second[" + value + "]";
      } else if (index == 2) {
         return "Union4.third[" + value + "]";
      } else { // index == 3
         return "Union4.fourth[" + value + "]";
      }
   }
}
