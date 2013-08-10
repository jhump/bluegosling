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
import java.util.concurrent.Executor;
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
 *    {@link ScheduledTaskManager}. This provides additional API for inspecting the status of task
 *    invocations and controlling the task, like pausing/suspending executions and canceling the
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
 * the {@link ScheduledTaskManager} for execution.
 * 
 * <h3>Features</h3>
 * <p>The main features in this service, not available with the standard scheduled executor API,
 * follow:
 * <ul>
 *    <li>Asynchronous processing using {@link FutureListener}s. All of the futures returned by
 *    this service are instances of {@link ListenableFuture}, which has a broader and more usable
 *    API than the standard {@link Future}, including the ability to add listeners to process
 *    results asynchronously.</li>
 *    <li>Greater control over repeated occurrences and how they are scheduled. Instead of a task
 *    either always repeating (unless an invocation fails) or never repeating (unless the logic
 *    itself schedules a successor when invoked), tasks specify a {@link ScheduleNextTaskPolicy}.
 *    Also, tasks specify a {@link Rescheduler}, which provides greater flexibility over the timing
 *    of subsequent tasks. {@link Rescheduler} implementations are provided for simple fixed-rate
 *    and fixed-delay scheduled.</li>
 *    <li>Notification of individual invocations. For repeated tasks, instead of only being able to
 *    wait for all invocations to finish (which generally only happens after an invocation fails)
 *    or cancel all subsequent invocations, this API provides granularity at individual task level.
 *    You can {@linkplain ScheduledTaskDefinition#addListener(ScheduledTaskListener) listen} for
 *    completions of any and all invocations. You can also {@linkplain ScheduledTask#cancel(boolean)
 *    cancel} individual invocations of a task.</li>
 *    <li>Greater job control. As mentioned above, you can cancel individual occurrences of a task
 *    instead of canceling the entire job. You can also {@linkplain ScheduledTaskDefinition#pause()
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
//TODO consider finer grain concurrency primitives throughout (e.g. other than synchronized methods)
//TODO tests!
public class ScheduledTaskManager implements ListenableScheduledExecutorService {

   /**
    * The concrete implementation of {@link ScheduledTask} used by this service. 
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private static class ScheduledTaskImpl<V, T> implements ScheduledTask<V, T> {
      private static final ThreadLocal<Boolean> runningListener = new ThreadLocal<Boolean>() {
         @Override protected Boolean initialValue() {
            return false;
         }
      };
      
      private static <T> FutureListener<T> protect(final FutureListener<T> listener) {
         return new FutureListener<T>() {
            @SuppressWarnings("synthetic-access") // access private static field of enclosing class
            @Override
            public void onCompletion(ListenableFuture<? extends T> completedFuture) {
               runningListener.set(true);
               try {
                  listener.onCompletion(completedFuture);
               } finally {
                  runningListener.set(false);
               }
            }
         };
      }
      
      private final ScheduledTaskDefinitionImpl<V, T> taskDef;
      private FutureListenerSet<V> listeners = new FutureListenerSet<V>(this);
      private ScheduledFuture<?> future;
      private volatile long actualStartMillis;
      private volatile long actualEndMillis;
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
         if (isDone()) {
            return false;
         }
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
      private V result() throws ExecutionException {
         if (failure == null) {
            return result;
         } else {
            throw new ExecutionException(failure);
         }
      }
      
      @Override
      public V get() throws InterruptedException, ExecutionException {
         if (!isDone()) {
            future.get();
         }
         return result();
      }

      @Override
      public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
         if (!isDone()) {
            future.get(timeout, unit);
         }
         return result();
      }

      @Override
      public boolean isCancelled() {
         return future.isCancelled();
      }

      @Override
      public boolean isDone() {
         // If we are running listener, task has completed but underlying future
         // isn't yet done since we invoke listeners from inside its Runnable (other
         // approaches involve spinning up other thread(s) to watch the underlying
         // future and run listeners once it's done, but that's even ickier)
         return runningListener.get() || future.isDone();
      }

      @Override
      public ScheduledTaskDefinition<V, T> taskDefinition() {
         return taskDef;
      }

      @Override
      public boolean hasStarted() {
         return actualStartMillis != 0;
      }

      @Override
      public long actualTaskStartMillis() {
         if (actualStartMillis == 0) {
            throw new IllegalStateException("Task not yet started");
         }
         return actualStartMillis;
      }

      @Override
      public long actualTaskEndMillis() {
         if (!isDone()) {
            throw new IllegalStateException("Task not yet finished");
         }
         return actualEndMillis;
      }

      @Override
      public boolean isFailed() {
         if (!isDone()) {
            return false;
         }
         return failure != null;
      }

      @Override
      public boolean isSuccessful() {
         if (!isDone()) {
            return false;
         }
         return failure == null;
      }
      
      void markStart() {
         actualStartMillis = System.currentTimeMillis();
      }
      
      void markEnd() {
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

      @Override
      public V getResult() {
         if (!isSuccessful()) {
            throw new IllegalStateException();
         }
         return result;
      }

      @Override
      public Throwable getFailure() {
         if (!isFailed()) {
            throw new IllegalStateException();
         }
         return failure;
      }

      @Override
      public void addListener(FutureListener<? super V> listener, Executor executor) {
         synchronized (this) {
            if (!isDone()) {
               listeners.addListener(protect(listener), executor);
               return;
            }
         }
         FutureListenerSet.runListener(this, listener, executor);
      }
      
      void runListeners() {
         FutureListenerSet<V> toExecute;
         synchronized (this) {
            toExecute = listeners;
            listeners = null;
         }
         if (toExecute != null) {
            toExecute.runListeners();
         }
      }

      @Override
      public void visit(FutureVisitor<? super V> visitor) {
         if (!isDone()) {
            throw new IllegalStateException();
         } else if (isCancelled()) {
            visitor.cancelled();
         } else if (isFailed()) {
            visitor.failed(failure);
         } else {
            visitor.successful(result);
         }
      }

      @Override
      public void await() throws InterruptedException {
         if (isDone()) {
            return;
         }
         try {
            future.get();
         } catch (Exception e) {
            // don't care if it failed, just waiting for it to finish
         }
      }

      @Override
      public boolean await(long limit, TimeUnit unit) throws InterruptedException {
         if (isDone()) {
            return true;
         }
         try {
            future.get(limit, unit);
         } catch (TimeoutException e) {
            return false;
         } catch (Exception e) {
            // don't care if it failed, just waiting for it to finish
         }
         return true;
      }
   }
   
   /**
    * The concrete implementation of {@link ScheduledTaskDefinition} used by this service. 
    * 
    * @author Joshua Humphries (jhumphries131@gmail.com)
    */
   private class ScheduledTaskDefinitionImpl<V, T> implements ScheduledTaskDefinition<V, T> {
      private final TaskDefinition<V, T> taskDef;
      private final Set<ScheduledTaskListener<? super V, ? super T>> listeners;
      private final long submitTimeMillis;
      private final Map<ScheduledTask<V, T>, Void> history;
      private ScheduledTaskImpl<V, T> first;
      private ScheduledTaskImpl<V, T> current;
      private ScheduledTaskImpl<V, T> latest;
      private long lastScheduledTimeNanos;
      private int executionCount;
      private int successCount;
      private int failureCount;
      private boolean cancelled;
      private boolean paused;
      private boolean finished;
      
      @SuppressWarnings("serial") // don't care about serializing our custom sub-class of LinkedHashMap
      ScheduledTaskDefinitionImpl(TaskDefinition<V, T> taskDef) {
         this.taskDef = taskDef;
         this.listeners =
               new LinkedHashSet<ScheduledTaskListener<? super V, ? super T>>(taskDef.listeners());
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
      public long initialDelayNanos() {
         return taskDef.initialDelayNanos();
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
      public Rescheduler rescheduler() {
         return taskDef.rescheduler();
      }
      
      @Override
      public long periodDelayNanos() {
         return taskDef.periodDelayNanos();
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
      public synchronized Set<ScheduledTaskListener<? super V, ? super T>> listeners() {
         return Collections.unmodifiableSet(
               new LinkedHashSet<ScheduledTaskListener<? super V, ? super T>>(listeners));
      }

      @Override
      public synchronized void addListener(ScheduledTaskListener<? super V, ? super T> listener) {
         listeners.add(listener);
      }

      @Override
      public synchronized boolean removeListener(ScheduledTaskListener<? super V, ? super T> listener) {
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
      public ScheduledTaskManager executor() {
         return ScheduledTaskManager.this;
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
               taskDef.initialDelayNanos(), TimeUnit.NANOSECONDS));
         lastScheduledTimeNanos = System.nanoTime() + taskDef.initialDelayNanos();
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
            long nextScheduledTimeNanos =
                  taskDef.rescheduler().scheduleNextStartTime(lastScheduledTimeNanos);
            long delayNanos = nextScheduledTimeNanos - System.nanoTime();
            task.setScheduledFuture(delegate.schedule(makeRunnable(task), delayNanos,
                  TimeUnit.NANOSECONDS));
            lastScheduledTimeNanos = nextScheduledTimeNanos;
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
            for (ScheduledTaskListener<? super V, ? super T> listener : listeners) {
               try {
                  listener.taskCompleted(current);
               } catch (Throwable ignored) {
                  // don't let listener cause the thread to die
               }
            }
            current.runListeners();
            executionCount++;
            if (current.isSuccessful()) {
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
   
   private ScheduledTaskManager(ScheduledExecutorService delegate) {
      this.delegate = delegate;
   }
   
   /**
    * Creates a new {@link ScheduledTaskManager} that uses the specified
    * {@link ScheduledExecutorService} as its basis for scheduling tasks.
    * 
    * @param service the underlying executor service
    * @return a {@link ScheduledTaskManager}
    */
   public static ScheduledTaskManager create(ScheduledExecutorService service) {
      return new ScheduledTaskManager(service);
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
      ScheduledTaskListener<V, T> listener =
            new ScheduledTaskListener<V, T>() {
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
      ScheduledTaskListener<V, T> listener =
            new ScheduledTaskListener<V, T>() {
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
         private final AtomicReference<CountDownLatch> awaitLatch =
               new AtomicReference<CountDownLatch>();
         
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
            if (d == null) {
               d = taskDef.latest();
               if (d == null) {
                  throw new IllegalStateException("Task is paused. Delay cannot be computed");
               }
            }
            return d;
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

         @Override
         public Void getMostRecentResult() {
            ScheduledTask<Void, Runnable> task = taskDef.latest();
            if (task == null) {
               throw new IllegalStateException();
            }
            return task.getResult();
         }

         @Override
         public void addListenerForEachInstance(final FutureListener<? super Void> listener,
               Executor executor) {
            final ListenableFuture<Void> self = this; 
            taskDef.addListener(new ScheduledTaskListener<Void, Runnable>() {
               @Override
               public void taskCompleted(ScheduledTask<? extends Void, ? extends Runnable> task) {
                  listener.onCompletion(self);
               }
            });
         }

         @Override
         public int executionCount() {
            return taskDef.executionCount();
         }
         
         @Override
         public boolean isSuccessful() {
            if (!isDone()) {
               return false;
            }
            ScheduledTask<Void, Runnable> task = taskDef.latest();
            return task != null && task.isSuccessful();
         }

         @Override
         public Void getResult() {
            if (!isDone()) {
               throw new IllegalStateException();
            }
            ScheduledTask<Void, Runnable> task = taskDef.latest();
            return task.getResult();
         }

         @Override
         public boolean isFailed() {
            if (!isDone()) {
               return false;
            }
            ScheduledTask<Void, Runnable> task = taskDef.latest();
            return task != null && task.isSuccessful();
         }

         @Override
         public Throwable getFailure() {
            if (!isDone()) {
               throw new IllegalStateException();
            }
            ScheduledTask<Void, Runnable> task = taskDef.latest();
            return task.getFailure();
         }

         @Override
         public void addListener(final FutureListener<? super Void> listener, Executor executor) {
            final ListenableFuture<Void> self = this; 
            taskDef.addListener(new ScheduledTaskListener<Void, Runnable>() {
               @Override
               public void taskCompleted(ScheduledTask<? extends Void, ? extends Runnable> task) {
                  if (isDone()) {
                     listener.onCompletion(self);
                  }
               }
            });
         }

         @Override
         public void visit(FutureVisitor<? super Void> visitor) {
            if (!isDone()) {
               throw new IllegalStateException();
            }
            if (isCancelled()) {
               visitor.cancelled();
               return;
            }
            ScheduledTask<Void, Runnable> task = taskDef.latest();
            task.visit(visitor);
         }

         private CountDownLatch getAwaitLatch() {
            CountDownLatch latch = awaitLatch.get();
            if (latch != null) {
               return latch;
            }
            latch = new CountDownLatch(1);
            return awaitLatch.compareAndSet(null, latch) ? latch : awaitLatch.get();
         }
         
         @Override
         public void await() throws InterruptedException {
            getAwaitLatch().await();
         }

         @Override
         public boolean await(long limit, TimeUnit unit) throws InterruptedException {
            return getAwaitLatch().await(limit, unit);
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
         public boolean isFailed() {
            return task.isFailed();
         }

         @Override
         public boolean isSuccessful() {
            return task.isSuccessful();
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

         @Override
         public Void getResult() {
            return task.getResult();
         }

         @Override
         public Throwable getFailure() {
            return task.getFailure();
         }

         @Override
         public void addListener(FutureListener<? super Void> listener, Executor executor) {
            task.addListener(listener, executor);
         }

         @Override
         public void visit(FutureVisitor<? super Void> visitor) {
            task.visit(visitor);
         }

         @Override
         public void await() throws InterruptedException {
            task.await();
         }

         @Override
         public boolean await(long limit, TimeUnit unit) throws InterruptedException {
            return task.await(limit, unit);
         }
      };
   }
   
   private static <V> CallableRepeatingScheduledTask<V> callableScheduledTask(
         final ScheduledTaskDefinition<V, Callable<V>> taskDef) {
      return new CallableRepeatingScheduledTask<V>() {
         private final AtomicReference<CountDownLatch> awaitLatch =
               new AtomicReference<CountDownLatch>();
         
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
         
         @Override
         public V getMostRecentResult() {
            ScheduledTask<V, Callable<V>> task = taskDef.latest();
            if (task == null) {
               throw new IllegalStateException();
            }
            return task.getResult();
         }

         @Override
         public void addListenerForEachInstance(final FutureListener<? super V> listener,
               Executor executor) {
            final ListenableFuture<V> self = this; 
            taskDef.addListener(new ScheduledTaskListener<V, Callable<V>>() {
               @Override
               public void taskCompleted(ScheduledTask<? extends V, ? extends Callable<V>> task) {
                  listener.onCompletion(self);
               }
            });
         }

         @Override
         public int executionCount() {
            return taskDef.executionCount();
         }
         
         @Override
         public boolean isSuccessful() {
            if (!isDone()) {
               return false;
            }
            ScheduledTask<V, Callable<V>> task = taskDef.latest();
            return task != null && task.isSuccessful();
         }

         @Override
         public V getResult() {
            if (!isDone()) {
               throw new IllegalStateException();
            }
            ScheduledTask<V, Callable<V>> task = taskDef.latest();
            return task.getResult();
         }

         @Override
         public boolean isFailed() {
            if (!isDone()) {
               return false;
            }
            ScheduledTask<V, Callable<V>> task = taskDef.latest();
            return task != null && task.isSuccessful();
         }

         @Override
         public Throwable getFailure() {
            if (!isDone()) {
               throw new IllegalStateException();
            }
            ScheduledTask<V, Callable<V>> task = taskDef.latest();
            return task.getFailure();
         }

         @Override
         public void addListener(final FutureListener<? super V> listener, Executor executor) {
            final ListenableFuture<V> self = this; 
            taskDef.addListener(new ScheduledTaskListener<V, Callable<V>>() {
               @Override
               public void taskCompleted(ScheduledTask<? extends V, ? extends Callable<V>> task) {
                  if (isDone()) {
                     listener.onCompletion(self);
                  }
               }
            });
         }

         @Override
         public void visit(FutureVisitor<? super V> visitor) {
            if (!isDone()) {
               throw new IllegalStateException();
            }
            if (isCancelled()) {
               visitor.cancelled();
               return;
            }
            ScheduledTask<V, Callable<V>> task = taskDef.latest();
            task.visit(visitor);
         }

         private CountDownLatch getAwaitLatch() {
            CountDownLatch latch = awaitLatch.get();
            if (latch != null) {
               return latch;
            }
            latch = new CountDownLatch(1);
            return awaitLatch.compareAndSet(null, latch) ? latch : awaitLatch.get();
         }
         
         @Override
         public void await() throws InterruptedException {
            getAwaitLatch().await();
         }

         @Override
         public boolean await(long limit, TimeUnit unit) throws InterruptedException {
            return getAwaitLatch().await(limit, unit);
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
         public boolean isFailed() {
            return task.isFailed();
         }

         @Override
         public boolean isSuccessful() {
            return task.isSuccessful();
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
         
         @Override
         public V getResult() {
            return task.getResult();
         }

         @Override
         public Throwable getFailure() {
            return task.getFailure();
         }

         @Override
         public void addListener(FutureListener<? super V> listener, Executor executor) {
            task.addListener(listener, executor);
         }

         @Override
         public void visit(FutureVisitor<? super V> visitor) {
            task.visit(visitor);
         }

         @Override
         public void await() throws InterruptedException {
            task.await();
         }

         @Override
         public boolean await(long limit, TimeUnit unit) throws InterruptedException {
            return task.await(limit, unit);
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