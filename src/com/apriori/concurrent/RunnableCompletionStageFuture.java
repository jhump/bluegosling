package com.apriori.concurrent;

import java.util.concurrent.RunnableFuture;

/**
 * A combination of {@link RunnableFuture} and {@link CompletionStageFuture}.
 *
 * @param <T> the type of the future value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface RunnableCompletionStageFuture<T>
      extends RunnableFuture<T>, CompletionStageFuture<T> {
}
