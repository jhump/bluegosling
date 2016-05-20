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
//otherwise it's a C.
@SuppressWarnings("unchecked")
public final class Union3<A, B, C> implements Choice.OfThree<A, B, C>, Serializable {
   private static final long serialVersionUID = 3859951374248326999L;

   private final Object value;
   private final int index;
   
   private Union3(Object value, int index) {
      assert index >= 0 && index < 3;
      this.value = value;
      this.index = index;
   }

   public static <A, B, C> Union3<A, B, C> withFirst(A a) {
      return new Union3<>(a, 0);
   }
   
   public static <A, B, C> Union3<A, B, C> withSecond(B b) {
      return new Union3<>(b, 1);
   }

   public static <A, B, C> Union3<A, B, C> withThird(C c) {
      return new Union3<>(c, 2);
   }

   public static <A, B, C> Union3<A, B, C> of(A a, B b, C c) {
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
      if (count > 1) {
         throw new IllegalArgumentException("Only one argument can be non-null");
      }
      if (a != null || count == 0) {
         return new Union3<>(a, 0);
      } else if (b != null) {
         return new Union3<>(b, 1);
      } else { // c != null
         return new Union3<>(c, 2);
      }
   }
   
   public static <A, B, C> Union3<A, B, C> firstOf(A a, B b, C c) {
      if (a != null) {
         return new Union3<>(a, 0);
      } else if (b != null) {
         return new Union3<>(b, 1);
      } else {
         return new Union3<>(c, 2);
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
   public <T> Union3<T, B, C> mapFirst(Function<? super A, ? extends T> function) {
      return index == 0 ? withFirst(function.apply((A) value)) : (Union3<T, B, C>) this;
   }
   
   @Override
   public <T> Union3<A, T, C> mapSecond(Function<? super B, ? extends T> function) {
      return index == 1 ? withSecond(function.apply((B) value)) : (Union3<A, T, C>) this;
   }

   @Override
   public <T> Union3<A, B, T> mapThird(Function<? super C, ? extends T> function) {
      return index == 2 ? withThird(function.apply((C) value)) : (Union3<A, B, T>) this;
   }

   @Override
   public <D> Union4<D, A, B, C> expandFirst() {
      if (index == 0) {
         return Union4.withSecond((A) value);
      } else if (index == 1) {
         return Union4.withThird((B) value);
      } else { // index == 2
         return Union4.withFourth((C) value);
      }
   }
   
   @Override
   public <D> Union4<A, D, B, C> expandSecond() {
      if (index == 0) {
         return Union4.withFirst((A) value);
      } else if (index == 1) {
         return Union4.withThird((B) value);
      } else { // index == 2
         return Union4.withFourth((C) value);
      }
   }
   
   @Override
   public <D> Union4<A, B, D, C> expandThird() {
      if (index == 0) {
         return Union4.withFirst((A) value);
      } else if (index == 1) {
         return Union4.withSecond((B) value);
      } else { // index == 2
         return Union4.withFourth((C) value);
      }
   }
   
   @Override
   public <D> Union4<A, B, C, D> expandFourth() {
      if (index == 0) {
         return Union4.withFirst((A) value);
      } else if (index == 1) {
         return Union4.withSecond((B) value);
      } else { // index == 2
         return Union4.withThird((C) value);
      }
   }
   
   public Union2<B, C> contractFirst(Function<? super A, Union2<B, C>> function) {
      if (index == 0) {
         return function.apply((A) value);
      } else if (index == 1) {
         return Union2.withFirst((B) value);
      } else { // index == 2
         return Union2.withSecond((C) value);
      }
   }

   public Union2<A, C> contractSecond(Function<? super B, Union2<A, C>> function) {
      if (index == 0) {
         return Union2.withFirst((A) value);
      } else if (index == 1) {
         return function.apply((B) value);
      } else { // index == 2
         return Union2.withSecond((C) value);
      }
   }

   public Union2<A, B> contractThird(Function<? super C, Union2<A, B>> function) {
      if (index == 0) {
         return Union2.withFirst((A) value);
      } else if (index == 1) {
         return Union2.withSecond((B) value);
      } else { // index == 2
         return function.apply((C) value);
      }
   }
   
   public Union3<A, B, C> flatMapFirst(Function<? super A, Union3<A, B, C>> function) {
      return index == 0 ? requireNonNull(function.apply((A) value)) : this;
   }
   
   public Union3<A, B, C> flatMapSecond(Function<? super B, Union3<A, B, C>> function) {
      return index == 1 ? requireNonNull(function.apply((B) value)) : this;
   }
   
   public Union3<A, B, C> flatMapThird(Function<? super C, Union3<A, B, C>> function) {
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
      if (!(o instanceof Union3)) {
         return false;
      }
      Union3<?, ?, ?> other = (Union3<?, ?, ?>) o;
      return index == other.index && Objects.equals(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return index ^ Objects.hashCode(value); 
   }
   
   @Override
   public String toString() {
      if (index == 0) {
         return "Union3.first[" + value + "]";
      } else if (index == 1) {
         return "Union3.second[" + value + "]";
      } else { // index == 2
         return "Union3.third[" + value + "]";
      }
   }
}
