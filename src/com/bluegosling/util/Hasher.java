package com.bluegosling.util;

import com.bluegosling.collections.views.TransformingList;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Allows adding/overriding {@code hashCode} and {@code equals} behavior for an object. The hasher
 * provides alternate computations for these operations than the original object. You can then wrap
 * the object using {@link #apply(Class, Object)}. The wrapped result uses the hasher's behavior for
 * implementing {@code hashCode} and {@code equals}, and the original value can be extracted via
 * the resulting object's {@link Supplier#get() get} method.
 *
 * @param <T> the type of object that is hashed
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Hasher<T> {
   /**
    * Computes a hash code for the given value.
    *
    * @param val a value
    * @return a hash code for the given value
    */
   int hashCode(T val);
   
   /**
    * Determines if the given two objects are equal.
    *
    * @param val1 a value
    * @param val2 another value
    * @return true if the objects are considered equal
    */
   boolean equals(T val1, T val2);
   
   /**
    * Returns a hashing wrapper for the given object. The value supplied by the wrapper is the
    * original value (the given object). The returned object uses this hasher's
    * {@link #equals(Object, Object)} and {@link #hashCode(Object)} methods to implements its own
    * {@code equals} and {@code hashCode} methods.
    *
    * <p>The returned object's implementation of {@link Object#equals(Object) equals} will return
    * false if given any object that cannot be {@linkplain Class#cast(Object) cast} to the given
    * type token.
    *
    * @param type the type of values that can be equal to the given value
    * @param value a value
    * @return a wrapper that uses this hasher to implement {@code hashCode} and {@code equals}
    */
   default Supplier<T> apply(Class<? extends T> type, T value) {
      return new Supplier<T>() {
         @Override
         public T get() {
            return value;
         }
         
         @Override
         public boolean equals(Object o) {
            return type.isInstance(o) && Hasher.this.equals(value, type.cast(o));
         }
         
         @Override
         public int hashCode() {
            return Hasher.this.hashCode(value);
         }
      };
   }
   
   /**
    * Returns a function that can be applied to any object to return a
    * {@linkplain #apply(Class, Object) hashing wrapper} for that object. This effectively "curries"
    * the type token which can simplify downstream usages of the hasher.
    *
    * @param type the type of values that can be equal to the given value
    * @return a function that returns hashing wrappers for the arguments given to it
    */
   default Function<T, Supplier<T>> forType(Class<? extends T> type) {
      return t -> apply(type, t);
   }

   /**
    * Returns a hasher that uses the given functions to extract field values. The field values are
    * then used to implement both {@code hashCode} and {@code equals}.
    *
    * @param fieldAccessors function for accessing the fields that should be inputs to
    *       {@code hashCode} and {@code equals} operations
    * @return a hasher that uses the given functions to extract field values
    */
   @SafeVarargs
   static <T> Hasher<T> forFields(Function<? super T, ?>... fieldAccessors) {
      return forFields(Arrays.asList(fieldAccessors));
   }

   /**
    * Returns a hasher that uses the given functions to extract field values. The field values are
    * then used to implement both {@code hashCode} and {@code equals}.
    *
    * @param fieldAccessors function for accessing the fields that should be inputs to
    *       {@code hashCode} and {@code equals} operations
    * @return a hasher that uses the given functions to extract field values
    */
   static <T> Hasher<T> forFields(List<? extends Function<? super T, ?>> fieldAccessors) {
      return new Hasher<T>() {
         @Override
         public int hashCode(T val) {
            return Objects.hash(
                  new TransformingList<>(fieldAccessors, t -> t.apply(val)).toArray());
         }

         @Override
         public boolean equals(T val, T other) {
            for (Function<? super T, ?> f : fieldAccessors) {
               if (!Objects.equals(f.apply(val), f.apply(other))) {
                  return false;
               }
            }
            return true;
         }
      };
   }
   
   /**
    * Returns a hasher that composes the given functional interfaces to implement {@code hashCode}
    * and {@code equals}.
    *
    * @param hashCode a function that computes hash codes
    * @param equals a predicate that tests whether two values are equal
    * @return a hasher that composes the given function interfaces
    */
   static <T> Hasher<T> of(ToIntFunction<? super T> hashCode,
         BiPredicate<? super T, ? super T> equals) {
      return new Hasher<T>() {
         @Override
         public int hashCode(T val) {
            return hashCode.applyAsInt(val);
         }

         @Override
         public boolean equals(T val, T other) {
            return equals.test(val, other);
         }
      };
   }
}
