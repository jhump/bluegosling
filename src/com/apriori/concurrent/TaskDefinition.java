package com.apriori.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The definition for a task that will be executed by a {@link BetterExecutorService}.
 * This includes several configuration options that are not possible using a normal
 * {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned upon completion of the task
 * @param <T> the type of the actual task: {@link Callable}, {@link Runnable}, or
 *       {@link RunnableWithResult}
 */
public interface TaskDefinition<V, T> {
   
   /**
    * Returns the underlying task. This is the object that will be submitted to
    * the {@code ExecutorService}.
    * 
    * @return the underlying task
    */
   T task();
   
   /**
    * Returns the underlying task as a {@link Callable}.
    * 
    * @return the underlying task
    */
   Callable<V> taskAsCallable();
   
   /**
    * Returns the delay, in milliseconds, from the time the task is submitted to
    * the time the first instance of the task should be invoked. A delay of zero
    * means that it should be exceuted immediately.
    * 
    * @return the delay in milliseconds for the first instance of the task
    */
   long initialDelayMillis();
   
   /**
    * Returns whether or not this is a repeating task vs. a one-time task. A task
    * is a one-time task if its {@link #scheduleNextTaskPolicy() ScheduleNextTaskPolicy}
    * is {@link ScheduleNextTaskPolicies#NEVER}. Otherwise, it is considered a
    * repeating task.
    * 
    * @return {@code true} if this is a repeating task; {@code} false otherwise
    */
   boolean isRepeating();
   
   /**
    * Returns whether or not a repeating task is scheduled based on a fixed
    * rate or with fixed delays between invocations.
    * 
    * @return {@code true} if this is a repeating task and it is scheduled based
    *       on a fixed rate; {@code false otherwise}
    */
   boolean isFixedRate();
   
   /**
    * Returns the period (for fixed rate tasks) or delay (for fixed delay tasks)
    * for this task. If this is not a repeating task, zero is returned. If this is
    * a repeating task and this method returns zero, then subsequent invocations are
    * scheduled immediately upon completion of the previous invocation, running the
    * tasks sequentially as quickly as possible.
    * 
    * @return the period or delay, in milliseconds, for this task
    */
   long periodDelayMillis();
   
   /**
    * Returns the {@link ScheduleNextTaskPolicy} for this repeating task. If this
    * task is not a repeating task, this returns {@link ScheduleNextTaskPolicies#NEVER}.
    * 
    * @return the {@code ScheduleNextTaskPolicy} for this task
    */
   ScheduleNextTaskPolicy<? super V, ? super T> scheduleNextTaskPolicy();
   
   /**
    * Returns the maximum size of invocation history to maintain for this task. This
    * is the maximum size of the collection returned by {@link ScheduledTaskDefinition#history()}
    * upon scheduling this task definition. If the task repeats more times than this
    * size, information for older invocations is dropped.
    * 
    * @return the maximum size of invocation history
    */
   int maxHistorySize();
   
   /**
    * Returns the set of listeners for this task. These objects are notified upon
    * completion (including failures/abnormal completions) of each invocation of the
    * task.
    * 
    * <p>The set of listeners can actually be changed after the task definition has been
    * scheduled using {@link ScheduledTaskDefinition#addListener(ScheduledTask.Listener)}
    * and {@link ScheduledTaskDefinition#removeListener(ScheduledTask.Listener)}.
    * 
    * @return the set of listeners for this task
    */
   Set<ScheduledTask.Listener<? super V, ? super T>> listeners();
   
   /**
    * Returns the {@link UncaughtExceptionHandler} for this task. If any invocation
    * throws an uncaught exception, this handler will be dispatched to handle it. If
    * the tasks's handler is never assigned (i.e. {@code null}) then the thread's
    * default handler is used.
    * 
    * @return the {@code UncaughtExceptionHandler} for this task
    * 
    * @see Thread#getUncaughtExceptionHandler()
    */
   UncaughtExceptionHandler uncaughtExceptionHandler();
   
   /**
    * The default maximum history size.
    */
   int DEFAULT_MAX_HISTORY_SIZE = 100;
   
   /**
    * A builder for configuring {@link TaskDefinition} instances.
    * 
    * @param <V> the type of value returned upon completion of the task
    * @param <T> the type of the actual task: {@link Callable}, {@link Runnable}, or
    *       {@link RunnableWithResult}
    */
   public class Builder<V, T> {
      /**
       * Returns a new builder for a task definition whose underlying task is a
       * {@link Callable}.
       * 
       * @param callable the underlying task
       * @return a new builder
       */
      public static <V> Builder<V, Callable<V>> forCallable(Callable<V> callable) {
         return new Builder<V, Callable<V>>(callable, callable);
      }
      
      /**
       * Returns a new builder for a task definition whose underlying task is a
       * {@link Runnable}.
       * 
       * @param runnable the underlying task
       * @return a new builder
       */
      public static Builder<Void, Runnable> forRunnable(Runnable runnable) {
         return new Builder<Void, Runnable>(runnable, Executors.callable(runnable, (Void) null));
      }

      /**
       * Returns a new builder for a task definition whose underlying task is a
       * {@link Runnable} that has an associated result value.
       * 
       * @param runnable the underlying task
       * @param result the result returned upon completion of this task
       * @return a new builder
       */
      public static <V> Builder<V, RunnableWithResult<V>> forRunnable(final Runnable runnable,
            final V result) {
         return new Builder<V, RunnableWithResult<V>>(new RunnableWithResult<V>() {
            @Override
            public Runnable getRunnable() {
               return runnable;
            }
            @Override
            public V getResult() {
               return result;
            }
         }, Executors.callable(runnable, result));
      }

      private final T task;
      private final Callable<V> callable;
      private final Set<ScheduledTask.Listener<? super V, ? super T>> listeners;
      private int maxHistorySize = DEFAULT_MAX_HISTORY_SIZE;
      private ScheduleNextTaskPolicy<? super V, ? super T> scheduleNextTaskPolicy;
      private UncaughtExceptionHandler exceptionHandler;
      private long initialDelayMillis;
      private boolean isRepeating;
      private boolean isFixedRate;
      private long periodDelayMillis;
      
      /**
       * Constructs a new builder.
       * @param task the underlying task
       * @param callable the underlying task as (possibly wrapped by) an instance of {@link Callable}
       */
      private Builder(T task, Callable<V> callable) {
         this.task = task;
         this.callable = callable;
         this.listeners = new LinkedHashSet<ScheduledTask.Listener<? super V, ? super T>>();
      }
      
      /**
       * Configures the maximum history size for the task.
       * 
       * @param numExecutions the maximum number of executions for which history information is retained
       * @return {@code this}, for method chaining
       */
      public Builder<V, T> keepHistoryFor(int numExecutions) {
         this.maxHistorySize = numExecutions;
         return this;
      }

      /**
       * Adds a listener to the task.
       * 
       * @param listener the listener
       * @return {@code this}, for method chaining
       */
      public Builder<V, T> withListener(ScheduledTask.Listener<? super V, ? super T> listener) {
         listeners.add(listener);
         return this;
      }

      /**
       * Configures the {@link ScheduleNextTaskPolicy} for this task.
       * 
       * <p>If this is never configured and the task is a repeating task, then it defaults to
       * {@link ScheduleNextTaskPolicies#ALWAYS}. If, on the other hand, the task is <em>not</em>
       * a repeating task, it defaults to {@link ScheduleNextTaskPolicies#NEVER}.
       * 
       * @param policy the {@code ScheduleNextTaskPolicy}
       * @return {@code this}, for method chaining
       * 
       * @see #repeatAtFixedRate(long, TimeUnit)
       * @see #repeatWithFixedDelay(long, TimeUnit)
       */
      public Builder<V, T> withScheduleNextTaskPolicy(ScheduleNextTaskPolicy<? super V, ? super T> policy) {
         this.scheduleNextTaskPolicy = policy;
         return this;
      }

      /**
       * Configures the {@link UncaughtExceptionHandler} for this task. If this is set, it will
       * be used instead of the thread's normal {@link UncaughtExceptionHandler} to handle any
       * uncaught exceptions (including {@code Throwable}s) raised by an instance of the task.
       * 
       * @param handler the {@code UncaughtExceptionHandler}
       * @return {@code this}, for method chaining
       */
      public Builder<V, T> withUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
         this.exceptionHandler = handler;
         return this;
      }

      /**
       * Configures the delay between the time this task is submitted for execution and the
       * time the first invocation starts.
       * 
       * @param delay the delay
       * @param unit the time unit of {@code delay}
       * @return {@code this}, for method chaining
       */
      public Builder<V, T> withInitialDelay(long delay, TimeUnit unit) {
         this.initialDelayMillis = unit.toMillis(delay);
         return this;
      }

      /**
       * Configures the task to repeat on a fixed rate schedule. As long as executions of
       * the task take less time than the specified period, each subsequent invocation will
       * be scheduled at even intervals around this period. So a period of one minute will
       * cause tasks to be scheduled one minute after the previous one, regardless of the
       * time needed by the previous one to execute. If one or more executions take longer
       * than the specified period, then the next task will be run as soon as possible but
       * the frequency will never be faster than the specified rate (so short executions
       * will not allow them to "catch up" to the schedule -- instead, some invocations
       * are simply skipped to keep pace).
       * 
       * @param period the period between invocations
       * @param unit the time unit of {@code period}
       * @return {@code this}, for method chaining
       */
      public Builder<V, T> repeatAtFixedRate(long period, TimeUnit unit) {
         this.isRepeating = true;
         this.isFixedRate = true;
         this.periodDelayMillis = unit.toMillis(period);
         return this;
      }

      /**
       * Configures the task to repeat with a fixed delay between invocations.
       * 
       * @param delay the delay between invocations
       * @param unit the time unit of {@code period}
       * @return {@code this}, for method chaining
       */
      public Builder<V, T> repeatWithFixedDelay(long delay, TimeUnit unit) {
         this.isRepeating = true;
         this.isFixedRate = false;
         this.periodDelayMillis = unit.toMillis(delay);
         return this;
      }
      
      /**
       * Builds a new {@link TaskDefinition} using the current configuration.
       * 
       * @return a new {@link TaskDefinition}
       */
      public TaskDefinition<V, T> build() {
         ScheduleNextTaskPolicy<? super V, ? super T> policy = scheduleNextTaskPolicy;
         if (policy == null) {
            policy = isRepeating ? ScheduleNextTaskPolicies.ALWAYS : ScheduleNextTaskPolicies.NEVER;
         }
         return new TaskDefinitionImpl<V, T>(task, callable, maxHistorySize, listeners,
               policy, exceptionHandler, initialDelayMillis, isFixedRate, periodDelayMillis);
      }

      /**
       * The concrete implementation of {@link TaskDefinition} returned by
       * {@link Builder#build()}.
       */
      private static class TaskDefinitionImpl<V, T> implements TaskDefinition<V, T> {
         private final T task;
         private final Callable<V> callable;
         private final int maxHistorySize;
         private final Set<ScheduledTask.Listener<? super V, ? super T>> listeners;
         private final ScheduleNextTaskPolicy<? super V, ? super T> scheduleNextTaskPolicy;
         private final UncaughtExceptionHandler exceptionHandler;
         private final long initialDelayMillis;
         private final boolean isFixedRate;
         private final long periodDelayMillis;

         TaskDefinitionImpl(T task, Callable<V> callable, int maxHistorySize,
               Set<ScheduledTask.Listener<? super V, ? super T>> listeners,
               ScheduleNextTaskPolicy<? super V, ? super T> scheduleNextPolicy,
               UncaughtExceptionHandler exceptionHandler, long initialDelayMillis,
               boolean isFixedRate, long periodDelayMillis) {
            this.task = task;
            this.callable = callable;
            this.maxHistorySize = maxHistorySize;
            this.listeners = new LinkedHashSet<ScheduledTask.Listener<? super V, ? super T>>(listeners);
            this.scheduleNextTaskPolicy = scheduleNextPolicy;
            this.exceptionHandler = exceptionHandler;
            this.initialDelayMillis = initialDelayMillis;
            this.isFixedRate = isFixedRate;
            this.periodDelayMillis = periodDelayMillis;
         }
         
         @Override
         public T task() {
            return task;
         }
         
         @Override
         public Callable<V> taskAsCallable() {
            return callable;
         }

         @Override
         public long initialDelayMillis() {
            return initialDelayMillis;
         }

         @Override
         public boolean isRepeating() {
            return scheduleNextTaskPolicy != ScheduleNextTaskPolicies.NEVER;
         }

         @Override
         public boolean isFixedRate() {
            return isFixedRate;
         }

         @Override
         public long periodDelayMillis() {
            return periodDelayMillis;
         }

         @Override
         public ScheduleNextTaskPolicy<? super V, ? super T> scheduleNextTaskPolicy() {
            return scheduleNextTaskPolicy;
         }

         @Override
         public int maxHistorySize() {
            return maxHistorySize;
         }

         @Override
         public Set<ScheduledTask.Listener<? super V, ? super T>> listeners() {
            return Collections.unmodifiableSet(listeners);
         }

         @Override
         public UncaughtExceptionHandler uncaughtExceptionHandler() {
            return exceptionHandler;
         }
      }
   }
}