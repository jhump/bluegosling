package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

// TODO: javadoc
public interface SerializingExecutor<K> {
   
   void execute(K k, Runnable task);
   
   default Executor newExecutorFor(K k) {
      return r -> execute(k, r);
   }

   default ListenableFuture<Void> submit(K k, Runnable task) {
      return submit(k, task, null);
   }
   
   default <T> ListenableFuture<T> submit(K k, Runnable task, T value) {
      SettableRunnableFuture<T> f = new SettableRunnableFuture<>(task, value);
      execute(k, f);
      return f;
   }

   default <T> ListenableFuture<T> submit(K k, Callable<T> task) {
      SettableRunnableFuture<T> f = new SettableRunnableFuture<>(task);
      execute(k, f);
      return f;
   }
}
