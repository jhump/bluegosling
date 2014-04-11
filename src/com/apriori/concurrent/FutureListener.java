package com.apriori.concurrent;

import java.util.function.Consumer;

/**
 * A listener for the completion of a future. When {@linkplain
 * ListenableFuture#addListener(FutureListener, java.util.concurrent.Executor) registered with a
 * future}, the listener will be called once the future completes.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result support by this listener
 */
@FunctionalInterface
public interface FutureListener<T> {

   /**
    * Invoked when a future completes.
    * 
    * @param completedFuture the completed future
    */
   void onCompletion(ListenableFuture<? extends T> completedFuture);

   /**
    * Creates a listener that will visit the completed future. When the listener is called from a
    * completed future, that listener uses the specified visitor to visit the completed future.
    * 
    * @param visitor the visitor
    * @return a listener that uses the visitor when invoked
    */
   public static <T> FutureListener<T> forVisitor(final FutureVisitor<T> visitor) {
      return (future) -> { future.visit(visitor); };
   }
   
   /**
    * Creates a listener that will execute the specified task when the future completes.
    * 
    * @param runnable the task to run when the future completes
    * @return a listener that invokes the specified task
    */
   public static FutureListener<Object> forRunnable(final Runnable runnable) {
      return (future) -> { runnable.run(); };
   }

   /**
    * Adapts a {@link Consumer} to the {@link FutureListener} interface.
    * 
    * @param consumer a consumer
    * @return a listener that calls {@code consumer.accept(completedFuture)} when invoked
    */
   static <T> FutureListener<T> forConsumer(Consumer<? super ListenableFuture<T>> consumer) {
      return (future) -> { consumer.accept(ListenableFuture.cast(future)); };
   }
   
   /**
    * Assembles a listener using all of the specified callbacks. Each callback is invoked depending
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
   static <T> FutureListener<T> o(Consumer<? super T> onSuccess,
         Consumer<? super Throwable> onFailure, Runnable onCancel) {
      return forVisitor(new FutureVisitor<T>() {
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
      });
   }
}