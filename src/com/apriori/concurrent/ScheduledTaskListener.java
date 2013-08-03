package com.apriori.concurrent;

/**
 * A listener for completions of {@link ScheduledTask}s.
 */
public interface ScheduledTaskListener<V, T> {
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
