package com.bluegosling.util;

import static java.util.Objects.requireNonNull;

import com.bluegosling.concurrent.FutureVisitor;
import com.bluegosling.concurrent.futures.fluent.FluentFuture;
import com.bluegosling.vars.Variable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
   }
   
   public FluentFuture<T> asFuture() {
      return isFailed()
            ? FluentFuture.failedFuture(getFailure())
            : FluentFuture.completedFuture(getResult());
   }
}
