package com.apriori.concurrent;

import java.util.concurrent.Executor;

// TODO: javadoc
public interface ListenableRepeatingFuture<T> extends ListenableScheduledFuture<T> {
   void addListenerForEachAttempt(FutureListener<? super T> listener, Executor executor);
   int executionCount();
}
