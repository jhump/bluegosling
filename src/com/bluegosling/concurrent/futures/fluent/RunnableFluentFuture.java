package com.bluegosling.concurrent.futures.fluent;

import java.util.concurrent.RunnableFuture;

/**
 * A combination of {@link RunnableFuture} and {@link FluentFuture}.
 *
 * @param <T> the type of the future value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
public interface RunnableFluentFuture<T> extends RunnableFuture<T>, FluentFuture<T> {
}
