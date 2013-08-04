package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;

import java.util.Collections;
import java.util.Set;

// TODO: javadoc
// TODO: tests
// TODO: equals, hashCode, toString, serialization
public class Holder<T> implements Possible<T> {
   
   public static <T> Holder<T> create() {
      return new Holder<T>();
   }

   public static <T> Holder<T> create(T t) {
      return new Holder<T>(t);
   }
   
   private T value;
   private boolean isPresent;
   
   private Holder() {
   }

   private Holder(T t) {
      value = t;
      isPresent = true;
   }
   
   public void set(T t) {
      value = t;
      isPresent = true;
   }
   
   public void clear() {
      value = null;
      isPresent = false;
   }

   @Override
   public boolean isPresent() {
      return isPresent;
   }

   @Override
   public Possible<T> or(Possible<T> alternate) {
      return isPresent ? this : alternate;
   }

   @Override
   public <U> Possible<U> transform(Function<T, U> function) {
      return isPresent
            ? Reference.set(function.apply(value))
            : Reference.<U>unset();
   }

   @Override
   public Possible<T> filter(Predicate<T> predicate) {
      return isPresent && predicate.apply(value)
            ? Reference.set(value)
            : Reference.<T>unset();
   }

   @Override
   public T get() {
      if (!isPresent) {
         throw new IllegalStateException();
      }
      return value;
   }

   @Override
   public T getOr(T alternate) {
      return isPresent ? value : alternate;
   }

   @Override
   public <X extends Throwable> T getOr(X throwable) throws X {
      if (!isPresent) {
         throw throwable;
      }
      return value;
   }

   @Override
   public Set<T> asSet() {
      return isPresent ? Collections.singleton(value) : Collections.<T>emptySet();
   }

   @Override
   public <R> R visit(Possible.Visitor<T, R> visitor) {
      return isPresent ? visitor.present(value) : visitor.absent();
   }
}
