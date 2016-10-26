package com.bluegosling.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.function.TriFunction;
import com.bluegosling.possible.Possible;
import com.bluegosling.possible.Reference;
import com.bluegosling.util.ValueType;
import com.google.common.base.Objects;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The result of an operation, which can either represent a successful value or a cause of failure.
 * 
 * <p>In some respects, this is like a {@link Future}, except the result is available immediately.
 * In other respects, it is like an {@link Optional} with extra information (cause of failure) when
 * the value is not present. Its API has similarities with both {@link Optional} and
 * {@link FluentFuture}.
 * 
 * @param <T> the type of value, for successful results
 * @param <E> the type of error, for failed results (typically, but not necessarily, a sub-type of
 *       {@link Throwable})
 */
@ValueType
//For efficiency, we store the value or error in a single Object field and then must cast to type
//variable T or E (which is an unchecked cast). This is safe due to the invariant ensured by the
//factory methods that create instances: if success then value is a T, otherwise it's an E.
@SuppressWarnings("unchecked")
public final class Result<T, E> implements Serializable {
   private static final long serialVersionUID = -5305908494573148254L;
   
   private final boolean success;
   private final Object value;
   
   private Result(boolean success, Object value) {
      this.success = success;
      this.value = value;
   }

   /**
    * A visitor of results, for implementing the visitor pattern (double-dispatch) with results.
    *
    * @param <T> the type of result value
    * @param <E> the type of result error
    * @param <R> the type returned by the visitor
    */
   public interface Visitor<T, E, R> {
      /**
       * Visits the value of a successful result.
       * 
       * @param t the result value
       * @return the product of the visit (can be {@code null})
       */
      R visitValue(T t);

      /**
       * Visits the value of a failed result.
       * 
       * @param e the result error
       * @return the product of the visit (can be {@code null})
       */
      R visitError(E e);
   }

   /**
    * Returns a successful result.
    *
    * @param t the value
    * @return a successful result
    */
   public static <T, E> Result<T, E> ok(T t) {
      return new Result<>(true, t);
   }

   /**
    * Returns a failed result.
    *
    * @param failure the cause of failure
    * @return a failed result
    */
   public static <T, E> Result<T, E> error(E failure) {
      return new Result<>(false, failure);
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
            return ok(lf.getResult());
         } else if (lf.isCancelled()) {
            return error(new CancellationException());
         } else {
            assert lf.isFailed();
            return error(lf.getFailure());
         }
      } else {
         boolean interrupted = false;
         try {
            while (true) {
               try {
                  return ok(f.get());
               } catch (ExecutionException e) {
                  return error(e.getCause());
               } catch (CancellationException e) {
                  return error(e);
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
    * Returns a future that represents the same value or failure as this result. If this result
    * represents a failure then the future will be a failed future. In that case, if the cause of
    * failure is a {@link Throwable}, that is is the cause of failure for the returned future, too.
    * Otherwise, the cause of failure for the future is a {@link FailedResultException} that wraps
    * the result's cause of failure.
    * 
    * @return a future that represents the same value or failure as this result
    */
   public FluentFuture<T> asFuture() {
      if (isFailed()) {
         E e = getFailure();
         return e instanceof Throwable
               ? FluentFuture.failedFuture((Throwable) e)
               : FluentFuture.failedFuture(new FailedResultException(e));
      }
      return FluentFuture.completedFuture(get());
   }

   /**
    * Gets the value of this result.
    *
    * @return the result value
    * @throws NoSuchElementException if this result represents a failure
    */
   public T get() {
      if (success) {
         return (T) value;
      }
      throw new NoSuchElementException();
   }
   
   /**
    * Gets the value of the given result or throws the cause of failure.
    * 
    * @param r the result
    * @return the result's value, if present
    * @throws E the cause of failure if this result is not successful
    * @throws NullPointerException if the result is not successful and its cause of failure is
    *       {@code null}
    */
   public static <T, E extends Throwable> T checkedGet(Result<? extends T, ? extends E> r)
         throws E {
      if (r.isSuccessful()) {
         return r.get();
      }
      throw r.getFailure();
   }
   
   /**
    * Gets the result's cause of failure.
    *
    * @return the cause of failure
    * @throws IllegalStateException if this result is successful
    */
   public E getFailure() {
      if (success) {
         throw new IllegalStateException();
      }
      return (E) value;
   }
   
   /**
    * Determines if this result is successful or not.
    *
    * @return true if this result is successful
    */
   public boolean isSuccessful() {
      return success;
   }
   
   /**
    * Determines if this result is a failure or not.
    *
    * @return true if this result is a failure
    */
   public boolean isFailed() {
      return !success;
   }
   
   /**
    * Returns a new result whose value is the result of mapping this result's value using the given
    * function. If this result is a failure then the returned result has the same cause of failure.
    * 
    * @param fn mapping function
    * @return a new result whose value is mapped via the given function
    */
   public <U> Result<U, E> map(Function<? super T, ? extends U> fn) {
      return success ? ok(fn.apply((T) value)) : (Result<U, E>) this;
   }
   
   /**
    * Returns a new result that is the result of mapping this result's value using the given
    * function. If this result is a failure then the returned result has the same cause of failure.
    * 
    * @param fn mapping function, from value to result object
    * @return a new result, mapped via the given function
    */
   public <U> Result<U, E> flatMap(
         Function<? super T, Result<? extends U, ? extends E>> fn) {
      return success ? cast(fn.apply((T) value)) : (Result<U, E>) this;
   }
   
   /**
    * Returns a new result whose cause of failure is the result of mapping this result's cause of
    * failure using the given function. If this result is a success then the returned result has the
    * same value.
    * 
    * @param fn mapping function, from one cause of failure to another
    * @return a new result whose cause of failure is mapped via the given function
    */
   public <F> Result<T, F> mapError(Function<? super E, ? extends F> fn) {
      return success ? (Result<T, F>) this : error(fn.apply((E) value));
   }

   /**
    * Recovers a failed result into a successful one. If this result is a failure, it is mapped to
    * a successful value using the given function and a successful result is returned. If this
    * result is a success then the returned result has the same value.
    * 
    * @param fn a mapping function, from a cause of failure to a successful value
    * @return a new result with the same value as this result or with a value provided by the given
    *       function if this result is a failure
    */
   public Result<T, E> recover(Function<? super E, ? extends T> fn) {
      return success ? this : ok(fn.apply((E) value));
   }
   
   /**
    * Gets the value of this result or the given value if this result is a failure.
    * 
    * @param other another value to use if this result is a failure
    * @return the result value or the given value if this result is a failure
    */
   public T orElse(T other) {
      return success ? (T) value : other;
   }
   
   /**
    * Gets the value of this result or the value returned by the given supplier if this result is a
    * failure.
    * 
    * @param other a supplier of another value to use if this result is a failure
    * @return the result value or a the value returned by the given supplier
    */
   public T orElseGet(Supplier<? extends T> other) {
      return success ? (T) value : other.get();
   }
   
   /**
    * Gets the value of this result or throws an exception provided by a given supplier.
    * 
    * @param failure a supplier of an exception to throw if this result is not successful
    * @return the result value if this result is a success
    * @throws Y if this result is a failure
    */
   public <Y extends Throwable> T orElseThrow(Supplier<? extends Y> failure) throws Y {
      if (!success) {
         throw failure.get();
      }
      return (T) value;
   }
   
   /**
    * Invokes the given action if this result is a success.
    * 
    * @param consumer an action that accepts this result's value
    */
   public void ifSuccessful(Consumer<? super T> consumer) {
      if (success) {
         consumer.accept((T) value);
      }
   }
   
   /**
    * Invokes the given action if this result is a failure.
    * 
    * @param consumer an action that accepts this result's cause of failure
    */
   public void ifFailed(Consumer<? super E> consumer) {
      if (!success) {
         consumer.accept((E) value);
      }
   }
   
   /**
    * Combines this result with another. If either result is a failure, the returned result is a
    * failure with the same cause. If both are failures, this result's cause is used.
    * 
    * @param other another result
    * @param fn a function that combines the values of this result and the given one
    * @return a new result whose value is the combination of this result and the given one or has
    *       the same cause of failure as this result or the given one
    */
   public <U, V> Result<V, E> combineWith(Result<U, E> other,
         BiFunction<? super T, ? super U, ? extends V> fn) {
      if (isFailed()) {
         return (Result<V, E>) this;
      } else if (other.isFailed()) {
         return (Result<V, E>) other;
      }
      return Result.ok(fn.apply(get(), other.get()));
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
   public <U, V, W> Result<W, E> combineWith(Result<U, E> other1, Result<V, E> other2,
         TriFunction<? super T, ? super U, ? super V, ? extends W> fn) {
      if (isFailed()) {
         return (Result<W, E>) this;
      } else if (other1.isFailed()) {
         return (Result<W, E>) other1;
      } else if (other2.isFailed()) {
         return (Result<W, E>) other2;
      }
      return Result.ok(fn.apply(get(), other1.get(), other2.get()));
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
   @SuppressWarnings("varargs") // for javac
   public static <T, E> Result<List<T>, E> join(
         Result<? extends T, ? extends E>... results) {
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
   public static <T, E> Result<List<T>, E> join(
         Iterable<? extends Result<? extends T, ? extends E>> results) {
      List<T> l = new ArrayList<>();
      Result<?, ? extends E> failure = null;
      for (Result<? extends T, ? extends E> i : results) {
         if (i.isFailed()) {
            failure = i;
            break;
         } else {
            l.add(i.get());
         }
      }
         
      if (failure != null) {
         return (Result<List<T>, E>) failure;
      }
      
      return Result.ok(Collections.unmodifiableList(l));
   }
   
   /**
    * Returns the first successful result from the given array. If none are successful then the last
    * one is returned.
    * 
    * @param results an array of results
    * @return the first successful result or the last result if none are successful
    * @throws NoSuchElementException if the given array is empty
    */
   @SafeVarargs
   @SuppressWarnings("varargs") // for javac
   public static <T, E> Result<T, E> firstSuccessfulOf(
         Result<? extends T, ? extends E>... results) {
      return firstSuccessfulOf(Arrays.asList(results));
   }

   /**
    * Returns the first successful result from the given collection. If none are successful then the
    * last one is returned.
    * 
    * @param results a collection of results
    * @return the first successful result as returned by the given collection's iteration order or
    *       the last given result if none are successful
    * @throws NoSuchElementException if the given iterable has no results
    */
   public static <T, E> Result<T, E> firstSuccessfulOf(
         Iterable<? extends Result<? extends T, ? extends E>> results) {
      Result<T, E> failure = null;
      for (Result<? extends T, ? extends E> i : results) {
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
    * Returns a list of the given results' values, substituting {@code null} as the value for any
    * failed result. The values in the returned list are in the same order as iteration order of the
    * given collection. The returned list is always the same size as the given collection.
    * 
    * @param results the result objects
    * @return the list of result values, which may contain {@code null} placeholders for
    *       failed results
    */
   public static <T> List<T> nullIfFailed(
         Collection<? extends Result<? extends T, ?>> results) {
      List<T> present = new ArrayList<T>(results.size());
      for (Result<? extends T, ?> o : results) {
         present.add(o.orElse(null));
      }
      return Collections.unmodifiableList(present);
   }
   
   /**
    * Returns a list of the given results' values, omitting values for any failed results. The
    * values in the returned list are in the same order as iteration order of the given collection.
    * The returned list may be smaller than the given collection, and can even be empty if the
    * given collection contained no successful result objects.
    * 
    * @param results the result objects
    * @return the list of result values, containing values only for the given successful results
    */
   public static <T> List<T> successfulOnly(Collection<? extends Result<? extends T, ?>> results) {
      List<T> present = new ArrayList<>(results.size());
      for (Result<? extends T, ?> o : results) {
         if (o.isSuccessful()) {
            present.add(o.get());
         }
      }
      return Collections.unmodifiableList(present);
   }

   /**
    * Returns a list of the given results' errors, omitting errors for any successful results. The
    * errors in the returned list are in the same order as iteration order of the given collection.
    * The returned list may be smaller than the given collection, and can even be empty if the
    * given collection contained no failed result objects.
    * 
    * @param results the result objects
    * @return the list of result errors, containing values only for the given failed results
    */
   public static <E> List<E> failedOnly(Collection<? extends Result<?, ? extends E>> results) {
      List<E> present = new ArrayList<>(results.size());
      for (Result<?, ? extends E> o : results) {
         if (o.isFailed()) {
            present.add(o.getFailure());
         }
      }
      return Collections.unmodifiableList(present);
   }

   /**
    * Extracts the result from a result whose value is also a result object. If this result is a
    * failure then the returned result has the same cause of failure.
    * 
    * @param result a result, extracted from the given result object
    * @return a result whose value is extracted from the given object or whose cause of failure is
    *       the same as the given result
    */
   public static <T, E> Result<T, E> dereference(
         Result<? extends Result<T, E>, ? extends E> result) {
      if (result.isFailed()) {
         return (Result<T, E>) result;
      } else {
         return result.get();
      }
   }

   /**
    * Casts an immediate value to a super-type. The compiler enforces invariance on generic types,
    * but this method allows immediate values to be treated as covariant.
    *
    * @param result a result value
    * @return the same immediate value, but with its type parameter as a super-type of the original
    */
   public static <T, U extends T, E, F extends E> Result<T, E> cast(
         Result<U, F> result) {
      // co-variance makes it safe since all operations return a T or E, none take a T or E
      return (Result<T, E>) result;
   }
   
   /**
    * Visits this result using the given visitor. If this is a successful result then 
    * {@link Visitor#visitValue(Object) visitor.visitValue(v)} is invoked. Otherwise,
    * {@link Visitor#visitError(Object) visitor.visitError(e)} is invoked.
    * 
    * @param visitor the visitor
    * @return the product of the visit
    */
   public <R> R visit(Visitor<? super T, ? super E, ? extends R> visitor) {
      return success ? visitor.visitValue((T) value) : visitor.visitError((E) value);
   }

   /**
    * Returns the result's value, as an optional. If this is a failed result or a successful result
    * whose value is {@code null}, this returns {@linkplain Optional#empty() empty}. Otherwise, the
    * returned optional value holds the result's value.
    * 
    * @return an optional values that represents this result's value
    */
   public Optional<T> asOptional() {
      return success ? Optional.ofNullable((T) value) : Optional.empty();
   }

   /**
    * Returns the result's value, as a possible. If this is a failed result, this returns an
    * empty/unset value. Otherwise, the returned possible value holds the result's value. Unlike
    * {@link #asOptional()}, a possible can represent a successful {@code null} value and is thus
    * distinguishable from the possible value of a failed result.
    * 
    * @return a possible values that represents this result's value
    */
   public Possible<T> asPossible() {
      return success ? Reference.setTo((T) value) : Reference.unset();
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof Result)) {
         return false;
      }
      Result<?, ?> other = (Result<?, ?>) o;
      return success == other.success
            && Objects.equal(value, other.value);
   }
   
   @Override
   public int hashCode() {
      return success ? Objects.hashCode(value) : ~Objects.hashCode(value);
   }
   
   @Override
   public String toString() {
      return success
            ? "Result.ok[" + value + "]"
            : "Result.error[" + value + "]";
   }
}
