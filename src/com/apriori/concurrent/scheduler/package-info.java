/**
 * A {@link com.apriori.concurrent.scheduler.ScheduledTaskManager ScheduledTaskManager} API, which
 * provides lots of scheduled task and job management functionality. The API is based on the
 * standard {@code ScheduledExecutorService} but with significant improvements and increased
 * functionality. The API here allows introspection on the status and history of scheduled tasks and
 * allows tasks to be configured with custom exception handling strategies. It also provides more
 * control over scheduling of subsequent instances for recurring tasks, the ability to pause/resume
 * recurring tasks, and cancel individual instances of recurring tasks.
 */
package com.apriori.concurrent.scheduler;