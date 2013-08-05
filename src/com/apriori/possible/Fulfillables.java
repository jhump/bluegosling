package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory methods for creating instances of {@link Fulfillable}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO: test
//TODO: should fulfillables be serializable?
public final class Fulfillables {
   private Fulfillables() {
   }

   /**
    * Creates a value that is already fulfilled.
    * 
    * @param value the fulfilled value
    * @return an object that is already fulfilled
    */
   public static <T> Fulfillable<T> fulfilled(final T value) {
      return new Fulfillable<T>() {

         @Override
         public boolean isPresent() {
            return true;
         }

         @Override
         public boolean fulfill(T t) {
            return false;
         }

         @Override
         public Possible<T> or(Possible<T> alternate) {
            return this;
         }

         @Override
         public <U> Possible<U> transform(Function<T, U> function) {
            return Reference.set(function.apply(value));
         }

         @Override
         public Possible<T> filter(Predicate<T> predicate) {
            return predicate.apply(value) ? this : Reference.<T>unset();
         }
         
         @Override
         public T get() {
            return value;
         }
         
         @Override
         public T getOr(T other) {
            return value;
         }
         
         @Override
         public <X extends Throwable> T getOrThrow(X throwable) throws X {
            return value;
         }

         @Override
         public Set<T> asSet() {
            return Collections.singleton(value);
         }

         @Override
         public <R> R visit(Possible.Visitor<T, R> visitor) {
            return visitor.present(value);
         }
      };
   }
   
   /**
    * Creates a new fulfillable value. The returned object is thread-safe.
    * 
    * @return a new fulfillable value
    */
   public static <T> Fulfillable<T> create() {
      return new Fulfillable<T>() {
         private final AtomicBoolean set = new AtomicBoolean();
         private volatile T t;
         
         @Override
         public boolean isPresent() {
            return set.get();
         }

         @Override
         public boolean fulfill(T value) {
            if (set.compareAndSet(false, true)) {
               this.t = value;
               return true;
            }
            return false;
         }
         
         @Override
         public Possible<T> or(Possible<T> alternate) {
            return isPresent() ? this : alternate;
         }

         @Override
         public <U> Possible<U> transform(Function<T, U> function) {
            return isPresent() ? Reference.set(function.apply(t)) : Reference.<U>unset();
         }

         @Override
         public Possible<T> filter(Predicate<T> predicate) {
            return isPresent() && predicate.apply(t) ? this : Reference.<T>unset();
         }
         
         @Override
         public T get() {
            if (!isPresent()) {
               throw new IllegalStateException("not fulfilled");
            }
            return t;
         }
         
         @Override
         public T getOr(T other) {
            return isPresent() ? t : other;
         }
         
         @Override
         public <X extends Throwable> T getOrThrow(X throwable) throws X {
            if (isPresent()) {
               return t;
            }
            throw throwable;
         }

         @Override
         public Set<T> asSet() {
            return isPresent() ? Collections.singleton(t) : Collections.<T>emptySet();
         }

         @Override
         public <R> R visit(Possible.Visitor<T, R> visitor) {
            return isPresent() ? visitor.present(t) : visitor.absent();
         }
      };
   }
}
