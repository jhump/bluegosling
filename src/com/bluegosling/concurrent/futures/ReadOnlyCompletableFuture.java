package com.bluegosling.concurrent.futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A completable future whose public API is read-only. Attempts to {@linkplain #complete(Object)
 * complete} or {@linkplain #obtrudeValue(Object) obtrude} a value or cause of failure will result
 * in {@link UnsupportedOperationException}s. Also, calls {@link #toCompletableFuture()} will
 * clone the future, returning a new {@link CompletableFuture} that is kept in sync with the
 * original, completing or failing with the same result or cause.
 * 
 * <p>In order for the future to be completed, this class provides protected API. This means that
 * this class must be sub-classed to be useful. See {@link RunnableReadOnlyCompletableFuture} for
 * an example.
 *
 * @param <T> the type of the future value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public class ReadOnlyCompletableFuture<T> extends InterruptibleCompletableFuture<T> {

   /**
    * Completes the future with the given value. This is like {@link #complete(Object)} except that
    * it is intentionally protected, not public, so as to be used by sub-classes and preserving the
    * read-only nature of the public API.
    *
    * @param value the result value
    * @return true if the future is completed with the given value or false if the future was
    *       already completed (successfully, exceptionally, or from cancellation)
    */
   protected boolean setValue(T value) {
      return super.complete(value);
   }

   /**
    * Marks the future as failed due to the given cause. This is like
    * {@link #completeExceptionally(Throwable)} except that it is intentionally protected, not
    * public, so as to be used by sub-classes and preserving the read-only nature of the public API.
    *
    * @param cause the cause of failure
    * @return true if the future is completed with the given exception or false if the future was
    *       already completed (successfully, exceptionally, or from cancellation)
    */
   protected boolean setFailure(Throwable cause) {
      return super.completeExceptionally(cause);
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This class always throws {@link UnsupportedOperationException} as the future cannot be
    * completed or changed via public API.
    * 
    * @deprecated This method should not be used. Sub-classes may use {@link #setValue(Object)} to
    * complete the future.
    */
   @Deprecated
   @Override
   public boolean complete(T value) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This class always throws {@link UnsupportedOperationException} as the future cannot be
    * completed or changed via public API.
    * 
    * @deprecated This method should not be used. Sub-classes may use {@link #setFailure(Throwable)}
    * to complete the future exceptionally.
    */
   @Deprecated
   @Override
   public boolean completeExceptionally(Throwable ex) {
      throw new UnsupportedOperationException();
   }

   /**
    * Returns a new {@link CompletableFuture} that completes with the same value or cause of failure
    * as this future. The returned future is a standard future, not a read-only one. So it can be
    * completed/obtruded. However, changes to the returned future will not impact this future.
    *
    * @see java.util.concurrent.CompletableFuture#toCompletableFuture()
    */
   @Override
   public CompletableFuture<T> toCompletableFuture() {
      CompletableFuture<T> ret = new CompletableFuture<>();
      this.whenComplete((t, th) -> {
         if (th != null) {
            ret.completeExceptionally(th);
         } else {
            ret.complete(t);
         }
      });
      return ret;
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This class always throws {@link UnsupportedOperationException} as the future cannot be
    * completed or changed via public API.
    * 
    * @deprecated This method should not be used. Sub-classes may use {@link #setValue(Object)} to
    * complete the future. Once completed, it cannot be changed. Instead, create a derived future
    * using various {@link CompletionStage} methods to define an alternate value.
    */
   @Deprecated
   @Override
   public void obtrudeValue(T value) {
      throw new UnsupportedOperationException();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This class always throws {@link UnsupportedOperationException} as the future cannot be
    * completed or changed via public API.
    * 
    * @deprecated This method should not be used. Sub-classes may use {@link #setFailure(Throwable)}
    * to complete the future exceptionally. Once completed, it cannot be changed. Instead, create a
    * derived future using various {@link CompletionStage} methods to define an alternate value.
    */
   @Deprecated
   @Override
   public void obtrudeException(Throwable ex) {
      throw new UnsupportedOperationException();
   }
}
