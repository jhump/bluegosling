package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * A {@linkplain Possible possible} value that is immutable and cannot be {@code null}. An optional
 * value is either {@linkplain #some(Object) some value} or {@linkplain #none() none}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the optional value
 */
//TODO: tests
public abstract class Optional<T> implements Possible<T> {

   private Optional() {
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Optional}. If
    * a value is present but the specified function returns {@code null} then {@linkplain
    * #none() no value} is returned.
    */
   @Override
   public abstract <U> Optional<U> transform(Function<T, U> function);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Optional}.
    */
   @Override
   public abstract Optional<T> filter(Predicate<T> predicate);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Optional}. If
    * the specified {@linkplain Possible possible} value is not an {@link Optional} then it will be
    * {@linkplain #asOptional(Possible) converted}. In particular, if the specified value is present
    * but {@code null} then {@linkplain #none() no value} is returned.
    */
   @Override
   public abstract Optional<T> or(Possible<T> alternate);
   
   /**
    * Returns the current value if one is present or the specified optional value if not.
    * 
    * @param alternate an alternate value
    * @return returns the current reference if a value is present or the alternate if not
    */
   public abstract Optional<T> or(Optional<T> alternate);

   /**
    * Returns an object with no value present.
    * 
    * @return an object with no value present
    */
   public static <T> Optional<T> none() {
      return None.instance();
   }
   
   /**
    * Creates an object with the specified value. The value cannot be {@code null}.
    * 
    * @param t the value
    * @return an object with the specified value
    * @throws NullPointerException if the specified value is {@code null}
    */   
   public static <T> Optional<T> some(T t) {
      return new Some<T>(t);
   }
   
   /**
    * Creates an object that represents the specified value. If the value is not {@code null} then
    * {@linkplain #some(Object) some value} is returned, otherwise {@linkplain #none() none}.
    * 
    * @param t the value
    * @return {@linkplain #some(Object) some value} with the specified value if not {@code null},
    *       otherwise {@linkplain #none() none}
    */
   public static <T> Optional<T> of(T t) {
      return t == null ? some(t) : Optional.<T>none();
   }
   
   
   /**
    * Converts a possible value to an optional one. If the specified object is an optional value, it
    * is returned. Otherwise, if the specified value is present and not {@code null} then 
    * {@linkplain #some(Object) some value} is returned; if the specified value is absent or
    * {@code null} then {@linkplain #none() none}.
    * 
    * @param possible a possible value
    * @return the possible value, converted to an optional one
    */
   public static <T> Optional<T> asOptional(Possible<T> possible) {
      if (possible instanceof Optional) {
         return (Optional<T>) possible;
      }
      return possible.isPresent() ? some(possible.get()) : Optional.<T>none();
   }
   
   /**
    * An optional value that has some value. The value cannot be {@code null}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the optional value
    */
   private static class Some<T> extends Optional<T> implements Serializable {

      private static final long serialVersionUID = 1511876184470865192L;
      
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
      public <X extends Throwable> T getOrThrow(X throwable) throws X {
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
   
   /**
    * An optional value that actually has no value. Can be thought of as representing no object or
    * as representing {@code null}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the optional value
    */
   private static class None<T> extends Optional<T> implements Serializable {
      
      private static final long serialVersionUID = 5598018120900214802L;
      
      /** 
       * The singleton object. Since optionals are immutable, and this form has no present value,
       * the same instance can be used for all usages.
       */
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
      public <X extends Throwable> T getOrThrow(X throwable) throws X {
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
      
      /**
       * Ensures that the singleton pattern is enforced during serialization.
       * 
       * @return {@link #INSTANCE}
       */
      private Object readResolve() {
         return INSTANCE;
      }
   }
}
