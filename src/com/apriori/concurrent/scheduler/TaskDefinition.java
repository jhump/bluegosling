package com.apriori.concurrent.scheduler;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The definition for a task that will be executed by a {@link ScheduledTaskManager}.
 * This includes several configuration options that are not possible using a normal
 * {@link java.util.concurrent.ScheduledExecutorService ScheduledExecutorService}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned upon completion of the task
 */
public interface TaskDefinition<V> {
   
   /**
    * Returns the implementation of the task. 
    * 
    * @return the task implementation
    */
   TaskImplementation<V> task();
   
   /**
    * Returns the delay, in the specified unit, from the time the task is submitted to
    * the time the first instance of the task should be invoked. A negative or
    * zero delay means that it should be executed immediately.
    * 
    * @param unit the unit of the returned value
    * @return the delay between the time the task is submitted for execution and the time that
    *       the first instance of the task is scheduled to start
    */
   long initialDelay(TimeUnit unit);
   
   /**
    * Returns whether or not this is a repeating task vs. a one-time task. A task
    * is a one-time task if its {@link #scheduleNextTaskPolicy() ScheduleNextTaskPolicy}
    * is {@link ScheduleNextTaskPolicies#NEVER}. Otherwise, it is considered a
    * repeating task. Repeating tasks will also have a non-null {@link #rescheduler() Rescheduler}.
    * 
    * @return {@code true} if this is a repeating task; {@code false} otherwise
    */
   boolean isRepeating();

   /**
    * Returns the rescheduler, used to compute the start time for subsequent invocations of a
    * repeating task. 
    *
    * @return the rescheduler or {@code null} if this is not a repeating task
    */
   Rescheduler<? super V> rescheduler();
   
   /**
    * Returns whether or not a repeating task is scheduled based on a fixed
    * rate or with fixed delays between invocations.
    * 
    * @return {@code true} if this is a repeating task and it is scheduled based
    *       on a fixed rate; {@code false} otherwise
    */
   boolean isFixedRate();
   
   /**
    * Returns the period (for fixed rate tasks) or delay (for fixed delay tasks)
    * for this task. If this is not a repeating task, zero is returned. If this is
    * a repeating task and this method returns zero, then a custom {@link #rescheduler()
    * Rescheduler} is in use and the period or delay between invocations isn't known.
    * 
    * @param unit the unit of the returned value
    * @return the period or delay, in the specified unit, for this task or zero if this is not
    *       a repeating task or if the period or delay of the current {@link Rescheduler}
    *       is unknown
    */
   long periodDelay(TimeUnit unit);
   
   /**
    * Returns the {@link ScheduleNextTaskPolicy} for this repeating task. If this
    * task is not a repeating task, this returns {@link ScheduleNextTaskPolicies#NEVER}.
    * 
    * @return the {@code ScheduleNextTaskPolicy} for this task
    */
   ScheduleNextTaskPolicy<? super V> scheduleNextTaskPolicy();
   
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
    * scheduled using {@link ScheduledTaskDefinition#addListener(ScheduledTaskListener)}
    * and {@link ScheduledTaskDefinition#removeListener(ScheduledTaskListener)}.
    * 
    * @return the set of listeners for this task
    */
   Set<ScheduledTaskListener<? super V>> listeners();
   
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
    * Returns a new builder for a task definition whose underlying task is a
    * {@link Callable}.
    * 
    * @param callable the underlying task
    * @return a new builder
    */
   static <V> Builder<V> forCallable(Callable<V> callable) {
      return new Builder<>(TaskImplementation.forCallable(callable));
   }

   /**
    * Returns a new builder for a task definition whose underlying task is a
    * {@link Callable}.
    * 
    * @param callable the underlying task
    * @return a new builder
    */
   static <V> Builder<V> forSupplier(Supplier<V> supplier) {
      return new Builder<>(TaskImplementation.forSupplier(supplier));
   }

   /**
    * Returns a new builder for a task definition whose underlying task is a
    * {@link Runnable}.
    * 
    * @param runnable the underlying task
    * @return a new builder
    */
   static Builder<Void> forRunnable(Runnable runnable) {
      return forRunnable(runnable, null);
   }

   /**
    * Returns a new builder for a task definition whose underlying task is a
    * {@link Runnable} that has an associated result value.
    * 
    * @param runnable the underlying task
    * @param result the result returned upon completion of this task
    * @return a new builder
    */
   static <V> Builder<V> forRunnable(final Runnable runnable, final V result) {
      return new Builder<>(TaskImplementation.forRunnable(runnable, result));
   }
   
   /**
    * A builder for configuring {@link TaskDefinition} instances.
    * 
    * @param <V> the type of value returned upon completion of the task
    */
   class Builder<V> {
      private final TaskImplementation<V> task;
      private final Set<ScheduledTaskListener<? super V>> listeners;
      private int maxHistorySize = DEFAULT_MAX_HISTORY_SIZE;
      private ScheduleNextTaskPolicy<? super V> scheduleNextTaskPolicy;
      private UncaughtExceptionHandler exceptionHandler;
      private long initialDelayNanos;
      private Rescheduler<? super V> rescheduler;
      
      /**
       * Constructs a new builder.
       * 
       * @param task the underlying task
       */
      Builder(TaskImplementation<V> task) {
         this.task = task;
         this.listeners = new LinkedHashSet<ScheduledTaskListener<? super V>>();
      }
      
      /**
       * Configures the maximum history size for the task.
       * 
       * @param numExecutions the maximum number of executions for which history information is
       *       retained
       * @return {@code this}, for method chaining
       */
      public Builder<V> keepHistoryFor(int numExecutions) {
         if (numExecutions < 1) {
            throw new IllegalArgumentException("History must allow at least one execution");
         }
         this.maxHistorySize = numExecutions;
         return this;
      }

      /**
       * Adds a listener to the task.
       * 
       * @param listener the listener
       * @return {@code this}, for method chaining
       */
      public Builder<V> withListener(ScheduledTaskListener<? super V> listener) {
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
      public Builder<V> withScheduleNextTaskPolicy(ScheduleNextTaskPolicy<? super V> policy) {
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
      public Builder<V> withUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
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
      public Builder<V> withInitialDelay(long delay, TimeUnit unit) {
         this.initialDelayNanos = unit.toNanos(delay);
         return this;
      }
      
      /**
       * Configures the task to recur using the given rescheduler for computing when subsequent
       * invocations will start.
       *
       * @param nextTaskScheduler computes when subsequent invocations will start
       * @return {@code this}, for method chaining
       */
      public Builder<V> repeat(Rescheduler<? super V> nextTaskScheduler) {
         this.rescheduler = nextTaskScheduler;
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
      public Builder<V> repeatAtFixedRate(long period, TimeUnit unit) {
         this.rescheduler = Rescheduler.atFixedRate(period, unit);
         return this;
      }

      /**
       * Configures the task to repeat with a fixed delay between invocations.
       * 
       * @param delay the delay between invocations
       * @param unit the time unit of {@code period}
       * @return {@code this}, for method chaining
       */
      public Builder<V> repeatWithFixedDelay(long delay, TimeUnit unit) {
         this.rescheduler = Rescheduler.withFixedDelay(delay, unit);
         return this;
      }
      
      /**
       * Builds a new {@link TaskDefinition} using the current configuration.
       * 
       * @return a new {@link TaskDefinition}
       */
      public TaskDefinition<V> build() {
         ScheduleNextTaskPolicy<? super V> policy;
         if (rescheduler == null) {
            policy = ScheduleNextTaskPolicies.NEVER;
         } else if (scheduleNextTaskPolicy == null) {
            policy = ScheduleNextTaskPolicies.ALWAYS;
         } else {
            policy = scheduleNextTaskPolicy;
         }
         return new TaskDefinitionImpl<V>(task, maxHistorySize, listeners, policy, exceptionHandler,
               initialDelayNanos, rescheduler);
      }

      /**
       * The concrete implementation of {@link TaskDefinition} returned by
       * {@link Builder#build()}.
       */
      private static class TaskDefinitionImpl<V> implements TaskDefinition<V> {
         private final TaskImplementation<V> task;
         private final int maxHistorySize;
         private final Set<ScheduledTaskListener<? super V>> listeners;
         private final ScheduleNextTaskPolicy<? super V> scheduleNextTaskPolicy;
         private final UncaughtExceptionHandler exceptionHandler;
         private final long initialDelayNanos;
         private final Rescheduler<? super V> rescheduler;

         TaskDefinitionImpl(TaskImplementation<V> task, int maxHistorySize,
               Set<ScheduledTaskListener<? super V>> listeners,
               ScheduleNextTaskPolicy<? super V> scheduleNextPolicy,
               UncaughtExceptionHandler exceptionHandler, long initialDelayNanos,
               Rescheduler<? super V> rescheduler) {
            this.task = task;
            this.maxHistorySize = maxHistorySize;
            this.listeners =
                  new LinkedHashSet<ScheduledTaskListener<? super V>>(listeners);
            this.scheduleNextTaskPolicy = scheduleNextPolicy;
            this.exceptionHandler = exceptionHandler;
            this.initialDelayNanos = initialDelayNanos;
            this.rescheduler = rescheduler;
         }
         
         @Override
         public TaskImplementation<V> task() {
            return task;
         }
         
         @Override
         public long initialDelay(TimeUnit unit) {
            return unit.convert(initialDelayNanos, TimeUnit.NANOSECONDS);
         }

         @Override
         public boolean isRepeating() {
            return scheduleNextTaskPolicy != ScheduleNextTaskPolicies.NEVER;
         }
         
         @Override
         public Rescheduler<? super V> rescheduler() {
            return rescheduler;
         }

         @Override
         public boolean isFixedRate() {
            if (rescheduler == null) {
               return false;
            }
            return Rescheduler.getFixedRatePeriodNanos(rescheduler) != 0;
         }

         @Override
         public long periodDelay(TimeUnit unit) {
            if (rescheduler == null) {
               return 0;
            }
            long periodNanos = Rescheduler.getFixedRatePeriodNanos(rescheduler);
            return unit.convert(
                  periodNanos != 0 ? periodNanos : Rescheduler.getFixedDelayNanos(rescheduler),
                  TimeUnit.NANOSECONDS);
         }

         @Override
         public ScheduleNextTaskPolicy<? super V> scheduleNextTaskPolicy() {
            return scheduleNextTaskPolicy;
         }

         @Override
         public int maxHistorySize() {
            return maxHistorySize;
         }

         @Override
         public Set<ScheduledTaskListener<? super V>> listeners() {
            return Collections.unmodifiableSet(listeners);
         }

         @Override
         public UncaughtExceptionHandler uncaughtExceptionHandler() {
            return exceptionHandler;
         }
      }
   }
}