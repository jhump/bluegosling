package com.apriori.concurrent;

import java.util.concurrent.Callable;

/**
 * A convenient sub-interface, for reducing the number of type parameters needed
 * when declaring a {@link ScheduledTask} whose underlying task type if a {@link Callable}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface CallableScheduledTask<V> extends ScheduledTask<V, Callable<V>> {
}