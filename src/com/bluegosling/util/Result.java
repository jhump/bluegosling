package com.bluegosling.util;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.function.TriFunction;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The result of an operation, which can either represent a successful value or a cause of failure.
 * In some respects, this like a {@link Future}, except the result is available immediately. In
 * other respects, it is like an {@link Optional} with extra information (cause of failure) when
 * the value is not present.
 * 
 * @param <T> the type of value
 */
public abstract class Result<T, X extends Throwable> {
   /**
    * Returns a successful result.
    *
    * @param t the value
    * @return a successful result
    */
   public static <T, X extends Throwable> Result<T, X> successful(T t) {
      return new Success<>(t);
   }

   /**
    * Returns a failed result.
    *
    * @param failure the cause of failure
    * @return a failed result
    */
   public static <T, X extends Throwable> Result<T, X> failed(X failure) {
      return new Failure<>(requireNonNull(failure));
   }

   /**
    * Returns a result with the same disposition as the given completed future. If the given
    * future was cancelled, the result will have a {@link CancellationException} as its cause of
    * failure.
    *
    * @param f a future
    * @return a result with the same disposition as the given future
    * @throws IllegalStateException if the given future is not completed
    */
   public static <T> Result<T, Throwable> fromCompletedFuture(Future<T> f) {
      if (!f.isDone()) {
         throw new IllegalStateException();
      }
      if (f instanceof FluentFuture) {
         FluentFuture<T> lf = (FluentFuture<T>) f;
         if (lf.isSuccessful()) {
            return successful(lf.getResult());
         } else if (lf.isCancelled()) {
            return failed(new CancellationException());
         } else {
            assert lf.isFailed();
            return failed(lf.getFailure());
         }
      } else {
         boolean interrupted = false;
         try {
            while (true) {
               try {
                  return successful(f.get());
               } catch (ExecutionException e) {
                  return failed(e.getCause());
               } catch (CancellationException e) {
                  return failed(e);
               } catch (InterruptedException e) {
                  interrupted = true;
               }
            }
         } finally {
            if (interrupted) {
               Thread.currentThread().interrupt();
            }
         }
      }
   }
   
   /**
    * Returns a future that represents the same value or failure as this result.
    * 
    * @return a future that represents the same value or failure as this result
    */
   public FluentFuture<T> asFuture() {
      return isFailed()
            ? FluentFuture.failedFuture(getFailure())
            : FluentFuture.completedFuture(get());
   }

   /**
    * Gets the value of this result.
    *
    * @return the result value
    * @throws NoSuchElementException if this result represents a failure
    */
   public abstract T get();
   
   /**
    * Gets the value of this result or throws the cause of failure.
    * 
    * @return the result value if present
    * @throws X the cause of failure if this result is not successful
    */
   public abstract T checkedGet() throws X;
   
   /**
    * Gets the result's cause of failure.
    *
    * @return the cause of failure
    * @throws IllegalStateException if this result is successful
    */
   public abstract X getFailure();
   
   /**
    * Determines if this result is successful or not.
    *
    * @return true if this result is successful
    */
   public abstract boolean isSuccessful();
   
   /**
    * Determines if this result is a failure or not.
    *
    * @return true if this result is a failure
    */
   public abstract boolean isFailed();
   
   /**
    * Returns a new result whose value is the result of mapping this result's value using the given
    * function. If this result is a failure then the returned result has the same cause of failure.
    * 
    * @param fn mapping function
    * @return a new result whose value is mapped via the given function
    */
   public abstract <U> Result<U, X> map(Function<? super T, ? extends U> fn);
   
   /**
    * Returns a new result that is the result of mapping this result's value using the given
    * function. If this result is a failure then the returned result has the same cause of failure.
    * 
    * @param fn mapping function, from value to result object
    * @return a new result, mapped via the given function
    */
   public abstract <U> Result<U, X> flatMap(
         Function<? super T, ? extends Result<? extends U, ? extends X>> fn);
   
   /**
    * Returns a new result whose cause of failure is the result of mapping this result's cause of
    * failure using the given function. If this result is a success then the returned result has the
    * same value.
    * 
    * @param fn mapping function, from one cause of failure to another
    * @return a new result whose cause of failure is mapped via the given function
    */
   public abstract <Y extends Throwable> Result<T, Y> mapException(
         Function<? super X, ? extends Y> fn);

   /**
    * Recovers a failed result into a successful one. If this result is a failure, it is mapped to
    * a successful value using the given function and a successful result is returned. If this
    * result is a success then the returned result has the same value.
    * 
    * @param fn a mapping function, from a cause of failure to a successful value
    * @return a new result with the same value as this result or with a value provided by the given
    *       function if this result is a failure
    */
   public abstract Result<T, X> recover(Function<? super X, ? extends T> fn);
   
   /**
    * Gets the value of this result or the given value if this result is a failure.
    * 
    * @param other another value to use if this result is a failure
    * @return the result value or the given value if this result is a failure
    */
   public abstract T orElse(T other);
   
   /**
    * Gets the value of this result or the value returned by the given supplier if this result is a
    * failure.
    * 
    * @param other a supplier of another value to use if this result is a failure
    * @return the result value or a the value returned by the given supplier
    */
   public abstract T orElseGet(Supplier<? extends T> other);
   
   /**
    * Gets the value of this result or throws an exception provided by a given supplier.
    * 
    * @param failure a supplier of an exception to throw if this result is not successful
    * @return the result value if this result is a success
    * @throws Y if this result is a failure
    */
   public abstract <Y extends Throwable> T orElseThrow(Supplier<? extends Y> failure) throws Y;
   
   /**
    * Invokes the given action if this result is a success.
    * 
    * @param consumer an action that accepts this result's value
    */
   public abstract void ifSuccessful(Consumer<? super T> consumer);
   
   /**
    * Invokes the given action if this result is a failure.
    * 
    * @param consumer an action that accepts this result's cause of failure
    */
   public abstract void ifFailed(Consumer<? super X> consumer);
   
   /**
    * Combines this result with another. If either result is a failure, the returned result is a
    * failure with the same cause. If both are failures, this result's cause is used.
    * 
    * @param other another result
    * @param fn a function that combines the values of this result and the given one
    * @return a new result whose value is the combination of this result and the given one or has
    *       the same cause of failure as this result or the given one
    */
   @SuppressWarnings("unchecked")
   public <U, V> Result<V, X> combineWith(Result<U, X> other,
         BiFunction<? super T, ? super U, ? extends V> fn) {
      if (isFailed()) {
         return (Result<V, X>) this;
      } else if (other.isFailed()) {
         return (Result<V, X>) other;
      }
      return Result.successful(fn.apply(get(), other.get()));
   }

   /**
    * Combines this result with two others. If any result is a failure, the returned result is a
    * failure with the same cause.
    * 
    * @param other1 another result
    * @param other1 yet another result
    * @param fn a function that combines the values of this result and the given two
    * @return a new result whose value is the combination of this result and the given two or has
    *       the same cause of failure as this result or of one of the given two
    */
   @SuppressWarnings("unchecked")
   public <U, V, W> Result<W, X> combineWith(Result<U, X> other1, Result<V, X> other2,
         TriFunction<? super T, ? super U, ? super V, ? extends W> fn) {
      if (isFailed()) {
         return (Result<W, X>) this;
      } else if (other1.isFailed()) {
         return (Result<W, X>) other1;
      } else if (other2.isFailed()) {
         return (Result<W, X>) other2;
      }
      return Result.successful(fn.apply(get(), other1.get(), other2.get()));
   }

   /**
    * Combines an array of results into a result whose value is a list. If any result is a failure,
    * the returned result is a failure with the same cause.
    * 
    * @param results the results to join
    * @return a new result whose value is the list of values for the given results or has
    *       the same cause of failure as one of the given results
    */
   @SafeVarargs
   static <T, X extends Throwable> Result<List<T>, X> join(
         Result<? extends T, ? extends X>... results) {
      return join(Arrays.asList(results));
   }
   
   /**
    * Combines a collection of results into a result whose value is a list. If any result is a
    * failure, the returned result is a failure with the same cause.
    * 
    * @param results the results to join
    * @return a new result whose value is the list of values for the given results or has
    *       the same cause of failure as one of the given results
    */
   @SuppressWarnings("unchecked")
   static <T, X extends Throwable> Result<List<T>, X> join(
         Iterable<? extends Result<? extends T, ? extends X>> results) {
      List<T> l = new ArrayList<>();
      Result<?, ? extends X> failure = null;
      for (Result<? extends T, ? extends X> i : results) {
         if (i.isFailed()) {
            failure = i;
            break;
         } else {
            l.add(i.get());
         }
      }
         
      if (failure != null) {
         return (Result<List<T>, X>) failure;
      }
      
      return Result.successful(Collections.unmodifiableList(l));
   }
   
   /**
    * Returns the first successful result from the given array.
    * 
    * @param results an array of results
    * @return the first successful result
    */
   @SafeVarargs
   static <T, X extends Throwable> Result<T, X> firstSuccessfulOf(
         Result<? extends T, ? extends X>... results) {
      return firstSuccessfulOf(Arrays.asList(results));
   }

   /**
    * Returns the first successful result from the given collection.
    * 
    * @param results a collection of results
    * @return the first successful result as returned by the given collection's iteration order
    */
   static <T, X extends Throwable> Result<T, X> firstSuccessfulOf(
         Iterable<? extends Result<? extends T, ? extends X>> results) {
      Result<T, X> failure = null;
      for (Result<? extends T, ? extends X> i : results) {
         if (i.isFailed()) {
            failure = cast(i);
         } else {
            return cast(i);
         }
      }
         
      if (failure != null) {
         return failure;
      }
      
      throw new NoSuchElementException();
   }

   /**
    * Extracts the result from a result whose value is also a result object. If this result is a
    * failure then the returned result has the same cause of failure.
    * 
    * @param result a result, extracted from the given result object
    * @return a result whose value is extracted from the given object or whose cause of failure is
    *       the same as the given result
    */
   @SuppressWarnings("unchecked")
   static <T, X extends Throwable> Result<T, X> dereference(
         Result<? extends Result<T, X>, ? extends X> result) {
      if (result.isFailed()) {
         return (Result<T, X>) result;
      } else {
         return result.get();
      }
   }

   /**
    * Casts an immediate value to a super-type. The compiler enforces invariance on generic types,
    * but this method allows immediate values to be treated as covariant.
    *
    * @param immediate an immediate value
    * @return the same immediate value, but with its type parameter as a super-type of the original
    */
   static <T, U extends T, X extends Throwable, Y extends X> Result<T, X> cast(
         Result<U, Y> result) {
      // co-variance makes it safe since all operations return a T or X, none take a T or X
      @SuppressWarnings("unchecked")
      Result<T, X> cast = (Result<T, X>) result;
      return cast;
   }

   /**
    * A successful result.
    *
    * @param <T> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Success<T, X extends Throwable> extends Result<T, X> {
      private final T t;
      
      Success(T t) {
         this.t = t;
      }
      
      @Override
      public T get() {
         return t;
      }

      @Override
      public T checkedGet() {
         return t;
      }

      @Override
      public X getFailure() {
         throw new IllegalStateException();
      }

      @Override
      public boolean isSuccessful() {
         return true;
      }

      @Override
      public boolean isFailed() {
         return false;
      }

      @Override
      public <U> Result<U, X> map(Function<? super T, ? extends U> fn) {
         return Result.successful(fn.apply(t));
      }

      @Override
      public <U> Result<U, X> flatMap(
            Function<? super T, ? extends Result<? extends U, ? extends X>> fn) {
         return cast(fn.apply(t));
      }

      @SuppressWarnings("unchecked")
      @Override
      public <Y extends Throwable> Result<T, Y> mapException(Function<? super X, ? extends Y> fn) {
         return (Result<T, Y>) this;
      }

      @Override
      public Result<T, X> recover(Function<? super X, ? extends T> fn) {
         return this;
      }
      
      @Override
      public T orElse(T other) {
         return t;
      }

      @Override
      public T orElseGet(Supplier<? extends T> other) {
         return t;
      }

      @Override
      public <Y extends Throwable> T orElseThrow(Supplier<? extends Y> failure) throws Y {
         return t;
      }
      
      @Override
      public void ifSuccessful(Consumer<? super T> consumer) {
         consumer.accept(t);
      }

      @Override
      public void ifFailed(Consumer<? super X> consumer) {
      }
   }
   
   /**
    * A failed result.
    *
    * @param <T> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Failure<T, X extends Throwable> extends Result<T, X> {
      private final X failure;
      
      Failure(X failure) {
         this.failure = failure;
      }
      
      @Override
      public T get() {
         throw new NoSuchElementException();
      }
      
      @Override
      public T checkedGet() throws X {
         throw failure;
      }

      @Override
      public X getFailure() {
         return failure;
      }

      @Override
      public boolean isSuccessful() {
         return false;
      }

      @Override
      public boolean isFailed() {
         return true;
      }

      @SuppressWarnings("unchecked")
      @Override
      public <U> Result<U, X> map(Function<? super T, ? extends U> fn) {
         return (Result<U, X>) this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public <U> Result<U, X> flatMap(
            Function<? super T, ? extends Result<? extends U, ? extends X>> fn) {
         return (Result<U, X>) this;
      }

      @Override
      public <Y extends Throwable> Result<T, Y> mapException(Function<? super X, ? extends Y> fn) {
         return Result.failed(fn.apply(failure));
      }

      @Override
      public Result<T, X> recover(Function<? super X, ? extends T> fn) {
         return Result.successful(fn.apply(failure));
      }  
      
      @Override
      public T orElse(T other) {
         return other;
      }

      @Override
      public T orElseGet(Supplier<? extends T> other) {
         return other.get();
      }

      @Override
      public <Y extends Throwable> T orElseThrow(Supplier<? extends Y> failure) throws Y {
         throw failure.get();
      }
      
      @Override
      public void ifSuccessful(Consumer<? super T> consumer) {
      }

      @Override
      public void ifFailed(Consumer<? super X> consumer) {
         consumer.accept(failure);
      }
   }
}
