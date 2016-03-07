package com.bluegosling.concurrent.executors;

import static java.util.Objects.requireNonNull;

import com.bluegosling.concurrent.Cancellable;
import com.bluegosling.concurrent.Duration;
import com.bluegosling.concurrent.FutureVisitor;
import com.bluegosling.concurrent.fluent.AbstractFluentFuture;
import com.bluegosling.concurrent.fluent.FluentExecutorService;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.RunnableFluentFuture;
import com.bluegosling.concurrent.fluent.SettableFluentFuture;
import com.bluegosling.concurrent.fluent.SettableRunnableFluentFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * An executor that runs multiple sequential pipelines. When tasks are submitted, they are enqueued
 * in a pipeline that runs its tasks sequentially instead of concurrently. However, submitting tasks
 * for multiple pipelines achieves concurrency since pipelines are independent of one another. This
 * is useful for concurrently executing callbacks where order of delivery to a single listener
 * matters. Using the listener as the pipeline key, this executor assures that callbacks are invoked
 * sequentially.
 * 
 * <p>This executor does not provide a thread pool for concurrent execution. Instead, it wraps
 * another and takes on the parallelism allowed by the wrapped executor. Note: since this executor
 * must maintains its own set of queues, independent of the underlying executor's main work queue
 * (for assuring serial delivery to each pipeline), it is possible for tasks to be accepted by this
 * executor but then later rejected by the underlying executor. Care should be exercised to
 * coordinate the life cycle of the underlying executor and the sources of submissions to pipelines.
 *
 * @param <K> the type of keys that identify a pipeline
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: tests
public class PipeliningExecutor<K> implements SerializingExecutor<K> {

   private static final int DEFAULT_MAX_BATCH_SIZE = 32;
   private static final Duration DEFAULT_MAX_BATCH_DURATION = Duration.millis(500);
   
   final Executor executor;
   final ConcurrentMap<K, Pipeline> pipelines = new ConcurrentHashMap<>();
   final int maxBatchSize;
   final long maxBatchDurationNanos;
   final Phaser phaser = new Phaser() {
      @Override protected boolean onAdvance(int phase, int registeredParties) {
         return false; // never terminates
      }
   };

   /**
    * Constructs a new pipelining executor that uses the specified executor for actually running
    * tasks. Any given pipeline is allowed to process a batch of up to 32 tasks or process for
    * up to 500 milliseconds (whichever occurs first) before yielding. 
    *
    * @param executor the executor that will run individual tasks across all pipelines
    */
   public PipeliningExecutor(Executor executor) {
      this(executor, DEFAULT_MAX_BATCH_SIZE, DEFAULT_MAX_BATCH_DURATION.length(),
            DEFAULT_MAX_BATCH_DURATION.unit());
   }

   /**
    * Constructs a new pipelining executor that uses the specified executor for actually running
    * tasks and runs up to the given number of tasks in a batch. Any given pipeline is allowed to
    * process a batch of up to the given number or process for up to 500 milliseconds (whichever
    * occurs first) before yielding. 
    *
    * @param executor the executor that will run individual tasks across all pipelines
    */
   public PipeliningExecutor(Executor executor, int maxBatchSize) {
      this(executor, maxBatchSize, DEFAULT_MAX_BATCH_DURATION.length(),
            DEFAULT_MAX_BATCH_DURATION.unit());
   }

   /**
    * Constructs a new pipelining executor that uses the specified executor for actually running
    * tasks and runs tasks in batches up to the given duration. Any given pipeline is allowed to
    * process a batch of up to 32 tasks or process for up to the given duration (whichever occurs
    * first) before yielding. 
    *
    * @param executor the executor that will run individual tasks across all pipelines
    */
   public PipeliningExecutor(Executor executor, long maxBatchDuration, TimeUnit unit) {
      this(executor, DEFAULT_MAX_BATCH_SIZE, maxBatchDuration, unit);
   }

   /**
    * Constructs a new pipelining executor that uses the specified executor for actually running
    * tasks and runs up to the given number of tasks in a batch, for up to the given duration. Any
    * given pipeline is allowed to process a batch of up to the given number of tasks or process for
    * up to the given duration (whichever occurs first) before yielding. 
    *
    * @param executor the executor that will run individual tasks across all pipelines
    */
   public PipeliningExecutor(Executor executor, int maxBatchSize, long maxBatchDuration,
         TimeUnit unit) {
      if (maxBatchSize <= 0) {
         throw new IllegalArgumentException("Max batch size must be positive");
      }
      if (maxBatchDuration < 0) {
         throw new IllegalArgumentException("Max batch duration cannot be negative");
      }
      this.executor = requireNonNull(executor);
      this.maxBatchSize = maxBatchSize;
      this.maxBatchDurationNanos = unit.toNanos(maxBatchDuration);
   }

   /**
    * Executes a task in sequence with others submitted for the same pipeline. The task can run
    * concurrently with other tasks submitted for different pipelines.
    * 
    * <p>A best effort is made to mark tasks as failed if they are accepted now but then later
    * rejected by the underlying executor. If the task is {@linkplain SettableFluentFuture settable} or
    * {@linkplain CompletableFuture completable}, it will be set/completed with the cause of
    * failure being a {@link RejectedExecutionException}. Otherwise, if the task is any other type
    * of {@link Future} or {@link Cancellable}, then it will be cancelled.
    * 
    * <p>Note that if the given task is not a form of future task and is not cancellable, <i>the
    * exception is swallowed</i>. Care must be exercised in coordinating task submission with the
    * underlying executor's life cycle to prevent such exception suppression. 
    *
    * @param pipelineKey the key that identifies the pipeline
    * @param task a task
    * @throws NullPointerException if either the pipeline key or task is null
    */
   @Override
   public void execute(K pipelineKey, Runnable task) {
      requireNonNull(pipelineKey);
      requireNonNull(task);
      // Atomically create the pipeline if necessary and enqueue this task therein.
      while (true) {
         Pipeline pipeline = pipelines.get(pipelineKey);
         if (pipeline == null) {
            pipeline = new Pipeline(pipelineKey, task);
            Pipeline existing = pipelines.putIfAbsent(pipelineKey,  pipeline);
            if (existing == null) {
               phaser.register();
               pipeline.start();
               return;
            }
            pipeline = existing;
         }
         if (pipeline.enqueue(task)) {
            return;
         }
         // If we get here, the pipeline we queried from the map concurrently finished its
         // last task and is now terminated. Help it clean up and then try again.
         remove(pipelineKey, pipeline);
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
   void remove(K pipelineKey, Pipeline pipeline) {
      if (pipelines.remove(pipelineKey,  pipeline)) {
         phaser.arriveAndDeregister();
      }
   }
   
   /**
    * Returns true if this executor has no tasks running or queued.
    *
    * @return true if this executor has no tasks running or queued
    * @see #awaitQuiescence()
    */
   public boolean isQuiescent() {
      return phaser.getUnarrivedParties() == 0;
   }

   /**
    * Waits for this executor to reach a point where no tasks are running or queued. Note that this
    * implies nothing about the state of the underlying executor as other sources could still be
    * submitting and running tasks with that executor.
    * 
    * <p>Note that the state of quiescence may be transient and will end as soon as another task is
    * submitted to this executor. This method is usually used after a source of callbacks
    * is stopped. At that point, no other tasks should be submitted. Once this executor has
    * quiesced, it will then be safe to shutdown the underlying executor. If the underlying executor
    * is shutdown too soon, submissions to this pipelining executor could be rejected with a
    * {@link RejectedExecutionException}.
    *
    * @throws InterruptedException if the current thread is interrupted while waiting
    */
   public void awaitQuiescence() throws InterruptedException {
      int phase = phaser.getPhase();
      if (isQuiescent()) {
         return;
      }
      phaser.awaitAdvanceInterruptibly(phase);
   }
   
   /**
    * Waits up to the specified amount of time for this executor to reach a point where no tasks are
    * running or queued.
    * 
    * @param limit the limit of how much time to wait
    * @param unit the unit for the time limit
    * @return true if quiescence was reached or false if the time limit elapsed first
    * @throws InterruptedException if the current thread is interrupted while waiting
    */
   public boolean awaitQuiescence(long limit, TimeUnit unit) throws InterruptedException {
      int phase = phaser.getPhase();
      if (isQuiescent()) {
         return true;
      }
      try {
         phaser.awaitAdvanceInterruptibly(phase, limit, unit);
         return true;
      } catch (TimeoutException e) {
         return false;
      }
   }
   
   /**
    * Aborts all pipelines, draining their queues and attempting to interrupt any currently
    * running tasks. Note that aborting pipelines this way does not prevent subsequent tasks from
    * being submitted.
    *
    * @return a map of all tasks that were drained, grouped by pipeline key
    */
   public Map<K, List<Runnable>> abortAll() {
      Map<K, List<Runnable>> aborted = new HashMap<>(pipelines.size() * 4 / 3);
      for (Iterator<Entry<K, Pipeline>> iter = pipelines.entrySet().iterator();
            iter.hasNext(); ) {
         Entry<K, Pipeline> entry = iter.next();
         iter.remove();
         List<Runnable> tasks = entry.getValue().abort();
         if (!tasks.isEmpty()) {
            aborted.put(entry.getKey(), tasks);
         }
      }
      return Collections.unmodifiableMap(aborted);
   }

   /**
    * Returns true if the specified pipeline has no tasks running or queued.
    *
    * @param pipelineKey the key that identifies the pipeline
    * @return true if the specified pipeline has no tasks running or queued
    * @see #awaitPipelineQuiescence(Object)
    * @throws NullPointerException if either the pipeline key or task is null
    */
   public boolean isPipelineQuiescent(K pipelineKey) {
      return pipelines.containsKey(requireNonNull(pipelineKey));
   }
   
   /**
    * Waits for the specified pipeline to reach a point where no tasks are running or queued.
    * 
    * <p>Note that the state of quiescence may be transient and will end as soon as another task is
    * submitted to for the same pipeline key.
    *
    * @param pipelineKey the key that identifies the pipeline
    * @throws NullPointerException if either the pipeline key or task is null
    * @throws InterruptedException if the current thread is interrupted while waiting
    */
   public void awaitPipelineQuiescence(K pipelineKey) throws InterruptedException {
      Pipeline pipeline = pipelines.get(requireNonNull(pipelineKey));
      if (pipeline == null) {
         // already quiesced
         return;
      }
      pipeline.terminated.await();
   }

   /**
    * Waits up to the specified amount of time for the specified pipeline to reach a point where no
    * tasks are running or queued.
    * 
    * <p>Note that the state of quiescence may be transient and will end as soon as another task is
    * submitted to for the same pipeline key.
    *
    * @param pipelineKey the key that identifies the pipeline
    * @param limit the limit of how much time to wait
    * @param unit the unit for the time limit
    * @return true if quiescence was reached or false if the time limit elapsed first
    * @throws NullPointerException if either the pipeline key or task is null
    * @throws InterruptedException if the current thread is interrupted while waiting
    */
   public boolean awaitPipelineQuiescence(K pipelineKey, long limit, TimeUnit unit)
         throws InterruptedException {
      Pipeline pipeline = pipelines.get(requireNonNull(pipelineKey));
      return pipeline == null || pipeline.terminated.await(limit, unit);
   }
   
   /**
    * Aborts the specified pipeline by draining its queue and attempting to interrupt its currently
    * running task. Aborting the pipeline does not prevent subsequent tasks from being submitted
    * for the same pipeline key.
    *
    * @param pipelineKey the key that identifies the pipeline
    * @return the list of tasks that were drained from the pipeline's queue
    * @throws NullPointerException if either the pipeline key or task is null
    */
   public List<Runnable> abortPipeline(K pipelineKey) {
      Pipeline pipeline = pipelines.get(requireNonNull(pipelineKey));
      return pipeline == null ? Collections.emptyList() : pipeline.abort();
   }
   
   List<Runnable> abortPipelineTasks(K pipelineKey, Predicate<Runnable> filter) {
      Pipeline pipeline = pipelines.get(pipelineKey);
      return pipeline == null ? Collections.emptyList() : pipeline.purge(filter);
   }
   
   /**
    * Creates a new service whose tasks are submitted to the specified pipeline. Shutting down the
    * returned service only affects submissions to the pipeline made by that service. Other tasks
    * submitted from other sources will continue to be accepted. Similarly, shutting down the
    * returned service in no way affects this {@link PipeliningExecutor}, nor its underlying
    * executor.
    * 
    * <p>This method is an adapter. For APIs that expect an {@link ExecutorService}, you can use
    * this to construct an executor that acts like a single-threaded executor and interleaves tasks
    * with others for the same pipeline.
    *
    * @param pipelineKey the key that identifies the pipeline
    * @return a new executor service whose tasks are submitted to the given pipeline
    * @throws NullPointerException if either the pipeline key or task is null
    */
   public FluentExecutorService newExecutorServiceFor(K pipelineKey) {
      return new SinglePipelineExecutorService<>(this, requireNonNull(pipelineKey));
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
      // these two elements are guarded by this object's intrinsic lock
      private final Queue<Runnable> queue = new LinkedList<>();
      private Runnable current;
      
      final CountDownLatch terminated = new CountDownLatch(1);
      private final K pipelineKey;
      
      /**
       * Constructs a new pipeline.
       *
       * @param pipelineKey the key that identifies this pipeline
       * @param firstTask the first task to run in the new pipeline
       */
      Pipeline(K pipelineKey, Runnable firstTask) {
         this.pipelineKey = pipelineKey;
         this.current = firstTask;
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
      
      void start() {
         runBatch();
      }
      
      /**
       * Runs the current batch using the underlying executor. When the batch completes, if there
       * are any remaining tasks, they are scheduled to run in a subsequent batch.
       */
      private void runBatch() {
         try {
            executor.execute(() -> {
               long nanosStart = System.nanoTime();
               long nanosEnd = nanosStart + maxBatchDurationNanos;
               if (nanosEnd < nanosStart) {
                  // avoid overflow
                  nanosEnd = Long.MAX_VALUE;
               } else {
                  assert nanosEnd > nanosStart || maxBatchDurationNanos == 0;
               }
               int taskCount = 0;
               Runnable c;
               synchronized (this) {
                  c = this.current;
               }
               assert c != null;
               while (true) {
                  try {
                     c.run();
                  } catch (Exception e) {
                     // TODO: log?
                  }
                  c = next();
                  if (c == null) {
                     return;
                  }
                  long now = System.nanoTime();
                  if (now >= nanosEnd || ++taskCount == maxBatchSize) {
                     // schedule the rest of the work in another batch and finish
                     runBatch();
                     return;
                  }
               }
            });
         } catch (RejectedExecutionException e) {
            List<Runnable> failed;
            synchronized (this) {
               failed = new ArrayList<>(queue.size() + 1);
               failed.add(current);
               failed.addAll(queue);
               queue.clear();
               current = null; // prevents new tasks from being enqueued with this pipeline
            }
            remove(pipelineKey, this);
            terminated.countDown();
            // fail all tasks
            for (Runnable r : failed) {
               rejectTask(r, e);
            }
         }
      }
      
      /**
       * Aborts the pipeline by draining its queue and attempting to interrupt the currently
       * running task.
       *
       * @return the list of tasks that were removed from the pipeline's queue
       */
      List<Runnable> abort() {
         List<Runnable> aborted;
         Runnable c;
         synchronized (this) {
            aborted = new ArrayList<>(queue);
            queue.clear();
            c = current;
         }
         if (c instanceof Future) {
            // try to interrupt current task
            ((Future<?>) c).cancel(true);
         } else if (c instanceof Cancellable) {
            ((Cancellable) c).cancel(true);
         }
         return Collections.unmodifiableList(aborted);
      }
      
      /**
       * Purges tasks from the queue that match the given filter. This is similar to
       * {@link #abort()}, except its conditional. Only items matching the filter are removed, and
       * the currently running task is only interrupted if it, too, matches the filter.
       *
       * @param filter the predicate used to filter which tasks to purge
       * @return the list of purged tasks
       */
      List<Runnable> purge(Predicate<Runnable> filter) {
         List<Runnable> purged = new ArrayList<>();
         Runnable c;
         synchronized (this) {
            for (Iterator<Runnable> iter = queue.iterator(); iter.hasNext();) {
               Runnable r = iter.next();
               if (filter.test(r)) {
                  iter.remove();
                  purged.add(r);
               }
            }
            c = current;
         }
         if (filter.test(c)) {
            // try to interrupt current task if it matches predicate
            if (c instanceof Future) {
               ((Future<?>) c).cancel(true);
            } else if (c instanceof Cancellable) {
               ((Cancellable) c).cancel(true);
            }
         }
         return Collections.unmodifiableList(purged);
      }
      
      /**
       * Tries to abort the given task with the given exception as the cause. If the given task is
       * a settable or completable future, then it will be marked as failed. If the given task is
       * any other kind of future or is {@linkplain Cancellable cancellable}, it will be cancelled.
       * For all other tasks, nothing can be done to communicate that the task was aborted, so the
       * failure is swallowed.
       *
       * @param failed the task that is being aborted
       * @param t the cause
       */
      private void rejectTask(Runnable failed, Throwable t) {
         if (failed instanceof SettableFluentFuture) {
            ((SettableFluentFuture<?>) failed).setFailure(t);
         } else if (failed instanceof CompletableFuture) {
            ((CompletableFuture<?>) failed).completeExceptionally(t);
         } else if (failed instanceof Future) {
            ((Future<?>) failed).cancel(false);
         } else if (failed instanceof Cancellable) {
            ((Cancellable) failed).cancel(false);
         }
      }
      
      /**
       * Runs the next item in the queue or cleans up this pipeline if the queue is empty.
       */
      private synchronized Runnable next() {
         current = queue.poll();
         if (current == null) {
            remove(pipelineKey, this);
            terminated.countDown();
         }
         return current;
      }
   }
   
   private static class SinglePipelineExecutorService<K> extends AbstractExecutorService
         implements FluentExecutorService {
      private final PipeliningExecutor<K> pipeliner;
      private final K pipelineKey;
      private final Object shutdownLock = new Object();
      private boolean shutdown;
      private final CountDownLatch terminatedLatch = new CountDownLatch(1);
      
      SinglePipelineExecutorService(PipeliningExecutor<K> pipeliner, K pipelineKey) {
         this.pipeliner = pipeliner;
         this.pipelineKey = pipelineKey;
      }
      
      @Override
      public void shutdown() {
         synchronized (shutdownLock) {
            if (shutdown) {
               // already shutdown, no need to submit another shutdown sentinel
               return;
            }
            shutdown = true;
         }
         FluentFuture<Void> sentinel = pipeliner.submit(pipelineKey, () -> {});
         // We don't open the latch from the sentinel in the event that the sentinel task
         // fails due to underlying executor rejecting tasks. So instead we submit a no-op
         // task and then consider this service terminated whenever that task completes,
         // be it successfully or exceptionally.
         sentinel.addListener(f -> { terminatedLatch.countDown(); }, SameThreadExecutor.get());
      }

      @Override
      public List<Runnable> shutdownNow() {
         shutdown();
         return pipeliner.abortPipelineTasks(pipelineKey,
               f -> f instanceof Tagged && ((Tagged) f).tag() == this);
      }

      @Override
      public boolean isShutdown() {
         synchronized (shutdownLock) {
            return shutdown;
         }
      }

      @Override
      public boolean isTerminated() {
         return terminatedLatch.getCount() == 0;
      }

      @Override
      public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
         return terminatedLatch.await(timeout, unit);
      }

      @Override
      public void execute(Runnable command) {
         synchronized (shutdownLock) {
            if (shutdown) {
               throw new RejectedExecutionException();
            }
            pipeliner.execute(pipelineKey, wrap(command));
         }
      }
      
      @SuppressWarnings("unchecked")
      private Tagged wrap(Runnable task) {
         if (task instanceof Tagged) {
            // no need to wrap if it is already tagged
            return (Tagged) task;
         } else if (task instanceof CompletableFuture) {
            return new WrappedCompletableFuture<Object>((CompletableFuture<Object>) task);
         } else if (task instanceof SettableFluentFuture) {
            return new WrappedSettableFuture<Object>((SettableFluentFuture<Object>) task);
         } else if (task instanceof Future) {
            return new WrappedFuture<Object>((Future<Object>) task);
         } else {
            return new WrappedTask<Runnable>(task);
         }
      }
      
      @Override
      protected <T> RunnableFuture<T> newTaskFor(Callable<T> task) {
         return new TaggedSettableFuture<>(task);
      }

      @Override
      protected <T> RunnableFuture<T> newTaskFor(Runnable task, T result) {
         return new TaggedSettableFuture<>(Executors.callable(task, result));
      }

      @Override
      public <T> FluentFuture<T> submit(Callable<T> task) {
         return (FluentFuture<T>) super.submit(task);
      }

      @Override
      public <T> FluentFuture<T> submit(Runnable task, T result) {
         return (FluentFuture<T>) super.submit(task, result);
      }

      @Override
      public FluentFuture<Void> submit(Runnable task) {
         return (FluentFuture<Void>) super.<Void>submit(task, null);
      }
      
      /**
       * All tasks that get issued to underlying {@link PipeliningExecutor} are tagged.
       *
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private interface Tagged extends Runnable {
         /**
          * The task's tag, which will be the {@link SinglePipelineExecutorService} that submitted
          * it.
          */
         SinglePipelineExecutorService<?> tag();
      }
      
      /**
       * A wrapper task. In order to tag tasks provided directly to
       * {@link SinglePipelineExecutorService#execute}, they must be wrapped.
       *
       * @param <T> the type of the wrapped task
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private interface Wrapped<T> extends Tagged, Cancellable {
         /** Returns the original task. */
         T unwrap();
      }
      
      /**
       * A simple, concrete wrapped task.
       *
       * @param <T> the type of the wrapped task
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class WrappedTask<T> implements Wrapped<T> {
         final T wrapped;
         
         WrappedTask(T wrapped) {
            this.wrapped = wrapped;
         }
         
         @Override
         public void run() {
            ((Runnable) wrapped).run();
         }

         @Override
         public boolean cancel(boolean mayInterruptIfRunning) {
            if (wrapped instanceof Cancellable) {
               return ((Cancellable) wrapped).cancel(mayInterruptIfRunning);
            } else {
               return false;
            }
         }

         @Override
         public SinglePipelineExecutorService<?> tag() {
            return SinglePipelineExecutorService.this;
         }
         
         @Override
         public T unwrap() {
            return wrapped;
         }
      }
      
      /**
       * A simple, concrete wrapped task for tasks that implement {@link Future}.
       *
       * @param <V> the type of the wrapped future's value
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class WrappedFuture<V> extends WrappedTask<Future<V>> implements RunnableFuture<V> {
         WrappedFuture(Future<V> future) {
            super(future);
         }
         
         @Override
         public boolean cancel(boolean mayInterruptIfRunning) {
            return unwrap().cancel(mayInterruptIfRunning);
         }

         @Override
         public boolean isCancelled() {
            return unwrap().isCancelled();
         }

         @Override
         public boolean isDone() {
            return unwrap().isDone();
         }

         @Override
         public V get() throws InterruptedException, ExecutionException {
            return unwrap().get();
         }

         @Override
         public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
               TimeoutException {
            return unwrap().get(timeout, unit);
         }
      }
      
      /**
       * A more complex wrapper for tasks that are instances of {@link AbstractFluentFuture}.
       * This must extend {@link AbstractFluentFuture} instead of the above {@link WrappedTask}
       * base class so that the {@link PipeliningExecutor} knows how to mark it as failed in
       * the event that a task is accepted but then later marked as failed due to being rejected by
       * the underlying executor.
       *
       * @param <V> the type of the wrapped future's value
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class WrappedSettableFuture<V> extends SettableFluentFuture<V>
            implements Wrapped<Future<V>>, RunnableFluentFuture<V> {
         private final SettableFluentFuture<V> future;
         
         WrappedSettableFuture(SettableFluentFuture<V> future) {
            this.future = future;
            // keep this future in sync with the wrapped future using mutual callbacks
            this.visitWhenDone(FutureVisitor.of(
                  r -> future.setValue(r),
                  t -> future.setFailure(t),
                  () -> future.cancel(false)));
            future.visitWhenDone(FutureVisitor.of(
                  r -> this.setValue(r),
                  t -> this.setFailure(t),
                  () -> this.setCancelled()));
         }
         
         @Override
         public void run() {
            ((Runnable) future).run();
         }
         
         @Override
         public SinglePipelineExecutorService<?> tag() {
            return SinglePipelineExecutorService.this;
         }
         
         @Override
         public Future<V> unwrap() {
            return future;
         }
      }

      /**
       * A more complex wrapper for tasks that are instances of {@link CompletableFuture}. This is
       * just like {@link WrappedSettableFuture}, except it adapts to an underlying task that is a
       * {@link CompletableFuture}.
       *
       * @param <V> the type of the wrapped future's value
       * 
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class WrappedCompletableFuture<V> extends SettableFluentFuture<V>
            implements Wrapped<Future<V>>, RunnableFluentFuture<V> {
         private final CompletableFuture<V> future;
         
         WrappedCompletableFuture(CompletableFuture<V> future) {
            this.future = future;
            // keep this future in sync with the wrapped future using mutual callbacks
            this.visitWhenDone(FutureVisitor.of(
                  r -> future.complete(r),
                  t -> future.completeExceptionally(t),
                  () -> future.cancel(false)));
            future.whenComplete(
                  (r, t) -> {
                     if (future.isCancelled()) {
                        this.setCancelled();
                     } else if (t != null) {
                        this.setFailure(t);
                     } else {
                        this.setValue(r);
                     }
                  });
         }
         
         @Override
         public void run() {
            ((Runnable) future).run();
         }
         
         @Override
         public SinglePipelineExecutorService<?> tag() {
            return SinglePipelineExecutorService.this;
         }
         
         @Override
         public Future<V> unwrap() {
            return future;
         }
      }
      
      /**
       * A tagged task. This is used by the various {@link SinglePipelineExecutorService#submit}
       * methods.
       *
       * @param <V>
       * @author Joshua Humphries (jhumphries131@gmail.com)
       */
      private class TaggedSettableFuture<V> extends SettableRunnableFluentFuture<V>
            implements Tagged {
         public TaggedSettableFuture(Callable<V> task) {
            super(task);
         }
         
         @Override
         public SinglePipelineExecutorService<?> tag() {
            return SinglePipelineExecutorService.this;
         }
      }
   }
}
