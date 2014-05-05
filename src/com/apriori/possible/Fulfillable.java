package com.apriori.possible;

import com.apriori.possible.Fulfillables.FulfillableImpl;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A possible value that can be set exactly once. Setting a value when not already present is
 * called "fulfilling" the value.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of fulfilled value
 */
public interface Fulfillable<T> extends Possible<T> {
   /**
    * Fulfills the value.
    * 
    * @param value the fulfilled value
    * @return true if the value was set to the specified value; false if the value was already
    *       fulfilled
    */
   boolean fulfill(T value);

   /**
    * Creates a value that is already fulfilled.
    * 
    * @param value the fulfilled value
    * @return an object that is already fulfilled
    */
   public static <T> Fulfillable<T> fulfilled(T value) {
      return new Fulfillable<T>() {

         @Override
         public boolean isPresent() {
            return true;
         }
         
         @Override
         public void ifPresent(Consumer<? super T> consumer) {
            consumer.accept(value);
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
         public <U> Possible<U> map(Function<? super T, ? extends U> function) {
            return Reference.setTo(function.apply(value));
         }

         @Override
         public <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
            return function.apply(value);
         }

         @Override
         public Possible<T> filter(Predicate<? super T> predicate) {
            return predicate.test(value) ? this : Reference.unset();
         }
         
         @Override
         public T get() {
            return value;
         }
         
         @Override
         public T orElse(T other) {
            return value;
         }
         
         @Override
         public T orElseGet(Supplier<? extends T> supplier) {
            return value;
         }
         
         @Override
         public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwable) throws X {
            return value;
         }

         @Override
         public Set<T> asSet() {
            return Collections.singleton(value);
         }

         @Override
         public <R> R visit(Possible.Visitor<? super T, R> visitor) {
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
      return new FulfillableImpl<T>();
   }
}