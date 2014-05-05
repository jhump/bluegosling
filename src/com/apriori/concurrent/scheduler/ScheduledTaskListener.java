package com.apriori.concurrent.scheduler;

/**
 * A listener for completions of {@link ScheduledTask}s.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of value returned by the task
 */
public interface ScheduledTaskListener<V> {
   /**
    * Called when the specified task completes (either normally or abnormally). Successful and
    * failed tasks can easily be distinguished using {@link ScheduledTask#isSuccessful()} and
    * {@link ScheduledTask#isFailed()}.
    * 
    * @param task the completed task
    */
   void taskCompleted(ScheduledTask<? extends V> task);
}
