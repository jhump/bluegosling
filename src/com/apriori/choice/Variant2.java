package com.apriori.choice;

import com.apriori.possible.Possible;
import com.apriori.possible.Reference;

import java.io.Serializable;
import java.util.function.Function;

//TODO: javadoc
//TODO: tests
public abstract class Variant2<A, B> implements Choice.OfTwo<A, B> {
   
   public static <A, B> Variant2<A, B> withFirst(A a) {
      return new First<A, B>(a);
   }
   
   public static <A, B> Variant2<A, B> withSecond(B b) {
      return new Second<A, B>(b);
   }

   public static <A, B> Variant2<A, B> of(A a, B b) {
      if (a != null && b != null) {
         throw new IllegalArgumentException("Only one argument can be non-null");
      }
      if (a != null || b == null) {
         return new First<A, B>(a);
      } else {
         return new Second<A, B>(b);
      }
   }
   
   public static <A, B> Variant2<A, B> firstOf(A a, B b) {
      if (a != null) {
         return new First<A, B>(a);
      } else {
         return new Second<A, B>(b);
      }
   }
      
   Variant2() {
   }
      
   @Override
   public abstract <T> Variant2<T, B> mapFirst(Function<? super A, ? extends T> function);
   
   @Override
   public abstract <T> Variant2<A, T> mapSecond(Function<? super B, ? extends T> function);
   
   @Override
   public abstract <C> Variant3<C, A, B> expandFirst();
   
   @Override
   public abstract <C> Variant3<A, C, B> expandSecond();
   
   @Override
   public abstract <C> Variant3<A, B, C> expandThird();
   
   public abstract B contractFirst(Function<? super A, ? extends B> function);
   
   public abstract A contractSecond(Function<? super B, ? extends A> function);

   public abstract Variant2<B, A> swap();
   
   public abstract Variant2<A, B> exchangeFirst(Function<? super A, ? extends B> function);

   public abstract Variant2<A, B> exchangeSecond(Function<? super B, ? extends A> function);
   
   public abstract Variant2<A, B> flatMapFirst(Function<? super A, Variant2<A, B>> function);

   public abstract Variant2<A, B> flatMapSecond(Function<? super B, Variant2<A, B>> function);
   
   private static class First<A, B> extends Variant2<A, B> implements Serializable {
      private static final long serialVersionUID = 7357985018452714303L;
      
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
      public <T> Variant2<T, B> mapFirst(Function<? super A, ? extends T> function) {
         return Variant2.<T, B>withFirst(function.apply(a));
      }
   
      @Override
      public <T> Variant2<A, T> mapSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant2<A, T> ret = (Variant2<A, T>) this;
         return ret;
      }

      @Override
      public <R> R visit(VisitorOfTwo<? super A, ? super B, R> visitor) {
         return visitor.visitFirst(a);
      }

      @Override
      public <C> Variant3<C, A, B> expandFirst() {
         return Variant3.withSecond(a);
      }
      
      @Override
      public <C> Variant3<A, C, B> expandSecond() {
         return Variant3.withFirst(a);
      }
      
      @Override
      public <C> Variant3<A, B, C> expandThird() {
         return Variant3.withFirst(a);
      }
      
      @Override
      public Variant2<B, A> swap() {
         return new Second<B, A>(a);
      }
      
      @Override
      public Variant2<A, B> exchangeFirst(Function<? super A, ? extends B> function) {
         return new Second<A, B>(function.apply(a));
      }

      @Override
      public Variant2<A, B> exchangeSecond(Function<? super B, ? extends A> function) {
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
      public Variant2<A, B> flatMapFirst(Function<? super A, Variant2<A, B>> function) {
         return function.apply(a);
      }

      @Override
      public Variant2<A, B> flatMapSecond(Function<? super B, Variant2<A, B>> function) {
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
         return "Variant2.First: " + a;
      }
   }

   private static class Second<A, B> extends Variant2<A, B> implements Serializable {
      private static final long serialVersionUID = -7388129812997474159L;
      
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
      public <T> Variant2<T, B> mapFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant2<T, B> ret = (Variant2<T, B>) this;
         return ret;
      }
   
      @Override
      public <T> Variant2<A, T> mapSecond(Function<? super B, ? extends T> function) {
         return Variant2.<A, T>withSecond(function.apply(b));
      }

      @Override
      public <R> R visit(VisitorOfTwo<? super A, ? super B, R> visitor) {
         return visitor.visitSecond(b);
      }

      @Override
      public <C> Variant3<C, A, B> expandFirst() {
         return Variant3.withThird(b);
      }
      
      @Override
      public <C> Variant3<A, C, B> expandSecond() {
         return Variant3.withThird(b);
      }
      
      @Override
      public <C> Variant3<A, B, C> expandThird() {
         return Variant3.withSecond(b);
      }
      
      @Override
      public Variant2<B, A> swap() {
         return new First<B, A>(b);
      }
      
      @Override
      public Variant2<A, B> exchangeFirst(Function<? super A, ? extends B> function) {
         return this;
      }

      @Override
      public Variant2<A, B> exchangeSecond(Function<? super B, ? extends A> function) {
         return new First<A, B>(function.apply(b));
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
      public Variant2<A, B> flatMapFirst(Function<? super A, Variant2<A, B>> function) {
         return this;
      }

      @Override
      public Variant2<A, B> flatMapSecond(Function<? super B, Variant2<A, B>> function) {
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
         return "Variant2.Second: " + b;
      }
   }
}
