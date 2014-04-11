package com.apriori.possible;

import com.apriori.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A possible value. A value might be present, but it may not be. This interface provides basically
 * the same API as {@link Optional}, but as an interface instead of a class. Other implementations
 * are provided in this package, including ones that allow present {@code null} values and ones that
 * are mutable.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the possible value
 */
// TODO: review docs!
public interface Possible<T> {
   
   /**
    * Returns true if a value is present.
    * 
    * @return true if a value is present; false otherwise.
    */
   boolean isPresent();
   
   /**
    * Returns the current possible value if present or the specified value if not.
    * 
    * @param alternate an alternate value
    * @return returns the current possible value if present or the alternate if not
    */
   Possible<T> or(Possible<T> alternate);
   
   /**
    * Returns the current possible value, transformed by the specified function, if present.
    * 
    * @param function the function used to transform the value
    * @return returns the current value, transformed by the specified function, if present;
    *       otherwise returns a possible value that is absent
    */
   <U> Possible<U> map(Function<? super T, ? extends U> function);
   
   <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function);
   
   /**
    * Filters the current value per the specified predicate. If a value is present and it matches
    * the specified predicate then it is returned. Otherwise a possible value that is absent is
    * returned.
    * 
    * @param predicate the predicated used to test the value
    * @return returns the current value if present and it matches the specified predicate; otherwise
    *       returns a possible value that is absent
    */
   Possible<T> filter(Predicate<? super T> predicate);
   
   /**
    * Gets the contained value if present.
    * 
    * @return the contained value
    * @throws IllegalStateException if a value is not present
    */
   T get();
   
   /**
    * Gets the contained value if present or the specified value if not.
    * 
    * @param alternate the alternate value
    * @return the contained value if present or the alternate if not
    */
   T orElse(T alternate);
   
   T orElseGet(Supplier<? extends T> supplier);
   
   /**
    * Gets the contained value or gets an exception from the specified source and throws it
    * 
    * @param throwableSupplier a supplier of the exception to throw if a value is not present
    * @return the contained value
    * @throws X if a value is not present
    */
   <X extends Throwable> T orElseThrow(Supplier<? extends X> throwableSupplier) throws X;
   
   void ifPresent(Consumer<? super T> consumer);
   
   /**
    * Returns a view of this possible value as a set. If a value is present, a singleton set with
    * that value is returned. Otherwise, an empty set is returned.
    * 
    * @return a singleton set with the contained value, if present; an empty set otherwise
    */
   Set<T> asSet();
   
   /**
    * Invokes the appropriate method on the specified visitor. Returns the value that the visitor
    * produces.
    * 
    * @param visitor a visitor
    * @return the value returned by the visitor
    */
   <R> R visit(Visitor<? super T, R> visitor);
   
   /**
    * A visitor of possible values. When visited, one of the methods will be invoked, depending on
    * whether or not a value is present.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of the possible value
    * @param <R> the type returned by the visitor
    */
   interface Visitor<T, R> {
      /**
       * Visits a possible value where a value is present.
       * 
       * @param t the contained value
       * @return the result of visiting the contained value
       */
      R present(T t);
      
      /**
       * Visits a possible value where no value is actually present.
       * 
       * @return the result of visiting a possible value where no value is actually present
       */
      R absent();
   }
   
   /**
    * Converts a possible {@code null} value to an optional one. If the given possible value is
    * present but {@code null}, an absent value is returned.
    * 
    * @param possible a possible value
    * @return the possible value, but with {@code null} values treated as absent
    */
   static <T> Possible<T> notNull(Possible<T> possible) {
      return possible.isPresent() ? Optionals.of(possible.get()) : Optionals.none();
   }
   
   /**
    * Returns a view of a future as a possible value. If the future is incomplete, fails, or is
    * cancelled then the value is not present. If and when the future completes successfully, the
    * value will become present. The actual value is the future's result.
    * 
    * @param future the future
    * @return a view of the future as a {@link Possible}
    */
   static <T> Possible<T> fromFuture(final Future<? extends T> future) {
      if (future instanceof ListenableFuture) {
         // ListenableFuture provides extra API that make this prettier/easier
         final ListenableFuture<? extends T> f = (ListenableFuture<? extends T>) future;
         return new Possible<T>() {
            
            @Override
            public boolean isPresent() {
               return f.isSuccessful();
            }
            
            @Override
            public void ifPresent(Consumer<? super T> consumer) {
               if (isPresent()) {
                  consumer.accept(f.getResult());
               }
            }

            @Override
            public Possible<T> or(Possible<T> alternate) {
               return f.isSuccessful() ? this : alternate;
            }
            
            // TODO: transform and filter should return views, not snapshots
            
            @Override
            public <U> Possible<U> map(Function<? super T, ? extends U> function) {
               return f.isSuccessful()
                     ? Reference.setTo(function.apply(f.getResult()))
                     : Reference.unset();
            }

            @Override
            public <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
               return f.isSuccessful()
                     ? function.apply(f.getResult())
                     : Reference.unset();
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
            public T orElse(T alternate) {
               return f.isSuccessful() ? f.getResult() : alternate;
            }

            @Override
            public T orElseGet(Supplier<? extends T> alternate) {
               return f.isSuccessful() ? f.getResult() : alternate.get();
            }

            @Override
            public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwable) throws X {
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
            public <R> R visit(Possible.Visitor<? super T, R> visitor) {
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
         public void ifPresent(Consumer<? super T> consumer) {
            if (isPresent()) {
               consumer.accept(value);
            }
         }

         // TODO: all API returning Possibles should return views instead of References
         
         @Override
         public Possible<T> or(Possible<T> alternate) {
            return isPresent() ? this : alternate;
         }

         @Override
         public <U> Possible<U> map(Function<? super T, ? extends U> function) {
            return isPresent()
                  ? Reference.setTo(function.apply(value))
                  : Reference.unset();
         }

         @Override
         public <U> Possible<U> flatMap(Function<? super T, ? extends Possible<U>> function) {
            return isPresent()
                  ? function.apply(value)
                  : Reference.unset();
         }

         @Override
         public Possible<T> filter(Predicate<? super T> predicate) {
            return isPresent() && predicate.test(value) ? this : Reference.unset();
         }

         @Override
         public T get() {
            if (!isPresent()) {
               throw new IllegalStateException();
            }
            return value;
         }

         @Override
         public T orElse(T alternate) {
            return isPresent() ? value : alternate;
         }

         @Override
         public T orElseGet(Supplier<? extends T> alternate) {
            return isPresent() ? value : alternate.get();
         }

         @Override
         public <X extends Throwable> T orElseThrow(Supplier<? extends X> throwable) throws X {
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
         public <R> R visit(Possible.Visitor<? super T, R> visitor) {
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
   static <T> List<T> presentOnly(Collection<? extends Possible<T>> possibles) {
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
   static <T> List<T> nullIfNotPresent(Collection<? extends Possible<T>> possibles) {
      List<T> present = new ArrayList<T>(possibles.size());
      for (Possible<T> p : possibles) {
         present.add(p.orElse(null));
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
   static <T> Possible<T> snapshot(Possible<T> possible) {
      return Reference.asReference(possible);
   }
}
