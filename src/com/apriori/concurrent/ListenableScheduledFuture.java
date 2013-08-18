package com.apriori.concurrent;

import java.util.concurrent.ScheduledFuture;

/**
 * A future that is both {@linkplain ScheduledFuture scheduled} and {@linkplain ListenableFuture
 * listenable}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the future result
 */
public interface ListenableScheduledFuture<T> extends ListenableFuture<T>, ScheduledFuture<T> {
}
