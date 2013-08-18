package com.apriori.concurrent;

import com.apriori.tuples.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Represents a set of listeners. {@link ListenableFuture} implementations can use this class to
 * store the set of registered listeners.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of result for the listenable future
 */
final class FutureListenerSet<T> implements Cloneable {
   private final ListenableFuture<T> future;
   private final List<Pair<FutureListener<? super T>, Executor>> listeners =
         new LinkedList<Pair<FutureListener<? super T>, Executor>>();
   
   /**
    * Creates a new listener set for the specified future.
    * 
    * @param future the future that will invoke the listeners upon completion
    */
   FutureListenerSet(ListenableFuture<T> future) {
      this.future = future;
   }
   
   @Override
   protected FutureListenerSet<T> clone() {
      FutureListenerSet<T> clone = new FutureListenerSet<T>(this.future);
      clone.listeners.addAll(this.listeners);
      return clone;
   }

   /**
    * Registers a listener.
    * 
    * @param listener the listener
    * @param executor the executed used to invoke the listener
    */
   void addListener(FutureListener<? super T> listener, Executor executor) {
      listeners.add(Pair.<FutureListener<? super T>, Executor>create(listener, executor));
   }
   
   /**
    * Invokes all registered listeners.
    */
   void runListeners() {
      for (Pair<FutureListener<? super T>, Executor> pair : listeners) {
         runListener(future, pair.getFirst(), pair.getSecond());
      }
   }
   
   /**
    * Invokes a single listener. This is a useful method for invoking listeners, even when a
    * {@link FutureListenerSet} is not used. It invokes the listener, but prevents exceptions
    * (thrown by potentially misbehaving or shutdown executors) from bubbling up.
    * 
    * @param future the completed future
    * @param listener the listener to invoke
    * @param executor the executor used to invoke the listener
    */
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
