package com.apriori.concurrent;

import java.util.concurrent.RunnableFuture;

// TODO: doc
interface RunnableListenableFuture<T> extends RunnableFuture<T>, ListenableFuture<T> {
}
