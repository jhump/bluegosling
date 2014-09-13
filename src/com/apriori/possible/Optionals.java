package com.apriori.possible;

import java.io.Serializable;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Utility methods related to {@link Optional}s, including conversion to and from the interface
 * {@link Possible}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests for cast, upcast
public final class Optionals {

   // TODO: presentOnly, nullIfNotPresent static methods

   private Optionals() {
   }
   
   // TODO: doc
   
   public static <T> Possible<T> toPossible(Optional<T> optional) {
      return optional.isPresent() ? new Some<>(optional.get()) : none();
   }

   public static <T> Optional<T> fromPossible(Possible<T> possible) {
      return possible.isPresent() ? Optional.ofNullable(possible.get()) : Optional.empty();
   }

   /**
    * Casts an optional value to a different type. The present value must be assignable to the new
    * type. Using this method is much more efficient than creating a new optional value with a
    * different type. Since optional values are immutable, the type on an existing instance can
    * safely be re-cast when a present value is an instance of the cast type.
    * 
    * @param from the optional value from which we are casting
    * @param target the type to which the value will be cast
    * @return {@code this} but re-cast to an {@code Optional<T>} where {@code T} is the target type
    * @throws ClassCastException if the optional value is present and is not an instance of the
    *       specified type
    */
   @SuppressWarnings("unchecked") // we're doing a type check using the token, so it will be safe
   public static <S, T> Optional<T> cast(Optional<S> from, Class<T> target) {
      if (from.isPresent()) {
         S s = from.get();
         if (target.isInstance(s)) {
            return (Optional<T>) from;
         } else {
            throw new ClassCastException(target.getName());
         }
      } else {
         return Optional.empty();
      }
   }

   /**
    * Upcasts an optional value to a super-type. This relies on the compiler to ensure that the
    * types are compatible, so no class token argument is needed. Also, this version will never
    * throw a {@link ClassCastException}.
    * 
    * @param from the optional value from which we are casting
    * @return {@code this} but re-cast to an {@code Optional<S>} where {@code S} is the super-type
    * @see #cast(Optional, Class)
    */
   @SuppressWarnings("unchecked") // since Optionals are immutable, upcast is always safe
   public static <S, T extends S> Optional<S> upcast(Optional<T> from) {
      return (Optional<S>) from;
   }
   
   /**
    * Returns an object with no value present.
    * 
    * @return an object with no value present
    */
   @SuppressWarnings("unchecked")
   static <T> Possible<T> none() {
      return (Possible<T>) None.INSTANCE;
   }
   
   /**
    * Creates an object that represents the specified value. If the value is not {@code null} then
    * {@linkplain #some(Object) some value} is returned, otherwise {@linkplain #none() none}.
    * 
    * @param t the value
    * @return {@linkplain #some(Object) some value} with the specified value if not {@code null},
    *       otherwise {@linkplain #none() none}
    */
   static <T> Possible<T> of(T t) {
      return t != null ? new Some<>(t) : none();
   }

   /**
    * An optional value that has some value. The value cannot be {@code null}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the optional value
    */
   static class Some<T> implements Possible<T>, Serializable {

      private static final long serialVersionUID = 1511876184470865192L;
      
      private final T t;
      
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
      public Possible<T> or(Possible<T> alternate) {
         return this;
      }
      
      @Override
      public <U> Possible<U> map(Function<? super T, ? extends U> function) {
         return of(function.apply(t));
      }

      @Override
      public <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
         return Possible.notNull(function.apply(t));
      }

      @Override
      public Possible<T> filter(Predicate<? super T> predicate) {
         return predicate.test(t) ? this : none();
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
         return o instanceof Some && t.equals(((Some<?>) o).t);
      }
      
      @Override
      public int hashCode() {
         return Some.class.hashCode() ^ t.hashCode();
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
   static class None<T> implements Possible<T>, Serializable {
      
      private static final long serialVersionUID = 5598018120900214802L;
      
      /** 
       * The singleton object. Since optionals are immutable, and this form has no present value,
       * the same instance can be used for all usages.
       */
      static final None<?> INSTANCE = new None<Object>();
      
      private None() {
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
      public Possible<T> or(Possible<T> alternate) {
         return Possible.notNull(alternate);
      }
      
      @Override
      public <U> Possible<U> map(Function<? super T, ? extends U> function) {
         return none();
      }

      @Override
      public <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
         return none();
      }

      @Override
      public Possible<T> filter(Predicate<? super T> predicate) {
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
