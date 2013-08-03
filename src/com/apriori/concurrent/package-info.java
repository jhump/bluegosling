/**
 * Classes that extend and enhance the API provided by the {@code java.util.concurrent} package.
 * The two key extensions follow:
 * <ol>
 * <li>{@link ScheduledTaskManager}: Improvements over the {@code ScheduledExecutorService} API for
 * job tracking and management. The API here allows introspection on the status and history of
 * scheduled tasks and allows tasks to be configured with custom exception handling strategies. It
 * also provides more control over scheduling of subsequent instances for recurring tasks, the
 * ability to pause/resume recurring tasks, and cancel individual instances of recurring tasks.</li>
 * <li>{@link ListenableFuture}: {@code Future}s that allow completion callbacks to be registered.
 * This means code has the flexibility of either doing things asynchronously with callbacks or
 * synchronously by blocking for the future to complete.
 * </li>
 * </ol>
 *
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
package com.apriori.concurrent;