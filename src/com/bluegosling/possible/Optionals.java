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
      return optional.isPresent()
            ? new OptionalPossible<>(optional)
            : OptionalPossible.empty();
   }

   public static <T> Optional<T> fromPossible(Possible<T> possible) {
      return Optional.ofNullable(possible.orElse(null));
   }

   /**
    * Upcasts an optional value to a super-type. This relies on the compiler to ensure that the
    * types are compatible, so no class token argument is needed. Also, this version will never
    * throw a {@link ClassCastException}.
    * 
    * @param from the optional value from which we are casting
    * @return {@code this} but re-cast to an {@code Optional<S>} where {@code S} is the super-type
    */
   @SuppressWarnings("unchecked") // since Optionals are immutable, upcast is always safe
   public static <S, T extends S> Optional<S> cast(Optional<T> from) {
      return (Optional<S>) from;
   }

   /**
    * A possible that wraps an {@link Optional}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the optional value
    */
   @ValueType
   static final class OptionalPossible<T> implements Possible<T>, Serializable {
      private static final long serialVersionUID = 1511876184470865192L;
      
      private static final OptionalPossible<?> EMPTY = new OptionalPossible<>(Optional.empty());
      
      @SuppressWarnings("unchecked") // safe because EMPTY is immutable and valueless
      static <T> Possible<T> empty() {
         return (Possible<T>) EMPTY;
      }
      
      private final Optional<T> o;
      
      OptionalPossible(Optional<T> o) {
         this.o = o;
      }

      @Override
      public boolean isPresent() {
         return o.isPresent();
      }
      
      @Override
      public void ifPresent(Consumer<? super T> consumer) {
         o.ifPresent(consumer);
      }

      @Override
      public T get() {
         return o.get();
      }

      @Override
      public T orElse(T alternate) {
         return o.orElse(alternate);
      }
      
      @Override
      public T orElseGet(Supplier<? extends T> supplier) {
         return o.orElseGet(supplier);
      }

      @Override
      public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwable) throws X {
         return o.orElseThrow(throwable);
      }

      @Override
      public Possible<T> or(Possible<T> alternate) {
         return o.isPresent() ? this : alternate;
      }
      
      @SuppressWarnings("unchecked")
      @Override
      public <U> Possible<U> map(Function<? super T, ? extends U> function) {
         return o.isPresent()
               ? toPossible(o.map(function))
               : (Possible<U>) this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
         return o.isPresent()
               ? Objects.requireNonNull(function.apply(o.get()))
               : (Possible<U>) this;
      }

      @Override
      public Possible<T> filter(Predicate<? super T> predicate) {
         return o.isPresent() && predicate.test(o.get()) ? this : empty();
      }

      @Override
      public Set<T> asSet() {
         return o.isPresent() ? Collections.singleton(o.get()) : Collections.emptySet();
      }

      @Override
      public <R> R visit(Possible.Visitor<? super T, R> visitor) {
         return o.isPresent()
               ? visitor.present(o.get())
               : visitor.absent();
      }
      
      @Override
      public boolean equals(Object other) {
         return other instanceof OptionalPossible
               && Objects.equals(o, ((OptionalPossible<?>) other).o);
      }
      
      @Override
      public int hashCode() {
         return o.hashCode();
      }
      
      @Override
      public String toString() {
         return o.toString();
      }
   }
}