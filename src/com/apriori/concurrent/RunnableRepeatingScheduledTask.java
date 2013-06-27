package com.apriori.concurrent;


/**
 * A convenient sub-interface, for reducing the number of type parameters needed
 * when declaring a {@link ScheduledTask} whose underlying task type is a {@link Runnable}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface RunnableRepeatingScheduledTask extends RepeatingScheduledTask<Void, Runnable> {
}
