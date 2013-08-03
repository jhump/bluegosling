package com.apriori.concurrent;

public interface FutureListener<T> {
   void onCompletion(ListenableFuture<? extends T> completedFuture);
}