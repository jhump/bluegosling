package com.apriori.concurrent;

import com.apriori.util.Sink;

// TODO: javadoc
// TODO: tests!
public final class FutureListeners {
   private FutureListeners() {
   }

   public static <T> FutureListener<T> forVisitor(final FutureVisitor<T> visitor) {
      return new FutureListener<T>() {
         @Override
         public void onCompletion(ListenableFuture<? extends T> completedFuture) {
            completedFuture.visit(visitor);
         }
      };
   }
   
   public static FutureListener<Object> forRunnable(final Runnable runnable) {
      return new FutureListener<Object>() {
         @Override
         public void onCompletion(ListenableFuture<?> completedFuture) {
            runnable.run();
         }
      };
   }

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
   
   public static <T> FutureListener<T> assemble(Sink<? super T> onSuccess,
         Sink<? super Throwable> onFailure, Runnable onCancel) {
      return forVisitor(new SimpleFutureVisitor.Builder<T>()
            .onSuccess(onSuccess).onFailure(onFailure).onCancel(onCancel)
            .build());
   }
}
