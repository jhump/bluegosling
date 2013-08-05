package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * A {@linkplain Possible possible} value that, even if present, can be {@code null}. A reference
 * can either be set or unset. If set, a value is present; if unset, the value is absent. References
 * are immutable. A similar possible value that is mutable is {@link Holder}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the possible value
 */
// TODO: tests
public abstract class Reference<T> implements Possible<T> {

   private Reference() {
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}.
    */
   @Override
   public abstract <U> Reference<U> transform(Function<T, U> function);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}.
    */
   @Override
   public abstract Reference<T> filter(Predicate<T> predicate);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}. If
    * the specified {@linkplain Possible possible} value is not a {@link Reference} then it will be
    * {@linkplain #asReference(Possible) converted}.
    */
   @Override
   public abstract Reference<T> or(Possible<T> alternate);

   /**
    * Returns the current reference if a value is present or the specified reference if not.
    * 
    * @param alternate an alternate value
    * @return returns the current reference if a value is present or the alternate if not
    */
   public abstract Reference<T> or(Reference<T> alternate);

   /**
    * Creates a reference where the value is unset (absent).
    * 
    * @return an unset reference
    */
   public static <T> Reference<T> unset() {
      return UnsetReference.instance();
   }
   
   /**
    * Creates a reference set to the specified value.
    * 
    * @param t the value
    * @return a set reference
    */
   public static <T> Reference<T> set(T t) {
      return new SetReference<T>(t);
   }
   
   /**
    * Converts a possible value to a reference. If the specified object is a reference, it is
    * returned. Otherwise, if the specified value is present then a reference set to the same value
    * is returned. If the specified value is absent then an unset reference is returned.
    * 
    * @param possible a possible value
    * @return the possible value, converted to a reference
    */
   public static <T> Reference<T> asReference(Possible<T> possible) {
      if (possible instanceof Reference) {
         return (Reference<T>) possible;
      }
      return possible.isPresent() ? set(possible.get()) : Reference.<T>unset();
   }
   
   /**
    * A reference with a value present.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the value
    */
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
      public <X extends Throwable> T getOrThrow(X throwable) throws X {
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

   /**
    * An unset reference (no value present).
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the value
    */
   private static class UnsetReference<T> extends Reference<T> implements Serializable {
      
      private static final long serialVersionUID = -7792329235520529671L;
      
      /** 
       * The singleton unset reference. Since references are immutable, and this form has no
       * present value, the same instance can be used for all usages.
       */
      private static final UnsetReference<?> INSTANCE = new UnsetReference<Object>();
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      private UnsetReference() {
      }
      
      @SuppressWarnings("unchecked") // only need one instance, regardless of type
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
