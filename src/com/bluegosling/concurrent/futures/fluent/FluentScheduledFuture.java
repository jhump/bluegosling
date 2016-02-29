package com.bluegosling.concurrent.futures.fluent;

import java.util.concurrent.ScheduledFuture;

/**
 * A future that is both {@linkplain ScheduledFuture scheduled} and {@linkplain FluentFuture
 * fluent}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the future result
 */
public interface FluentScheduledFuture<T> extends FluentFuture<T>, ScheduledFuture<T> {
}
