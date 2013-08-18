package com.apriori.concurrent;

/**
 * A listener for the completion of a future. When {@linkplain
 * ListenableFuture#addListener(FutureListener, java.util.concurrent.Executor) registered with a
 * future}, the listener will be called once the future completes.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of future result support by this listener
 */
public interface FutureListener<T> {
   /**
    * Invoked when a future completes.
    * 
    * @param completedFuture the completed future
    */
   void onCompletion(ListenableFuture<? extends T> completedFuture);
}