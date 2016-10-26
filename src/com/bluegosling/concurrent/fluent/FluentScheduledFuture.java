package com.bluegosling.concurrent.fluent;

import java.util.concurrent.ScheduledFuture;

import com.google.common.util.concurrent.ListenableScheduledFuture;

/**
 * A future that is both {@linkplain ScheduledFuture scheduled} and {@linkplain FluentFuture
 * fluent}.
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 *
 * @param <T> the type of the future result
 */
public interface FluentScheduledFuture<T> extends FluentFuture<T>, ListenableScheduledFuture<T> {
}
