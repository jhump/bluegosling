package com.bluegosling.possible;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.bluegosling.util.ValueType;

/**
 * Utility methods related to {@link Optional}s, including conversion to and from the interface
 * {@link Possible}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: tests for cast, upcast
public final class Optionals {

   private Optionals() {
   }
   
   // TODO: doc
   
   public static <T> List<T> nullIfNotPresent(
         Collection<? extends Optional<? extends T>> optionals) {
      List<T> present = new ArrayList<T>(optionals.size());
      for (Optional<? extends T> o : optionals) {
         present.add(o.orElse(null));
      }
      return Collections.unmodifiableList(present);
   }
   
   static <T> List<T> presentOnly(Collection<? extends Optional<? extends T>> optionals) {
      List<T> present = new ArrayList<T>(optionals.size());
      for (Optional<? extends T> o : optionals) {
         if (o.isPresent()) {
            try {
               present.add(o.get());
            } catch (NoSuchElementException e) {
               // this should happen rarely if ever, but could occur with a mutable object if a
               // race occurs and the value becomes absent after the call to isPresent()
            }
         }
      }
      return Collections.unmodifiableList(present);
   }
   
   public static <T> Possible<T> toPossible(Optional<T> optional) {
      return ofNullable(optional.orElse(null));
   }

   public static <T> Optional<T> fromPossible(Possible<T> possible) {
      return Optional.ofNullable(possible.orElse(null));
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
   static <T> Possible<T> empty() {
      return (Possible<T>) OptionalPossible.NONE;
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
      return new OptionalPossible<>(Objects.requireNonNull(t));
   }
   
   // TODO: doc
   static <T> Possible<T> ofNullable(T t) {
      return t == null ? empty() : of(t);
   }

   /**
    * An possible that wraps an {@link Optional}..
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the optional value
    */
   @ValueType
   static final class OptionalPossible<T> implements Possible<T>, Serializable {
      private static final long serialVersionUID = 1511876184470865192L;
      
      private static final OptionalPossible<?> NONE = new OptionalPossible<>(null);
      
      private final T t;
      
      OptionalPossible(T t) {
         this.t = t;
      }

      @Override
      public boolean isPresent() {
         return t != null;
      }
      
      @Override
      public void ifPresent(Consumer<? super T> consumer) {
         if (t != null) {
            consumer.accept(t);
         }
      }

      @Override
      public T get() {
         if (t == null) {
            throw new NoSuchElementException();
         }
         return t;
      }

      @Override
      public T orElse(T alternate) {
         return t == null ? alternate : t;
      }
      
      @Override
      public T orElseGet(Supplier<? extends T> supplier) {
         return t == null ? supplier.get() : t;
      }

      @Override
      public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwable) throws X {
         if (t == null) {
            throw throwable.get();
         }
         return t;
      }

      @Override
      public Possible<T> or(Possible<T> alternate) {
         return t == null ? alternate : this;
      }
      
      @SuppressWarnings("unchecked")
      @Override
      public <U> Possible<U> map(Function<? super T, ? extends U> function) {
         if (t == null) {
            return (Possible<U>) this;
         }
         return ofNullable(function.apply(t));
      }

      @SuppressWarnings("unchecked")
      @Override
      public <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
         return t != null
               ? Objects.requireNonNull(function.apply(t))
               : (Possible<U>) this;
      }

      @Override
      public Possible<T> filter(Predicate<? super T> predicate) {
         return t != null && predicate.test(t) ? this : empty();
      }

      @Override
      public Set<T> asSet() {
         return t != null ? Collections.singleton(t) : Collections.emptySet();
      }

      @Override
      public <R> R visit(Possible.Visitor<? super T, R> visitor) {
         return t != null
               ? visitor.present(t)
               : visitor.absent();
      }
      
      @Override
      public boolean equals(Object other) {
         return other instanceof OptionalPossible
               && Objects.equals(t, ((OptionalPossible<?>) other).t);
      }
      
      @Override
      public int hashCode() {
         return Objects.hashCode(t);
      }
      
      @Override
      public String toString() {
         return t != null
               ? "Possible[" + t + "]"
               : "Possible.empty";
      }
   }
}
