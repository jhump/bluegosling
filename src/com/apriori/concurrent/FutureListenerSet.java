package com.apriori.concurrent;

import com.apriori.tuples.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;


final class FutureListenerSet<T> implements Cloneable {
   private final ListenableFuture<T> future;
   private final List<Pair<FutureListener<? super T>, Executor>> listeners =
         new LinkedList<Pair<FutureListener<? super T>, Executor>>();
   
   FutureListenerSet(ListenableFuture<T> future) {
      this.future = future;
   }
   
   @Override
   protected FutureListenerSet<T> clone() {
      FutureListenerSet<T> clone = new FutureListenerSet<T>(this.future);
      clone.listeners.addAll(this.listeners);
      return clone;
   }

   void addListener(FutureListener<? super T> listener, Executor executor) {
      listeners.add(Pair.<FutureListener<? super T>, Executor>create(listener, executor));
   }
   
   void runListeners() {
      for (Pair<FutureListener<? super T>, Executor> pair : listeners) {
         runListener(future, pair.getFirst(), pair.getSecond());
      }
   }
   
   static <T> void runListener(final ListenableFuture<T> future,
         final FutureListener<? super T> listener, Executor executor) {
      try {
         executor.execute(new Runnable() {
            @Override public void run() {
               listener.onCompletion(future);
            }
         });
      } catch (RuntimeException e) {
         // TODO: log?
      }
   }
}
