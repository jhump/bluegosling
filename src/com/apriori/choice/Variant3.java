package com.apriori.choice;

import com.apriori.possible.Possible;
import com.apriori.possible.Reference;

import java.io.Serializable;
import java.util.function.Function;

//TODO: javadoc
//TODO: tests
public abstract class Variant3<A, B, C> implements Choice.OfThree<A, B, C> {
   
   public static <A, B, C> Variant3<A, B, C> withFirst(A a) {
      return new First<A, B, C>(a);
   }
   
   public static <A, B, C> Variant3<A, B, C> withSecond(B b) {
      return new Second<A, B, C>(b);
   }

   public static <A, B, C> Variant3<A, B, C> withThird(C c) {
      return new Third<A, B, C>(c);
   }

   public static <A, B, C> Variant3<A, B, C> of(A a, B b, C c) {
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
         return new First<A, B, C>(a);
      } else if (b != null) {
         return new Second<A, B, C>(b);
      } else { // c != null
         return new Third<A, B, C>(c);
      }
   }
   
   public static <A, B, C> Variant3<A, B, C> firstOf(A a, B b, C c) {
      if (a != null) {
         return new First<A, B, C>(a);
      } else if (b != null) {
         return new Second<A, B, C>(b);
      } else {
         return new Third<A, B, C>(c);
      }
   }
   
   Variant3() {
   }
      
   @Override
   public abstract <T> Variant3<T, B, C> transformFirst(Function<? super A, ? extends T> function);
   
   @Override
   public abstract <T> Variant3<A, T, C> transformSecond(Function<? super B, ? extends T> function);

   @Override
   public abstract <T> Variant3<A, B, T> transformThird(Function<? super C, ? extends T> function);

   @Override
   public abstract <D> Variant4<D, A, B, C> expandFirst();
   
   @Override
   public abstract <D> Variant4<A, D, B, C> expandSecond();
   
   @Override
   public abstract <D> Variant4<A, B, D, C> expandThird();
   
   @Override
   public abstract <D> Variant4<A, B, C, D> expandFourth();
   
   public abstract Variant2<B, C> contractFirst(Function<? super A, Variant2<B, C>> function);

   public abstract Variant2<A, C> contractSecond(Function<? super B, Variant2<A, C>> function);

   public abstract Variant2<A, B> contractThird(Function<? super C, Variant2<A, B>> function);
   
   private static class First<A, B, C> extends Variant3<A, B, C> implements Serializable {
      private static final long serialVersionUID = 4838795688805648189L;

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
      public boolean hasThird() {
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
      public C getThird() {
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
      public Possible<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public <T> Variant3<T, B, C> transformFirst(Function<? super A, ? extends T> function) {
         return Variant3.<T, B, C>withFirst(function.apply(a));
      }
   
      @Override
      public <T> Variant3<A, T, C> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant3<A, T, C> ret = (Variant3<A, T, C>) this;
         return ret;
      }

      @Override
      public <T> Variant3<A, B, T> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant3<A, B, T> ret = (Variant3<A, B, T>) this;
         return ret;
      }

      @Override
      public <R> R visit(VisitorOfThree<? super A, ? super B, ? super C, R> visitor) {
         return visitor.visitFirst(a);
      }

      @Override
      public <D> Variant4<D, A, B, C> expandFirst() {
         return Variant4.withSecond(a);
      }
      
      @Override
      public <D> Variant4<A, D, B, C> expandSecond() {
         return Variant4.withFirst(a);
      }
      
      @Override
      public <D> Variant4<A, B, D, C> expandThird() {
         return Variant4.withFirst(a);
      }
      
      @Override
      public <D> Variant4<A, B, C, D> expandFourth() {
         return Variant4.withFirst(a);
      }
      
      @Override
      public Variant2<B, C> contractFirst(Function<? super A, Variant2<B, C>> function) {
         Variant2<B, C> result = function.apply(a);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public Variant2<A, C> contractSecond(Function<? super B, Variant2<A, C>> function) {
         return Variant2.withFirst(a);
      }

      @Override
      public Variant2<A, B> contractThird(Function<? super C, Variant2<A, B>> function) {
         return Variant2.withFirst(a);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof First && a.equals(((First<?, ?, ?>) o).a);
      }
      
      @Override
      public int hashCode() {
         return First.class.hashCode() ^ a.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant3.First: " + a;
      }
   }

   private static class Second<A, B, C> extends Variant3<A, B, C> implements Serializable {
      private static final long serialVersionUID = -6220280815969591787L;

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
      public boolean hasThird() {
         return false;
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
      public C getThird() {
         throw new IllegalStateException();
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
      public Possible<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public <T> Variant3<T, B, C> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant3<T, B, C> ret = (Variant3<T, B, C>) this;
         return ret;
      }
   
      @Override
      public <T> Variant3<A, T, C> transformSecond(Function<? super B, ? extends T> function) {
         return Variant3.<A, T, C>withSecond(function.apply(b));
      }
   
      @Override
      public <T> Variant3<A, B, T> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant3<A, B, T> ret = (Variant3<A, B, T>) this;
         return ret;
      }

      @Override
      public <R> R visit(VisitorOfThree<? super A, ? super B, ? super C, R> visitor) {
         return visitor.visitSecond(b);
      }

      @Override
      public <D> Variant4<D, A, B, C> expandFirst() {
         return Variant4.withThird(b);
      }
      
      @Override
      public <D> Variant4<A, D, B, C> expandSecond() {
         return Variant4.withThird(b);
      }
      
      @Override
      public <D> Variant4<A, B, D, C> expandThird() {
         return Variant4.withSecond(b);
      }
      
      @Override
      public <D> Variant4<A, B, C, D> expandFourth() {
         return Variant4.withSecond(b);
      }
      
      @Override
      public Variant2<B, C> contractFirst(Function<? super A, Variant2<B, C>> function) {
         return Variant2.withFirst(b);
      }

      @Override
      public Variant2<A, C> contractSecond(Function<? super B, Variant2<A, C>> function) {
         Variant2<A, C> result = function.apply(b);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }

      @Override
      public Variant2<A, B> contractThird(Function<? super C, Variant2<A, B>> function) {
         return Variant2.withSecond(b);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Second && b.equals(((Second<?, ?, ?>) o).b);
      }
      
      @Override
      public int hashCode() {
         return Second.class.hashCode() ^ b.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant3.Second: " + b;
      }
   }

   private static class Third<A, B, C> extends Variant3<A, B, C> implements Serializable {
      private static final long serialVersionUID = -3300981589931061342L;

      private final C c;
      
      Third(C c) {
         this.c = c;
      }
      
      @Override
      public boolean hasFirst() {
         return false;
      }
   
      @Override
      public boolean hasSecond() {
         return false;
      }

      @Override
      public boolean hasThird() {
         return true;
      }

      @Override
      public Object get() {
         return c;
      }
      
      @Override
      public A getFirst() {
         throw new IllegalStateException();
      }
   
      @Override
      public B getSecond() {
         throw new IllegalStateException();
      }
   
      @Override
      public C getThird() {
         return c;
      }
   
      @Override
      public Possible<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Possible<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Possible<C> tryThird() {
         return Reference.setTo(c);
      }
   
      @Override
      public <T> Variant3<T, B, C> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant3<T, B, C> ret = (Variant3<T, B, C>) this;
         return ret;
      }
   
      @Override
      public <T> Variant3<A, T, C> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant3<A, T, C> ret = (Variant3<A, T, C>) this;
         return ret;
      }
   
      @Override
      public <T> Variant3<A, B, T> transformThird(Function<? super C, ? extends T> function) {
         return Variant3.<A, B, T>withThird(function.apply(c));
      }

      @Override
      public <R> R visit(VisitorOfThree<? super A, ? super B, ? super C, R> visitor) {
         return visitor.visitThird(c);
      }

      @Override
      public <D> Variant4<D, A, B, C> expandFirst() {
         return Variant4.withFourth(c);
      }
      
      @Override
      public <D> Variant4<A, D, B, C> expandSecond() {
         return Variant4.withFourth(c);
      }
      
      @Override
      public <D> Variant4<A, B, D, C> expandThird() {
         return Variant4.withFourth(c);
      }
      
      @Override
      public <D> Variant4<A, B, C, D> expandFourth() {
         return Variant4.withThird(c);
      }
      
      @Override
      public Variant2<B, C> contractFirst(Function<? super A, Variant2<B, C>> function) {
         return Variant2.withSecond(c);
      }

      @Override
      public Variant2<A, C> contractSecond(Function<? super B, Variant2<A, C>> function) {
         return Variant2.withSecond(c);
      }

      @Override
      public Variant2<A, B> contractThird(Function<? super C, Variant2<A, B>> function) {
         Variant2<A, B> result = function.apply(c);
         if (result == null) {
            throw new NullPointerException();
         }
         return result;
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Third && c.equals(((Third<?, ?, ?>) o).c);
      }
      
      @Override
      public int hashCode() {
         return Third.class.hashCode() ^ c.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant3.Third: " + c;
      }
   }
}
