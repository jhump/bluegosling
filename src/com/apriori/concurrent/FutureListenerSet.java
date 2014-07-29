package com.apriori.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Represents a set of listeners. {@link ListenableFuture} implementations can use this class to
 * manage the set of registered listeners.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of result for the listenable future
 */
class FutureListenerSet<T> implements Runnable {

   /**
    * A node in a linked list of listeners.
    *
    * @param <T> the type of the associated future
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ListenerNode<T> {
      final FutureListener<? super T> listener;
      final Executor executor;
      int index;
      ListenerNode<T> next;
      
      ListenerNode(FutureListener<? super T> listener, Executor executor) {
         this.listener = listener;
         this.executor = executor;
      }
      
      void setNext(ListenerNode<T> next) {
         this.next = next;
         this.index = next == null ? 0 : next.index + 1;
      }
   }

   /**
    * The future whose listeners are tracked by this set.
    */
   private final ListenableFuture<T> future;
   
   /**
    * A linked list of listeners. When the future is completed, indicating that subsequent listeners
    * added should be executed immediately, the mark will be set. When the mark is set, the
    * reference is also cleared (so that listeners can be garbage collected). To add a listener, we
    * CAS the head of the list.
    */
   private final AtomicMarkableReference<ListenerNode<T>> listeners =
         new AtomicMarkableReference<>(null, false);
   
   /**
    * Creates a new listener set for the specified future.
    * 
    * @param future the future that will invoke the listeners upon completion
    */
   FutureListenerSet(ListenableFuture<T> future) {
      this.future = future;
   }
   
   /**
    * Returns a task that will execute a snapshot of the set of listeners. This can be used to
    * invoke listeners more than once since {@link #run()} can only be invoked once. Listeners added
    * to the set after this snapshot is taken will not be invoked when the returned task is run.
    *
    * @return a task that represents a snapshot of the listeners and will invoke those listeners
    *       when run
    */
   public Runnable snapshot() {
      ListenerNode<T> node = listeners.getReference();
      return node == null ? () -> {} : () -> runListeners(node);
   }

   /**
    * Registers a listener. If the future is already done then the listener will be run immediately.
    * 
    * @param listener the listener
    * @param executor the executed used to invoke the listener
    */
   void addListener(FutureListener<? super T> listener, Executor executor) {
      boolean mark[] = new boolean[1];
      ListenerNode<T> node = new ListenerNode<>(listener, executor);
      while (true) {
         ListenerNode<T> head = listeners.get(mark);
         if (mark[0]) {
            // future is done; run listener immediately
            assert future.isDone();
            runListener(future, listener, executor);
            return;
         }
         node.setNext(head);
         if (listeners.compareAndSet(head, node, false, false)) {
            return;
         }
      }
   }
   
   /**
    * Invokes all registered listeners. This should only be invoked when the corresponding future
    * is complete. Subsequent attempts to add a listener to the set will result in the listener
    * being executed immediately.
    * 
    * @throws IllegalStateException if this method is called more than once
    */
   @Override public void run() {
      assert future.isDone();
      boolean mark[] = new boolean[1];
      ListenerNode<T> node;
      while (true) {
         node = listeners.get(mark);
         if (mark[0]) {
            // we've already run the listeners!
            throw new IllegalStateException();
         }
         if (listeners.compareAndSet(node, null, false, true)) {
            break;
         }
      }
      if (node != null) {
         runListeners(node);
      }
   }

   /**
    * Runs the set of listeners included in the given linked list.
    * 
    * @param a linked list of listener + executor pairs 
    */
   private void runListeners(ListenerNode<T> node) {
      // Listeners are pushed onto head of linked list when registered. But we want to run them
      // in the order they were registered, which means tail first and head last. So we reverse
      // the order of the listeners into this array.
      @SuppressWarnings("unchecked")
      ListenerNode<T> array[] = (ListenerNode<T>[]) new ListenerNode<?>[node.index + 1];
      while (node != null) {
         array[node.index] = node;
         node = node.next;
      }
      for (ListenerNode<T> n : array) {
         runListener(future, n.listener, n.executor);
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
         executor.execute(() -> listener.onCompletion(future));
      } catch (RuntimeException e) {
         // TODO: log?
      }
   }
}
