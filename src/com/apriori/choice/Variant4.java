package com.apriori.choice;

import com.apriori.choice.Choices.Choices4;
import com.apriori.choice.Choices.Visitor4;
import com.apriori.possible.Reference;
import com.apriori.util.Function;

import java.io.Serializable;

//TODO: javadoc
//TODO: tests
public abstract class Variant4<A, B, C, D> implements Choices4<A, B, C, D> {
   
   public static <A, B, C, D> Variant4<A, B, C, D> withFirst(A a) {
      return new First<A, B, C, D>(a);
   }
   
   public static <A, B, C, D> Variant4<A, B, C, D> withSecond(B b) {
      return new Second<A, B, C, D>(b);
   }

   public static <A, B, C, D> Variant4<A, B, C, D> withThird(C c) {
      return new Third<A, B, C, D>(c);
   }

   public static <A, B, C, D> Variant4<A, B, C, D> withFourth(D d) {
      return new Fourth<A, B, C, D>(d);
   }

   public static <A, B, C, D> Variant4<A, B, C, D> of(A a, B b, C c, D d) {
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
         return new First<A, B, C, D>(a);
      } else if (b != null) {
         return new Second<A, B, C, D>(b);
      } else if (c != null) {
         return new Third<A, B, C, D>(c);
      } else { // d != null
         return new Fourth<A, B, C, D>(d);
      }
   }
   
   public static <A, B, C, D> Variant4<A, B, C, D> firstOf(A a, B b, C c, D d) {
      if (a != null) {
         return new First<A, B, C, D>(a);
      } else if (b != null) {
         return new Second<A, B, C, D>(b);
      } else if (c != null) {
         return new Third<A, B, C, D>(c);
      } else  {
         return new Fourth<A, B, C, D>(d);
      }
   }
   
   Variant4() {
   }
      
   @Override
   public abstract Reference<A> tryFirst();

   @Override
   public abstract Reference<B> trySecond();

   @Override
   public abstract Reference<C> tryThird();

   @Override
   public abstract Reference<D> tryFourth();

   @Override
   public abstract <T> Variant4<T, B, C, D> transformFirst(Function<? super A, ? extends T> function);
   
   @Override
   public abstract <T> Variant4<A, T, C, D> transformSecond(Function<? super B, ? extends T> function);

   @Override
   public abstract <T> Variant4<A, B, T, D> transformThird(Function<? super C, ? extends T> function);

   @Override
   public abstract <T> Variant4<A, B, C, T> transformFourth(Function<? super D, ? extends T> function);

   @Override
   public abstract <E> Variant5<A, B, C, D, E> expand();
   
   private static class First<A, B, C, D> extends Variant4<A, B, C, D> implements Serializable {
      private static final long serialVersionUID = 1278033061274741885L;
      
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
      public boolean hasFourth() {
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
      public D getFourth() {
         throw new IllegalStateException();
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.setTo(a);
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.unset();
      }
   
      @Override
      public <T> Variant4<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
         return Variant4.<T, B, C, D>withFirst(function.apply(a));
      }
   
      @Override
      public <T> Variant4<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant4<A, T, C, D> ret = (Variant4<A, T, C, D>) this;
         return ret;
      }

      @Override
      public <T> Variant4<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant4<A, B, T, D> ret = (Variant4<A, B, T, D>) this;
         return ret;
      }

      @Override
      public <T> Variant4<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         Variant4<A, B, C, T> ret = (Variant4<A, B, C, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(Visitor4<? super A, ? super B, ? super C, ? super D, R> visitor) {
         return visitor.visitFirst(a);
      }

      @Override
      public <E> Variant5<A, B, C, D, E> expand() {
         return Variant5.withFirst(a);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof First && a.equals(((First<?, ?, ?, ?>) o).a);
      }
      
      @Override
      public int hashCode() {
         return First.class.hashCode() ^ a.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant4.First: " + a;
      }
   }

   private static class Second<A, B, C, D> extends Variant4<A, B, C, D> implements Serializable {
      private static final long serialVersionUID = 1774970819047259340L;
      
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
      public boolean hasFourth() {
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
      public D getFourth() {
         throw new IllegalStateException();
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.setTo(b);
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.unset();
      }
   
      @Override
      public <T> Variant4<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant4<T, B, C, D> ret = (Variant4<T, B, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> Variant4<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
         return Variant4.<A, T, C, D>withSecond(function.apply(b));
      }
   
      @Override
      public <T> Variant4<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant4<A, B, T, D> ret = (Variant4<A, B, T, D>) this;
         return ret;
      }
   
      @Override
      public <T> Variant4<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         Variant4<A, B, C, T> ret = (Variant4<A, B, C, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(Visitor4<? super A, ? super B, ? super C, ? super D, R> visitor) {
         return visitor.visitSecond(b);
      }

      @Override
      public <E> Variant5<A, B, C, D, E> expand() {
         return Variant5.withSecond(b);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Second && b.equals(((Second<?, ?, ?, ?>) o).b);
      }
      
      @Override
      public int hashCode() {
         return Second.class.hashCode() ^ b.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant4.Second: " + b;
      }
   }

   private static class Third<A, B, C, D> extends Variant4<A, B, C, D> implements Serializable {
      private static final long serialVersionUID = 503783209463764131L;

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
      public boolean hasFourth() {
         return false;
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
      public D getFourth() {
         throw new IllegalStateException();
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.setTo(c);
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.unset();
      }
   
      @Override
      public <T> Variant4<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant4<T, B, C, D> ret = (Variant4<T, B, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> Variant4<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant4<A, T, C, D> ret = (Variant4<A, T, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> Variant4<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
         return Variant4.<A, B, T, D>withThird(function.apply(c));
      }
   
      @Override
      public <T> Variant4<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
         @SuppressWarnings("unchecked") // since fourth not present, can safely recast that variable
         Variant4<A, B, C, T> ret = (Variant4<A, B, C, T>) this;
         return ret;
      }
      
      @Override
      public <R> R visit(Visitor4<? super A, ? super B, ? super C, ? super D, R> visitor) {
         return visitor.visitThird(c);
      }

      @Override
      public <E> Variant5<A, B, C, D, E> expand() {
         return Variant5.withThird(c);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Third && c.equals(((Third<?, ?, ?, ?>) o).c);
      }
      
      @Override
      public int hashCode() {
         return Third.class.hashCode() ^ c.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant4.Third: " + c;
      }
   }

   private static class Fourth<A, B, C, D> extends Variant4<A, B, C, D> implements Serializable {
      private static final long serialVersionUID = 5537605081223447679L;
      
      private final D d;
      
      Fourth(D d) {
         this.d = d;
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
         return false;
      }

      @Override
      public boolean hasFourth() {
         return true;
      }

      @Override
      public Object get() {
         return d;
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
         throw new IllegalStateException();
      }
   
      @Override
      public D getFourth() {
         return d;
      }
   
      @Override
      public Reference<A> tryFirst() {
         return Reference.unset();
      }
   
      @Override
      public Reference<B> trySecond() {
         return Reference.unset();
      }
   
      @Override
      public Reference<C> tryThird() {
         return Reference.unset();
      }
   
      @Override
      public Reference<D> tryFourth() {
         return Reference.setTo(d);
      }
   
      @Override
      public <T> Variant4<T, B, C, D> transformFirst(Function<? super A, ? extends T> function) {
         @SuppressWarnings("unchecked") // since first not present, can safely recast that variable
         Variant4<T, B, C, D> ret = (Variant4<T, B, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> Variant4<A, T, C, D> transformSecond(Function<? super B, ? extends T> function) {
         @SuppressWarnings("unchecked") // since second not present, can safely recast that variable
         Variant4<A, T, C, D> ret = (Variant4<A, T, C, D>) this;
         return ret;
      }
   
      @Override
      public <T> Variant4<A, B, T, D> transformThird(Function<? super C, ? extends T> function) {
         @SuppressWarnings("unchecked") // since third not present, can safely recast that variable
         Variant4<A, B, T, D> ret = (Variant4<A, B, T, D>) this;
         return ret;
      }
   
      @Override
      public <T> Variant4<A, B, C, T> transformFourth(Function<? super D, ? extends T> function) {
         return Variant4.<A, B, C, T>withFourth(function.apply(d));
      }
      
      @Override
      public <R> R visit(Visitor4<? super A, ? super B, ? super C, ? super D, R> visitor) {
         return visitor.visitFourth(d);
      }

      @Override
      public <E> Variant5<A, B, C, D, E> expand() {
         return Variant5.withFourth(d);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Fourth && d.equals(((Fourth<?, ?, ?, ?>) o).d);
      }
      
      @Override
      public int hashCode() {
         return Fourth.class.hashCode() ^ d.hashCode();
      }
      
      @Override
      public String toString() {
         return "Variant4.Fourth: " + d;
      }
   }
}
