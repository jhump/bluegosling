package com.apriori.possible;

import com.apriori.concurrent.ListenableFuture;
import com.apriori.util.Function;
import com.apriori.util.Predicate;
import com.apriori.util.Source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Utility methods for instances of {@link Possible}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public final class Possibles {
   private Possibles() {
   }
   
   /**
    * Returns a view of a future as a possible value. If the future is incomplete, fails, or is
    * cancelled then the value is not present. If and when the future completes successfully, the
    * value will become present. The actual value is the future's result.
    * 
    * @param future the future
    * @return a view of the future as a {@link Possible}
    */
   public static <T> Possible<T> fromFuture(final Future<? extends T> future) {
      if (future instanceof ListenableFuture) {
         // ListenableFuture provides extra API that make this prettier/easier
         final ListenableFuture<? extends T> f = (ListenableFuture<? extends T>) future;
         return new Possible<T>() {
            
            @Override
            public boolean isPresent() {
               return f.isSuccessful();
            }

            @Override
            public Possible<T> or(Possible<T> alternate) {
               return f.isSuccessful() ? this : alternate;
            }

            @Override
            public <U> Possible<U> transform(Function<? super T, ? extends U> function) {
               return f.isSuccessful()
                     ? Reference.<U>setTo(function.apply(f.getResult()))
                     : Reference.<U>unset();
            }

            @Override
            public Possible<T> filter(Predicate<? super T> predicate) {
               return f.isSuccessful() && predicate.test(f.getResult())
                     ? this : Reference.<T>unset();
            }

            @Override
            public T get() {
               if (!f.isSuccessful()) {
                  throw new IllegalStateException();
               }
               return f.getResult();
            }

            @Override
            public T getOr(T alternate) {
               return f.isSuccessful() ? f.getResult() : alternate;
            }

            @Override
            public <X extends Throwable> T getOrThrow(X throwable) throws X {
               if (!f.isSuccessful()) {
                  throw throwable;
               }
               return f.getResult();
            }

            @Override
            public <X extends Throwable> T getOrThrow(Source<X> throwable) throws X {
               if (!f.isSuccessful()) {
                  throw throwable.get();
               }
               return f.getResult();
            }

            @Override
            public Set<T> asSet() {
               return f.isSuccessful() ? Collections.<T>singleton(f.getResult())
                     : Collections.<T>emptySet();
            }

            @Override
            public <R> R visit(Possible.Visitor<T, R> visitor) {
               return f.isSuccessful() ? visitor.present(f.getResult()) : visitor.absent();
            }
         };
      }
      // if not a ListenableFuture, then it's a little uglier...
      return new Possible<T>() {
         private volatile Boolean isPresent;
         private volatile T value;
         
         private synchronized boolean determineIfPresent() {
            if (isPresent == null) {
               boolean interrupted = false;
               while (true) {
                  try {
                     value = future.get();
                     isPresent = true;
                     break;
                  } catch (InterruptedException e) {
                     interrupted = true;
                  } catch (ExecutionException e) {
                     isPresent = false;
                     break;
                  } catch (CancellationException e) {
                     isPresent = false;
                     break;
                  }
               }
               if (interrupted) {
                  Thread.currentThread().interrupt();
               }
            }
            return isPresent;
         }
         
         @Override
         public boolean isPresent() {
            return future.isDone() && determineIfPresent();
         }

         @Override
         public Possible<T> or(Possible<T> alternate) {
            return isPresent() ? this : alternate;
         }

         @Override
         public <U> Possible<U> transform(Function<? super T, ? extends U> function) {
            return isPresent()
                  ? Reference.<U>setTo(function.apply(value))
                  : Reference.<U>unset();
         }

         @Override
         public Possible<T> filter(Predicate<? super T> predicate) {
            return isPresent() && predicate.test(value) ? this : Reference.<T>unset();
         }

         @Override
         public T get() {
            if (!isPresent()) {
               throw new IllegalStateException();
            }
            return value;
         }

         @Override
         public T getOr(T alternate) {
            return isPresent() ? value : alternate;
         }

         @Override
         public <X extends Throwable> T getOrThrow(X throwable) throws X {
            if (!isPresent()) {
               throw throwable;
            }
            return value;
         }

         @Override
         public <X extends Throwable> T getOrThrow(Source<X> throwable) throws X {
            if (!isPresent()) {
               throw throwable.get();
            }
            return value;
         }

         @Override
         public Set<T> asSet() {
            return isPresent() ? Collections.<T>singleton(value) : Collections.<T>emptySet();
         }

         @Override
         public <R> R visit(Possible.Visitor<T, R> visitor) {
            return isPresent() ? visitor.present(value) : visitor.absent();
         }
      };
   }

   /**
    * Returns a list of the present values from the specified collection of possible values.
    * Possible values where no value is present are excluded. So the resulting list will have fewer
    * elements than the specified collection in the event that not all values are present. The order
    * of items in the list matches the iteration order of the specified collection.
    * 
    * @param possibles a collection of possible values
    * @return a list of present values
    */
   public static <T> List<T> presentOnly(Collection<? extends Possible<T>> possibles) {
      List<T> present = new ArrayList<T>(possibles.size());
      for (Possible<T> p : possibles) {
         if (p.isPresent()) {
            try {
               present.add(p.get());
            } catch (IllegalStateException e) {
               // this should happen rarely if ever, but could occur with a mutable object if a
               // race occurs and the value becomes absent after the call to isPresent()
            }
         }
      }
      return present;
   }

   /**
    * Returns a list of the values, {@code null} if no value is present, from the specified
    * collection of possible values. The resulting list will always have exactly the same number of
    * elements as the specified collection, even if not all values are present. The order of items
    * in the list matches the iteration order of the specified collection.
    * 
    * @param possibles a collection of possible values
    * @return a list of values extracted from the collection, {@code null} for objects where a value
    *       was not present 
    */
   public static <T> List<T> nullIfNotPresent(Collection<? extends Possible<T>> possibles) {
      List<T> present = new ArrayList<T>(possibles.size());
      for (Possible<T> p : possibles) {
         present.add(p.getOr(null));
      }
      return present;
   }
   
   /**
    * Captures the current possible value into a new immutable possible value. The returned object
    * is an immutable copy of the specified value. If the specified object is mutable and later
    * changes, those changes will not be reflected in the returned snapshot.
    * 
    * @param possible a possible value
    * @return an immutable snapshot of the possible value
    */
   public static <T> Possible<T> snapshot(Possible<T> possible) {
      if (possible.isPresent()) {
         try {
            return Reference.setTo(possible.get());
         } catch (IllegalStateException e) {
            // just in case there's a race condition such that value becomes absent in between
            // calls to isPresent() and get()
         }
      }
      return Reference.unset();
   }
}
