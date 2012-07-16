package com.apriori.concurrent;

/**
 * A {@link Runnable} combined with a specified result value. This interface
 * represents the pair of parameters as used by
 * {@link java.util.concurrent.ExecutorService#submit(Runnable, Object) ExecutorService.submit(Runnable, V)}
 * and {@link java.util.concurrent.Executors#callable(Runnable, Object) Executors.callable(Runnable, V)}.
 * 
 * <p>None of the APIs in this package actually use this interface. It is just used
 * for type-safety to parameterize {@link TaskDefinition}s.
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <V> the type of the result
 * 
 * @see TaskDefinition.Builder#forRunnable(Runnable, Object)
 */
public interface RunnableWithResult<V> {
   /** Returns the {@link Runnable} for this pair. */
   Runnable getRunnable();
   /** Returns the result value for this pair. */
   V getResult();
}
