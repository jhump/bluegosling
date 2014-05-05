/**
 * New APIs that provide significant improvements and increased functionality to the standard
 * {@code ScheduledExecutorService} API. The API here allows introspection on the status and
 * history of scheduled tasks and allows tasks to be configured with custom exception handling
 * strategies. It also provides more control over scheduling of subsequent instances for recurring
 * tasks, the ability to pause/resume recurring tasks, and cancel individual instances of recurring
 * tasks. See {@link com.apriori.concurrent.scheduler.ScheduledTaskManager ScheduledTaskManager} for
 * more.
 */
package com.apriori.concurrent.scheduler;