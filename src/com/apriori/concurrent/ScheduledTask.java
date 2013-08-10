package com.apriori.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

/**
 * The type of {@link ScheduledFuture} returned by submissions to a
 * {@link ScheduledTaskManager}. This extension provides various utility
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
//TODO: should extend ListenableScheduledFuture<V>
public interface ScheduledTask<V, T> extends ListenableScheduledFuture<V> {
   /**
    * Returns the task definition for which this task was created.
    * 
    * @return the task's definition
    */
   ScheduledTaskDefinition<V, T> taskDefinition();
   
   /**
    * Returns whether the scheduled task has begun executing or not.
    * 
    * @return true if the task has begun executing (including if it's already finished); false
    *       otherwise
    */
   boolean hasStarted();
   
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
}