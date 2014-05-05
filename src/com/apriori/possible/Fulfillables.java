package com.apriori.possible;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

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
      private static AtomicReferenceFieldUpdater<FulfillableImpl, Reference> updater =
            AtomicReferenceFieldUpdater.newUpdater(FulfillableImpl.class, Reference.class, "value");

      private volatile Reference<T> value;
      
      FulfillableImpl() {
      }

      @Override
      public boolean isPresent() {
         return value != null;
      }
      
      @Override
      public boolean fulfill(T t) {
         return updater.compareAndSet(this, null, Reference.setTo(t));
      }
      
      @Override
      public T get() {
         if (value == null) {
            throw new NoSuchElementException("not fulfilled");
         }
         return value.get();
      }
      
      @Override
      public Set<T> asSet() {
         // since it can never be unset, we can just return a singleton set if
         // the value has been fulfilled
         if (value != null) {
            return Collections.singleton(value.get());
         }
         return super.asSet();
      }
   }
}
