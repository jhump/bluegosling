package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * A {@linkplain possible} value that, even if present, can be null.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the possible value
 */
// TODO: javadoc
// TODO: tests
public abstract class Reference<T> implements Possible<T> {

   private Reference() {
   }
   
   @Override
   public abstract <U> Reference<U> transform(Function<T, U> function);
   
   @Override
   public abstract Reference<T> filter(Predicate<T> predicate);
   
   @Override
   public abstract Reference<T> or(Possible<T> alternate);
   
   public abstract Reference<T> or(Reference<T> alternate);

   public static <T> Reference<T> unset() {
      return UnsetReference.instance();
   }
   
   public static <T> Reference<T> set(T t) {
      return new SetReference<T>(t);
   }
   
   public static <T> Reference<T> asReference(Possible<T> possible) {
      if (possible instanceof Reference) {
         return (Reference<T>) possible;
      }
      return possible.isPresent() ? set(possible.get()) : Reference.<T>unset();
   }
   
   private static class SetReference<T> extends Reference<T> implements Serializable {

      private static final long serialVersionUID = 7364438623841250389L;
      
      private final T t;
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      SetReference(T t) {
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
      public Reference<T> or(Possible<T> alternate) {
         return this;
      }
      
      @Override
      public Reference<T> or(Reference<T> alternate) {
         return this;
      }
      
      @Override
      public <U> Reference<U> transform(Function<T, U> function) {
         return set(function.apply(t));
      }

      @Override
      public Reference<T> filter(Predicate<T> predicate) {
         return predicate.apply(t) ? this : UnsetReference.<T>instance();
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
         if (!(o instanceof SetReference)) {
            return false;
         }
         SetReference<?> other = (SetReference<?>) o;
         return t == null ? other.t == null : t.equals(other.t);
      }
      
      @Override
      public int hashCode() {
         return SetReference.class.hashCode() ^ (t == null ? 0 : t.hashCode());
      }
      
      @Override
      public String toString() {
         return "Reference: " + t;
      }
   }

   private static class UnsetReference<T> extends Reference<T> implements Serializable {
      
      private static final long serialVersionUID = -7792329235520529671L;
      private static final UnsetReference<?> INSTANCE = new UnsetReference<Object>();
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      private UnsetReference() {
      }
      
      @SuppressWarnings("unchecked") // only need one instance (immutable and type arg is unused)
      static <T> UnsetReference<T> instance() {
         return (UnsetReference<T>) INSTANCE;
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
      public Reference<T> or(Possible<T> alternate) {
         return asReference(alternate);
      }
      
      @Override
      public Reference<T> or(Reference<T> alternate) {
         return alternate;
      }
      
      @Override
      public <U> Reference<U> transform(Function<T, U> function) {
         return instance();
      }

      @Override
      public Reference<T> filter(Predicate<T> predicate) {
         return this;
      }
      
      @Override
      public boolean equals(Object o) {
         return o == this;
      }
      
      @Override
      public int hashCode() {
         return UnsetReference.class.hashCode();
      }
      
      @Override
      public String toString() {
         return "Reference, unset";
      }
      
      private Object readResolve() {
         return INSTANCE;
      }
   }
}
