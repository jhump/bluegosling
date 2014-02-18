package com.apriori.concurrent;

import com.apriori.possible.Fulfillable;
import com.apriori.possible.Possible;
import com.apriori.possible.Reference;
import com.apriori.util.Function;
import com.apriori.util.Predicate;
import com.apriori.util.Source;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A future whose result is set programmatically. This exposes protected API in
 * {@link AbstractListenableFuture} as public API. It also provides a view of the future as an
 * instance of {@link Fulfillable}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result
 */
public class SettableListenableFuture<T> extends AbstractListenableFuture<T> {

   @Override
   public boolean setValue(T result) {
      return super.setValue(result);
   }

   @Override
   public boolean setFailure(Throwable failure) {
      return super.setFailure(failure);
   }
   
   /**
    * Returns a view of this future as a {@link Fulfillable}. Fulfilling the returned object will
    * successfully complete the future.
    * 
    * @return a view of this future as a {@link Fulfillable}
    */
   public Fulfillable<T> asFulfillable() {
      return new Fulfillable<T>() {
         @Override
         public boolean isPresent() {
            return isSuccessful();
         }

         @Override
         public boolean fulfill(T value) {
            return setValue(value);
         }

         @Override
         public T get() {
            if (!isDone() || isCancelled()) {
               throw new IllegalStateException("not yet fulfilled");
            } else if (isFailed()) {
               throw new IllegalStateException("failed to fulfill", getFailure());
            }
            return getResult();
         }

         @Override
         public T getOr(T alternate) {
            return isPresent() ? getResult() : alternate;
         }

         @Override
         public <X extends Throwable> T getOrThrow(X throwable) throws X {
            if (isPresent()) {
               return getResult();
            }
            throw throwable;
         }

         @Override
         public <X extends Throwable> T getOrThrow(Source<X> throwable) throws X {
            if (isPresent()) {
               return getResult();
            }
            throw throwable.get();
         }

         @Override
         public Possible<T> or(Possible<T> alternate) {
            return isPresent() ? this : alternate;
         }

         // TODO: transform and filter should return views, not snapshots
         
         @Override
         public <U> Possible<U> transform(Function<? super T, ? extends U> function) {
            return isPresent() ? Reference.<U>setTo(function.apply(getResult()))
                  : Reference.<U>unset();
         }

         @Override
         public Possible<T> filter(Predicate<? super T> predicate) {
            return isPresent() && predicate.test(getResult()) ? this : Reference.<T>unset();
         }
         
         @Override
         public Set<T> asSet() {
            // since it can never be unset, we can just return a singleton set if
            // the value has been fulfilled
            if (isPresent()) {
               return Collections.singleton(getResult());
            }
            // otherwise, we return a view that is empty but will become a singleton
            // set once the value is fulfilled
            return new AbstractSet<T>() {
               @Override
               public Iterator<T> iterator() {
                  return new Iterator<T>() {
                     boolean consumed;
                     
                     @Override
                     public boolean hasNext() {
                        // This could possibly return true after returning false, but that should
                        // be harmless. Since a fulfillable can only be set once and cannot be
                        // cleared, it can never return false after returning true.
                        return isPresent() && !consumed;
                     }

                     @Override
                     public T next() {
                        if (consumed || !isPresent()) {
                           throw new NoSuchElementException();
                        }
                        consumed = true;
                        return get();
                     }

                     @Override
                     public void remove() {
                        throw new UnsupportedOperationException();
                     }
                  };
               }

               @Override
               public int size() {
                  return isPresent() ? 1 : 0;
               }
            };
         }

         @Override
         public <R> R visit(Possible.Visitor<? super T, R> visitor) {
            return isPresent() ? visitor.present(getResult()) : visitor.absent();
         }
      };
   }
}
