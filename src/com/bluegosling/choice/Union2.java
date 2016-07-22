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
//methods that create instances: if index == 0 then value is an A, otherwise it's a B.
@SuppressWarnings("unchecked")
public final class Union2<A, B> implements Choice.OfTwo<A, B>, Serializable {
   private static final long serialVersionUID = 3078358490669146744L;
   
   private static final Union2<?, ?> EMPTY = withFirst(null);
   
   private final Object value;
   private final int index;
   
   private Union2(Object value, int index) {
      assert index >= 0 && index < 2;
      this.value = value;
      this.index = index;
   }

   public static <A, B> Union2<A, B> withFirst(A a) {
      return new Union2<>(a, 0);
   }
   
   public static <A, B> Union2<A, B> withSecond(B b) {
      return new Union2<>(b, 1);
   }

   public static <A, B> Union2<A, B> of(A a, B b) {
      if ((a == null) == (b == null)) {
         // both are null or both are non-null
         throw new IllegalArgumentException("Exactly one argument must be non-null");
      }
      return a != null ? new Union2<>(a, 0) : new Union2<>(b, 1);
   }
   
   public static <A, B> Union2<A, B> firstOf(A a, B b) {
      if (a != null) {
         return new Union2<>(a, 0);
      } else if (b != null) {
         return new Union2<>(b, 1);
      } else {
         return (Union2<A, B>) EMPTY;
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
   
   @Override
   public <T> Union2<T, B> mapFirst(Function<? super A, ? extends T> function) {
      return index == 0 ? withFirst(function.apply((A) value)) : (Union2<T, B>) this;
   }
   
   @Override
   public <T> Union2<A, T> mapSecond(Function<? super B, ? extends T> function) {
      return index == 1 ? withSecond(function.apply((B) value)) : (Union2<A, T>) this;
   }
   
   @Override
   public <C> Union3<C, A, B> expandFirst() {
      return index == 0 ? Union3.withSecond((A) value) : Union3.withThird((B) value);
   }
   
   @Override
   public <C> Union3<A, C, B> expandSecond() {
      return index == 0 ? Union3.withFirst((A) value) : Union3.withThird((B) value);
   }
   
   @Override
   public <C> Union3<A, B, C> expandThird() {
      return index == 0 ? Union3.withFirst((A) value) : Union3.withSecond((B) value);
   }
   
   public Union2<B, A> swap() {
      return new Union2<>(value, 1 - index);
   }
   
   public Union2<A, B> exchangeFirst(Function<? super A, ? extends B> function) {
      return index == 0 ? withSecond(function.apply((A) value)) : this;
   }

   public Union2<A, B> exchangeSecond(Function<? super B, ? extends A> function) {
      return index == 1 ? withFirst(function.apply((B) value)) : this;
   }

   public B contractFirst(Function<? super A, ? extends B> function) {
      return index == 0 ? function.apply((A) value) : (B) value;
   }
   
   public A contractSecond(Function<? super B, ? extends A> function) {
      return index == 1 ? function.apply((B) value) : (A) value;
   }

   public Union2<A, B> flatMapFirst(Function<? super A, Union2<A, B>> function) {
      return index == 0 ? requireNonNull(function.apply((A) value)) : this;
   }

   public Union2<A, B> flatMapSecond(Function<? super B, Union2<A, B>> function) {
      return index == 1 ? requireNonNull(function.apply((B) value)) : this;
   }

   @Override
   public <R> R visit(VisitorOfTwo<? super A, ? super B, R> visitor) {
      return index == 0 ? visitor.visitFirst((A) value) : visitor.visitSecond((B) value);
   }
   
   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Union2)) {
         return false;
      }
      Union2<?, ?> other = (Union2<?, ?>) o;
      return index == other.index && Objects.equals(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return index ^ Objects.hashCode(value); 
   }
   
   @Override
   public String toString() {
      return index == 0
            ? "Union2.first[" + value + "]"
            : "Union2.second[" + value + "]";
   }
}
