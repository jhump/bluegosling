package com.bluegosling.concurrent;

import java.util.function.Consumer;

/**
 * Interface for the visitor pattern with {@link ListenableFuture}s. When a visitor is passed to
 * {@link ListenableFuture#visit(FutureVisitor)}, the future calls one of these methods, depending
 * on the actual disposition of the future.
 * 
 * <p>This is particularly handy, when combined with {@link
 * FutureListener#forVisitor(FutureVisitor)}, for implementing listeners and asynchronously
 * handling futures when they complete.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of result value visited
 */
public interface FutureVisitor<T> {
   /**
    * Invoked when the visited future completed successfully.
    * 
    * @param result the result of the visited future
    */
   void successful(T result);
   
   /**
    * Invoked when the visited future failed.
    * 
    * @param failure the cause of failure of the visited future
    */
   void failed(Throwable failure);
   
   /**
    * Invoked when the visited future was cancelled.
    */
   void cancelled();

   /**
    * Assembles a visitor using all of the specified callbacks. Which callback is invoked depends
    * on the disposition of the completed futures.
    * 
    * @param onSuccess invoked when the completed future is successful; the future's result is
    *       passed to the consumer
    * @param onFailure invoked when the completed future has failed; the cause of failure is passed
    *       to the consumer
    * @param onCancel invoked when the completed future is cancelled
    * @return a listener that will call one of the specified callbacks when invoked
    * 
    * @see SimpleFutureVisitor.Builder
    */
   static <T> FutureVisitor<T> of(Consumer<? super T> onSuccess,
         Consumer<? super Throwable> onFailure, Runnable onCancel) {
      return new FutureVisitor<T>() {
         @Override
         public void successful(T result) {
            onSuccess.accept(result);
         }

         @Override
         public void failed(Throwable failure) {
            onFailure.accept(failure);
         }

         @Override
         public void cancelled() {
            onCancel.run();
         }
      };
   }
}
