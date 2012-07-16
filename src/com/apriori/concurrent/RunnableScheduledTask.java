package com.apriori.concurrent;

/**
 * A convenient sub-interface, for reducing the number of type parameters needed
 * when declaring a {@link ScheduledTask} whose underlying task type if a {@link Runnable}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface RunnableScheduledTask extends ScheduledTask<Void, Runnable> {
}