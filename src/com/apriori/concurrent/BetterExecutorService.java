package com.apriori.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An extension and enhancement to the API provided by {@link ScheduledExecutorService}.
 * 
 * <p>This implementation provides greater configurability over scheduled tasks and greater
 * optics into the results of executions of the tasks. It is API-compatible with the standard
 * {@link ScheduledExecutorService}, so it should behave the same if using only the standard API
 * and not any of the extensions herein.
 * 
 * <h3>Key Interfaces</h3>
 * <dl>
 *  <dt>{@link TaskDefinition}</dt>
 *    <dd>The definition of a scheduled task. This defines when a task runs, if it is repeated and
 *    how often, what actual code is executed when the task is run, etc.</dd>
 *  <dt>{@link ScheduledTaskDefinition}</dt>
 *    <dd>A {@link TaskDefinition} that has been scheduled for execution with a
 *    {@link BetterExecutorService}. This provides additional API for inspecting the status of task
 *    invocations and controlling the task, like pausing/suspending executions and cancelling the
 *    task.</dd>
 *  <dt>{@link ScheduledTask}</dt>
 *    <dd>A single invocation of a {@link ScheduledTaskDefinition}. If the task is defined as a
 *    repeating task, there will be multiple such invocations of this task over time. This interface
 *    is also a {@link ScheduledFuture} and is returned when submitting scheduled tasks to the
 *    executor service.</dd>
 *  <dt>{@link RepeatingScheduledTask}</dt>
 *    <dd>Represents all invocations of a repeating {@link ScheduledTaskDefinition}. This is for
 *    API and behavioral compatibility with the {@link ScheduledFuture}s returned by the standard
 *    {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} and
 *    {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}
 *    methods.</dd>
 * </dl>
 * 
 * <p>Client code uses a {@link TaskDefinition.Builder} to construct a task and define all of the
 * parameters for its execution. It is then {@linkplain #schedule(TaskDefinition) scheduled} with 
 * the {@link BetterExecutorService} for execution.
 * 
 * <h3>Features</h3>
 * <p>The main features in this service, not available with the standard scheduled executor API,
 * follow:
 * <ul>
 *    <li>Greater control over repeated occurrences. Instead of a task either always repeating
 *    (unless an invocation fails) or never repeating (unless the logic itself schedules a
 *    successor when invoked), tasks can specify a {@link ScheduleNextTaskPolicy}.</li>
 *    <li>Notification of individual invocations. For repeated tasks, instead of only being able to
 *    wait for all invocations to finish (which generally only happens after an invocation fails)
 *    or cancel all subsequent invocations, this API provides granuality at individual task level.
 *    You can {@linkplain ScheduledTaskDefinition#addListener(ScheduledTask.Listener) listen} for
 *    completions of any and all invocations. You can also {@linkplain ScheduledTask#cancel(boolean)
 *    cancel} individual invocations of a task.</li>
 *    <li>Greater job control. As mentioned above, you can cancel individual occurrences of a task
 *    instead of cancelling the entire job. You can also {@linkplain ScheduledTaskDefinition#pause()
 *    pause} execution of a task temporarily. This does not attempt to suspend any thread currently
 *    executing the task, but simply stops scheduling future instances of the job until the task
 *    is {@linkplain ScheduledTaskDefinition#resume() resumed}.</li>
 *    <li>Exception handling. You can specify an {@link UncaughtExceptionHandler} for each task to
 *    handle job failures. This, combined with a {@link ScheduleNextTaskPolicy}, provides much more
 *    flexibility in handling exceptions thrown by task invocations.</li>
 *    <li>Job History. In addition to listening for task completions, you can access the results of
 *    recent invocations for a repeated task - the task's {@linkplain
 *    ScheduledTaskDefinition#history() history}.
 *    </li>
 * </ul>
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
//TODO javadoc below!!!
//TODO consider finer grain concurrency primitives -- e.g. something other than synchronized methods
//TODO tests!
public class BetterExecutorService implements ScheduledExecutorService {

   /**
    * The concrete implementation of {@link ScheduledTask} used by this service. 
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ScheduledTaskImpl<V, T> implements ScheduledTask<V, T> {
      private final ScheduledTaskDefinitionImpl<V, T> taskDef;
      private ScheduledFuture<?> future;
      long actualStartMillis;
      private long actualEndMillis;
      private volatile V result;
      private volatile Throwable failure;
      
      ScheduledTaskImpl(ScheduledTaskDefinitionImpl<V, T> taskDef) {
         this.taskDef = taskDef;
      }
      
      void setScheduledFuture(ScheduledFuture<?> future) {
         this.future = future;
      }
      
      @Override
      public long getDelay(TimeUnit unit) {
         return future.getDelay(unit);
      }

      @Override
      public int compareTo(Delayed other) {
         return future.compareTo(other);
      }

      @Override
      public boolean cancel(boolean interrupt) {
         // to prevent deadlock, must always acquire lock for definition before the acquiring the
         // lock for task instance
         synchronized (taskDef) {
            synchronized (this) {
               if (future.cancel(interrupt)) {
                  setFailure(new CancellationException());
                  if (!hasStarted() && !taskDef.isPaused() && !taskDef.isFinished()) {
                     taskDef.scheduleNext(this);
                  }
                  return true;
               }
               return false;
            }
         }
      }

      /**
       * Returns the resolved value of the future or throws an exception if
       * the task failed. This requires that the task be completed. Since we schedule
       * tasks on the underlying {@link ScheduledExceutorService} as {@link Runnable}s,
       * there is no value returned. Also, we wrap the tasks with logic that provides
       * greater safety in exception handling (so the {@link ScheduledFuture} that
       * we get from the underlying service will never raise an exception or return
       * an actual value).
       * 
       * @return the result of the completed task
       * @throws ExecutionException if the task failed
       */
      private V getResult() throws ExecutionException {
         if (failure == null) {
            return result;
         } else {
            throw new ExecutionException(failure);
         }
      }
      
      @Override
      public V get() throws InterruptedException, ExecutionException {
         future.get();
         return getResult();
      }

      @Override
      public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
         future.get(timeout, unit);
         return getResult();
      }

      @Override
      public boolean isCancelled() {
         return future.isCancelled();
      }

      @Override
      public boolean isDone() {
         return future.isDone();
      }

      @Override
      public ScheduledTaskDefinition<V, T> taskDefinition() {
         return taskDef;
      }

      @Override
      public synchronized boolean hasStarted() {
         return actualStartMillis != 0;
      }

      @Override
      public synchronized long actualTaskStartMillis() {
         if (actualStartMillis == 0) {
            throw new IllegalStateException("Task not yet started");
         }
         return actualStartMillis;
      }

      @Override
      public synchronized long actualTaskEndMillis() {
         if (!future.isDone()) {
            throw new IllegalStateException("Task not yet finished");
         }
         return actualEndMillis;
      }

      @Override
      public boolean failed() {
         return !succeeded();
      }

      @Override
      public boolean succeeded() {
         if (!future.isDone()) {
            throw new IllegalStateException("Task not yet finished");
         }
         return failure == null;
      }
      
      synchronized void markStart() {
         actualStartMillis = System.currentTimeMillis();
      }
      
      synchronized void markEnd() {
         actualEndMillis = System.currentTimeMillis();
      }
      
      synchronized boolean setResult(V result) {
         if (this.failure == null && this.result == null) {
            this.result = result;
            return true;
         }
         return false;
      }
      
      synchronized boolean setFailure(Throwable t) {
         if (this.failure == null && this.result == null) {
            this.failure = t;
            return true;
         }
         return false;
      }
   }
   
   /**
    * The concrete implementation of {@link ScheduledTaskDefinition} used by this service. 
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class ScheduledTaskDefinitionImpl<V, T> implements ScheduledTaskDefinition<V, T> {
      private final TaskDefinition<V, T> taskDef;
      private final Set<ScheduledTask.Listener<? super V, ? super T>> listeners;
      private final long submitTimeMillis;
      private final Map<ScheduledTask<V, T>, Void> history;
      private ScheduledTaskImpl<V, T> first;
      private ScheduledTaskImpl<V, T> current;
      private ScheduledTaskImpl<V, T> latest;
      private long lastScheduledTimeMillis;
      private int executionCount;
      private int successCount;
      private int failureCount;
      private boolean cancelled;
      private boolean paused;
      private boolean finished;
      
      @SuppressWarnings("serial") // don't care about serializing our custom sub-class of LinkedHashMap
      ScheduledTaskDefinitionImpl(TaskDefinition<V, T> taskDef) {
         this.taskDef = taskDef;
         this.listeners = new LinkedHashSet<ScheduledTask.Listener<? super V, ? super T>>(taskDef.listeners());
         this.submitTimeMillis = System.currentTimeMillis();
         this.history = new LinkedHashMap<ScheduledTask<V, T>, Void>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ScheduledTask<V, T>, Void> entry) {
               return size() > maxHistorySize();
            }
         };
      }
      
      @Override
      public T task() {
         return taskDef.task();
      }

      @Override
      public Callable<V> taskAsCallable() {
         return taskDef.taskAsCallable();
      }

      @Override
      public long initialDelayMillis() {
         return taskDef.initialDelayMillis();
      }

      @Override
      public boolean isRepeating() {
         return taskDef.isRepeating();
      }

      @Override
      public boolean isFixedRate() {
         return taskDef.isFixedRate();
      }

      @Override
      public long periodDelayMillis() {
         return taskDef.periodDelayMillis();
      }

      @Override
      public ScheduleNextTaskPolicy<? super V, ? super T> scheduleNextTaskPolicy() {
         return taskDef.scheduleNextTaskPolicy();
      }

      @Override
      public int maxHistorySize() {
         return taskDef.maxHistorySize();
      }

      @Override
      public synchronized Set<ScheduledTask.Listener<? super V, ? super T>> listeners() {
         return Collections.unmodifiableSet(new LinkedHashSet<ScheduledTask.Listener<? super V, ? super T>>(listeners));
      }

      @Override
      public synchronized void addListener(ScheduledTask.Listener<? super V, ? super T> listener) {
         listeners.add(listener);
      }

      @Override
      public synchronized boolean removeListener(ScheduledTask.Listener<? super V, ? super T> listener) {
         return listeners.remove(listener);
      }

      @Override
      public UncaughtExceptionHandler uncaughtExceptionHandler() {
         return taskDef.uncaughtExceptionHandler();
      }

      @Override
      public long submitTimeMillis() {
         return submitTimeMillis;
      }

      @Override
      public BetterExecutorService executor() {
         return BetterExecutorService.this;
      }

      @Override
      public synchronized int executionCount() {
         return executionCount;
      }

      @Override
      public synchronized int failureCount() {
         return failureCount;
      }

      @Override
      public synchronized int successCount() {
         return successCount;
      }

      @Override
      public synchronized List<ScheduledTask<V, T>> history() {
         List<ScheduledTask<V, T>> ret = new ArrayList<ScheduledTask<V, T>>(history.keySet());
         Collections.reverse(ret);
         return Collections.unmodifiableList(ret);
      }

      @Override
      public synchronized ScheduledTaskImpl<V, T> latest() {
         return latest;
      }

      @Override
      public synchronized ScheduledTaskImpl<V, T> current() {
         return current;
      }

      @Override
      public synchronized boolean isFinished() {
         return finished || cancelled;
      }
      
      @Override
      public synchronized boolean cancel(boolean interrupt) {
         if (!isFinished()) {
            cancelled = true;
            paused = false;
            if (current != null) {
               current.cancel(interrupt);
            }
            return true;
         } else {
            return false;
         }
      }
      
      @Override
      public synchronized boolean isCancelled() {
         return cancelled;
      }
      
      @Override
      public synchronized boolean pause() {
         if (!paused && !finished && !cancelled) {
            paused = true;
            if (current.cancel(false) && !current.hasStarted()) {
               current = null;
            }
            return true;
         } else {
            return false;
         }
      }
      
      @Override
      public synchronized boolean isPaused() {
         return paused;
      }
      
      @Override
      public synchronized boolean resume() {
         if (paused) {
            if (current == null) {
               scheduleNext(latest);
            }
            paused = false;
            return true;
         } else {
            return false;
         }
      }

      /**
       * Returns the first scheduled invocation of the task. This method can only be
       * accessed once. Thereafter, it will always return {@code null}. This is used
       * internally to return a {@link ScheduledFuture} to callers of the methods
       * on the {@link ScheduledExecutorService} interface. It can only be called
       * once so that the reference can be cleared and the first task eventually
       * garbage collected.
       * 
       * @return the first scheduled invocation of the task once and {@code null}
       *       thereafter
       */
      synchronized ScheduledTaskImpl<V, T> first() {
         ScheduledTaskImpl<V, T> ret = first;
         first = null; // don't need it anymore, so let it be collected
         return ret;
      }
      
      /**
       * Schedules the first invocation of the task. This schedules it based on
       * the task definition's initial delay time.
       */
      synchronized void scheduleFirst() {
         ScheduledTaskImpl<V, T> task = new ScheduledTaskImpl<V, T>(this);
         current = task;
         first = task;
         task.setScheduledFuture(delegate.schedule(makeRunnable(task),
               taskDef.initialDelayMillis(), TimeUnit.MILLISECONDS));
         lastScheduledTimeMillis = System.currentTimeMillis() + taskDef.initialDelayMillis();
      }

      /**
       * Schedules a subsequent invocation of the task. This may not actually
       * schedule anything if it is determined, based on the {@link ScheduleNextTaskPolicy},
       * that the task is finished. If a next task should be scheduled, the delay
       * time is computed based on the task definition's fixed rate or fixed delay.
       * 
       * @param prevTask the previous (just completed) invocation of the task
       */
      synchronized void scheduleNext(ScheduledTaskImpl<V, T> prevTask) {
         if (!isRepeating() || !shouldScheduleNext(prevTask)) {
            finished = true;
            paused = false;
            nextTask(null);
         } else if (cancelled || paused) {
            nextTask(null);
         } else {
            ScheduledTaskImpl<V, T> task = new ScheduledTaskImpl<V, T>(this);
            long periodDelayMillis = taskDef.periodDelayMillis();
            long nextScheduledTimeMillis;
            // if previous task was cancelled before it started, schedule its successor as if it
            // started on time and took no time to complete
            if (isFixedRate()) {
               long previousStartMillis = prevTask.hasStarted()
                     ? prevTask.actualTaskStartMillis()
                     : System.currentTimeMillis() + prevTask.getDelay(TimeUnit.MILLISECONDS);
               nextScheduledTimeMillis = nextTimeForFixedRate(lastScheduledTimeMillis,
                     previousStartMillis, periodDelayMillis);
            } else {
               long previousEndMillis = prevTask.hasStarted()
                     ? prevTask.actualTaskEndMillis()
                     : System.currentTimeMillis() + prevTask.getDelay(TimeUnit.MILLISECONDS);
               nextScheduledTimeMillis = previousEndMillis + periodDelayMillis;
            }
            task.setScheduledFuture(delegate.schedule(makeRunnable(task),
                  nextScheduledTimeMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
            lastScheduledTimeMillis = nextScheduledTimeMillis;
            nextTask(task);
         }
      }
      
      /**
       * Determines if another task should be scheduled. If the policy throws while determining
       * if another task should be scheduled the no subsequent task is scheduled and a stack-trace
       * for whatever is thrown will be printed to stderr.
       * 
       * @param prevTask the previous task instance, which can inform the decision for whether to
       *       schedule another or not
       * @return {@code true} if another task should be scheduled or {@code false} otherwise
       */
      private boolean shouldScheduleNext(ScheduledTaskImpl<V, T> prevTask) {
         try {
            return scheduleNextTaskPolicy().shouldScheduleNext(prevTask);
         } catch (Throwable t) {
            t.printStackTrace();
            return false;
         }
      }
      
      /**
       * Sets the current invocation of the task and processes the previous one (if there
       * was one) for execution count stats.
       * 
       * @param task the next invocation of the task that is to become the current invocation
       */
      private void nextTask(ScheduledTaskImpl<V, T> task) {
         if (current != null) {
            for (ScheduledTask.Listener<? super V, ? super T> listener : listeners) {
               try {
                  listener.taskCompleted(current);
               } catch (Throwable ignored) {
                  // don't let listener cause the thread to die
               }
            }
            executionCount++;
            if (current.succeeded()) {
               successCount++;
            } else {
               failureCount++;
            }
            history.put(current, null);
            latest = current;
         }
         current = task;
      }
      
      /**
       * Creates a runnable for the specified task that will be scheduled on
       * the underlying {@link ScheduledExecutorService}. The runnable wraps
       * the task with logic that handles uncaught exceptions and schedules
       * the next task invocation. 
       * 
       * @param task the task that will be scheduled
       * @return a runnable 
       */
      private Runnable makeRunnable(final ScheduledTaskImpl<V, T> task) {
         final Callable<V> taskAsCallable = taskDef.taskAsCallable();
         return new Runnable() {
            @Override
            public void run() {
               synchronized (ScheduledTaskDefinitionImpl.this) {
                  // if task definition was paused before we marked the start of the thread then
                  // it thinks this scheduled instance never started (and won't run) so fulfill
                  // that thought
                  if (isPaused() || task.isCancelled()) {
                     return;
                  }
                  task.markStart();
               }
               try {
                  V result = taskAsCallable.call();
                  task.markEnd();
                  task.setResult(result);
               } catch (Throwable t) {
                  task.markEnd();
                  task.setFailure(t);
                  UncaughtExceptionHandler handler = uncaughtExceptionHandler();
                  if (handler == null) {
                     handler = Thread.currentThread().getUncaughtExceptionHandler();
                  }
                  if (handler != null) {
                     try {
                        handler.uncaughtException(Thread.currentThread(), t);
                     } catch (Throwable ignored) {
                     }
                  } else {
                     t.printStackTrace();
                  }
               }
               scheduleNext(task);
            }
         };
      }
   }

   /**
    * Returns the next scheduled time for a fixed rate repeating task.
    * 
    * @param lastScheduledTimeMillis the time that the previous invocation was
    *       scheduled to start
    * @param lastInvocationStartTimeMillis the time that the previous invocation
    *       actually started
    * @param periodMillis the fixed rate period
    * @return the next scheduled time for the task, which could actually be in the past
    */
   static long nextTimeForFixedRate(long lastScheduledTimeMillis, long lastInvocationStartTimeMillis,
         long periodMillis) {
      long result = lastScheduledTimeMillis + periodMillis;
      if (periodMillis > 0 &&
            result < lastInvocationStartTimeMillis) {
         // if execution took longer than period, update scheduled time to skip
         // past the missed invocation times
         long catchup = lastInvocationStartTimeMillis - result;
         long missedCycles = catchup / periodMillis;
         if (catchup % periodMillis > 0) {
            missedCycles++;
         }
         result += periodMillis * missedCycles;
      }
      return result;
   }

   final ScheduledExecutorService delegate;
   
   private BetterExecutorService(ScheduledExecutorService delegate) {
      this.delegate = delegate;
   }
   
   /**
    * Improves the specified {@link ScheduledExecutorService} by wrapping it in a
    * {@link BetterExecutorService}.
    * 
    * @param service the underlying executor service
    * @return a {@link BetterExecutorService}
    */
   public static BetterExecutorService improve(ScheduledExecutorService service) {
      return new BetterExecutorService(service);
   }
   
   @Override
   public <V> CallableScheduledTask<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
      return callableScheduledTask(scheduleInternal(
            TaskDefinition.Builder.forCallable(task).withInitialDelay(delay, unit).build()).first());
   }
   
   @Override
   public RunnableScheduledTask schedule(Runnable task, long delay, TimeUnit unit) {
      return runnableScheduledTask(scheduleInternal(
            TaskDefinition.Builder.forRunnable(task).withInitialDelay(delay, unit).build()).first());
   }

   public <V> CallableRepeatingScheduledTask<V> scheduleAtFixedRate(Callable<V> task,
         long initialDelay, long period, TimeUnit unit) {
      ScheduledTaskDefinitionImpl<V, Callable<V>> taskDef = scheduleInternal(
            TaskDefinition.Builder.forCallable(task).withInitialDelay(initialDelay, unit)
            .repeatAtFixedRate(period, unit)
            // for API compatibility with normal ScheduledExecutorService, we only continue
            // scheduling instances of the task if it succeeds
            .withScheduleNextTaskPolicy(ScheduleNextTaskPolicies.ON_SUCCESS)
            .build());
      taskDef.first(); // invoke so we clear the ref
      return callableScheduledTask(taskDef);
   }
  
   @Override
   public RunnableRepeatingScheduledTask scheduleAtFixedRate(Runnable task, long initialDelay,
         long period, TimeUnit unit) {
      ScheduledTaskDefinitionImpl<Void, Runnable> taskDef = scheduleInternal(
            TaskDefinition.Builder.forRunnable(task).withInitialDelay(initialDelay, unit)
            .repeatAtFixedRate(period, unit)
            // for API compatibility with normal ScheduledExecutorService, we only continue
            // scheduling instances of the task if it succeeds
            .withScheduleNextTaskPolicy(ScheduleNextTaskPolicies.ON_SUCCESS)
            .build());
      taskDef.first(); // invoke now so we clear the ref
      return runnableScheduledTask(taskDef);
   }
   
   public <V> CallableRepeatingScheduledTask<V> scheduleWithFixedDelay(Callable<V> task,
         long initialDelay, long delay, TimeUnit unit) {
      ScheduledTaskDefinitionImpl<V, Callable<V>> taskDef = scheduleInternal(
            TaskDefinition.Builder.forCallable(task).withInitialDelay(initialDelay, unit)
            .repeatWithFixedDelay(delay, unit)
            // for API compatibility with normal ScheduledExecutorService, we only continue
            // scheduling instances of the task if it succeeds
            .withScheduleNextTaskPolicy(ScheduleNextTaskPolicies.ON_SUCCESS)
            .build());
      taskDef.first(); // invoke now so we clear the ref
      return callableScheduledTask(taskDef);
   }

   @Override
   public RunnableRepeatingScheduledTask scheduleWithFixedDelay(Runnable task, long initialDelay,
         long delay, TimeUnit unit) {
      ScheduledTaskDefinitionImpl<Void, Runnable> taskDef = scheduleInternal(
            TaskDefinition.Builder.forRunnable(task).withInitialDelay(initialDelay, unit)
            .repeatWithFixedDelay(delay, unit)
            // for API compatibility with normal ScheduledExecutorService, we only continue
            // scheduling instances of the task if it succeeds
            .withScheduleNextTaskPolicy(ScheduleNextTaskPolicies.ON_SUCCESS)
            .build());
      taskDef.first(); // invoke now so we clear the ref
      return runnableScheduledTask(taskDef);
   }
   
   @Override
   public <V> List<Future<V>> invokeAll(Collection<? extends Callable<V>> tasks)
         throws InterruptedException {
      return delegate.invokeAll(tasks);
   }

   @Override
   public <V> List<Future<V>> invokeAll(Collection<? extends Callable<V>> tasks, long timeout,
         TimeUnit unit) throws InterruptedException {
      return delegate.invokeAll(tasks, timeout, unit);
   }

   @Override
   public <V> V invokeAny(Collection<? extends Callable<V>> tasks) throws InterruptedException,
         ExecutionException {
      return delegate.invokeAny(tasks);
   }

   @Override
   public <V> V invokeAny(Collection<? extends Callable<V>> tasks, long timeout, TimeUnit unit)
         throws InterruptedException, ExecutionException, TimeoutException {
      return delegate.invokeAny(tasks, timeout, unit);
   }

   @Override
   public boolean isShutdown() {
      return delegate.isShutdown();
   }

   @Override
   public boolean isTerminated() {
      return delegate.isTerminated();
   }

   @Override
   public void shutdown() {
      delegate.shutdown();
   }

   @Override
   public List<Runnable> shutdownNow() {
      return delegate.shutdownNow();
   }

   @Override
   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return delegate.awaitTermination(timeout,  unit);
   }

   @Override
   public <V> CallableScheduledTask<V> submit(Callable<V> task) {
      return callableScheduledTask(scheduleInternal(TaskDefinition.Builder.forCallable(task).build()).first());
   }

   @Override
   public RunnableScheduledTask submit(Runnable task) {
      return runnableScheduledTask(scheduleInternal(TaskDefinition.Builder.forRunnable(task).build()).first());
   }

   @Override
   public <V> ScheduledTask<V, RunnableWithResult<V>> submit(Runnable task, V result) {
      return scheduleInternal(TaskDefinition.Builder.forRunnable(task, result).build()).first();
   }

   @Override
   public void execute(Runnable task) {
      submit(task);
   }
   
   static <V, T> V getTaskResult(ScheduledTaskDefinition<V, T> taskDef)
         throws InterruptedException, ExecutionException {
      final AtomicReference<ScheduledTask<? extends V, ? extends T>> finalTask =
            new AtomicReference<ScheduledTask<? extends V, ? extends T>>();
      final CountDownLatch latch = new CountDownLatch(1);
      ScheduledTask.Listener<V, T> listener =
            new ScheduledTask.Listener<V, T>() {
               @Override
               public void taskCompleted(
                     ScheduledTask<? extends V, ? extends T> task) {
                  finalTask.set(task);
                  latch.countDown();
               }
            };
      taskDef.addListener(listener);
      try {
         latch.await();
         return finalTask.get().get();
      } finally {
         taskDef.removeListener(listener);
      }
   }

   static <V, T> V getTaskResult(ScheduledTaskDefinition<V, T> taskDef, long timeout,
         TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
      final AtomicReference<ScheduledTask<? extends V, ? extends T>> finalTask =
            new AtomicReference<ScheduledTask<? extends V, ? extends T>>();
      final CountDownLatch latch = new CountDownLatch(1);
      ScheduledTask.Listener<V, T> listener =
            new ScheduledTask.Listener<V, T>() {
               @Override
               public void taskCompleted(
                     ScheduledTask<? extends V, ? extends T> task) {
                  finalTask.set(task);
                  latch.countDown();
               }
            };
      taskDef.addListener(listener);
      try {
         if (latch.await(timeout, unit)) {
            throw new TimeoutException();
         }
         return finalTask.get().get();
      } finally {
         taskDef.removeListener(listener);
      }
   }

   private static RunnableRepeatingScheduledTask runnableScheduledTask(
         final ScheduledTaskDefinition<Void, Runnable> taskDef) {
      return new RunnableRepeatingScheduledTask() {
         @Override
         public boolean isDone() {
            return taskDef.isFinished();
         }
         
         @Override
         public boolean isCancelled() {
            return taskDef.isCancelled();
         }
         
         @Override
         public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
               TimeoutException {
            return getTaskResult(taskDef, timeout, unit);
         }
         
         @Override
         public Void get() throws InterruptedException, ExecutionException {
            return getTaskResult(taskDef);
         }
         
         @Override
         public boolean cancel(boolean mayInterruptIfRunning) {
            return taskDef.cancel(true);
         }
         
         private Delayed getDelayed() {
            Delayed d = taskDef.current();
            return d == null ? taskDef.latest() : d;
         }
         
         @Override
         public int compareTo(Delayed o) {
            return getDelayed().compareTo(o);
         }
         
         @Override
         public long getDelay(TimeUnit unit) {
            return getDelayed().getDelay(unit);
         }
         
         @Override
         public ScheduledTaskDefinition<Void, Runnable> taskDefinition() {
            return taskDef;
         }
      };
   }
   
   private static RunnableScheduledTask runnableScheduledTask(final ScheduledTask<Void, Runnable> task) {
      return new RunnableScheduledTask() {
         @Override
         public ScheduledTaskDefinition<Void, Runnable> taskDefinition() {
            return task.taskDefinition();
         }
         
         @Override
         public boolean hasStarted() {
            return task.hasStarted();
         }

         @Override
         public long actualTaskStartMillis() {
            return task.actualTaskStartMillis();
         }

         @Override
         public long actualTaskEndMillis() {
            return task.actualTaskEndMillis();
         }

         @Override
         public boolean failed() {
            return task.failed();
         }

         @Override
         public boolean succeeded() {
            return task.succeeded();
         }

         @Override
         public long getDelay(TimeUnit unit) {
            return task.getDelay(unit);
         }

         @Override
         public int compareTo(Delayed other) {
            return task.compareTo(other);
         }

         @Override
         public boolean cancel(boolean interrupt) {
            return task.cancel(interrupt);
         }

         @Override
         public Void get() throws InterruptedException, ExecutionException {
            task.get();
            return null;
         }

         @Override
         public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
               TimeoutException {
            task.get(timeout, unit);
            return null;
         }

         @Override
         public boolean isCancelled() {
            return task.isCancelled();
         }

         @Override
         public boolean isDone() {
            return task.isDone();
         }
      };
   }
   
   private static <V> CallableRepeatingScheduledTask<V> callableScheduledTask(
         final ScheduledTaskDefinition<V, Callable<V>> taskDef) {
      return new CallableRepeatingScheduledTask<V>() {

         @Override
         public ScheduledTaskDefinition<V, Callable<V>> taskDefinition() {
            return taskDef;
         }

         private Delayed getDelayed() {
            Delayed d = taskDef.current();
            return d == null ? taskDef.latest() : d;
         }
         
         @Override
         public long getDelay(TimeUnit unit) {
            return getDelayed().getDelay(unit);
         }

         @Override
         public int compareTo(Delayed o) {
            return getDelayed().compareTo(o);
         }

         @Override
         public boolean cancel(boolean mayInterruptIfRunning) {
            return taskDef.cancel(mayInterruptIfRunning);
         }

         @Override
         public boolean isCancelled() {
            return taskDef.isCancelled();
         }

         @Override
         public boolean isDone() {
            return taskDef.isFinished();
         }

         @Override
         public V get() throws InterruptedException, ExecutionException {
            return getTaskResult(taskDef);
         }

         @Override
         public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
               TimeoutException {
            return getTaskResult(taskDef, timeout, unit);
         }
      };
   }
   
   private static <V> CallableScheduledTask<V> callableScheduledTask(final ScheduledTask<V, Callable<V>> task) {
      return new CallableScheduledTask<V>() {
         @Override
         public ScheduledTaskDefinition<V, Callable<V>> taskDefinition() {
            return task.taskDefinition();
         }

         @Override
         public boolean hasStarted() {
            return task.hasStarted();
         }

         @Override
         public long actualTaskStartMillis() {
            return task.actualTaskStartMillis();
         }

         @Override
         public long actualTaskEndMillis() {
            return task.actualTaskEndMillis();
         }

         @Override
         public boolean failed() {
            return task.failed();
         }

         @Override
         public boolean succeeded() {
            return task.succeeded();
         }

         @Override
         public long getDelay(TimeUnit unit) {
            return task.getDelay(unit);
         }

         @Override
         public int compareTo(Delayed other) {
            return task.compareTo(other);
         }

         @Override
         public boolean cancel(boolean interrupt) {
            return task.cancel(interrupt);
         }

         @Override
         public V get() throws InterruptedException, ExecutionException {
            return task.get();
         }

         @Override
         public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
               TimeoutException {
            return task.get(timeout, unit);
         }
         
         @Override
         public boolean isCancelled() {
            return task.isCancelled();
         }

         @Override
         public boolean isDone() {
            return task.isDone();
         }
      };
   }

   public <V, T> ScheduledTaskDefinition<V, T> schedule(TaskDefinition<V, T> taskDef) {
      return scheduleInternal(taskDef);
   }

   private <V, T> ScheduledTaskDefinitionImpl<V, T> scheduleInternal(TaskDefinition<V, T> taskDef) {
      ScheduledTaskDefinitionImpl<V, T> scheduledTaskDef = new ScheduledTaskDefinitionImpl<V, T>(taskDef);
      scheduledTaskDef.scheduleFirst();
      
      return scheduledTaskDef;
   }
}