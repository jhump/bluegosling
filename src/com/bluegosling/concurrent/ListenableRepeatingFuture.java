package com.bluegosling.concurrent;

import java.util.concurrent.Executor;

/**
 * A {@link ListenableScheduledFuture} that represents a repeating task. The future is not completed
 * until after the last occurrence of the repeating task finishes. Typically, the last occurrence is
 * the first failed occurrence or a scheduled (or running) occurrence at the time the task is
 * cancelled.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the future result, usually {@code Void}
 */
public interface ListenableRepeatingFuture<T> extends ListenableScheduledFuture<T> {
   /**
    * Returns the result returned by the most recent completed occurrence. This can only be called
    * after there has been at least one successfully completed occurrence.
    * 
    * @return the result of most recent successfully completed occurrence
    * @throws IllegalStateException if there have been no successfully completed occurrences of this
    *       task
    */
   T getMostRecentResult();
   
   /**
    * Adds a listener that is invoked after each occurrence of the task. This listener is invoked
    * on completion of each occurrence that runs <em>after</em> the listener is registered. If the
    * future is already done and no more occurrences will run, the listener will be invoked
    * immediately.
    * 
    * <p>If the specified executor runs the task asynchronously then it is possible that the next
    * scheduled occurrence of this task will start (and possibly finish) before or while the
    * listener is executing. If the listener requires consistency and must avoid this possible race,
    * use a {@link SameThreadExecutor}.
    * 
    * @param listener
    * @param executor
    */
   void addListenerForEachInstance(FutureListener<? super T> listener, Executor executor);
   
   /**
    * Returns the number of occurrences of this task that have completed. This excludes any possibly
    * running occurrence, so this value will not move from zero to one until after an occurrence has
    * completed.
    * 
    * @return the number of completed occurrences of this task
    */
   int executionCount();
}
