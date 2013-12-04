package com.apriori.concurrent;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An executor that runs multiple sequential pipelines. When tasks are submitted, they are enqueued
 * in a pipeline that runs its tasks sequentially instead of concurrently. However, submitting tasks
 * for multiple pipelines achieves concurrency since pipelines are independent of one another. This
 * is useful for concurrently executing callbacks where order of delivery to a single listener
 * matters. Using the listener as the pipeline key, this executor assures that callbacks are invoked
 * sequentially. 
 *
 * @param <P> the type of objects that identify pipelines
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class PipeliningExecutorService<P> {

   final Executor executor;
   final ConcurrentMap<P, Pipeline> pipelines =
         new ConcurrentHashMap<P, Pipeline>();
   final AtomicInteger pipelineCount = new AtomicInteger();
   final Lock quietLock = new ReentrantLock();
   final Condition isQuiet = quietLock.newCondition();
   
   /**
    * Constructs a new pipelining executor that uses the specified executor for actually running
    * tasks. 
    *
    * @param executor the executor that will run individual tasks across all pipelines
    */
   public PipeliningExecutorService(Executor executor) {
      this.executor = executor;
   }
   
   /**
    * Submits a task for the specified pipeline. The task can run concurrently with other tasks
    * submitted for different pipelines. It will not execute until all previously submitted tasks
    * for the same pipeline have completed.
    *
    * @param pipeline the pipeline in which to run the task
    * @param task a task
    * @return a future that represents the completion of the given task
    */
   public <T> ListenableFuture<T> submit(P pipeline, Callable<T> task) {
      ListenableFutureTask<T> future = new ListenableFutureTask<T>(task);
      enqueue(pipeline, future);
      return future;
   }

   /**
    * Submits a task for the specified pipeline. The task can run concurrently with other tasks
    * submitted for different pipelines. It will not execute until all previously submitted tasks
    * for the same pipeline have completed.
    *
    * @param pipeline the pipeline in which to run the task
    * @param task a task
    * @param result the result of the task when it completes
    * @return a future that represents the completion of the given task
    */
   public <T> ListenableFuture<T> submit(P pipeline, Runnable task, T result) {
      ListenableFutureTask<T> future = new ListenableFutureTask<T>(task, result);
      enqueue(pipeline, future);
      return future;
   }

   /**
    * Submits a task for the specified pipeline. The task can run concurrently with other tasks
    * submitted for different pipelines. It will not execute until all previously submitted tasks
    * for the same pipeline have completed.
    *
    * @param pipeline the pipeline in which to run the task
    * @param task a task
    * @return a future that represents the completion of the given task
    */
   public ListenableFuture<Void> submit(P pipeline, Runnable task) {
      ListenableFutureTask<Void> future = new ListenableFutureTask<Void>(task, null);
      enqueue(pipeline, future);
      return future;
   }
   
   /**
    * Enqueues the given task with the specified pipeline.
    *
    * @param pipeline the key that identifies the pipeline in which the task is enqueued
    * @param task the task
    */
   private void enqueue(P pipeline, Runnable task) {
      while (true) {
         Pipeline p = pipelines.get(pipeline);
         if (p == null) {
            p = new Pipeline(pipeline, task);
            Pipeline existing = pipelines.putIfAbsent(pipeline,  p);
            if (existing == null) {
               pipelineCount.incrementAndGet();
               p.run();
               return;
            }
            p = existing;
         }
         if (p.enqueue(task)) {
            return;
         }
      }
   }
   
   /**
    * Removes the specified pipeline. To prevent this executor from hanging on to references to
    * no-longer-needed pipeline keys, this method is used to clean up when the queue for a given
    * pipeline becomes empty.
    *
    * @param pipelineKey the key that identifies the pipeline
    * @param pipeline the {@link Pipeline} object whose queue is empty
    */
   private void remove(P pipelineKey, Pipeline pipeline) {
      boolean removed = pipelines.remove(pipelineKey,  pipeline);
      assert removed;
      if (pipelineCount.decrementAndGet() == 0) {
         quietLock.lock();
         try {
            isQuiet.signalAll();
         } finally {
            quietLock.unlock();
         }
      }
   }
   
   /**
    * Waits for this executor to reach a point where no tasks are running or queued. Note that this
    * implies nothing about the state of the underlying executor as other sources could still be
    * submitting and running tasks with that executor.
    * 
    * <p>Note that the state of quiescance may be transient and will end as soon as another task is
    * submitted to this executor. This method is usually used after a source of callbacks
    * is stopped. At that point, no other tasks should be submitted. Once this executor has
    * quiesced, it will then be safe to shutdown the underlying executor. If the underlying executor
    * is shutdown too soon, submissions to this pipelining executor could be rejected with a
    * {@link RejectedExecutionException}.
    *
    * @throws InterruptedException if the current thread is interrupted while waiting
    */
   public void awaitQuiescance() throws InterruptedException {
      quietLock.lock();
      try {
         while (true) {
            // possible, although unlikely, to be negative if task is submitted and finishes so
            // fast that there's a race between the task decrementing on completion and the original
            // enqueue operation incrementing
            if (pipelineCount.get() <= 0) {
               return;
            }
            isQuiet.await();
         }
      } finally {
         quietLock.unlock();
      }
   }
   
   /**
    * Waits up to the specified amount of time for this executor to reach a point where no tasks are
    * running or queued.
    * 
    * @param limit the limit of how much time to wait
    * @param unit the unit for the time limit
    * @return true if quiescance was reached or false if the time limit elapsed first
    * @throws InterruptedException if the current thread is interrupted while waiting
    */
   public boolean awaitQuiescance(long limit, TimeUnit unit) throws InterruptedException {
      long startNanos = System.nanoTime();
      quietLock.lock();
      try {
         while (true) {
            // possible, although unlikely, to be negative if task is submitted and finishes so
            // fast that there's a race between the task decrementing on completion and the original
            // enqueue operation incrementing
            if (pipelineCount.get() <= 0) {
               return true;
            }
            long elapsedNanos = System.nanoTime() - startNanos;
            long remainingNanos = unit.toNanos(limit) - elapsedNanos;
            if (!isQuiet.await(remainingNanos, TimeUnit.NANOSECONDS)) {
               return false;
            }
         }
      } finally {
         quietLock.unlock();
      }
   }
   
   /**
    * Represents a single in-progress pipeline. If all tasks complete and the queue for this
    * pipeline is exhausted, the pipeline will remove itself as clean-up. Any subsequent tasks
    * submitted for the same pipeline will cause a new {@link Pipeline} and associated queue to
    * be created.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class Pipeline {
      private P pipelineKey;
      private Runnable current;
      private final Queue<Runnable> queue = new ArrayDeque<Runnable>();
      
      /**
       * Constructs a new pipeline.
       *
       * @param pipelineKey the key that identifies this pipeline
       * @param current the first task to run in the new pipeline
       */
      Pipeline(P pipelineKey, Runnable current) {
         this.pipelineKey = pipelineKey;
         this.current = current;
      }
      
      /**
       * Enqueues the given task.
       *
       * @param task the task to enqueue
       * @return true if the task was enqueued; false if this pipeline is cleaning itself up and
       *       the caller should instead construct and use a new pipeline object
       */
      synchronized boolean enqueue(Runnable task) {
         if (current == null) {
            return false;
         }
         queue.add(task);
         return true;
      }
      
      /**
       * Runs the head of the queue using the underlying executor.
       */
      synchronized void run() {
         executor.execute(new Runnable() {
            @SuppressWarnings("synthetic-access") // current member is private
            @Override public void run() {
               try {
                  current.run();
               } finally {
                  runNext();
               }
            }
         });
      }
      
      /**
       * Runs the next item in the queue or cleans up this pipeline if the queue is empty.
       */
      @SuppressWarnings("synthetic-access") // remove(...) in enclosing class is private
      synchronized void runNext() {
         current = queue.poll();
         if (current != null) {
            run();
         } else {
            remove(pipelineKey, this);
         }
      }
   }
}
