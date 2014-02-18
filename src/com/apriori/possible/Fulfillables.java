package com.apriori.possible;

import com.apriori.util.Function;
import com.apriori.util.Predicate;
import com.apriori.util.Source;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Factory methods for creating instances of {@link Fulfillable}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
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
         public <U> Possible<U> transform(Function<? super T, ? extends U> function) {
            return Reference.<U>setTo(function.apply(value));
         }

         @Override
         public Possible<T> filter(Predicate<? super T> predicate) {
            return predicate.test(value) ? this : Reference.<T>unset();
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
         public <X extends Throwable> T getOrThrow(Source<X> throwable) throws X {
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
   
   private static class FulfillableImpl<T> implements Fulfillable<T> {
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
      public Possible<T> or(Possible<T> alternate) {
         return value != null ? this : alternate;
      }

      // TODO: transform and filter should return views, not snapshots

      @Override
      public <U> Possible<U> transform(Function<? super T, ? extends U> function) {
         return value != null
               ? Reference.<U>setTo(function.apply(value.get()))
               : Reference.<U>unset();
      }

      @Override
      public Possible<T> filter(Predicate<? super T> predicate) {
         return value != null && predicate.test(value.get()) ? this : Reference.<T>unset();
      }
      
      @Override
      public T get() {
         if (value == null) {
            throw new IllegalStateException("not fulfilled");
         }
         return value.get();
      }
      
      @Override
      public T getOr(T other) {
         return value != null ? value.get() : other;
      }
      
      @Override
      public <X extends Throwable> T getOrThrow(X throwable) throws X {
         if (value == null) {
            throw throwable;
         }
         return value.get();
      }

      @Override
      public <X extends Throwable> T getOrThrow(Source<X> throwable) throws X {
         if (value == null) {
            throw throwable.get();
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
         return value != null ? visitor.present(value.get()) : visitor.absent();
      }
   }
}
