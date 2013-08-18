package com.apriori.concurrent;

import com.apriori.possible.Fulfillable;

/**
 * A future whose result is set programmatically. This merely exposes protected API in
 * {@link SimpleListenableFuture} as public API.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result
 */
public class SettableListenableFuture<T> extends SimpleListenableFuture<T> {

   @Override
   public boolean setValue(T result) {
      return super.setValue(result);
   }

   @Override
   public boolean setFailure(Throwable failure) {
      return super.setFailure(failure);
   }
   
   @Override
   public Fulfillable<T> asFulfillable() {
      return super.asFulfillable();
   }
}
