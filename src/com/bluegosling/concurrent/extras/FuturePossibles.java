package com.bluegosling.concurrent.extras;

import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.SettableFluentFuture;
import com.bluegosling.possible.AbstractDynamicPossible;
import com.bluegosling.possible.Fulfillable;
import com.bluegosling.possible.Possible;
import com.bluegosling.util.Throwables;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

public final class FuturePossibles {
   private FuturePossibles() {
   }
   
   /**
    * Returns a view of a future as a possible value. If the future is incomplete, has failed, or
    * has been cancelled then the value is not present. If and when the future completes
    * successfully, the value will become present. The actual value is the future's result.
    * 
    * @param future the future
    * @return a view of the future as a {@link Possible}
    */
   public static <T> Possible<T> possibleFromFuture(Future<? extends T> future) {
      if (future instanceof FluentFuture) {
         return new FluentFuturePossible<T>((FluentFuture<? extends T>) future);
      } else {
         return new FuturePossible<T>(future);
      }
   }
   
   public static <T> Fulfillable<T> fulfillableFromFuture(SettableFuture<T> future) {
      return new ListenableFutureFulfillable<>(future);
   }
   
   public static <T> Fulfillable<T> fulfillableFromFuture(SettableFluentFuture<T> future) {
      return new FluentFutureFulfillable<>(future);
   }
   
   public static <T> Fulfillable<T> fulfillableFromFuture(CompletableFuture<T> future) {
      return new CompletableFutureFulfillable<>(future);
   }
   
   private static class FluentFuturePossible<T> extends AbstractDynamicPossible<T> {
      private final FluentFuture<? extends T> future;
      
      FluentFuturePossible(FluentFuture<? extends T> future) {
         this.future = future;
      }
      
      @Override
      public boolean isPresent() {
         return future.isSuccessful();
      }
      
      @Override
      public T get() {
         if (!future.isSuccessful()) {
            throw new IllegalStateException();
         }
         return future.getResult();
      }
   }

   private static class FuturePossible<T> extends AbstractDynamicPossible<T> {
      private final Future<? extends T> future;
      private volatile Boolean isPresent;
      private volatile T value;
      
      FuturePossible(Future<? extends T> future) {
         this.future = future;
      }
      
      private synchronized boolean determineIfPresent() {
         if (isPresent != null) {
            return isPresent;
         }
         synchronized (this) {
            // double-checked locking so we only compute these values once
            if (isPresent != null) {
               return isPresent;
            }
            boolean interrupted = false;
            while (true) {
               try {
                  value = future.get();
                  isPresent = true;
               } catch (InterruptedException e) {
                  interrupted = true;
                  continue;
               } catch (ExecutionException e) {
                  isPresent = false;
               } catch (CancellationException e) {
                  isPresent = false;
               }
               break;
            }
            if (interrupted) {
               Thread.currentThread().interrupt();
            }
            return isPresent;
         }
      }
      
      @Override
      public boolean isPresent() {
         return future.isDone() && determineIfPresent();
      }
      
      @Override
      public T get() {
         if (!isPresent()) {
            throw new IllegalStateException();
         }
         return value;
      }
   }

   /**
    * A view of a settable listenable future as a {@link Fulfillable}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ListenableFutureFulfillable<T> extends AbstractDynamicPossible<T>
   implements Fulfillable<T> {
      private final SettableFuture<T> future;
      
      ListenableFutureFulfillable(SettableFuture<T> future) {
         this.future = future;
      }
      
      @Override
      public boolean isPresent() {
         if (!future.isDone() || future.isCancelled()) {
            return false;
         }
         try {
            Uninterruptibles.getUninterruptibly(future);
            return true;
         } catch (ExecutionException e) {
            return false;
         }
      }
      
      @Override
      public boolean fulfill(T value) {
         return future.set(value);
      }

      @Override
      public T get() {
         if (!future.isDone() || future.isCancelled()) {
            throw new NoSuchElementException("not yet fulfilled");
         }
         try {
            return Uninterruptibles.getUninterruptibly(future);
         } catch (ExecutionException e) {
            throw Throwables.withCause(new NoSuchElementException("failed to fulfill"),
                  e.getCause());
         }
      }
         
      @Override
      public Set<T> asSet() {
         // once completed, the future is immutable
         if (future.isDone() && !future.isCancelled()) {
            try {
               return Collections.singleton(Uninterruptibles.getUninterruptibly(future));
            } catch (ExecutionException e) {
               return Collections.emptySet();
            }
         }
         return super.asSet();
      }
   }
   
   /**
    * A view of a settable fluent future as a {@link Fulfillable}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class FluentFutureFulfillable<T> extends AbstractDynamicPossible<T>
   implements Fulfillable<T> {
      private final SettableFluentFuture<T> future;
      
      FluentFutureFulfillable(SettableFluentFuture<T> future) {
         this.future = future;
      }
      
      @Override
      public boolean isPresent() {
         return future.isSuccessful();
      }
      
      @Override
      public boolean fulfill(T value) {
         return future.setValue(value);
      }

      @Override
      public T get() {
         if (!future.isDone() || future.isCancelled()) {
            throw new NoSuchElementException("not yet fulfilled");
         } else if (future.isFailed()) {
            throw Throwables.withCause(new NoSuchElementException("failed to fulfill"),
                  future.getFailure());
         }
         return future.getResult();
      }
         
      @Override
      public Set<T> asSet() {
         // once completed, the future is immutable
         if (future.isDone()) {
            return future.isSuccessful()
                  ? Collections.singleton(future.getResult())
                  : Collections.emptySet();
         }
         return super.asSet();
      }
   }

   /**
    * A view of a completable fluent future as a {@link Fulfillable}.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class CompletableFutureFulfillable<T> extends AbstractDynamicPossible<T>
   implements Fulfillable<T> {
      private final CompletableFuture<T> future;
      
      CompletableFutureFulfillable(CompletableFuture<T> future) {
         this.future = future;
      }
      
      @Override
      public boolean isPresent() {
         return future.isDone() && !future.isCancelled() && !future.isCompletedExceptionally();
      }
      
      @Override
      public boolean fulfill(T value) {
         return future.complete(value);
      }

      @Override
      public T get() {
         if (!future.isDone() || future.isCancelled()) {
            throw new NoSuchElementException("not yet fulfilled");
         }
         try {
            return Uninterruptibles.getUninterruptibly(future);
         } catch (ExecutionException e) {
            throw Throwables.withCause(new NoSuchElementException("failed to fulfill"),
                  e.getCause());
         }
      }
         
      @Override
      public Set<T> asSet() {
         // once completed, the future is immutable
         if (future.isDone() && !future.isCancelled()) {
            try {
               return Collections.singleton(Uninterruptibles.getUninterruptibly(future));
            } catch (ExecutionException e) {
               return Collections.emptySet();
            }
         }
         return super.asSet();
      }
   }
}
