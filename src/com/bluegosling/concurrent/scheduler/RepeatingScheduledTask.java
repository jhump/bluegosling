package com.bluegosling.concurrent.scheduler;

import com.bluegosling.concurrent.futures.fluent.FluentRepeatingFuture;


/**
 * Represents a repeating task. Canceling a repeating task will cancel all future occurrences of
 * the task. This future will not complete until canceled or the last invocation of the task
 * completes. The latter usually occurs when an invocation fails and causes future occurrences to be
 * aborted.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned upon completion of the task
 */
public interface RepeatingScheduledTask<V> extends FluentRepeatingFuture<V> {
   /**
    * Returns the task definition for which this task was created.
    * 
    * @return the task's definition
    */
   ScheduledTaskDefinition<V> taskDefinition();
}
