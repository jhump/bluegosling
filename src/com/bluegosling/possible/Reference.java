package com.bluegosling.possible;

import java.io.Serializable;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A {@linkplain Possible possible} value that, even if present, can be {@code null}. A reference
 * can either be set or unset. If set, a value is present; if unset, the value is absent. References
 * are immutable. A similar possible value that is mutable is {@link Holder}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the possible value
 */
//TODO: tests for cast, upcast
public abstract class Reference<T> implements Possible<T> {

   private Reference() {
   }

   /**
    * Creates a reference where the value is unset (absent).
    * 
    * @return an unset reference
    */
   @SuppressWarnings("unchecked")
   public static <T> Reference<T> unset() {
      return (Reference<T>) UnsetReference.INSTANCE;
   }
   
   /**
    * Creates a reference set to the specified value.
    * 
    * @param t the value
    * @return a set reference
    */
   public static <T> Reference<T> setTo(T t) {
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
      return possible.isPresent() ? setTo(possible.get()) : Reference.<T>unset();
   }
   
   /**
    * Casts a reference to a different type. The present value must be assignable to the new type.
    * Using this method is much more efficient than creating a new reference value with a different
    * type. Since reference values are immutable, the type on an existing instance can safely be
    * re-cast when a present value is {@code null} or an instance of the cast type.
    * 
    * @param from the reference value from which we are casting
    * @param target the type to which the value will be cast
    * @return {@code this} but re-cast to a {@code Reference<T>} where {@code T} is the target type
    * @throws ClassCastException if the reference is set but the value is neither an instance of the
    *       specified type nor {@code null}
    */
   @SuppressWarnings("unchecked") // we're doing a type check using the token, so it will be safe
   public static <S, T> Reference<T> cast(Reference<S> from, Class<T> target) {
      if (from.isPresent()) {
         S s = from.get();
         if (s == null || target.isInstance(s)) {
            return (Reference<T>) from;
         } else {
            throw new ClassCastException(target.getName());
         }
      } else {
         return unset();
      }
   }

   /**
    * Upcasts an reference to a super-type. This relies on the compiler to ensure that the types are
    * compatible, so no class token argument is needed. Also, this version will never throw a
    * {@link ClassCastException}.
    * 
    * @param from the reference value from which we are casting
    * @return {@code this} but re-cast to a {@code Reference<S>} where {@code S} is the super-type
    * @see #cast(Reference, Class)
    */
   @SuppressWarnings("unchecked") // since References are immutable, upcast is always safe
   public static <S, T extends S> Reference<S> upcast(Reference<T> from) {
      return (Reference<S>) from;
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}.
    */
   @Override
   public abstract <U> Reference<U> map(Function<? super T, ? extends U> function);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}.
    */
   @Override
   public abstract <U> Reference<U> flatMap(Function<? super T, ? extends Possible<U>> function);
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}.
    */
   @Override
   public abstract Reference<T> filter(Predicate<? super T> predicate);
   
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
      public void ifPresent(Consumer<? super T> consumer) {
         consumer.accept(t);
      }

      @Override
      public T get() {
         return t;
      }

      @Override
      public T orElse(T alternate) {
         return t;
      }
      
      @Override
      public T orElseGet(Supplier<? extends T> supplier) {
         return t;
      }

      @Override
      public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwable) throws X {
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
      public <U> Reference<U> map(Function<? super T, ? extends U> function) {
         return setTo(function.apply(t));
      }

      @Override
      public <U> Reference<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
         return asReference(function.apply(t));
      }

      @Override
      public Reference<T> filter(Predicate<? super T> predicate) {
         return predicate.test(t) ? this : unset();
      }

      @Override
      public Set<T> asSet() {
         return Collections.singleton(t);
      }

      @Override
      public <R> R visit(Possible.Visitor<? super T, R> visitor) {
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
      static final UnsetReference<?> INSTANCE = new UnsetReference<Object>();
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      private UnsetReference() {
      }
      
      @Override
      public boolean isPresent() {
         return false;
      }
      
      @Override
      public void ifPresent(Consumer<? super T> consumer) {
      }

      @Override
      public T get() {
         throw new NoSuchElementException();
      }

      @Override
      public T orElse(T alternate) {
         return alternate;
      }
      
      @Override
      public T orElseGet(Supplier<? extends T> supplier) {
         return supplier.get();
      }

      @Override
      public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwable) throws X {
         throw throwable.get();
      }

      @Override
      public Set<T> asSet() {
         return Collections.emptySet();
      }

      @Override
      public <R> R visit(Possible.Visitor<? super T, R> visitor) {
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
      public <U> Reference<U> map(Function<? super T, ? extends U> function) {
         return unset();
      }

      @Override
      public <U> Reference<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
         return unset();
      }

      @Override
      public Reference<T> filter(Predicate<? super T> predicate) {
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
