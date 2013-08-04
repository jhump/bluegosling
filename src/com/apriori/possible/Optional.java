package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.Collections;
import java.util.Set;

//TODO: javadoc
//TODO: tests
//TODO: equals, hashCode, toString, serialization
public abstract class Optional<T> implements Possible<T> {

   private Optional() {
   }
   
   @Override
   public abstract <U> Optional<U> transform(Function<T, U> function);
   
   @Override
   public abstract Optional<T> filter(Predicate<T> predicate);
   
   @Override
   public abstract Optional<T> or(Possible<T> alternate);
   
   public abstract Optional<T> or(Optional<T> alternate);

   public static <T> Optional<T> none() {
      return None.instance();
   }
   
   public static <T> Optional<T> some(T t) {
      return new Some<T>(t);
   }
   
   public static <T> Optional<T> of(T t) {
      return t == null ? some(t) : Optional.<T>none();
   }
   
   public static <T> Optional<T> asOptional(Possible<T> possible) {
      if (possible instanceof Optional) {
         return (Optional<T>) possible;
      }
      return possible.isPresent() ? some(possible.get()) : Optional.<T>none();
   }
   
   private static class Some<T> extends Optional<T> {

      private final T t;
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      Some(T t) {
         if (t == null) {
            throw new NullPointerException();
         }
         this.t = t;
      }

      @Override
      public boolean isPresent() {
         return true;
      }

      @Override
      public T get() {
         return t;
      }

      @Override
      public T getOr(T alternate) {
         return t;
      }

      @Override
      public <X extends Throwable> T getOr(X throwable) throws X {
         return t;
      }

      @Override
      public Optional<T> or(Possible<T> alternate) {
         return this;
      }
      
      @Override
      public Optional<T> or(Optional<T> alternate) {
         return this;
      }
      
      @Override
      public <U> Optional<U> transform(Function<T, U> function) {
         return of(function.apply(t));
      }

      @Override
      public Optional<T> filter(Predicate<T> predicate) {
         return predicate.apply(t) ? this : None.<T>instance();
      }

      @Override
      public Set<T> asSet() {
         return Collections.singleton(t);
      }

      @Override
      public <R> R visit(Possible.Visitor<T, R> visitor) {
         return visitor.present(t);
      }
      
      @Override
      public boolean equals(Object o) {
         return o instanceof Some && t.equals(((Some<?>) o).t);
      }
      
      @Override
      public int hashCode() {
         return Some.class.hashCode() ^ (t == null ? 0 : t.hashCode());
      }
      
      @Override
      public String toString() {
         return "Optional: " + t;
      }
   }
   
   private static class None<T> extends Optional<T> {
      
      private static final None<?> INSTANCE = new None<Object>();
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      private None() {
      }
      
      @SuppressWarnings("unchecked") // only need one instance (immutable and type arg is unused)
      static <T> None<T> instance() {
         return (None<T>) INSTANCE;
      }
      
      @Override
      public boolean isPresent() {
         return false;
      }

      @Override
      public T get() {
         throw new IllegalStateException();
      }

      @Override
      public T getOr(T alternate) {
         return alternate;
      }

      @Override
      public <X extends Throwable> T getOr(X throwable) throws X {
         throw throwable;
      }

      @Override
      public Set<T> asSet() {
         return Collections.emptySet();
      }

      @Override
      public <R> R visit(Possible.Visitor<T, R> visitor) {
         return visitor.absent();
      }

      @Override
      public Optional<T> or(Possible<T> alternate) {
         return asOptional(alternate);
      }
      
      @Override
      public Optional<T> or(Optional<T> alternate) {
         return alternate;
      }
      
      @Override
      public <U> Optional<U> transform(Function<T, U> function) {
         return instance();
      }

      @Override
      public Optional<T> filter(Predicate<T> predicate) {
         return this;
      }
      
      @Override
      public boolean equals(Object o) {
         return o == this;
      }
      
      @Override
      public int hashCode() {
         return None.class.hashCode();
      }
      
      @Override
      public String toString() {
         return "Optional, none";
      }
      
      private Object readResolve() {
         return INSTANCE;
      }
   }
}
