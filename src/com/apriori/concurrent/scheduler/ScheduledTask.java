package com.apriori.concurrent.scheduler;

import com.apriori.concurrent.ListenableScheduledFuture;

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
 */
public interface ScheduledTask<V> extends ListenableScheduledFuture<V> {
   /**
    * Returns the task definition for which this task was created.
    * 
    * @return the task's definition
    */
   ScheduledTaskDefinition<V> taskDefinition();
   
   /**
    * Returns whether the scheduled task has begun executing or not.
    * 
    * @return true if the task has begun executing (including if it's already finished); false
    *       otherwise
    */
   boolean isStarted();

   /**
    * Returns the scheduled start time of this invocation, in milliseconds.
    * This is measured as milliseconds elapsed since midnight, January 1, 1970 UTC.
    * 
    * @return the scheduled start time of this invocation
    */
   long scheduledStartTimeMillis();

   /**
    * Returns the actual start time of this invocation, in milliseconds.
    * This is measured as milliseconds elapsed since midnight, January 1, 1970 UTC.
    * 
    * @return the actual start time of this invocation
    * 
    * @throws IllegalStateException if the task has not actually started yet
    */
   long actualStartTimeMillis();

   /**
    * Returns the actual end time of this invocation, in milliseconds. This is the
    * time the task either completed normally or the time it aborted due to thrown
    * exception.
    * 
    * @return the end time of this invocation
    * 
    * @throws IllegalStateException if the task not yet completed
    */
   long finishTimeMillis();
}