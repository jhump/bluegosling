package com.apriori.concurrent;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// TODO: tests
// TODO: javadoc
public class PipeliningExecutorService<P> {

   private final ListenableExecutorService executor;
   private final ConcurrentMap<P, Pipeline> pipelines =
         new ConcurrentHashMap<P, Pipeline>();
   
   public PipeliningExecutorService(ListenableExecutorService executor) {
      this.executor = executor;
   }
   
   public <T> ListenableFuture<T> submit(P pipeline, Callable<T> task) {
      ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
      enqueue(pipeline, future);
      return future;
   }

   public <T> ListenableFuture<T> submit(P pipeline, Runnable task, T result) {
      ListenableFutureTask<T> future = new ListenableFutureTask<T>(task, result);
      enqueue(pipeline, future);
      return future;
   }

   public ListenableFuture<Void> submit(P pipeline, Runnable task) {
      ListenableFutureTask<Void> future = new ListenableFutureTask<Void>(task, null);
      enqueue(pipeline, future);
      return future;
   }
   
   private void enqueue(P pipeline, Runnable task) {
      Pipeline p = pipelines.get(pipeline);
      // TODO
   }
   
   private class Pipeline {
      private Runnable current;
      private final Queue<Runnable> queue = new LinkedList<Runnable>();
      
      Pipeline(Runnable current) {
         this.current = current;
      }
      
      // TODO
   }
}
