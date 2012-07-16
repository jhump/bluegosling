package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

/**
 * The type of {@link ScheduledFuture} returned by submissions to a
 * {@link BetterExecutorService}. This extension provides various utility
 * methods as well as access to the {@link ScheduledTaskDefinition} for which
 * the task was created.
 * 
 * <p>A {@code ScheduledTask} basically represents a single invocation of
 * a {@code ScheduledTaskDefinition}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned upon completion of the task
 * @param <T> the type of the actual task: {@link Callable}, {@link Runnable}, or
 *       {@link RunnableWithResult}
 */
public interface ScheduledTask<V, T> extends ScheduledFuture<V> {
   /**
    * Returns the task definition for which this task was created.
    * 
    * @return the task's definition
    */
   ScheduledTaskDefinition<V, T> taskDefinition();
   
   /**
    * Returns the actual start time of this invocation, in milliseconds.
    * This is measured as milliseconds elapsed since midnight, January 1, 1970 UTC.
    * 
    * @return the start time of this invocation
    * 
    * @throws IllegalStateException if the task has not actually started yet
    */
   long actualTaskStartMillis();

   /**
    * Returns the actual end time of this invocation, in milliseconds. This is the
    * time the task either completed normally or the time it aborted due to thrown
    * exception.
    * 
    * @return the end time of this invocation
    * 
    * @throws IllegalStateException if the task not yet completed
    */
   long actualTaskEndMillis();
   
   /**
    * Returns whether this task failed or not. A task failed if it completed
    * abnormally due to throwing an uncaught exception.
    * 
    * @return {@code true} if the task failed; {@code false} otherwise
    * 
    * @throws IllegalStateException if the task not yet completed
    */
   boolean failed();
   
   /**
    * Returns whether this task succeeded or not. A task succeeded if it
    * completed normally (no exception thrown).
    * 
    * @return {@code true} if the task succeeded; {@code false} otherwise
    * 
    * @throws IllegalStateException if the task not yet completed
    */
   boolean succeeded();

   /**
    * A listener for completions of {@link ScheduledTask}s.
    */
   public interface Listener<V, T> {
      /**
       * Called when the specified task completes (either normally or
       * abnormally). You can easily distinguish between successful and
       * failed tasks via {@link ScheduledTask#succeeded()} and
       * {@link ScheduledTask#failed()}.
       * 
       * @param task the completed task
       */
      void taskCompleted(ScheduledTask<? extends V, ? extends T> task);
   }
}