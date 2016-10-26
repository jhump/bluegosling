package com.bluegosling.concurrent.scheduler;

import com.bluegosling.collections.TransformingList;
import com.bluegosling.concurrent.Scheduled;
import com.bluegosling.concurrent.fluent.FluentFuture;
import com.bluegosling.concurrent.fluent.FluentScheduledExecutorService;
import com.bluegosling.concurrent.fluent.FutureListener;
import com.bluegosling.concurrent.fluent.Rescheduler;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An extension and enhancement to the API provided by {@link ScheduledExecutorService}.
 * 
 * <p>This implementation provides greater configurability over scheduled tasks and greater
 * optics into the results of executions of the tasks. It is API-compatible with the standard
 * {@link ScheduledExecutorService}, so it should behave the same if using only the standard API
 * and not any of the extensions herein.
 * 
 * <h3>Features</h3>
 * <p>The main features in this service, not available with the standard scheduled executor API,
 * follow:
 * <ul>
 *    <li>Asynchronous processing using {@link FutureListener}s. All of the futures returned by
 *    this service are instances of {@link FluentFuture}, which has a broader and more usable
 *    API than the standard {@link Future}, including the ability to add listeners to process
 *    results asynchronously.</li>
 *    <li>Greater control over repeated occurrences and how they are scheduled. Instead of a task
 *    either always repeating (unless an invocation fails) or never repeating (unless the logic
 *    itself schedules a successor when invoked), tasks specify a {@link ScheduleNextTaskPolicy}.
 *    Also, tasks specify a {@link Rescheduler}, which provides greater flexibility over the timing
 *    of subsequent tasks. {@link Rescheduler} implementations are provided for simple fixed-rate
 *    and fixed-delay schedules.</li>
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
// TODO: tests!
public class ScheduledTaskManager implements FluentScheduledExecutorService {
   
   private final ThreadPoolExecutor executor;
   
   @SuppressWarnings("unchecked") // we'll only put Runnables into it, promise
   private static BlockingQueue<Runnable> createWorkQueue() {
      BlockingQueue<?> queue = new DelayQueue<Delayed>();
      return (BlockingQueue<Runnable>) queue;
   }
   
   /**
    * Constructs a new task manager that uses the specified number of threads to execute tasks.
    * 
    * @param numThreads the number of threads in the pool
    */
   public ScheduledTaskManager(int numThreads) {
      executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.NANOSECONDS, 
            createWorkQueue());
   }

   /**
    * Constructs a new task manager with the specified number of threads and specified thread
    * factory.
    * 
    * @param numThreads the number of threads in the pool
    * @param threadFactory used to create threads in the pool
    */
   public ScheduledTaskManager(int numThreads, ThreadFactory threadFactory) {
      executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.NANOSECONDS,
            createWorkQueue(), threadFactory);
   }

   /**
    * Constructs a new task manager with the specified number of threads, specified thread factory,
    * and specified rejection handler.
    * 
    * @param numThreads the number of threads in the pool
    * @param threadFactory used to create threads in the pool
    * @param rejectedHandler handles rejected tasks (those submitted after the task manager is
    *       shutdown)
    */
   public ScheduledTaskManager(int numThreads, ThreadFactory threadFactory,
         RejectedExecutionHandler rejectedHandler) {
      executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.NANOSECONDS,
            createWorkQueue(), threadFactory, rejectedHandler);
   }

   /**
    * Constructs a new task manager with the specified number of threads and specified rejection
    * handler.
    * 
    * @param numThreads the number of threads in the pool
    * @param rejectedHandler handles rejected tasks (those submitted after the task manager is
    *       shutdown)
    */
   public ScheduledTaskManager(int numThreads, RejectedExecutionHandler rejectedHandler) {
      executor = new ThreadPoolExecutor(numThreads, numThreads, 0, TimeUnit.NANOSECONDS,
            createWorkQueue(), rejectedHandler);
   }
   
   /**
    * Schedules the specified task for execution. If the specified delay is zero or negative, it is
    * scheduled for immediate execution. The returned object will not only be a fluent future,
    * it will be a {@link ScheduledTask}.
    */
   @Override
   public <V> ScheduledTask<V> schedule(Callable<V> task, long delay, TimeUnit unit) {
      return scheduleInternal(
            TaskDefinition.forCallable(task).withInitialDelay(delay, unit).build())
            .scheduleFirst();
   }
   
   /**
    * Schedules the specified task for execution. If the specified delay is zero or negative, it is
    * scheduled for immediate execution. The returned object will not only be a fluent future,
    * it will be a {@link ScheduledTask}.
    */
   @Override
   public ScheduledTask<Void> schedule(Runnable task, long delay, TimeUnit unit) {
      return scheduleInternal(
            TaskDefinition.forRunnable(task).withInitialDelay(delay, unit).build())
            .scheduleFirst();
   }

   /**
    * Schedules the specified task for periodic execution. If the specified initial delay is zero or
    * negative, it is scheduled for immediate execution. The task will execute at a fixed rate per
    * the specified period, but never more than a single instance will be executing at a given time.
    * Instead of running tasks concurrently, if a single occurrence takes longer than the period,
    * subsequent instances will be delayed and be scheduled to start as soon as the slow occurrence
    * completes.
    * 
    * <p>This is very similar to {@link #scheduleAtFixedRate(Runnable, long, long, TimeUnit)}, which
    * is API provided by {@link ScheduledExecutorService}. But this version can run a callable that
    * produces a value. The returned future has additional methods for inspecting the results
    * produced by past executions.
    * 
    * @param task the task to execute
    * @param initialDelay the delay from submission time when the first occurrence should execute
    * @param period the period of time between occurrences
    * @param unit the unit for {@code initialDelay} and {@code period}
    * @return a fluent scheduled future that can be used to inspect the task's state and past
    *       results
    * @throws NullPointerException if {@code task} or {@code unit} is {@code null}
    * @throws IllegalArgumentException if {@code period} is non-positive
    * 
    * @see #scheduleAtFixedRate(Runnable, long, long, TimeUnit)
    */
   public <V> RepeatingScheduledTask<V> scheduleAtFixedRate(Callable<V> task,
         long initialDelay, long period, TimeUnit unit) {
      ScheduledTaskDefinitionImpl<V> taskDef = 
            scheduleInternal(TaskDefinition.forCallable(task)
                  .withInitialDelay(initialDelay, unit)
                  .repeatAtFixedRate(period, unit)
                  // for API compatibility with normal ScheduledExecutorService, we only continue
                  // scheduling instances of the task if it succeeds
                  .withScheduleNextTaskPolicy(ScheduleNextTaskPolicies.ON_SUCCESS)
                  .build());
      taskDef.scheduleFirst();
      return new RepeatingScheduledTaskImpl<V>(taskDef);
   }
  
   @Override
   public RepeatingScheduledTask<Void> scheduleAtFixedRate(Runnable task, long initialDelay,
         long period, TimeUnit unit) {
      ScheduledTaskDefinitionImpl<Void> taskDef =
            scheduleInternal(TaskDefinition.forRunnable(task)
                  .withInitialDelay(initialDelay, unit)
                  .repeatAtFixedRate(period, unit)
                  // for API compatibility with normal ScheduledExecutorService, we only continue
                  // scheduling instances of the task if it succeeds
                  .withScheduleNextTaskPolicy(ScheduleNextTaskPolicies.ON_SUCCESS)
                  .build());
      taskDef.scheduleFirst();
      return new RepeatingScheduledTaskImpl<Void>(taskDef);
   }
   
   /**
    * Schedules the specified task for repeated execution. If the specified initial delay is zero or
    * negative, it is scheduled for immediate execution. The task will execute with a fixed delay
    * between invocations.
    * 
    * <p>This is very similar to {@link #scheduleWithFixedDelay(Runnable, long, long, TimeUnit)},
    * which is API provided by {@link ScheduledExecutorService}. But this version can run a callable
    * that produces a value. The returned future has additional methods for inspecting the results
    * produced by past executions.
    * 
    * @param task the task to execute
    * @param initialDelay the delay from submission time when the first occurrence should execute
    * @param delay the delay between occurrences
    * @param unit the unit for {@code initialDelay} and {@code delay}
    * @return a fluent scheduled future that can be used to inspect the task's state and past
    *       results
    * @throws NullPointerException if {@code task} or {@code unit} is {@code null}
    * @throws IllegalArgumentException if {@code delay} is non-positive
    * 
    * @see #scheduleWithFixedDelay(Runnable, long, long, TimeUnit)
    */
   public <V> RepeatingScheduledTask<V> scheduleWithFixedDelay(Callable<V> task,
         long initialDelay, long delay, TimeUnit unit) {
      ScheduledTaskDefinitionImpl<V> taskDef =
            scheduleInternal(TaskDefinition.forCallable(task)
                  .withInitialDelay(initialDelay, unit)
                  .repeatWithFixedDelay(delay, unit)
                  // for API compatibility with normal ScheduledExecutorService, we only continue
                  // scheduling instances of the task if it succeeds
                  .withScheduleNextTaskPolicy(ScheduleNextTaskPolicies.ON_SUCCESS)
                  .build());
      taskDef.scheduleFirst();
      return new RepeatingScheduledTaskImpl<V>(taskDef);
   }

   @Override
   public RepeatingScheduledTask<Void> scheduleWithFixedDelay(Runnable task, long initialDelay,
         long delay, TimeUnit unit) {
      ScheduledTaskDefinitionImpl<Void> taskDef = scheduleInternal(
            TaskDefinition.forRunnable(task)
            .withInitialDelay(initialDelay, unit)
            .repeatWithFixedDelay(delay, unit)
            // for API compatibility with normal ScheduledExecutorService, we only continue
            // scheduling instances of the task if it succeeds
            .withScheduleNextTaskPolicy(ScheduleNextTaskPolicies.ON_SUCCESS)
            .build());
      taskDef.scheduleFirst();
      return new RepeatingScheduledTaskImpl<Void>(taskDef);
   }
   
   private void cancelUnwantedTasks() {
      //TODO: policy on whether to wait for or cancel delayed tasks
   }
   
   @Override
   public void shutdown() {
      executor.shutdown();
      cancelUnwantedTasks();
   }

   @Override
   public List<Runnable> shutdownNow() {
      return new TransformingList.ReadOnly<Runnable, Runnable>(executor.shutdownNow(),
            (input) -> {
               Runnable r = ((AbstractStampedTask) input).unwrap();
               if (r instanceof Future) {
                  ((Future<?>) r).cancel(false);
               }
               return r;
            });
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This implementation is the same as using the {@link #schedule(Callable, long, TimeUnit)}
    * method but indicating a zero delay so the task is scheduled for immediate execution. The
    * returned future is not just a fluent future, but is a {@link ScheduledTask}.
    */
   @Override
   public <V> ScheduledTask<V> submit(Callable<V> task) {
      return scheduleInternal(TaskDefinition.forCallable(task).build())
            .scheduleFirst();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This implementation is the same as using the {@link #schedule(Runnable, long, TimeUnit)}
    * method but indicating a zero delay so the task is scheduled for immediate execution. The
    * returned future is not just a fluent future, but is a {@link ScheduledTask}.
    */
   @Override
   public ScheduledTask<Void> submit(Runnable task) {
      return scheduleInternal(TaskDefinition.forRunnable(task).build())
            .scheduleFirst();
   }

   /**
    * {@inheritDoc}
    * 
    * <p>This implementation is similar to using the {@link #schedule(Runnable, long, TimeUnit)}
    * method with a zero delay so the task is scheduled for immediate execution. The key difference
    * being that this form allows a non-null value to be used as the result. The returned future is
    * not just a fluent future, but is a {@link ScheduledTask}.
    */
   @Override
   public <V> ScheduledTask<V> submit(Runnable task, V result) {
      return scheduleInternal(TaskDefinition.forRunnable(task, result).build())
            .scheduleFirst();
   }
   
   private Runnable wrap(Runnable task) {
      if (task instanceof Scheduled) {
         return new ScheduledStampedTask((Scheduled) task);
      } else if (task instanceof Delayed) {
         return new DelayedStampedTask((Delayed) task);
      } else {
         return new ImmediateStampedTask(task);
      }
   }

   @Override
   public void execute(Runnable task) {
      if (isShutdown()) {
         executor.getRejectedExecutionHandler().rejectedExecution(task, executor);
      }
      Runnable wrappedTask = wrap(task);
      executor.prestartCoreThread();
      executor.getQueue().add(wrappedTask);
      
      if (isShutdown() && executor.getQueue().remove(wrappedTask)) {
         // if we were racing with concurrent call to shutdown and lost, then reject this execution
         executor.getRejectedExecutionHandler().rejectedExecution(task, executor);
      }
   }

   /**
    * Schedules the specified task definition for execution. On return, the first occurrence of the
    * task (or only occurrence if not a repeating task) will be scheduled.
    * 
    * @param taskDef the definition of the task to schedule
    * @return the scheduled form of the task definition
    */
   public <V> ScheduledTaskDefinition<V> schedule(TaskDefinition<V> taskDef) {
      ScheduledTaskDefinitionImpl<V> scheduledTaskDef = scheduleInternal(taskDef);
      scheduledTaskDef.scheduleFirst();
      return scheduledTaskDef;
   }
   
   private <V> ScheduledTaskDefinitionImpl<V> scheduleInternal(TaskDefinition<V> taskDef) {
      return new ScheduledTaskDefinitionImpl<V>(taskDef, this);
   }
   
   private abstract static class AbstractStampedTask implements Scheduled, Runnable {
      private static final AtomicLong stampSequence = new AtomicLong();
      
      private final long stamp = stampSequence.incrementAndGet();
      
      @Override
      public int compareTo(Delayed o) {
         int result = Scheduled.COMPARATOR.compare(this,  o);
         if (result == 0 && o instanceof AbstractStampedTask) {
            long otherStamp = ((AbstractStampedTask) o).stamp;
            result = stamp > otherStamp ? 1 : (stamp < otherStamp ? -1 : 0); 
         }
         return result;
      }
      
      public abstract Runnable unwrap();
   }
   
   private static class ScheduledStampedTask extends AbstractStampedTask {
      private final Scheduled wrapped;
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      ScheduledStampedTask(Scheduled wrapped) {
         if (!(wrapped instanceof Runnable)) {
            throw new IllegalArgumentException();
         }
         this.wrapped = wrapped;
      }

      @Override
      public long getScheduledNanoTime() {
         return wrapped.getScheduledNanoTime();
      }

      @Override
      public long getDelay(TimeUnit unit) {
         return wrapped.getDelay(unit);
      }

      @Override
      public void run() {
         ((Runnable) wrapped).run();
      }

      @Override
      public Runnable unwrap() {
         return (Runnable) wrapped;
      }
   }

   private static class DelayedStampedTask extends AbstractStampedTask {
      private final Delayed wrapped;
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      DelayedStampedTask(Delayed wrapped) {
         if (!(wrapped instanceof Runnable)) {
            throw new IllegalArgumentException();
         }
         this.wrapped = wrapped;
      }

      @Override
      public long getScheduledNanoTime() {
         return System.nanoTime() + wrapped.getDelay(TimeUnit.NANOSECONDS);
      }

      @Override
      public long getDelay(TimeUnit unit) {
         return wrapped.getDelay(unit);
      }

      @Override
      public void run() {
         ((Runnable) wrapped).run();
      }

      @Override
      public Runnable unwrap() {
         return (Runnable) wrapped;
      }
   }
   
   private static class ImmediateStampedTask extends AbstractStampedTask {
      private final long scheduledNanoTime = System.nanoTime(); // run immediately
      private final Runnable task;
      
      @SuppressWarnings("synthetic-access") // super-class ctor is private
      ImmediateStampedTask(Runnable task) {
         this.task = task;
      }
      
      @Override
      public long getScheduledNanoTime() {
         return scheduledNanoTime;
      }

      @Override
      public long getDelay(TimeUnit unit) {
         return unit.convert(scheduledNanoTime - System.nanoTime(), TimeUnit.NANOSECONDS);
      }

      @Override
      public void run() {
         task.run();
      }

      @Override
      public Runnable unwrap() {
         return task;
      }
   }
   
   @Override
   public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
         throws InterruptedException {
      List<Future<T>> results = new ArrayList<Future<T>>(tasks.size());
      for (Callable<T> task : tasks) {
         results.add(submit(task));
      }
      for (Future<T> future : results) {
         ((FluentFuture<T>) future).await();
      }
      return results;
   }

   @Override
   public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
         TimeUnit unit) throws InterruptedException {
      long deadline = System.nanoTime() + unit.toNanos(timeout);
      List<Future<T>> results = new ArrayList<Future<T>>(tasks.size());
      for (Callable<T> task : tasks) {
         long remainingNanos = deadline - System.nanoTime();
         if (remainingNanos < 0) {
            // don't bother submitting a task for execution if deadline has already expired
            results.add(FluentFuture.cancelledFuture());
         } else {
            results.add(submit(task));
         }
      }
      for (Future<T> future : results) {
         long remainingNanos = deadline - System.nanoTime();
         if (remainingNanos < 0
               || !((FluentFuture<T>) future).await(remainingNanos, TimeUnit.NANOSECONDS)) {
            future.cancel(false);
         }
      }
      return results;
   }

   @Override
   public boolean isShutdown() {
      return executor.isShutdown();
   }

   @Override
   public boolean isTerminated() {
      return executor.isTerminated();
   }

   @Override
   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return executor.awaitTermination(timeout, unit);
   }

   @Override
   public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
         ExecutionException {
      return executor.invokeAny(tasks);
   }

   @Override
   public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
         throws InterruptedException, ExecutionException, TimeoutException {
      return executor.invokeAny(tasks, timeout, unit);
   }
}
