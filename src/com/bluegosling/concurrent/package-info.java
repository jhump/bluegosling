/**
 * Classes that extend and enhance the API provided by the {@code java.util.concurrent} package. Of
 * particular note:
 * 
 * <h3>{@link com.bluegosling.concurrent.fluent.FluentFuture}</h3>
 * This interface is a {@code Future} that allows completion callbacks to be registered. This means
 * code has the flexibility of either doing things asynchronously with callbacks or synchronously
 * by blocking for the future to complete. It also provides numerous other new methods to greatly
 * improve the ease of use of futures. This interface is accompanied by several classes with useful
 * static methods to further expand the types of tasks you can perform using futures.
 * 
 * <h3>{@link com.bluegosling.concurrent.executors.SerializingExecutor}</h3>
 * An executor that maintains multiple queues, each identified by a "key". Tasks associated with the
 * same key run sequentially. Tasks associated different keys can run concurrently. There are two
 * implementations provided. {@linkplain com.bluegosling.concurrent.executors.PipeliningExecutor One} that wraps a
 * given executor and enforces the parallelism and sequencing independent of the underlying
 * execution mechanism, and {@linkplain com.bluegosling.concurrent.executors.ActorThreadPool another} that is a
 * novel thread pool implementation.
 * 
 * <h3>{@link com.bluegosling.concurrent.locks.HierarchicalLock}</h3>
 * A lock that is similar to a {@code ReadWriteLock} except that it also provides a lock hierarchy.
 * Acquiring a lock (in any mode) requires acquiring the lock's parent in shared (e.g. "read") mode.
 * This is similar to locking constructs in RDBMSs, where you have related locks for a database, a
 * table, and a record in a table. 
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: add references to other key APIs? Duration, NonReentrantLock, DoubleInstanceLock,
// ManagedBlockers, CompletableExecutorService, RateLimiter, ThreadLimitingExecutorService,
// AbstractQueuedReferenceSynchronizer?
package com.bluegosling.concurrent;
