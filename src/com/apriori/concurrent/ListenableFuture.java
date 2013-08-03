package com.apriori.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

// TODO: javadoc
public interface ListenableFuture<T> extends Future<T>, Cancellable {
   boolean isSuccessful();
   T getResult();
   
   boolean isFailed();
   Throwable getFailure();
   
   void addListener(FutureListener<? super T> listener, Executor executor);
   
   void visit(FutureVisitor<? super T> visitor);
}
