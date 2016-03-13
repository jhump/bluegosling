/**
 * Implementations and variations of {@link java.util.concurrent.Executor}s. This package contains
 * numerous decorators for executors and executor services, including ones that wrap all submitted
 * tasks, that can propagate context (for example thread-local state) from submitter thread to
 * worker thread, and that return {@link java.util.concurrent.CompletableFuture}s instead of plain
 * {@link java.util.concurrent.Future}s.
 * 
 * <p>Also in this package are implementations of a new
 * {@link com.bluegosling.concurrent.executors.SerializingExecutor} interface, which is an executor
 * that maintains multiple queues, each identified by a "key". Tasks associated with the same key
 * run sequentially. Tasks associated with different keys can run concurrently. There are two
 * implementations provided.
 * {@linkplain com.bluegosling.concurrent.executors.PipeliningExecutor One} that wraps a given
 * executor and enforces the parallelism and sequencing independent of the underlying execution
 * mechanism, and {@linkplain com.bluegosling.concurrent.executors.ActorThreadPool another} that is
 * a novel thread pool implementation.
 */
package com.bluegosling.concurrent.executors;