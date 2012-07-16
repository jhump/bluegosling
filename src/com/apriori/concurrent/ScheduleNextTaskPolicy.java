package com.apriori.concurrent;

import java.util.concurrent.Callable;

/**
 * A policy that governs whether a task should be repeated or retried.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned upon completion of the task
 * @param <T> the type of the actual task: {@link Callable}, {@link Runnable}, or
 *       {@link RunnableWithResult}
 */
public interface ScheduleNextTaskPolicy<V, T> {
   /**
    * Determines whether a given task definition should be invoked again.
    * 
    * @param latest the most recently completed invocation
    * @return {@code true} if the task should be invoked again; {@code false} otherwise
    */
   boolean shouldScheduleNext(ScheduledTask<? extends V, ? extends T> latest);
}