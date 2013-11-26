package com.apriori.concurrent;

import com.apriori.util.Sink;

/**
 * Factory methods for creating instances of {@link FutureListener}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests!
public final class FutureListeners {
   private FutureListeners() {
   }

   /**
    * Creates a listener that will visit the completed future. When the listener is called from a
    * completed future, that listener uses the specified visitor to visit the completed future.
    * 
    * @param visitor the visitor
    * @return a listener that uses the visitor when invoked
    */
   public static <T> FutureListener<T> forVisitor(final FutureVisitor<T> visitor) {
      return new FutureListener<T>() {
         @Override
         public void onCompletion(ListenableFuture<? extends T> completedFuture) {
            completedFuture.visit(visitor);
         }
      };
   }
   
   /**
    * Creates a listener that will execute the specified task when the future completes.
    * 
    * @param runnable the task to run when the future completes
    * @return a listener that invokes the specified task
    */
   public static FutureListener<Object> forRunnable(final Runnable runnable) {
      return new FutureListener<Object>() {
         @Override
         public void onCompletion(ListenableFuture<?> completedFuture) {
            runnable.run();
         }
      };
   }

   /**
    * Adapts a {@link Sink} to the {@link FutureListener} interface.
    * 
    * @param sink a sink
    * @return a listener that calls {@code sink.accept(completedFuture)} when invoked
    */
   public static <T> FutureListener<T> forSink(final Sink<? super ListenableFuture<T>> sink) {
      return new FutureListener<T>() {
         @Override
         public void onCompletion(ListenableFuture<? extends T> completedFuture) {
            // methods on future return Ts but don't accept Ts, so upcasting the type arg is safe
            @SuppressWarnings("unchecked")
            ListenableFuture<T> future = (ListenableFuture<T>) completedFuture;
            sink.accept(future);
         }
      };
   }
   
   /**
    * Assembles a listener using all of the specified callbacks. Each callback is invoked depending
    * on the disposition of the completed futures.
    * 
    * <p>This method uses only functional interfaces, which will make it easier to assemble
    * listeners in Java 8.
    * 
    * @param onSuccess invoked when the completed future is successful; the future's result is
    *       passed to the sink
    * @param onFailure invoked when the completed future has failed; the cause of failure is passed
    *       to the sink
    * @param onCancel invoked when the completed future is cancelled
    * @return a listener that will call one of the specified callbacks when invoked
    */
   public static <T> FutureListener<T> assemble(Sink<? super T> onSuccess,
         Sink<? super Throwable> onFailure, Runnable onCancel) {
      return forVisitor(new SimpleFutureVisitor.Builder<T>()
            .onSuccess(onSuccess).onFailure(onFailure).onCancel(onCancel)
            .build());
   }
}
