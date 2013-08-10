package com.apriori.concurrent;

import java.util.concurrent.Callable;

/**
 * Represents a repeating task. Cancelling a repeating task will cancel all future occurrences of
 * the task. This future will not complete until the last execution of the task completes, usually
 * when an execution fails and causes future occurrences to be aborted.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned upon completion of the task
 * @param <T> the type of the actual task: {@link Callable}, {@link Runnable}, or
 *       {@link RunnableWithResult}
 */
//TODO: should extend ListenableRepeatingFuture<V>
public interface RepeatingScheduledTask<V, T> extends ListenableRepeatingFuture<V> {
   /**
    * Returns the task definition for which this task was created.
    * 
    * @return the task's definition
    */
   ScheduledTaskDefinition<V, T> taskDefinition();
}
