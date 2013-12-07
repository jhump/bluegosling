/**
 * Classes that extend and enhance the API provided by the {@code java.util.concurrent} package.
 * 
 * <h3>{@link com.apriori.concurrent.ScheduledTaskManager}</h3>
 * This class and its associated types provide an improvement over the
 * {@code ScheduledExecutorService} API for job tracking and management. The API here allows
 * introspection on the status and history of scheduled tasks and allows tasks to be configured
 * with custom exception handling strategies. It also provides more control over scheduling of
 * subsequent instances for recurring tasks, the ability to pause/resume recurring tasks, and cancel
 * individual instances of recurring tasks.
 * 
 * <h3>{@link com.apriori.concurrent.ListenableFuture}</h3>
 * This interface is a {@code Future} that allows completion callbacks to be registered. This means
 * code has the flexibility of either doing things asynchronously with callbacks or synchronously
 * by blocking for the future to complete. It also provides numerous other new methods to greatly
 * improve the ease of use of futures. This interface is accompanied by several classes with useful
 * static methods to further expand the types of tasks you can perform using futures.
 * 
 * <h3>{@link com.apriori.concurrent.PipeliningExecutorService}</h3>
 * An executor service that maintains multiple "pipelines" for sequential processing. Tasks
 * associated with the same pipeline run sequentially. Tasks for multiple pipelines run
 * concurrently.
 * 
 * <h3>{@link com.apriori.concurrent.HierarchicalLock}</h3>
 * A lock that is similar to a {@code ReadWriteLock} except that it also provides a lock hierarchy.
 * Acquiring a lock (in any mode) requires acquiring the lock's parent in shared (e.g. "read") mode.
 * This is similar to locking constructs in RDBMSs, where you have related locks for a database, a
 * table, and a record in a table. 
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.concurrent;