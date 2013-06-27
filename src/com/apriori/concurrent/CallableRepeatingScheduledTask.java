package com.apriori.concurrent;

import java.util.concurrent.Callable;

/**
 * A convenient sub-interface, for reducing the number of type parameters needed
 * when declaring a {@link RepeatingScheduledTask} whose underlying task type is a {@link Callable}.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface CallableRepeatingScheduledTask<V> extends RepeatingScheduledTask<V, Callable<V>> {
}
