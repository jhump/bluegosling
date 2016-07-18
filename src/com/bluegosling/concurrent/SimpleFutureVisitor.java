package com.bluegosling.concurrent;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * A simple visitor implementation that is suitable for sub-classing.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future value that is visited
 */
public class SimpleFutureVisitor<T> implements FutureVisitor<T> {

   /**
    * Performs a default action when visiting a future. This implementation does nothing but can
    * be overridden to provide default behavior.
    */
   public void defaultAction() {
   }
   
   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation just calls {@link #defaultAction()}.
    */
   @Override
   public void successful(T result) {
      defaultAction();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation just calls {@link #defaultAction()}.
    */
   @Override
   public void failed(Throwable failure) {
      defaultAction();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This default implementation calls the {@link #failed(Throwable)} visit method with a fresh
    * instance of {@link CancellationException}.
    */
   @Override
   public void cancelled() {
      failed(new CancellationException());
   }

   /**
    * A builder for constructing visitors from {@link Consumer}s and {@link Runnable}s.
    * 
    * <p>If any action is not defined when building the visitor, the default implementation in
    * {@link SimpleFutureVisitor} is used.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    *
    * @param <T> the type of future value that is visited
    */
   public static class Builder<T> {
      private Runnable defaultAction;
      private Consumer<? super T> onSuccess;
      private Consumer<? super Throwable> onFailure;
      private Runnable onCancel;
      
      /**
       * Defines the default action for a visitor.
       * 
       * @param defaultAction a runnable that performs the default action
       * @return {@code this}, for method chaining
       */
      public Builder<T> defaultAction(Runnable defaultAction) {
         this.defaultAction = defaultAction;
         return this;
      }
      
      /**
       * Defines the action for visiting successful futures.
       * 
       * @param onSuccess a consumer that accepts the future result and performs the action for
       *       successful futures
       * @return {@code this}, for method chaining
       */
      public Builder<T> onSuccess(Consumer<? super T> onSuccess) {
         this.onSuccess = onSuccess;
         return this;
      }

      /**
       * Defines the action for visiting failed futures.
       * 
       * @param onFailure a consumer that accepts the cause of failure and performs the action for
       *       failed futures
       * @return {@code this}, for method chaining
       */
      public Builder<T> onFailure(Consumer<? super Throwable> onFailure) {
         this.onFailure = onFailure;
         return this;
      }
      
      /**
       * Defines the action for visiting cancelled futures.
       * 
       * @param onCancel a runnable that performs the action for cancelled futures
       * @return {@code this}, for method chaining
       */
      public Builder<T> onCancel(Runnable onCancel) {
         this.onCancel = onCancel;
         return this;
      }
      
      /**
       * Builds a visitor using the action definitions specified.
       * 
       * @return a new {@link SimpleFutureVisitor} that overrides default behavior using actions
       *       defined via this builder
       */
      public FutureVisitor<T> build() {
         final Runnable defaultAction = this.defaultAction;
         final Consumer<? super T> onSuccess = this.onSuccess;
         final Consumer<? super Throwable> onFailure = this.onFailure;
         final Runnable onCancel = this.onCancel;
         return new SimpleFutureVisitor<T>() {
            @Override
            public void defaultAction() {
               if (defaultAction != null) {
                  defaultAction.run();
               } else {
                  super.defaultAction();
               }
            }
            
            @Override
            public void successful(T result) {
               if (onSuccess != null) {
                  onSuccess.accept(result);
               } else {
                  super.successful(result);
               }
            }

            @Override
            public void failed(Throwable failure) {
               if (onFailure != null) {
                  onFailure.accept(failure);
               } else {
                  super.failed(failure);
               }
            }

            @Override
            public void cancelled() {
               if (onCancel != null) {
                  onCancel.run();
               } else {
                  super.cancelled();
               }
            }
         };
      }
   }
}
