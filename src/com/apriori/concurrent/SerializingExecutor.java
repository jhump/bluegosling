package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * An executor that serializes multiple tasks that correspond to the same key. So tasks submitted
 * for the same key will be executed in FIFO order. If tasks are submitted for different keys, they
 * may be executed concurrently.
 * 
 * <p>This is useful for establishing serial pipelines, where concurrency is acheived through the
 * use of multiple pipelines. One specific use is for sending events to a listener. So that one
 * slow listener doesn't block others, each can be its own pipeline. All notifications for a
 * listener are then delivered in FIFO order, but a single slow listener doesn't have to stall
 * deliver of notifications to other listeners. 
 *
 * @param <K> the type of key, used to group tasks into serial pipelines
 * 
 * @see Executor
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface SerializingExecutor<K> {
   
   /**
    * Executes the given task, associated with the given key. The task will run <em>after</em> any
    * previously submitted tasks for the same key and <em>before</em> any subsequently submitted
    * tasks for the same key.
    *
    * @param k the key
    * @param task the task
    * 
    * @see Executor#execute(Runnable)
    */
   void execute(K k, Runnable task);
   
   /**
    * Returns an executor that associates all tasks with the given key. This is useful to adapt this
    * class to the standard {@link Executor} interface.
    *
    * @param k the key
    * @return an executor that associates all tasks with the given key
    */
   default Executor newExecutorFor(K k) {
      return r -> execute(k, r);
   }

   /**
    * Executes the given task, associated with the given key, and returns a future that will
    * complete when the task completes or is cancelled.
    *
    * @param k the key
    * @param task the task
    * @return a future that will complete when the task completes or is cancelled
    * 
    * @see #execute(Object, Runnable)
    */
   default ListenableFuture<Void> submit(K k, Runnable task) {
      return submit(k, task, null);
   }
   
   /**
    * Executes the given task, associated with the given key, and returns a future that will
    * complete when the task completes or is cancelled. On successful completion, the future's value
    * will be that of the given value.
    *
    * @param k the key
    * @param task the task
    * @param value the value of the future on successful completion
    * @return a future that will complete when the task completes or is cancelled
    * 
    * @see #execute(Object, Runnable)
    */
   default <T> ListenableFuture<T> submit(K k, Runnable task, T value) {
      SettableRunnableFuture<T> f = new SettableRunnableFuture<>(task, value);
      execute(k, f);
      return f;
   }

   /**
    * Executes the given task, associated with the given key, and returns a future that will
    * complete when the task completes or is cancelled. On successful completion, the future's value
    * will be the value returned by the given task
    *
    * @param k the key
    * @param task the task, which produces the value of the future on success
    * @return a future that will complete when the task completes or is cancelled
    * 
    * @see #execute(Object, Runnable)
    */
   default <T> ListenableFuture<T> submit(K k, Callable<T> task) {
      SettableRunnableFuture<T> f = new SettableRunnableFuture<>(task);
      execute(k, f);
      return f;
   }
}
