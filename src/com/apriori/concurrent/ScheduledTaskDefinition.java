package com.apriori.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * A {@link TaskDefinition} that has been scheduled using a {@link BetterExecutorService}.
 * This provides additional methods for introspection into the state of the scheduled tasks,
 * including looking at past invocations of the task and canceling or pausing/resuming the
 * scheduled tasks.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned upon completion of the task
 * @param <T> the type of the actual task: {@link Callable}, {@link Runnable}, or
 *       {@link RunnableWithResult}
 */
public interface ScheduledTaskDefinition<V, T> extends TaskDefinition<V, T> {
   /**
    * Returns the timestamp, in milliseconds, that this task definition was submitted
    * to a {@link BetterExecutorService}. This is measured as milliseconds elapsed since
    * midnight, January 1, 1970 UTC.
    * 
    * @return the time the task definition was submitted
    */
   long submitTimeMillis();
   
   /**
    * Returns the {@link BetterExecutorService} to which this task definition
    * was submitted.
    * 
    * @return the {@link BetterExecutorService} to which this task definition
    *       was submitted 
    */
   BetterExecutorService executor();
   
   /**
    * Returns the total number of times that tasks have been executed for this
    * task definition. This number includes both normal and abnormal completions.
    * 
    * @return the count of executions
    */
   int executionCount();
   
   /**
    * Returns the number of times that tasks have been failed (completed abnormally)
    * for this task definition.
    * 
    * @return the count of failed executions
    */
   int failureCount();
   
   /**
    * Returns the number of times that tasks have succeeded for this task
    * definition.
    * 
    * @return the count of successful executions
    */
   int successCount();
   
   /**
    * Returns the history of past executions for this task definition. Only a
    * limited number of past executions are retained and returned in this
    * collection. Iteration order of the returned collection is such that
    * the most recent execution is returned first and the oldest retained
    * execution is last.
    * 
    * @return the history of past executions
    */
   Collection<ScheduledTask<V, T>> history();
   
   /**
    * Returns details for the most recently completed invocation of this task
    * definition. This is equivalent to:
    * <pre>
    * scheduledTaskDefinition.history().iterator().next();
    * </pre>
    * @return details for the latest invocation
    */
   ScheduledTask<V, T> latest();
   
   /**
    * Returns details for the currently scheduled invocation, which might be
    * executing currently. This instance will not appear in the history until
    * it has completed.
    * 
    * <p>This will be {@code null} if there is no invocation currently running
    * or scheduled to run. This will be the case when the task definition is
    * {@linkplain #isFinished() finished} or {@linkplain #isCancelled() cancelled}.
    * It can also occur when the task definition is {@linkplain #isPaused() paused}.
    *   
    * @return the currently scheduled invocation
    */
   ScheduledTask<V, T> current();
   
   /**
    * Adds a listener that will be notified when task invocations complete.
    *  
    * @param listener the listener
    */
   void addListener(ScheduledTask.Listener<? super V, ? super T> listener);
   
   /**
    * Removes a listener. No notifications will be sent to the listener after
    * it is removed.
    * 
    * @param listener the listener
    * @return {@code true} if the listener was removed; {@code false} otherwise,
    *       such as if the listener was never added to this task definition or
    *       has already been removed
    */
   boolean removeListener(ScheduledTask.Listener<? super V, ? super T> listener);
   
   /**
    * Returns whether or not the task definition is finished. It is finished when
    * all invocations are completed and the {@link #scheduleNextTaskPolicy() ScheduleNextTaskPolicy}
    * indicates that no next task need be scheduled.
    * 
    * @return {@code true} if the task definition is finished; {@code false} otherwise
    */
   boolean isFinished();
   
   /**
    * Cancels this task definition. No more invocations will be scheduled. If so
    * specified, any currently running invocation will be interrupted.
    * 
    * <p>Canceling a task definition more than once has no effect. Subsequent calls
    * will return the same thing as the first call.
    * 
    * @param interrupt {@code true} if a currently running invocation should be
    *       interrupted via {@link Thread#interrupt()}
    * @return {@code true} if the task was cancelled; {@code false} if it could
    *       not be cancelled because it was already finished
    */
   boolean cancel(boolean interrupt);
   
   /**
    * Returns whether or not the task definition is cancelled. This will be the case
    * if the task definition was {@linkplain #cancel(boolean) cancelled} before it
    * finished normally.
    * 
    * @return {@code true} if the task definition is cancelled; {@code false} otherwise
    */
   boolean isCancelled();
   
   /**
    * Pauses this task definition. If an invocation is currently running, it will be
    * allowed to complete. After the currently running invocation completes, no
    * further invocations will be scheduled until it is {@linkplain #resume() resumed}.
    * 
    * <p>Pausing a task that is already paused has no effect. Subsequent calls will
    * return the same thing as the initial call.
    * 
    * <p>If a task definition needs to be paused with no invocation running, then the
    * caller can check {@link #current()} after pausing. If it is not {@code null} then
    * a task is executing and the caller will need to {@linkplain ScheduledTask#get() block
    * until it finishes}.
    * 
    * @return {@code true} if the task definition was paused; {@code false} if it could
    *       not be paused because it was already finished or cancelled
    *       
    * @see #resume()       
    */
   boolean pause();
   
   /**
    * Returns whether or not this task definition is currently paused.
    * 
    * @return {@code true} if the task definition is paused; {@code false} otherwise
    */
   boolean isPaused();
   
   /**
    * Resumes this task definition. If an invocation is still currently running then
    * subsequent invocations will be scheduled normally upon its completion (as if the
    * task definition were never paused). If no invocation is running, the next
    * invocation is scheduled. If the task definition were paused for less than a
    * repeating task's period/delay then subsequent invocations may be scheduled on
    * time, with no delay introduced from the short time it was paused. If it were
    * paused for more than the period/delay then the next invocation will be scheduled
    * to start immediately. This may cause an invocation in the regular schedule of a
    * fixed rate repeating task to be missed, just as if an invocation took too long
    * to execute.
    * 
    * <p>Resuming a task definition that is not paused has no effect.
    * 
    * @return {@code true} if the task definition was resumed; {@code false} if it could
    *       not be resumed because it was already finished or cancelled
    *       
    * @see #pause()
    */
   boolean resume();
}