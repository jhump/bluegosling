package com.apriori.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

//TODO:javadoc
//TODO:test
public class Fulfillables {

   public static <T> Fulfillable<T> fulfilled(final T value) {
      return new Fulfillable<T>() {

         @Override
         public boolean isFulfilled() {
            return true;
         }

         @Override
         public boolean fulfill(T t) {
            return false;
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
         public Set<T> asSet() {
            return Collections.singleton(value);
         }
      };
   }
   
   public static <T> Fulfillable<T> create() {
      return new Fulfillable<T>() {
         private final AtomicBoolean set = new AtomicBoolean();
         private volatile T t;
         
         @Override
         public boolean isFulfilled() {
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
         public T get() {
            if (!set.get()) {
               throw new IllegalStateException("not fulfilled");
            }
            return t;
         }
         
         @Override
         public T getOr(T other) {
            return set.get() ? t : other;
         }
         
         @Override
         public Set<T> asSet() {
            return set.get() ? Collections.singleton(t) : Collections.<T>emptySet();
         }
      };
   }
}
