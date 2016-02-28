package com.bluegosling.possible;

import com.bluegosling.concurrent.unsafe.UnsafeReferenceFieldUpdater;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Utility implementations of {@link Fulfillable}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
final class Fulfillables {
   private Fulfillables() {
   }
   
   static class FulfillableImpl<T> extends AbstractDynamicPossible<T> implements Fulfillable<T> {
      @SuppressWarnings("rawtypes") // class tokens require use of raw types
      private static UnsafeReferenceFieldUpdater<FulfillableImpl, Object[]> updater =
            new UnsafeReferenceFieldUpdater<>(FulfillableImpl.class, Object[].class, "value");

      private volatile T[] value;
      
      FulfillableImpl() {
      }

      @Override
      public boolean isPresent() {
         return value != null;
      }
      
      @Override
      public boolean fulfill(T t) {
         @SuppressWarnings("unchecked")
         T v[] = (T[]) new Object[] { t };
         return updater.compareAndSet(this, null, v);
      }
      
      @Override
      public T get() {
         if (value == null) {
            throw new NoSuchElementException("not fulfilled");
         }
         return value[0];
      }
      
      @Override
      public Set<T> asSet() {
         // since it can never be unset, we can just return a singleton set if
         // the value has been fulfilled
         if (value != null) {
            return Collections.singleton(value[0]);
         }
         return super.asSet();
      }
   }
}
