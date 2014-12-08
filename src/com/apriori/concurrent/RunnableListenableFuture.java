package com.apriori.concurrent;

import java.util.concurrent.RunnableFuture;

/**
 * A combination of {@link RunnableFuture} and {@link ListenableFuture}.
 *
 * @param <T> the type of the future value
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
interface RunnableListenableFuture<T> extends RunnableFuture<T>, ListenableFuture<T> {
}
