package com.bluegosling.possible;

import java.io.Serializable;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.bluegosling.util.ValueType;

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
@ValueType
public final class Reference<T> implements Possible<T>, Serializable {
   private static final long serialVersionUID = -7778850069219799999L;

   private static final Reference<?> UNSET = new Reference<>();
   
   private final boolean set;
   private final T t;

   private Reference() {
      set = false;
      t = null;
   }
   
   private Reference(T t) {
      this.set = true;
      this.t = t;
   }

   /**
    * Creates a reference where the value is unset (absent).
    * 
    * @return an unset reference
    */
   @SuppressWarnings("unchecked")
   public static <T> Reference<T> unset() {
      return (Reference<T>) UNSET;
   }
   
   /**
    * Creates a reference set to the specified value.
    * 
    * @param t the value
    * @return a set reference
    */
   public static <T> Reference<T> setTo(T t) {
      return new Reference<>(t);
   }
   
   /**
    * Converts a possible value to a reference. If the specified object is a reference, it is
    * returned. Otherwise, if the specified value is present then a reference set to the same value
    * is returned. If the specified value is absent then an unset reference is returned.
    * 
    * @param possible a possible value
    * @return the possible value, converted to a reference
    */
   public static <T> Reference<T> asReference(Possible<? extends T> possible) {
      if (possible instanceof Reference) {
         return upcast((Reference<? extends T>) possible);
      }
      return possible.isPresent() ? setTo(possible.get()) : Reference.unset();
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
   
   @Override
   public boolean isPresent() {
      return set;
   }
   
   @Override
   public void ifPresent(Consumer<? super T> consumer) {
      if (set) {
         consumer.accept(t);
      }
   }

   @Override
   public T get() {
      if (!set) {
         throw new NoSuchElementException();
      }
      return t;
   }

   @Override
   public T orElse(T alternate) {
      return set ? t : alternate;
   }
   
   @Override
   public T orElseGet(Supplier<? extends T> supplier) {
      return set ? t : supplier.get();
   }

   @Override
   public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwable) throws X {
      if (!set) {
         throw throwable.get();
      }
      return t;
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}.
    */
   @Override
   public <U> Reference<U> map(Function<? super T, ? extends U> function) {
      if (!set) {
         return unset();
      }
      return setTo(function.apply(t));
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}.
    */
   @Override
   public <U> Reference<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
      return set ? asReference(function.apply(t)) : unset();
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}.
    */
   @Override
   public Reference<T> filter(Predicate<? super T> predicate) {
      return set && predicate.test(t) ? this : unset();
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>Overrides the return type to indicate that it will be an instance of {@link Reference}. If
    * the specified {@linkplain Possible possible} value is not a {@link Reference} then it will be
    * {@linkplain #asReference(Possible) converted}.
    */
   @Override
   public Possible<T> or(Possible<T> alternate) {
      return set ? this : alternate;
   }

   /**
    * Returns the current reference if a value is present or the specified reference if not.
    * 
    * @param alternate an alternate value
    * @return returns the current reference if a value is present or the alternate if not
    */
   public Reference<T> or(Reference<T> alternate) {
      return set ? this : alternate;
   }
   
   @Override
   public Set<T> asSet() {
      return set ? Collections.singleton(t) : Collections.emptySet();
   }

   @Override
   public <R> R visit(Possible.Visitor<? super T, R> visitor) {
      return set ? visitor.present(t) : visitor.absent();
   }
   
   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Reference)) {
         return false;
      }
      Reference<?> other = (Reference<?>) o;
      if (!set && !other.set) {
         return true;
      }
      return set && other.set && Objects.equals(t, other.t);
   }
   
   @Override
   public int hashCode() {
      return set ? Objects.hashCode(t) : -1;
   }
   
   @Override
   public String toString() {
      return set
            ? "Reference[" + t + "]"
            : "Reference.unset";
   }
}
