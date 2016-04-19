package com.bluegosling.util;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.bluegosling.concurrent.FutureVisitor;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.function.TriFunction;
import com.bluegosling.vars.Variable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Like a {@link Future}, except it represents a result that is available immediately. Unlike a
 * {@link Variable}, it can represent either a successful result value or a cause of failure.
 * 
 * <p>Its API closely resembles that of {@link FluentFuture} except that, since the value is
 * immediately available, no methods ever block. So methods such as {@link FluentFuture#isDone()
 * isDone()} or {@link FluentFuture#await() await()} are unnecessary and thus absent.
 * 
 * @param <T> the type of value
 */
public abstract class Immediate<T> {

   /**
    * Returns a successful immediate value.
    *
    * @param t the value
    * @return a successful immediate value
    */
   public static <T> Immediate<T> successful(T t) {
      return new Success<>(t);
   }

   /**
    * Returns an immediate failure.
    *
    * @param failure the cause of failure
    * @return an immediate failure
    */
   public static <T> Immediate<T> failed(Throwable failure) {
      return new Failure<>(requireNonNull(failure));
   }

   /**
    * Returns an immediately cancelled value.
    *
    * @return an immediately cancelled value
    */
   @SuppressWarnings("unchecked")
   public static <T> Immediate<T> cancelled() {
      return (Immediate<T>) Cancelled.INSTANCE;
   }
   
   /**
    * Returns an immediate value with the same disposition as the given completed future.
    *
    * @param f a future
    * @return an immediate value with the same disposition as the given future
    * @throws IllegalStateException if the given future is not completed
    */
   public static <T> Immediate<T> fromCompletedFuture(Future<T> f) {
      if (!f.isDone()) {
         throw new IllegalStateException();
      }
      if (f instanceof FluentFuture) {
         FluentFuture<T> lf = (FluentFuture<T>) f;
         if (lf.isSuccessful()) {
            return successful(lf.getResult());
         } else if (lf.isCancelled()) {
            return cancelled();
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
                  return cancelled();
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
    * Gets this object's immediate value. This parallels {@link Future#get()}.
    *
    * @return the object's immediate value if successful
    * @throws ExecutionException if the immediate value is a failure
    * @throws CancellationException if the immediate value is cancelled
    * @see Future#get()
    */
   public abstract T get() throws ExecutionException;
   
   /**
    * Gets this object's successful result. This is similar to {@link #get()} if this object has a
    * successful result. It behaves differently if this object is cancelled or failed.
    *
    * @return this object's successful result
    * @throws IllegalStateException if this object is cancelled or failed
    * @see FluentFuture#getResult()
    */
   public abstract T getResult();
   
   /**
    * Gets this object's cause of failure.
    *
    * @return this object's cause of failure.
    * @throws IllegalStateException if this object is successful or cancelled
    * @see FluentFuture#getResult()
    */
   public abstract Throwable getFailure();
   
   /**
    * Determines if this object is cancelled or not.
    *
    * @return true if this object is cancelled
    * @see Future#isCancelled()
    */
   public abstract boolean isCancelled();

   /**
    * Determines if this object is successful or not.
    *
    * @return true if this object is successful
    * @see FluentFuture#isSuccessful()
    */
   public abstract boolean isSuccessful();
   
   /**
    * Determines if this object failed or not.
    *
    * @return true if this object failed
    * @see FluentFuture#isFailed()
    */
   public abstract boolean isFailed();
   
   /**
    * Visits the result of this object using the given {@link FutureVisitor}. Depending on this
    * object's disposition, exactly one of the visitor's methods will be invoked.
    *
    * @param visitor the visitor
    */
   public abstract void visit(FutureVisitor<T> visitor);
   
   public abstract <U> Immediate<U> map(Function<? super T, ? extends U> fn);
   
   public abstract <U> Immediate<U> flatMap(
         Function<? super T, ? extends Immediate<? extends U>> fn);
   
   public <U> Immediate<U> mapImmediate(Function<? super Immediate<T>, ? extends U> fn) {
      if (isCancelled()) {
         return Immediate.cancelled();
      }
      try {
         return Immediate.successful(fn.apply(this));
      } catch (Throwable th) {
         return Immediate.failed(th);
      }
   }

   public abstract Immediate<T> mapException(Function<Throwable, ? extends Throwable> fn);

   public abstract Immediate<T> recover(Function<Throwable, ? extends T> fn);
   
   @SuppressWarnings("unchecked")
   public <U, V> Immediate<V> combineWith(Immediate<U> other,
         BiFunction<? super T, ? super U, ? extends V> fn) {
      if (isCancelled() || other.isCancelled()) {
         return Immediate.cancelled();
      }
      if (isFailed()) {
         return (Immediate<V>) this;
      } else if (other.isFailed()) {
         return (Immediate<V>) other;
      }
      try {
         return Immediate.successful(fn.apply(getResult(), other.getResult()));
      } catch (Throwable th) {
         return Immediate.failed(th);
      }
   }

   @SuppressWarnings("unchecked")
   public <U, V, W> Immediate<W> combineWith(Immediate<U> other1, Immediate<V> other2,
         TriFunction<? super T, ? super U, ? super V, ? extends W> fn) {
      if (isCancelled() || other1.isCancelled() || other2.isCancelled()) {
         return Immediate.cancelled();
      }
      if (isFailed()) {
         return (Immediate<W>) this;
      } else if (other1.isFailed()) {
         return (Immediate<W>) other1;
      } else if (other2.isFailed()) {
         return (Immediate<W>) other2;
      }
      try {
         return Immediate.successful(fn.apply(getResult(), other1.getResult(), other2.getResult()));
      } catch (Throwable th) {
         return Immediate.failed(th);
      }
   }

   @SafeVarargs
   static <T> Immediate<List<T>> join(Immediate<? extends T>... immediates) {
      return join(Arrays.asList(immediates));
   }
   
   @SuppressWarnings("unchecked")
   static <T> Immediate<List<T>> join(Iterable<? extends Immediate<? extends T>> immediates) {
      List<T> l = new ArrayList<>();
      Immediate<?> failure = null;
      for (Immediate<? extends T> i : immediates) {
         if (i.isCancelled()) {
            return Immediate.cancelled();
         } else if (i.isFailed()) {
            failure = i;
         } else {
            l.add(i.getResult());
         }
      }
         
      if (failure != null) {
         return (Immediate<List<T>>) failure;
      }
      
      return Immediate.successful(Collections.unmodifiableList(l));
   }
   
   @SafeVarargs
   static <T> Immediate<T> firstSuccessfulOf(Immediate<? extends T>... immediates) {
      return firstSuccessfulOf(Arrays.asList(immediates));
   }

   static <T> Immediate<T> firstSuccessfulOf(
         Iterable<? extends Immediate<? extends T>> immediates) {
      boolean cancelled = false;
      Immediate<T> failure = null;
      for (Immediate<? extends T> i : immediates) {
         if (i.isCancelled()) {
            cancelled = true;
         } else if (i.isFailed()) {
            failure = cast(i);
         } else {
            return cast(i);
         }
      }
         
      if (failure != null) {
         return failure;
      }
      assert cancelled;
      return Immediate.cancelled();
   }

   @SuppressWarnings("unchecked")
   static <T> Immediate<T> dereference(Immediate<? extends Immediate<T>> immediate) {
      if (immediate.isCancelled()) {
         return Immediate.cancelled();
      } else if (immediate.isFailed()) {
         return (Immediate<T>) immediate;
      } else {
         return immediate.getResult();
      }
   }

   /**
    * Casts an immediate value to a super-type. The compiler enforces invariance on generic types,
    * but this method allows immediate values to be treated as covariant.
    *
    * @param immediate an immediate value
    * @return the same immediate value, but with its type parameter as a super-type of the original
    */
   static <T, U extends T> Immediate<T> cast(Immediate<U> immediate) {
      // co-variance makes it safe since all operations return a T, none take a T
      @SuppressWarnings("unchecked")
      Immediate<T> cast = (Immediate<T>) immediate;
      return cast;
   }
   
   /**
    * An immediately successful value.
    *
    * @param <T> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Success<T> extends Immediate<T> {
      private final T t;
      
      Success(T t) {
         this.t = t;
      }
      
      @Override
      public T get() throws ExecutionException {
         return t;
      }

      @Override
      public T getResult() {
         return t;
      }

      @Override
      public Throwable getFailure() {
         throw new IllegalStateException();
      }

      @Override
      public boolean isCancelled() {
         return false;
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
      public void visit(FutureVisitor<T> visitor) {
         visitor.successful(t);
      }

      @Override
      public <U> Immediate<U> map(Function<? super T, ? extends U> fn) {
         try {
            return Immediate.successful(fn.apply(t));
         } catch (Throwable th) {
            return Immediate.failed(th);
         }
      }

      @Override
      public <U> Immediate<U> flatMap(Function<? super T, ? extends Immediate<? extends U>> fn) {
         try {
            return cast(fn.apply(t));
         } catch (Throwable th) {
            return Immediate.failed(th);
         }
      }

      @Override
      public Immediate<T> mapException(Function<Throwable, ? extends Throwable> fn) {
         return this;
      }

      @Override
      public Immediate<T> recover(Function<Throwable, ? extends T> fn) {
         return this;
      }
   }
   
   /**
    * An immediately failed value.
    *
    * @param <T> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Failure<T> extends Immediate<T> {
      private final Throwable failure;
      
      Failure(Throwable failure) {
         this.failure = failure;
      }
      
      @Override
      public T get() throws ExecutionException {
         throw new ExecutionException(failure);
      }

      @Override
      public T getResult() {
         throw new IllegalStateException();
      }

      @Override
      public Throwable getFailure() {
         return failure;
      }

      @Override
      public boolean isCancelled() {
         return false;
      }

      @Override
      public boolean isSuccessful() {
         return false;
      }

      @Override
      public boolean isFailed() {
         return true;
      }

      @Override
      public void visit(FutureVisitor<T> visitor) {
         visitor.failed(failure);
      }

      @SuppressWarnings("unchecked")
      @Override
      public <U> Immediate<U> map(Function<? super T, ? extends U> fn) {
         return (Immediate<U>) this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public <U> Immediate<U> flatMap(Function<? super T, ? extends Immediate<? extends U>> fn) {
         return (Immediate<U>) this;
      }

      @Override
      public Immediate<T> mapException(Function<Throwable, ? extends Throwable> fn) {
         try {
            return Immediate.failed(fn.apply(failure));
         } catch (Throwable th) {
            return Immediate.failed(th);
         }
      }

      @Override
      public Immediate<T> recover(Function<Throwable, ? extends T> fn) {
         try {
            return Immediate.successful(fn.apply(failure));
         } catch (Throwable th) {
            return Immediate.failed(th);
         }
      }      
   }
   
   /**
    * An immediately cancelled value.
    *
    * @param <T> the type of value
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */

   private static class Cancelled<T> extends Immediate<T> {
      /**
       * Since cancelled values have no real state (no result value, no cause of failure), only one
       * instance is needed.
       */
      static final Cancelled<Object> INSTANCE = new Cancelled<>();
      
      @Override
      public T get() throws ExecutionException {
         throw new CancellationException();
      }

      @Override
      public T getResult() {
         throw new IllegalStateException();
      }

      @Override
      public Throwable getFailure() {
         throw new IllegalStateException();
      }

      @Override
      public boolean isCancelled() {
         return true;
      }

      @Override
      public boolean isSuccessful() {
         return false;
      }

      @Override
      public boolean isFailed() {
         return false;
      }

      @Override
      public void visit(FutureVisitor<T> visitor) {
         visitor.cancelled();
      }

      @Override
      public <U> Immediate<U> map(Function<? super T, ? extends U> fn) {
         return Immediate.cancelled();
      }

      @Override
      public <U> Immediate<U> flatMap(Function<? super T, ? extends Immediate<? extends U>> fn) {
         return Immediate.cancelled();
      }

      @Override
      public Immediate<T> mapException(Function<Throwable, ? extends Throwable> fn) {
         return this;
      }

      @Override
      public Immediate<T> recover(Function<Throwable, ? extends T> fn) {
         return this;
      }
   }
   
   public FluentFuture<T> asFuture() {
      return isFailed()
            ? FluentFuture.failedFuture(getFailure())
            : FluentFuture.completedFuture(getResult());
   }
}
