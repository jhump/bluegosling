package com.apriori.concurrent;

import java.util.concurrent.ScheduledFuture;

// TODO: javadoc
public interface ListenableScheduledFuture<T> extends ListenableFuture<T>, ScheduledFuture<T> {
}
