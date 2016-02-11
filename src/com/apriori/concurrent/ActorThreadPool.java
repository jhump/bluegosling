package com.apriori.concurrent;

import static java.util.Objects.requireNonNull;

import com.apriori.concurrent.contended.ContendedInteger;
import com.apriori.concurrent.unsafe.UnsafeLongFieldUpdater;
import com.apriori.concurrent.unsafe.UnsafeReferenceFieldUpdater;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * A thread pool that runs multiple sequential streams. Each stream can be considered a series of
 * tasks associated with an actor. All tasks for an actor are executed sequentially. Parallelism is
 * achieved by running tasks for multiple actors, each actor's stream being independent and running
 * concurrently.
 * 
 * <p>This thread pool uses multiple work queues (one per actor) and work stealing. A given actor
 * is pinned to a thread in the pool. But if that thread gets busy processing tasks for other
 * actors, a different (idle) thread in the pool can "steal" that actor, thus pinning the actor to a
 * new thread.
 * 
 * <p>Pinning actors to threads can help with cache performance and locality of reference. Allowing
 * other threads to steal actors helps with fairness and can improve throughput.
 * 
 * <p>This is similar in functionality to a {@link PipeliningExecutor}, which wraps another
 * executor instead of providing its own thread pool. Because a {@link PipeliningExecutor} does not
 * control the way the executor dispatches tasks to actual threads, it can do neither thread-pinning
 * nor work-stealing.
 * 
 * <p>The definitions for "core" and "maximum" pool sizes differ a bit from a
 * {@link ThreadPoolExecutor}. The core pool consists of the threads that aren't ever allowed to
 * expire. They are kept alive until the executor terminates. The maximum pool size is the peak size
 * allowed in cases of where the number of concurrent active actors exceeds the size of the core
 * pool. A {@link ThreadPoolExecutor} only adds threads beyond the core pool size when its work
 * queue is full. However, an {@link ActorThreadPool} uses unbounded queues, one per actor and adds
 * threads beyond the core pool size whenever an actor is added but all existing threads are busy
 * with previously submitted tasks for other actors.
 *
 * @param <T> the type of actor with which each task is associated
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: more tests
public class ActorThreadPool<T> implements SerializingExecutor<T> {
   
   @SuppressWarnings("rawtypes")
   private static UnsafeLongFieldUpdater<ActorThreadPool> poolSizeLimitsUpdater =
         new UnsafeLongFieldUpdater<>(ActorThreadPool.class, "poolSizeLimits");
   
   @SuppressWarnings("rawtypes")
   private static UnsafeLongFieldUpdater<ActorThreadPool> keepAliveNanosUpdater =
         new UnsafeLongFieldUpdater<>(ActorThreadPool.class, "keepAliveNanos");
   
   static final int DEFAULT_MAX_BATCH_SIZE = 32;
   static final Duration DEFAULT_MAX_BATCH_DURATION = Duration.millis(500);
   static final Duration DEFAULT_KEEP_ALIVE_DURATION = Duration.seconds(30);
   
   public static class Builder {
      private int corePoolSize = Runtime.getRuntime().availableProcessors();
      private int maximumPoolSize = corePoolSize * 4;
      private Duration keepAliveDuration = DEFAULT_KEEP_ALIVE_DURATION;
      private ThreadFactory threadFactory = Executors.defaultThreadFactory();
      private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;
      private Duration maxBatchDuration = DEFAULT_MAX_BATCH_DURATION;
      
      Builder() {
      }
      
      public Builder setPoolSize(int poolSize) {
         return setCorePoolSize(poolSize).setMaximumPoolSize(poolSize);
      }
      
      public Builder setCorePoolSize(int corePoolSize) {
         this.corePoolSize = corePoolSize;
         return this;
      }
      
      public Builder setMaximumPoolSize(int maximumPoolSize) {
         this.maximumPoolSize = maximumPoolSize;
         return this;
      }
      
      public Builder setKeepAlive(long keepAlive, TimeUnit unit) {
         this.keepAliveDuration = Duration.of(keepAlive, unit);
         return this;
      }
      
      public Builder setThreadFactory(ThreadFactory threadFactory) {
         this.threadFactory = threadFactory;
         return this;
      }
      
      public Builder setMaxBatchSize(int maxBatchSize) {
         this.maxBatchSize = maxBatchSize;
         return this;
      }
      
      public Builder setMaxBatchDuration(long maxBatchDuration, TimeUnit unit) {
         this.maxBatchDuration = Duration.of(maxBatchDuration, unit);
         return this;
      }
      
      public <T> ActorThreadPool<T> build() {
         return new ActorThreadPool<>(corePoolSize, maximumPoolSize, keepAliveDuration.length(),
               keepAliveDuration.unit(), threadFactory, maxBatchSize, maxBatchDuration.length(),
               maxBatchDuration.unit());
      }
   }
   
   public static Builder newBuilder() {
      return new Builder();
   }

   /**
    * The thread pool's synchronizer. Used as a lock to synchronize removals and additions of
    * worker threads. Also maintains thread pool's state (number of threads in the pool, whether the
    * thread pool is running or shutdown, etc). Coordinates threads that are awaiting termination
    * of the thread pool.
    */
   final Sync sync = new Sync();
   
   /**
    * A map of all active actors. Each actors has its own queue of tasks.
    */
   final ConcurrentMap<T, ActorQueue<T>> actorQueues = new ConcurrentHashMap<>();
   
   /**
    * The count of active threads, incremented and decremented as threads become active or idle.
    */
   final LongAdder activeCount = new LongAdder(); 
   
   /**
    * The total count of tasks submitted to the thread pool.
    */
   final LongAdder taskCount = new LongAdder();

   /**
    * The total count of tasks completed.
    */
   final LongAdder completedTaskCount = new LongAdder();

   /**
    * The total count of batches executed by the thread pool.
    */
   final LongAdder batchCount = new LongAdder();

   /**
    * The total count of times an idle worker has stolen an actor from another busy worker.
    */
   final LongAdder stealCount = new LongAdder();
   
   /**
    * The pool of workers.
    */
   volatile Worker<T> workers[];
   
   /**
    * Encodes both {@code corePoolSize} (lower 32 bits) and {@code maximumPoolSize} (upper 32 bits).
    * Combined into a single 64-bit field so they can be updated together atomically.
    */
   volatile long poolSizeLimits;
   
   /**
    * Tracks the largest (in number of threads) the pool has ever been.
    */
   int largestPoolSize;
   
   /**
    * The duration, in nanoseconds, for which idle threads are kept alive. 
    */
   volatile long keepAliveNanos;
   
   /**
    * The thread factory used to create new worker threads in the pool.
    */
   volatile ThreadFactory threadFactory;
   
   /**
    * Maximum batch size when processing tasks for an actor.
    */
   final int maxBatchSize;
   
   /**
    * Maximum duration, in nanoseconds, for a single batch.
    */
   final long maxBatchDurationNanos;
   
   /**
    * Creates a new thread pool with the given size. Idle threads are retained for 30 seconds before
    * terminating. Threads are created using a {@linkplain Executors#defaultThreadFactory() default
    * thread factory}. When processing any given actor, a batch will be processed of up to 32 tasks
    * or for up to 500 milliseconds (whichever occurs first) before moving to a different actor.
    *
    * @param poolSize the size of the pool, which is both the core pool size and maximum size
    */
   public ActorThreadPool(int poolSize) {
      this(poolSize, poolSize);
   }

   /**
    * Creates a new thread pool with the given size. Idle threads are retained for 30 seconds before
    * terminating. Threads are created using a {@linkplain Executors#defaultThreadFactory() default
    * thread factory}. When processing any given actor, a batch will be processed of up to 32 tasks
    * or for up to 500 milliseconds (whichever occurs first) before moving to a different actor.
    *
    * @param corePoolSize the size of the core pool
    * @param maximumPoolSize the maximum number of threads allowed in the pool
    */
   public ActorThreadPool(int corePoolSize, int maximumPoolSize) {
      this(corePoolSize, maximumPoolSize, DEFAULT_KEEP_ALIVE_DURATION.length(),
            DEFAULT_KEEP_ALIVE_DURATION.unit(), Executors.defaultThreadFactory());
   }

   /**
    * Creates a new thread pool with the given size and keep-alive time. Threads are created using a
    * {@linkplain Executors#defaultThreadFactory() default thread factory}. When processing any
    * given actor, a batch will be processed of up to 32 tasks or for up to 500 milliseconds
    * (whichever occurs first) before moving to a different actor.
    *
    * @param corePoolSize the size of the core pool
    * @param maximumPoolSize the maximum number of threads allowed in the pool
    * @param keepAliveTime the duration for which an idle thread is retained
    * @param unit the unit for the keep-alive time
    */
   public ActorThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
         TimeUnit unit) {
      this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory());
   }

   /**
    * Creates a new thread pool with the given size and thread factory. Idle threads are retained
    * for 30 seconds before terminating. When processing any given actor, a batch will be processed
    * of up to 32 tasks or for up to 500 milliseconds (whichever occurs first) before moving to a
    * different actor.
    *
    * @param corePoolSize the size of the core pool
    * @param maximumPoolSize the maximum number of threads allowed in the pool
    * @param threadFactory the factory used to create worker threads
    */
   public ActorThreadPool(int corePoolSize, int maximumPoolSize, ThreadFactory threadFactory) {
      this(corePoolSize, maximumPoolSize, DEFAULT_KEEP_ALIVE_DURATION.length(),
            DEFAULT_KEEP_ALIVE_DURATION.unit(), threadFactory);
   }

   /**
    * Creates a new thread pool with the given size, keep-alive time, and thread factory. When
    * processing any given actor, a batch will be processed of up to 32 tasks or for up to 500
    * milliseconds (whichever occurs first) before moving to a different actor. 
    *
    * @param corePoolSize the size of the core pool
    * @param maximumPoolSize the maximum number of threads allowed in the pool
    * @param keepAliveTime the duration for which an idle thread is retained
    * @param unit the unit for the keep-alive time
    * @param threadFactory the factory used to create worker threads
    */
   public ActorThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
         ThreadFactory threadFactory) {
      this(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory,
            DEFAULT_MAX_BATCH_SIZE, DEFAULT_MAX_BATCH_DURATION.length(),
            DEFAULT_MAX_BATCH_DURATION.unit());
   }
   
   /**
    * Creates a new thread pool with the given size, keep-alive time, thread factory, and maximum
    * batch size. When processing any given actor, a batch will be processed of up to the given
    * number of tasks or for up to 500 milliseconds (whichever occurs first) before moving to a
    * different actor. 
    *
    * @param corePoolSize the size of the core pool
    * @param maximumPoolSize the maximum number of threads allowed in the pool
    * @param keepAliveTime the duration for which an idle thread is retained
    * @param unit the unit for the keep-alive time
    * @param threadFactory the factory used to create worker threads
    * @param maxBatchSize the maximum size of a batch of tasks processed for a single worker
    */
   public ActorThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
         ThreadFactory threadFactory, int maxBatchSize) {
      this(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, maxBatchSize,
            DEFAULT_MAX_BATCH_DURATION.length(), DEFAULT_MAX_BATCH_DURATION.unit());
   }

   /**
    * Creates a new thread pool with the given size, keep-alive time, thread factory, and maximum
    * batch size and duration. When processing any given actor, a batch will be processed of up to
    * the given number of tasks or for up to the given duration (whichever occurs first) before
    * moving to a different actor. 
    *
    * @param corePoolSize the size of the core pool
    * @param maximumPoolSize the maximum number of threads allowed in the pool
    * @param keepAliveTime the duration for which an idle thread is retained
    * @param keepAliveUnit the unit for the keep-alive time
    * @param threadFactory the factory used to create worker threads
    * @param maxBatchSize the maximum size of a batch of tasks processed for a single worker
    * @param maxBatchDuration the maximum duration for processing a batch of tasks for a single
    *       worker
    * @param maxBatchDurationUnit the unit for the maximum batch duration
    */
   public ActorThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime,
         TimeUnit keepAliveUnit, ThreadFactory threadFactory, int maxBatchSize,
         long maxBatchDuration, TimeUnit maxBatchDurationUnit) {
      if (corePoolSize < 0) {
         throw new IllegalArgumentException("corePoolSize must be non-negative");
      }
      if (maximumPoolSize <= 0) {
         throw new IllegalArgumentException("maximumPoolSize must be positive");
      }
      if (maximumPoolSize < corePoolSize) {
         throw new IllegalArgumentException("maximumPoolSize must be >= corePoolSize");
      }
      if (keepAliveTime < 0) {
         throw new IllegalArgumentException("keepAliveTime must be non-negative");
      }
      if (maxBatchSize <= 0) {
         throw new IllegalArgumentException("maxBatchSize must be positive");
      }
      if (maxBatchDuration < 0) {
         throw new IllegalArgumentException("maxBatchDuration must be non-negative");
      }
      requireNonNull(threadFactory);
      this.poolSizeLimits = poolSizeLimits(corePoolSize, maximumPoolSize);
      this.keepAliveNanos = keepAliveUnit.toNanos(keepAliveTime);
      this.threadFactory = threadFactory;
      @SuppressWarnings("unchecked")
      Worker<T> ws[]= (Worker<T>[]) new Worker<?>[Math.max(1, corePoolSize)];
      this.workers = ws;
      this.maxBatchSize = maxBatchSize;
      this.maxBatchDurationNanos = maxBatchDurationUnit.toNanos(maxBatchDuration);
   }
   
   /**
    * Computes {@link #poolSizeLimits} value that encodes both the given sizes.
    */
   private long poolSizeLimits(int corePoolSize, int maximumPoolSize) {
      return ((long) maximumPoolSize << 32) | corePoolSize;
   }

   @Override
   public void execute(T t, Runnable task) {
      requireNonNull(t);
      requireNonNull(task);
      while (true) {
         if (isShutdown()) {
            throw new RejectedExecutionException();
         }
         ActorQueue<T> queue = actorQueues.get(t);
         if (queue == null) {
            queue = new ActorQueue<>(this, t, task);
            ActorQueue<T> existing = actorQueues.putIfAbsent(t, queue);
            if (existing == null) {
               if (assignToWorker(queue, null, false)) {
                  taskCount.increment();
                  return;
               } else {
                  actorQueues.remove(t, queue);
                  throw new RejectedExecutionException();
               }
            }
            queue = existing;
         }
         if (queue.add(task)) {
            while (true) {
               if (isShutdown() && queue.remove(task)) {
                  // we were racing with shutdown
                  throw new RejectedExecutionException();
               }
               Worker<T> w = queue.getWorker();
               // worker could be null if the thread that added first task to the queue is still
               // (concurrently) trying to find a worker to accept the new actor
               if (w == null || !w.tryNotify()) {
                  // let other thread find worker and then try again (in case the other thread
                  // can't find a worker due to thread pool being shutdown)
                  Thread.yield();
                  continue;
               }
               taskCount.increment();
               return;
            }
         }
         actorQueues.remove(t, queue);
      }
   }

   /**
    * Assigns the given new actor to a worker. If adequate worker threads exist in the pool (or if
    * no more threads are allowed), an existing worker thread is used. Otherwise, a new worker 
    * thread is added to the pool, and the given actor assigned to it.
    *
    * @param queue the queue for the new actor
    * @param movingFrom the worker that previously owned the actor or {@code null} if this is a new
    *       actor
    * @param force if true the actor is assigned even if the thread pool is shutting down
    * @return true if the actor was assigned to a worker or false if it could not be done due to the
    *       executor shutting down
    */
   boolean assignToWorker(ActorQueue<T> queue, Worker<T> movingFrom, boolean force) {
      int attemptNumber = 0;
      while (true) {
         if (!prestartCoreThread()) {
            // All core threads started. Now check if we need another thread beyond the core pool.
            if (sync.getThreadCount() < actorQueues.size() && startNewThread(queue)) {
               return true;
            }
         }
         if (!force && sync.isShutdown()) {
            return false;
         }
         // Pick a worker. If an actor's queue is exhausted and then a new task comes in, we create
         // a new queue for it. We want that queue to always end up initially pinned to the same
         // thread to reduce cache misses on the core that runs the task. Since non-core threads are
         // potentially transient, we only hash the actor to one of the core threads. But we'll also
         // try to wake up an idle thread, so if the core thread it gets hashed to is busy, an idle
         // one can steal it. (If all threads are busy, it will be left on the preferred one.)
         Worker<T> ws[] = workers;
         int threadCount = Math.min(sync.getThreadCount(), ws.length);
         int coreSize = Math.min(threadCount, getCorePoolSize());
         int idx = queue.hashCode() % coreSize;
         Worker<T> w = ws[idx];
         while (true) {
            // don't assign it to the same worker it's coming from
            if (w == null || w != movingFrom) {
               break;
            }
            idx = (idx + 1) % coreSize;
            w = ws[idx];
         }
         if (w != null && w.add(queue, attemptNumber++, movingFrom)) {
            if (!force && sync.isShutdown() && w.remove(queue)) {
               // lost the race with concurrent shutdown
               return false;
            }
            awakeOneWorker(w);
            return true;
         }
      }
   }

   /**
    * Starts an idle core thread. If the core pool has already been filled with worker threads then
    * no action is taken.
    *
    * @return true if a worker thread was created or false if the core pool is already full
    */
   public boolean prestartCoreThread() {
      if (sync.getThreadCount() >= getCorePoolSize()) {
         return false;
      }
      sync.lock();
      int threadCount = sync.getThreadCount();
      try {
         if (sync.isShutdown()) {
            return false;
         }
         if (threadCount >= getCorePoolSize()) {
            return false;
         }
         Worker<T> ws[] = workers;
         if (threadCount >= ws.length) {
            int newSz = ws.length + (ws.length >> 1);
            if (newSz < ws.length) {
               // overflow
               newSz = Integer.MAX_VALUE;
            }
            ws = Arrays.copyOf(ws, newSz);
         }
         ws[threadCount] = new Worker<>(this, threadCount, null);
         workers = ws;
         updateIfLargest(++threadCount);
         return true;
      } finally {
         sync.unlock(threadCount);
      }
   }

   /**
    * Starts a new worker thread unless the executor is shutting down or the maximum pool size would
    * be exceeded.
    *
    * @param actor the queue for the new actor that the new worker thread will process
    * @return true if a new worker was created or false if the executor is shutting down or the
    *       maximum pool size has already been reached
    */
   private boolean startNewThread(ActorQueue<T> actor) {
      if (sync.getThreadCount() >= getMaximumPoolSize()) {
         return false;
      }
      sync.lock();
      int threadCount = sync.getThreadCount();
      try {
         if (sync.isShutdown()) {
            return false;
         }
         if (threadCount >= getMaximumPoolSize()) {
            return false;
         }
         Worker<T> ws[] = workers;
         if (threadCount >= ws.length) {
            int newSz = ws.length + (ws.length >> 1);
            if (newSz < ws.length) {
               // overflow
               newSz = Integer.MAX_VALUE;
            }
            ws = Arrays.copyOf(ws, newSz);
         }
         ws[threadCount] = new Worker<>(this, threadCount, actor);
         workers = ws;
         updateIfLargest(++threadCount);
         return true;
      } finally {
         sync.unlock(threadCount);
      }
   }
   
   /**
    * Updates internal metrics on the largest observed pool size. If the given thread count is
    * greater than the previous largest, it is updated.
    *
    * @param threadCount the new thread count, for consideration as the largest observed count
    */
   private void updateIfLargest(int threadCount) {
      // we don't need to use CAS because this method is only called while sync is held exclusively
      if (threadCount >= largestPoolSize) {
         largestPoolSize = threadCount;
      }
   }

   /**
    * Starts all core threads in an idle state. Core threads are typically created lazily, as new
    * actor tasks are submitted to the pool. This can be used to start all of them. If the core
    * pool has already been filled then no new worker threads will be created.
    *
    * @return the number of worker threads created, possibly zero
    */
   public int prestartAllCoreThreads() {
      if (sync.getThreadCount() >= getCorePoolSize()) {
         return 0;
      }
      int ret = 0;
      for (; prestartCoreThread(); ret++);
      return ret;
   }

   /**
    * Returns the size of the core pool of worker threads. The actual number of threads in the pool
    * may be fewer since worker threads are created lazily as new actor tasks are submitted.
    *
    * @return the size of the core pool
    */
   public int getCorePoolSize() {
      // narrowing conversion masks away upper bits (leaving only the core pool size)
      return (int) poolSizeLimits;
   }
   
   /**
    * Changes the size of the core pool of worker threads. This may also update the maximum pool
    * size since the maximum must always be greater than or equal to the core pool size. If the new
    * size is less than the previous value, idle worker threads will be allowed to terminate. If the
    * new size is greater than the previous value, new core pool threads may be created to ensure
    * that all active actors have a worker thread but without exceeding the new core pool size.
    *
    * @param corePoolSize the new size of the core pool
    */
   public void setCorePoolSize(int corePoolSize) {
      if (corePoolSize <= 0) {
         throw new IllegalArgumentException("corePoolSize must be positive");
      }
      // atomically update corePoolSize and maybe maximumPoolSize
      int oldSize;
      while (true) {
         long l = poolSizeLimits;
         int m = (int) (l >> 32);
         if (m < corePoolSize) {
            m = corePoolSize;
         }
         long newLimits = poolSizeLimits(corePoolSize, m);
         if (poolSizeLimitsUpdater.compareAndSet(this, l, newLimits)) {
            oldSize = (int) l; // truncating to lower 32-bits leaves corePoolSize
            break;
         }
      }
      if (oldSize > corePoolSize) {
         // wake up idle workers in case they now need to terminate
         forEachWorker(Worker::awake);
      } else if (oldSize < corePoolSize) {
         while (actorQueues.size() > sync.getThreadCount() && prestartCoreThread());
      }
   }
   
   /**
    * Returns the maximum size for the pool of worker threads. The number of threads will not be
    * allowed to exceed this value.
    *
    * @return the maximum size for the pool
    */
   public int getMaximumPoolSize() {
      return (int) (poolSizeLimits >> 32);
   }

   /**
    * Sets the maximum size for the pool of worker threads. This value cannot be set to a number
    * smaller than the core pool size. If the new size is smaller than the previous size, the pool
    * of threads may exceed this value until worker threads complete their current task and then
    * terminate to comply with the new maximum.
    *
    * @param maximumPoolSize the new maximum size for the pool
    */
   public void setMaximumPoolSize(int maximumPoolSize) {
      if (maximumPoolSize <= 0) {
         throw new IllegalArgumentException("maximumPoolSize must be positive");
      }
      // atomically update maximumPoolSize
      int oldSize;
      while (true) {
         long l = poolSizeLimits;
         int c = (int) l;
         if (maximumPoolSize < c) {
            throw new IllegalArgumentException("maximumPoolSize must be >= corePoolSize");
         }
         long newLimits = poolSizeLimits(c, maximumPoolSize);
         if (poolSizeLimitsUpdater.compareAndSet(this, l, newLimits)) {
            oldSize = (int) (l >> 32);
            break;
         }
      }
      // reconcile
      if (oldSize > maximumPoolSize) {
         // wake up idle workers in case they now need to terminate
         forEachWorker(Worker::awake);
      } else if (oldSize < maximumPoolSize) {
         while (actorQueues.size() > sync.getThreadCount() && startNewThread(null));
      }
   }

   /**
    * Returns the duration for which idle (non-core) worker threads are kept alive.
    *
    * @param unit the unit of the returned value
    * @return the duration for which idle threads are kept alive
    */
   public long getKeepAliveTime(TimeUnit unit) {
      return unit.convert(keepAliveNanos, TimeUnit.NANOSECONDS);
   }

   /**
    * Sets the duration for which idle (non-core) worker threads are kept alive.
    *
    * @param keepAliveTime
    * @param unit
    */
   public void setKeepAliveTime(long keepAliveTime, TimeUnit unit) {
      if (keepAliveTime < 0) {
         throw new IllegalArgumentException("keepAliveTime must be non-negative");
      }
      long newValue = unit.toNanos(keepAliveTime);
      long oldValue = keepAliveNanosUpdater.getAndSet(this, newValue);
      if (oldValue < newValue) {
         // keep-alive time has been shrunk, so wake up idle workers to make sure they respect
         // the new keep-alive and terminate sooner if necessary
         forEachWorker(Worker::awake);
      }
   }

   /**
    * Returns the count of active worker threads. This does not include idle threads in the pool,
    * only threads that are actively processing a task.
    *
    * @return the count of active worker threads
    */
   public int getActiveCount() {
      long c = activeCount.longValue();
      assert c > 0;
      assert c <= Integer.MAX_VALUE;
      return (int) c;
   }

   /**
    * Returns the total number of times that an idle worker has stolen an actor from another busy
    * worker.
    *
    * @return the total number of times that a thread has stolen work from another 
    */
   public long getStealCount() {
      return stealCount.longValue();
   }

   /**
    * Returns the total number of tasks submitted to this executor. This includes queued tasks that
    * have not yet begun.
    *
    * @return the total number of tasks submitted to this executor
    */
   public long getTaskCount() {
      return taskCount.longValue();
   }

   /**
    * Returns the total number of tasks completed by this executor. This includes tasks that were
    * cancelled.
    *
    * @return the total number of tasks completed by this executor
    */
   public long getCompletedTaskCount() {
      return completedTaskCount.longValue();
   }

   /**
    * Returns the total number of batches executed by this executor. Each batch will include at
    * least one processed task (which may have been cancelled).
    *
    * @return the total number of batches processed by this executor
    */
   public long getBatchCount() {
      return batchCount.longValue();
   }

   /**
    * Returns the largest observed size of the thread pool.
    *
    * @return the largest observed size of the thread pool
    */
   public int getLargestPoolSize() {
      return largestPoolSize;
   }

   /**
    * Returns the current size of the thread pool. This includes both active and idle workers.
    *
    * @return the current size of the thread pool
    */
   public int getCurrentPoolSize() {
      return sync.getThreadCount();
   }

   /**
    * Returns the factory used to create worker threads.
    *
    * @return the factory used to create worker threads
    */
   public ThreadFactory getThreadFactory() {
      return threadFactory;
   }
   
   /**
    * Sets the factory used to create worker threads. This has no impact on threads already created.
    * Existing threads are retained. The new factory will only be used in the event that new threads
    * are added to the pool.
    *
    * @param threadFactory the new factory used to create worker threads
    */
   public void setThreadFactory(ThreadFactory threadFactory) {
      this.threadFactory = requireNonNull(threadFactory);
   }

   /**
    * Shuts down the executor. No new tasks will be accepted. Attempts to submit tasks after this
    * will result in {@link RejectedExecutionException}s. This starts the termination process.
    * 
    * <p>If the executor has already been shutdown then this has no effect.
    */
   public void shutdown() {
      if (sync.shutdown()) {
         forEachWorker(Worker::awake);
      }
   }

   /**
    * Shuts down the executor immediately, interrupting in-process tasks and removing all queued
    * tasks. No new tasks will be accepted. Attempts to submit tasks after this will result in
    * {@link RejectedExecutionException}s.
    *
    * <p>If the executor has already been shutdown then this has no effect and an empty list of
    * removed tasks is returned.
    * 
    * @return the list of removed tasks that were queued but never executed
    */
   public Map<T, List<Runnable>> shutdownNow() {
      Map<T, List<Runnable>> tasks = new HashMap<>(actorQueues.size() * 4 / 3);
      // using a lock so that concurrent calls to shutdownNow results in one call "winning" and
      // getting all tasks vs. letting them race and possibly split the outstanding tasks
      sync.lock();
      try {
         sync.shutdownWhileLocked();
         for (ActorQueue<T> queue : actorQueues.values()) {
            tasks.put(queue.actor, queue.drain());
         }
      } finally {
         sync.unlock(sync.getThreadCount());
      }
      forEachWorker(Worker::interrupt);
      return tasks;
   }

   /**
    * Returns true if this executor is shutdown. A shutdown executor does not accept any new tasks.
    * But it may continue processing already-queued tasks. When the executor is both shutdown and
    * has completed all tasks, then it has {@linkplain #isTerminated() terminated}.
    *
    * @return true if this executor is shutdown; false if it is still accepting tasks
    */
   public boolean isShutdown() {
      return sync.isShutdown();
   }

   /**
    * Returns true if this executor is terminated. The executor is terminated after a call to
    * {@link #shutdown()} or {@link #shutdownNow()} once all worker threads terminate.
    *
    * @return true if this executor is terminated
    */
   public boolean isTerminated() {
      return sync.isTerminated();
   }

   /**
    * Returns true if this executor is in the process of terminating. This is the state after a call
    * to {@link #shutdown()} or {@link #shutdownNow()} but before all worker threads have finished.
    * While terminating, the executor is waiting on in-progress and queued tasks to complete. 
    *
    * @return true if this executor is in the process of terminating
    */
   public boolean isTerminating() {
      return sync.isTerminating();
   }

   /**
    * Blocks until this executor has finished terminating. If the executor is already terminated
    * then this returns immediately.
    *
    * @param timeLimit the limit of time to wait
    * @param unit the unit for the time limit
    * @return true if the executor terminated or false if the time limit elapsed first
    * @throws InterruptedException if the thread is interrupted while waiting
    */
   public boolean awaitTermination(long timeLimit, TimeUnit unit) throws InterruptedException {
      return sync.awaitTermination(unit.toNanos(timeLimit));
   }
   
   /**
    * Executes the given consumer for each worker. The given consumer may be invoked for the same
    * worker repeatedly if interference is detected and the operation retried.
    * 
    * <p>This is used to wake up or interrupt worker threads, like to make sure they notice changes
    * in state when the executor is shutdown or when idle threads should terminate after the pool
    * size shrinks (via {@link #setCorePoolSize(int)} or {@link #setMaximumPoolSize(int)}).
    */
   void forEachWorker(Consumer<Worker<T>> cons) {
      long stamp = sync.getStamp();
      while (true) {
         if (!Sync.validateStamp(stamp)) {
            // there is a concurrent change to the worker pool in progress; try again
            Thread.yield();
            stamp = sync.getStamp();
            continue;
         }
         Worker<T> ws[] = workers;
         for (Worker<T> w : ws) {
            if (w == null) {
               break;
            }
            cons.accept(w);
         }
         long newStamp = sync.getStamp();
         if (newStamp == stamp) {
            // no interference, so we're done
            break;
         }
         stamp = newStamp; // interference detected, try again
      }
   }
   
   /**
    * Awakens a single worker. This starts at the index after that of the given worker and examines
    * each one in turn to find one that can be awakened.
    *
    * @param start the worker sending the signal; traversal of workers starts adjacent to it
    */
   public void awakeOneWorker(Worker<T> start) {
      long stamp = sync.getStamp();
      while (true) {
         Worker<T> ws[] = workers;
         int s;
         if (!Sync.validateStamp(stamp) || ws[s = start.getIndex()] != start) {
            // there is a concurrent change to the worker pool in progress; try again
            Thread.yield();
            stamp = sync.getStamp();
            continue;
         }
         int len = ws.length;
         for (int i = s + 1; i != s; i++) {
            if (i >= len) {
               i = -1;
               continue;
            }
            Worker<T> w = ws[i];
            if (w == null) {
               if (i < s) {
                  // can't get to loop termination condition because pool has been concurrently
                  // changed, so break now
                  break;
               }
               i = -1;
               continue;
            }
            if (w != start && w.awake()) {
               return;
            }
         }
         // double-check stamp because if something changed then we need to try again
         long newStamp = sync.getStamp();
         if (newStamp == stamp) {
            return;
         }
         stamp = newStamp;
      }
   }

   /**
    * Tries to steal an actor from a busy worker so that it can be processed by an idle worker.
    *
    * @param stealer the idle worker that wants to steal work
    * @return the stolen task to run or {@code null} if there is no work to steal
    */
   ActorQueue<T> tryStealFromOtherWorker(Worker<T> stealer) {
      long stamp = sync.getStamp();
      while (true) {
         Worker<T> ws[] = workers;
         int s;
         if (!Sync.validateStamp(stamp) || ws[s = stealer.getIndex()] != stealer) {
            // there is a concurrent change to the worker pool in progress; try again
            Thread.yield();
            stamp = sync.getStamp();
            continue;
         }
         int len = ws.length;
         for (int i = s + 1; i != s; i++) {
            if (i >= len) {
               i = -1;
               continue;
            }
            Worker<T> w = ws[i];
            if (w == null) {
               if (i < s) {
                  // can't get to loop termination condition because pool has been concurrently
                  // changed, so break now
                  break;
               }
               i = -1;
               continue;
            }
            if (w != stealer) {
               ActorQueue<T> q = w.tryStealActor(stealer);
               if (q != null) {
                  return q;
               }
            }
         }
         // double-check stamp because if something changed then we need to try again
         long newStamp = sync.getStamp();
         if (newStamp == stamp) {
            return null;
         }
         stamp = newStamp;
      }
   }
   
   /**
    * Tries to remove the given worker thread from the pool. If the pool has already shrunk to just
    * the core pool threads, this returns false and the thread is retained. This will also return
    * false if a concurrent thread has enqueued a new task for this worker to process.
    *
    * @param w the worker that might be removed
    * @param wStamp the worker's stamp, used to detect concurrent submission of tasks to the worker,
    *       or -1 to indicate that the worker should be removed even if a task is concurrently
    *       submitted
    * @return true if the worker was removed from the pool; false otherwise
    */
   boolean tryRemoveWorker(Worker<T> w, long wStamp) {
      if (sync.getThreadCount() <= getCorePoolSize()) {
         return false;
      }
      sync.lock();
      int threadCount = sync.getThreadCount();
      try {
         // check again now that we have the lock
         if (threadCount <= getCorePoolSize()) {
            return false;
         }
         
         int wIdx;
         if (wStamp == -1) {
            wIdx = w.nullify();
         } else {
            wIdx = w.tryNullify(wStamp);
            if (wIdx == -1) {
               return false;
            }
         }
         assert wIdx < threadCount && wIdx >= 0;
         assert workers[wIdx] == w;
         if (wIdx == --threadCount) {
            // we're the last worker in array? just clear out unused slot
            workers[wIdx] = null;
         } else {
            // instead of shuffling array, just swap places with last worker
            Worker<T> ws[] = workers;
            Worker<T> last = ws[threadCount];
            int oldIndex = last.move(wIdx);
            assert oldIndex == threadCount;
            // we increment the stamp around these operations so that a concurrent thread that is
            // traversing the workers array can detect interference
            sync.stamp(true);
            ws[wIdx] = last;
            ws[threadCount] = null;
            sync.stamp(false);
         }
         return true;
      } finally {
         sync.unlock(threadCount);
      }
   }

   /**
    * Removes the given worker thread from the pool. This method is only used when the executor is
    * terminating and all threads are being removed.
    *
    * @param w the worker thread to remove
    */
   void removeWorker(Worker<T> w) {
      sync.lock();
      int threadCount = sync.getThreadCount();
      try {
         int wIdx = w.nullify();
         if (wIdx == --threadCount) {
            // we're the last worker in array? just clear out unused slot
            workers[wIdx] = null;
         } else {
            // instead of shuffling array, just swap places with last worker
            Worker<T> ws[] = workers;
            Worker<T> last = ws[threadCount];
            int oldIndex = last.move(wIdx);
            assert oldIndex == threadCount: "" + oldIndex + " != " + threadCount;
            // we increment the stamp around these operations so that a concurrent thread that is
            // traversing the workers array can detect interference
            sync.stamp(true);
            ws[wIdx] = last;
            ws[threadCount] = null;
            sync.stamp(false);
         }
      } finally {
         sync.unlock(threadCount);
      }
   }

   /**
    * The synchronizer for the thread pool. This serves as a lock, for synchronizing attempts to
    * add and remove threads. It also serves as a barrier, for threads waiting for the thread pool
    * to shutdown. Finally, it serves as the source-of-truth for the number of active threads. This
    * number reflects intent so, at any moment in time, may deviate from reality, like when a
    * new "reserved" thread hasn't yet started or when an idle thread is concurrently terminating.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Sync extends AbstractQueuedLongSynchronizer {
      private static final long serialVersionUID = -8509921792932608294L;

      private static final long LOCK_MASK =         0x8000000000000000L;
      
      private static final long STATE_MASK =        0x6000000000000000L;
      private static final long RUNNING_STATE =     0x0000000000000000L;
      private static final long TERMINATING_STATE = 0x2000000000000000L;
      private static final long TERMINATED_STATE =  0x4000000000000000L;
      
      private static final long STAMP_WRITING =     0x1000000000000000L;
      private static final long STAMP_WIDE_MASK =   0x1fffffff00000000L;
      private static final long STAMP_MASK =        0x0fffffff00000000L;
      private static final long STAMP_INC =         0x0000000100000000L;
      private static final long STAMP_SHIFT = 32;

      // We don't define a THREAD_COUNT_MASK because a narrowing conversion from long to int
      // suffices to mask away the high bits.
      
      // arguments for #acquire(long)
      private static final long MODE_LOCK = 1;
      private static final long MODE_SHUTDOWN = 2;

      /**
       * Set of threads that are waiting for the executor to terminate. We don't use the
       * synchronizers waiter nodes because those are FIFO waiters to acquire the sync (as a lock).
       * These threads aren't waiting for that, so mixing them can lead to liveness problems. 
       */
      final Queue<Thread> terminateWaiters = new ConcurrentLinkedQueue<>();
      
      Sync() {
      }
      
      /**
       * Returns true if the executor is shutdown.
       *
       * @return true if the executor is shutdown
       */
      boolean isShutdown() {
         return (getState() & STATE_MASK) != RUNNING_STATE;
      }

      /**
       * Returns true if the executor is terminated.
       *
       * @return true if the executor is terminated
       */
      boolean isTerminated() {
         return (getState() & STATE_MASK) == TERMINATED_STATE;
      }

      /**
       * Returns true if the executor is terminating.
       *
       * @return true if the executor is terminating
       */
      boolean isTerminating() {
         return (getState() & STATE_MASK) == TERMINATING_STATE;
      }

      /**
       * Returns the stamp for the pool. Every lock and unlock operation updates the stamp so
       * a concurrent reader can detect a change without having to block for a shared lock.
       *
       * @return the stamp
       */
      long getStamp() {
         return getState() & STAMP_WIDE_MASK;
      }
      
      /**
       * Validates the given stamp. The stamp is invalid if a "write bit" is set, indicating that
       * another thread is concurrently modifying the worker pool. In this case, the stamp observer
       * should yield and read the stamp again until it sees a valid stamp.
       *
       * @param stamp a stamp
       * @return true if the stamp is valid
       */
      static boolean validateStamp(long stamp) {
         return (stamp & STAMP_WRITING) == 0;
      }

      /**
       * Returns the number of threads in the pool. While this synchronizer is locked, a thread may
       * be created and then this count updated. So it is possible for the actual number of threads
       * and the value reported by this method to be off by one.
       *
       * @return the number of threads in the pool
       */
      int getThreadCount() {
         // narrowing conversion masks away the unneeded bits
         return (int) getState();
      }
      
      /**
       * Waits until the executor terminates.
       *
       * @param nanosTimeout the duration, in nanoseconds, to wait
       * @return true if the executor terminated; false if the time limit elapsed first
       * @throws InterruptedException if the thread is interrupted while waiting
       */
      boolean awaitTermination(long nanosTimeout) throws InterruptedException {
         long start = System.nanoTime();
         if (Thread.interrupted()) {
            throw new InterruptedException();
         }
         if (isTerminated()) {
            return true;
         }
         Thread th = Thread.currentThread();
         terminateWaiters.add(th);
         try {
            while (true) {
               if (isTerminated()) {
                  return true;
               }
               long nanosLeft = nanosTimeout - System.nanoTime() + start;
               if (nanosLeft <= 0) {
                  return false;
               }
               LockSupport.parkNanos(nanosLeft);
               if (Thread.interrupted()) {
                  throw new InterruptedException();
               }
            }
         } finally {
            terminateWaiters.remove(th);
         }
      }
      
      /**
       * Marks the executor as shutdown. The return flag is does not guarantee that the calling
       * thread was the one to shut the executor down; e.g. it is possible for two racing threads to
       * both call this method and both get a return value of {@code true}. (So don't use it to make
       * important decisions that could effect correctness.)
       * 
       * <p>If another thread has this synchronizer locked (e.g. is adjusting the size of the pool)
       * then this will block until the lock is available. However, the lock is not held when this
       * method returns.
       *
       * @return true if the executor was not already shutdown at the onset of this method
       */
      boolean shutdown() {
         if (isShutdown()) {
            return false;
         }
         acquire(MODE_SHUTDOWN);
         release(-1); // dummy argument, just to notify any other thread waiting for the lock
         return true;
      }

      /**
       * Marks the executor as shutdown, requiring that the caller has already locked this
       * synchronizer.
       */
      void shutdownWhileLocked() {
         long s = getState();
         assert (s & LOCK_MASK) != 0;
         if ((s & STATE_MASK) == RUNNING_STATE) {
            // we already hold the lock, so no need for CAS
            setState(s | TERMINATING_STATE);
         }
      }

      /**
       * Locks this synchronizer in exclusive mode. Non-reentrant.
       */
      void lock() {
         acquire(MODE_LOCK);
      }

      /**
       * Unlocks this synchronizer and records the thread count as given.
       *
       * @param newThreadCount the new thread count
       */
      void unlock(int newThreadCount) {
         assert newThreadCount >= 0;
         release(newThreadCount);
      }

      /**
       * Increments the stamp value. This is used to indicate changes to other threads when no
       * other detectable change has yet been recorded (like moving a worker in the workers array).
       * Such changes should only be made while this sync is locked.
       * 
       * <p>The argument indicates whether to set a "write bit" in the stamp. This should bit should
       * be set if an operation is modifying the worker pool.
       * 
       * @param writing true if the caller is writing to the worker pool and thus the "write bit" in
       *       the stamp should be set; false if the "write bit" should be unset
       */
      void stamp(boolean writing) {
         while (true) {
            long s = getState();
            assert (s & LOCK_MASK) != 0;
            long newState;
            newState = (s & ~STAMP_WIDE_MASK) | ((s + STAMP_INC) & STAMP_MASK);
            if (writing) {
               newState |= STAMP_WRITING;
            }
            if (compareAndSetState(s, newState)) {
               break;
            }
         }
      }

      @Override
      protected boolean tryAcquire(long mode) {
         while (true) {
            long s = getState();
            if ((s & LOCK_MASK) != 0) {
               // already locked so we must wait
               return false;
            }
            long newState;
            boolean terminated = false;
            if (mode == MODE_SHUTDOWN) {
               if ((s & STATE_MASK) != RUNNING_STATE) {
                  // already shutdown
                  return true;
               }
               int threadCount = (int) s;
               if (threadCount == 0) {
                  newState = s | TERMINATED_STATE;
                  terminated = true;
               } else {
                  newState = s | TERMINATING_STATE;
               }
            } else {
               assert mode == MODE_LOCK;
               newState = (s & ~STAMP_MASK) | ((s + STAMP_INC) & STAMP_MASK) | LOCK_MASK;
            }
            if (compareAndSetState(s, newState)) {
               if (terminated) {
                  // if we just terminated the executor, wake up the waiters
                  while (true) {
                     Thread th = terminateWaiters.poll();
                     if (th == null) {
                        break;
                     }
                     LockSupport.unpark(th);
                  }
               }
               return true;
            }
         }
      }
      
      @Override
      protected boolean tryRelease(long newThreadCount) {
         if (newThreadCount == -1) {
            // dummy release to wake up any queued waiters
            return true;
         }
         assert newThreadCount <= Integer.MAX_VALUE;
         boolean terminated = false;
         while (true) {
            long s = getState();
            assert (s & LOCK_MASK) != 0;
            long st = s & STATE_MASK;
            if (newThreadCount == 0 && st == TERMINATING_STATE) {
               st = TERMINATED_STATE;
               terminated = true;
            }
            long newState = st | ((s + STAMP_INC) & STAMP_MASK) | newThreadCount;
            if (compareAndSetState(s, newState)) {
               if (terminated) {
                  // if we just terminated the executor, wake up the waiters
                  while (true) {
                     Thread th = terminateWaiters.poll();
                     if (th == null) {
                        break;
                     }
                     LockSupport.unpark(th);
                  }
               }
               return true;
            }
         }
      }
      
      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         long s = getState();
         long st = s & STATE_MASK;
         if (st == TERMINATED_STATE) {
            sb.append("Terminated, ");
         } else if (st == TERMINATING_STATE) {
            sb.append("Terminating, ");
         } else {
            assert (s & STATE_MASK) == RUNNING_STATE;
            sb.append("Running, ");
         }
         sb.append((int) s);
         sb.append(" thread(s), stamp = ");
         sb.append((s & STAMP_MASK) >> STAMP_SHIFT);
         if ((s & LOCK_MASK) == 0) {
            sb.append(", Unlocked");
         } else {
            sb.append(", Locked");
         }
         return sb.toString();
      }
   }

   /**
    * Represents an actor and holds the queue of tasks for the actor. An actor (and its queue) is
    * pinned to a worker thread. When the queue is exhausted, it is removed. If a subsequent task
    * for the same actor is submitted to the executor, a new queue is created.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ActorQueue<T> {
      
      @SuppressWarnings("rawtypes")
      private static UnsafeReferenceFieldUpdater<ActorQueue, Worker> workerUpdater =
            new UnsafeReferenceFieldUpdater<>(ActorQueue.class, Worker.class, "worker");
      
      private enum TaskResult {
         /**
          * Indicates that a task was found and is now marked as running.
          */
         TASK_FOUND,
         
         /**
          * Indicates that no task was found; this queue is empty.
          */
         NO_TASK,
         
         /**
          * Indicates that no task is ready because one is still running.
          */
         NOT_READY
      }
      
      private static final int STATE_REMOVED =    0x80000000;
      private static final int STATE_RUNNING =    0x40000000;
      private static final int STATE_COUNT_MASK = 0x3fffffff;
      
      /**
       * The thread pool that processes tasks in this actor queue.
       */
      private final ActorThreadPool<?> owner;
      
      /**
       * The actor to which this queue belongs.
       */
      final T actor;
      
      /**
       * The actual queue of tasks.
       */
      private final ConcurrentLinkedDeque<Runnable> tasks = new ConcurrentLinkedDeque<>();
      private final ContendedInteger state = new ContendedInteger();
      private Runnable current;
      private volatile Worker<T> worker;
      
      /**
       * Constructs a new actor queue for the given actor, in the given thread pool, with the given
       * initial task.
       *
       * @param owner the executor that owns this actor queue
       * @param actor the actor to whose tasks are held in this queue
       * @param initialTask the initial task to enqueue
       */
      ActorQueue(ActorThreadPool<?> owner, T actor, Runnable initialTask) {
         this.owner = owner;
         this.actor = actor;
         this.state.set(1);
         tasks.addLast(initialTask);
      }
      
      @Override
      public int hashCode() {
         return actor.hashCode();
      }
      
      /**
       * Queries for the next task in this actor's queue. On success, the task can be executed by
       * running {@code this}.
       *
       * @return {@link #NO_TASK} if there are no more tasks and this actor is in the process of
       *       removal, {@link #NOT_READY} if this actor's current task is still being processed,
       *       or {@link #TASK_FOUND} if a task was found and de-queued.
       * @see #run()
       */
      TaskResult nextTask() {
         while (true) {
            int s = state.get();
            if ((s & STATE_REMOVED) != 0) {
               return TaskResult.NO_TASK;
            }
            if ((s & STATE_RUNNING) != 0) {
               return TaskResult.NOT_READY;
            }
            if (s != 0) {
               // atomically decrement task count and mark as running
               if (state.compareAndSet(s, (s - 1) | STATE_RUNNING)) {
                  while (true) {
                     Runnable r = tasks.pollFirst();
                     if (r != null) {
                        this.current = r;
                        return TaskResult.TASK_FOUND;
                     }
                     // state was reserved for a task, but we're racing with thread
                     // that is adding it to queue
                     Thread.yield();
                  }
               }
            } else if (state.compareAndSet(s, STATE_REMOVED)) {
               owner.actorQueues.remove(actor, this);
               return TaskResult.NO_TASK;
            }
         }
      }
      
      /**
       * Runs the actor's current task. The current task is the one most recently de-queued from a
       * call to {@link #nextTask()}. No further tasks can be de-queued and run until this one
       * completes.
       */
      void runTask() {
         try {
            current.run();
         } finally {
            owner.completedTaskCount.increment();
            current = null;
            // no longer running
            while (true) {
               int s = state.get();
               assert (s & STATE_RUNNING) != 0;
               if (state.compareAndSet(s, s & ~STATE_RUNNING)) {
                  break;
               }
            }
         }
      }
      
      /**
       * Adds a task to this worker's queue.
       *
       * @param r the task
       * @return true if the task was successfully added; false if this actor is not accepting
       *       tasks because it is in the process of removal
       */
      boolean add(Runnable r) {
         while (true) {
            int s = state.get();
            if ((s & STATE_REMOVED) != 0) {
               return false;
            }
            if ((s & STATE_COUNT_MASK) == STATE_COUNT_MASK) {
               throw new RejectedExecutionException("Actor queue has too many queued tasks");
            }
            if (state.compareAndSet(s, s + 1)) {
               break;
            }
         }
         // successfully reserved a spot, so we can now add to queue
         tasks.addLast(r);
         return true;
      }
      
      /**
       * Removes the given task from this actor's queue.
       *
       * @param r the task
       * @return true if the task was successfully removed; false otherwise
       */
      boolean remove(Runnable r) {
         if (tasks.remove(r)) {
            while (true) {
               // we got it out of the queue, but now we must "un-reserve" its slot
               int s = state.get();
               if ((s & STATE_COUNT_MASK) == 0) {
                  // doh! another thread has "de-queued" the item, so we have to put it back
                  tasks.addLast(r);
                  return false;
               }
               if (s == 1) {
                  // if we're removing last item from the queue, expunge it
                  if (state.compareAndSet(s, STATE_REMOVED)) {
                     owner.actorQueues.remove(actor, this);
                     return true;
                  }
               } else if (state.compareAndSet(s, s - 1)) {
                  return true;
               }
            }
         }
         return false;
      }
      
      /**
       * Drains the contents of this actor's queue and returns the contents in a list. On return,
       * this actor's queue will be empty.
       *
       * @return a list of the pending tasks that were drained
       */
      List<Runnable> drain() {
         ArrayList<Runnable> results = new ArrayList<>();
         while (!tasks.isEmpty()) {
            // poll from the tail so we don't interfere with a worker processing from the head
            Runnable r = tasks.peekLast();
            if (r != null && remove(r)) {
               results.add(r);
            }
         }
         results.trimToSize();
         // since we polled from the end, it's in reverse order, so fix it up
         Collections.reverse(results);
         return results;
      }

      /**
       * Gets this actor's worker thread. Might be null if this actor hasn't yet been added to a
       * worker.
       *
       * @return this actor's worker thread or {@code null}
       */
      Worker<T> getWorker() {
         return worker;
      }

      /**
       * Sets this actor's worker thread. This is used when stealing the actor. Other attempts to
       * set the value use a {@linkplain #compareAndSetWorker(Worker, Worker) CAS} operation to
       * detect interference.
       *
       * @param worker the actor's new worker thread
       */
      void setWorker(Worker<T> worker) {
         this.worker = worker;
      }
      
      /**
       * Sets this actor's worker thread only if it matches the expected value.
       *
       * @param expected the expected worker thread
       * @param updated the new worker thread
       * @return true if the worker thread was updated; false if the actual value did not match the
       *       expected value
       */
      boolean compareAndSetWorker(Worker<T> expected, Worker<T> updated) {
         return workerUpdater.compareAndSet(this, expected, updated);
      }
   }
   
   /**
    * A worker thread in the pool. A worker has a set of actors whose queues it processes. If the
    * worker gets busy processing a task, an idle worker can steal one of its actors. Non-core idle
    * workers will time out and terminate. All other workers terminate when the executor is
    * shutdown.
    *
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class Worker<T> implements Runnable {
      
      @SuppressWarnings("rawtypes")
      private static UnsafeLongFieldUpdater<Worker> workerIndexAndStampUpdater =
            new UnsafeLongFieldUpdater<>(Worker.class, "workerIndexAndStamp");

      private static final long WORKER_PARK_MASK =  0x0000000300000000L;
      private static final long WORKER_PARKED    =  0x0000000100000000L;
      private static final long WORKER_UNPARKED  =  0x0000000200000000L;
      private static final long WORKER_STAMP_MASK = 0xfffffffc00000000L;
      private static final long WORKER_STAMP_INC =  0x0000000400000000L;
      private static final long WORKER_INDEX_MASK = 0x00000000ffffffffL;
      
      private final ActorThreadPool<T> owner;
      private final Thread thread;
      private final ConcurrentLinkedDeque<ActorQueue<T>> actors = new ConcurrentLinkedDeque<>();
      private volatile long workerIndexAndStamp;
      
      /**
       * Creates a new worker with the given initial actor to process.
       *
       * @param owner the executor that owns the worker
       * @param index the position of this worker in the pool's worker array
       * @param actor the initial actor to process
       */
      Worker(ActorThreadPool<T> owner, int index, ActorQueue<T> actor) {
         this.owner = owner;
         this.thread = owner.threadFactory.newThread(this);
         if (actor != null) {
            actors.add(actor);
            actor.setWorker(this);
         }
         this.thread.start();
         owner.activeCount.increment(); // start off active
         assert index >= 0;
         this.workerIndexAndStamp = index;
      }
      
      /**
       * Gets the index in the array of workers where this worker can be found.
       *
       * @return this worker's index in the executor's array of workers
       */
      int getIndex() {
         // narrowing conversion effectively masks away the upper 32 bits (the stamp)
         return (int) workerIndexAndStamp;
      }
      
      /**
       * The worker thread's main logic. This effectively loops through pinned actors, de-queuing
       * a task and processing it. Work can be stolen from another thread if this worker's queue
       * is exhausted. The method exits if the thread remains idle for the configured keep-alive
       * time or when the executor is terminating.
       */
      @Override
      public void run() {
         long lastRunNanos = System.nanoTime();
         boolean terminated = false;
         try {
            while (true) {
               boolean shutdown = owner.isShutdown();
               long stamp = workerIndexAndStamp & WORKER_STAMP_MASK;
               // clear interrupt status before starting next task
               Thread.interrupted();
               ActorQueue<T> q = findActor();
               if (q != null) {
                  // process a batch for this actor
                  int batchSize = 0;
                  long batchStartNanos = System.nanoTime();
                  while (true) {
                     try {
                        q.runTask();
                     } catch (Throwable t) {
                        try {
                           thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
                        } catch (Throwable t2) {
                           // eek!
                           t2.printStackTrace();
                        }
                     }
                     lastRunNanos = System.nanoTime();
                     // if maximum pool size shrank, we may need to shed this worker
                     if (owner.getCurrentPoolSize() > owner.getMaximumPoolSize()
                           && owner.tryRemoveWorker(this, -1)) {
                        while (true) {
                           ActorQueue<T> actor = actors.poll();
                           if (actor == null) {
                              break;
                           }
                           boolean assigned = owner.assignToWorker(actor, this, true);
                           assert assigned;
                        }
                        terminated = true;
                        break;
                     }
                     // try to process next task in the batch
                     if (lastRunNanos - batchStartNanos >= owner.maxBatchDurationNanos
                           || ++batchSize >= owner.maxBatchSize) {
                        // at the limit for one batch
                        break;
                     }
                     ActorQueue.TaskResult result = q.nextTask();
                     if (result != ActorQueue.TaskResult.TASK_FOUND) {
                        // no next task for this batch
                        if (result == ActorQueue.TaskResult.NO_TASK) {
                           actors.remove(q); // clean-up
                        }
                        break;
                     }
                  }
                  owner.batchCount.increment();
               } else {
                  // no task ready so we're no longer active
                  owner.activeCount.decrement();
                  if (shutdown) {
                     break;
                  } else {
                     long idleNanos = System.nanoTime() - lastRunNanos;
                     long nanosLeft = owner.keepAliveNanos - idleNanos;
                     if (nanosLeft <= 0
                           || owner.getCurrentPoolSize() > owner.getMaximumPoolSize()) {
                        if (owner.tryRemoveWorker(this, stamp)) {
                           terminated = true;
                           break;
                        }
                        // can't remove this worker (it must be a core thread), so
                        // just wait for another task
                        nanosLeft = 0;
                     }
                     // clear interrupt status before waiting for next task
                     Thread.interrupted();
                     // findNext() above will mark this worker as parked if it finds nothing
                     if (nanosLeft == 0) {
                        LockSupport.park();
                     } else {
                        LockSupport.parkNanos(nanosLeft);
                     }
                     markNotParked();
                     owner.activeCount.increment(); // active again
                  }
               }
            }
         } catch (RuntimeException | Error e) {
            // TODO: here just for testing/debugging
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("Thread: " + Thread.currentThread().getName());
            e.printStackTrace(pw);
            pw.close();
            System.err.println(sw.toString());
            //throw e;
         } finally {
            if (!terminated) {
               owner.removeWorker(this);
               assert owner.isShutdown();
               // Ideally, we'd also assert that there are no actors in need of workers. But this
               // can't reliably be done due to races, due to the way actors and workers operate
               // without locks. We can't even assert that our own set of actors is empty since a
               // steal could be in-progress (where the actor has been stolen, but is still
               // attributed to this worker instead of the stealer).
            }
         }
      }
      
      /**
       * Finds the next actor that this worker should process. This de-queues a work item from the
       * actors that are pinned to this worker. When multiple actors are pinned to a thread, the
       * thread will round-robin over the available actors. If no work is available in the worker's
       * queue, it will try to steal work from another worker thread.
       *
       * @return the next actor to process or {@code null} if no work is available
       */
      private ActorQueue<T> findActor() {
         ActorQueue<T> q = doFindActor();
         if (q != null) {
            return q;
         }
         // Not found? We'll mark ourselves as parked, but we need to try again. Otherwise, a new
         // submission might have happened while not marked as parked (meaning submitter won't have
         // attempted to awake us) and we'll miss it.
         markParked();
         q = doFindActor();
         if (q != null) {
            // Found one! So we can mark ourselves as not parked since we have a task to process.
            if (markNotParked()) {
               // if we were awoken while we repeated the search, we need to try to propagate that
               // unpark to another idle worker, just to make sure we don't miss a chance to steal
               // on a new submission
               owner.awakeOneWorker(this);
            }
         }
         return q;
      }
      
      /**
       * Marks this worker as parked. This is done just before the worker goes idle.
       */
      private void markParked() {
         while (true) {
            long w = workerIndexAndStamp;
            assert (w & WORKER_PARK_MASK) == 0;
            long parked = w | WORKER_PARKED;
            if (workerIndexAndStampUpdater.compareAndSet(this, w, parked)) {
               return;
            }
         }
      }

      /**
       * Marks this worker as not parked. This is done right after the worker wakes up, or if the
       * worker was prevented from becoming idle.
       *
       * @return true if another thread tried to unpark this worker
       */
      private boolean markNotParked() {
         while (true) {
            long w = workerIndexAndStamp;
            assert (w & WORKER_PARK_MASK) != 0;
            long notParked = w & ~WORKER_PARK_MASK;
            if (workerIndexAndStampUpdater.compareAndSet(this, w, notParked)) {
               return (w & WORKER_UNPARKED) != 0;
            }
         }
      }

      /**
       * Searches for an actor to process, looking first in this worker's queue of actors and then
       * falling back to trying to steal an actor from another worker. On return, the found actor
       * is ready to run and has already had one of its tasks readied.
       *
       * @return the actor to process or {@code null} if there is nothing to process now
       */
      private ActorQueue<T> doFindActor() {
         // find a ready task in our own work queue
         for (Iterator<ActorQueue<T>> iter = actors.iterator(); iter.hasNext(); ) {
            ActorQueue<T> actor = iter.next();
            ActorQueue.TaskResult result = actor.nextTask();
            if (result == ActorQueue.TaskResult.NO_TASK) {
               iter.remove();
            } else if (result == ActorQueue.TaskResult.TASK_FOUND) {
               if (iter.hasNext()) {
                  // not the last actor? move it to the end of the queue to give the others a chance
                  // in the next round of finding a task
                  iter.remove();
                  actors.add(actor);
               }
               return actor;
            }                  
         }
         // nothing local, so try to steal an actor from another worker
         return owner.tryStealFromOtherWorker(this);
      }
      
      /**
       * Attempts to remove an actor from this worker's queue so it can be processed by another,
       * idle worker.
       *
       * @param stealer the idle worker that is trying to steal work
       * @return the task that was stolen or {@code null} if nothing is available
       */
      ActorQueue<T> tryStealActor(Worker<T> stealer) {
         for (Iterator<ActorQueue<T>> iter = actors.descendingIterator(); iter.hasNext(); ) {
            ActorQueue<T> actor = iter.next();
            ActorQueue.TaskResult result = actor.nextTask();
            if (result == ActorQueue.TaskResult.NO_TASK) {
               iter.remove();
            } else if (result == ActorQueue.TaskResult.TASK_FOUND) {
               // move this actor to new worker
               iter.remove();
               stealer.actors.add(actor);
               actor.setWorker(stealer);
               owner.stealCount.increment();
               return actor;
            }
         }
         return null;
      }
      
      /**
       * Tries to pin an actor to this worker. Returns true if the actor was accepted (possibly
       * by another thread concurrently stealing from this worker). False if the actor was not
       * accepted (because this worker is terminating).
       *
       * @param actor the new actor that this worker should process
       * @param attemptNumber the attempt number for adding this actor to a worker
       * @param movingFrom the worker that previously owned the actor or {@code null}
       * @return true if the actor is accepted; false if the caller should try adding to another
       *       worker
       */
      boolean add(ActorQueue<T> actor, int attemptNumber, Worker<T> movingFrom) {
         if (!actor.compareAndSetWorker(movingFrom, this)) {
            // this actor was concurrently stolen by a different worker (racing with this
            // thread during a previous attempt) 
            assert movingFrom != null || attemptNumber > 0;
            return true;
         }
         actors.addLast(actor);
         if (!tryNotify()) {
            // this worker is going away; have to try another
            actors.removeLastOccurrence(actor);
            // if the CAS fails, the actor was concurrently stolen; so it was accepted, just not by
            // this worker
            return !actor.compareAndSetWorker(this, null);
         }
         return true;
      }
      
      /**
       * Removes an actor from this worker. This is used to take back an actor that may have been
       * added while racing with a call to shutdown. If the remove succeeds, then the actor is
       * considered to have been added after shutdown. If the remove fails, then the actor is
       * considered to have been added before shutdown (and means the worker has already de-queued
       * it and started processing, or another worker stole it).
       *
       * @param actor the actor to remove from this worker
       * @return true if the remove succeeds
       */
      boolean remove(ActorQueue<T> actor) {
         return actors.removeLastOccurrence(actor);
      }
      
      /**
       * Notifies the worker of a new work item placed in its queue.
       *
       * @return true if the worker was notified or false if the worker is unusable (because it is
       *       terminating)
       */
      boolean tryNotify() {
         while (true) {
            long i = workerIndexAndStamp;
            if ((i & WORKER_INDEX_MASK) == WORKER_INDEX_MASK) {
               return false; // already nullified
            }
            if (workerIndexAndStampUpdater.compareAndSet(this, i, i + WORKER_STAMP_INC)) {
               awake();
               return true;
            }
         }
      }
      
      /**
       * Wakes this worker thread up if it is idle and parked. Returns false if the thread was not
       * awakened because it is not idle.
       */
      boolean awake() {
         while (true) {
            long w = workerIndexAndStamp;
            if ((w & WORKER_PARK_MASK) != WORKER_PARKED) {
               // doesn't need to be awakened
               return false;
            }
            if (((int) w) == -1) {
               // worker has been "nullified" and is shutting down
               return false;
            }
            long unparked = (w & ~WORKER_PARKED) | WORKER_UNPARKED;
            if (workerIndexAndStampUpdater.compareAndSet(this, w, unparked)) {
               break;
            }
         }
         LockSupport.unpark(thread);
         return true;
      }

      /**
       * Interrupts this worker thread.
       */
      void interrupt() {
         thread.interrupt();
      }

      /**
       * Marks this worker as unusable by setting the index to -1.
       *
       * @param expectedState the expected stamp for the worker (to detect interference)
       * @return the worker's array index on success or -1 on failure
       */
      int tryNullify(long expectedStamp) {
         while (true) {
            long i = workerIndexAndStamp;
            if ((i & WORKER_STAMP_MASK) != expectedStamp) {
               return -1;
            }
            long newIndex = i | WORKER_INDEX_MASK; // set all bits (equivalent to index of -1)
            if (workerIndexAndStampUpdater.compareAndSet(this, i, newIndex)) {
               return (int) i;
            }
         }
      }
      
      /**
       * Unconditionally marks this worker as unusable by setting the index to -1.
       *
       * @return the worker's array index
       */
      int nullify() {
         while (true) {
            long i = workerIndexAndStamp;
            long newIndex = i | WORKER_INDEX_MASK; // set all bits (equivalent to index of -1)
            if (workerIndexAndStampUpdater.compareAndSet(this, i, newIndex)) {
               return (int) i;
            }
         }
      }
      
      /**
       * Moves this worker to the given index
       *
       * @param toIndex the worker's new array index
       * @return the worker's previous array index
       */
      int move(int toIndex) {
         while (true) {
            long i = workerIndexAndStamp;
            long newIndex = (i & ~WORKER_INDEX_MASK) | toIndex;
            if (workerIndexAndStampUpdater.compareAndSet(this, i, newIndex)) {
               return (int) i;
            }
         }
      }
   }
}
