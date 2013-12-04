package com.apriori.concurrent;

/**
 * Represents an activity that can be cancelled. The activity may be in the future or may be a
 * concurrently executing activity.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface Cancellable {
   /**
    * Cancels the activity.
    *
    * @param mayInterrupt if true and the activity is executing concurrently, the thread executing
    *       the activity will be interrupted
    * @return true if the activity was cancelled; false if it could not be cancelled because the
    *       activity has already completed
    */
   boolean cancel(boolean mayInterrupt);
}
